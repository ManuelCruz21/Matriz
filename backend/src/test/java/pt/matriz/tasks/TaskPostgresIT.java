package pt.matriz.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real API/security/JDBC integration; never uses the application's configured database. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class TaskPostgresIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11-bookworm")
        .withDatabaseName("matriz_ficha04_test")
        .withUsername("matriz_test")
        .withPassword(UUID.randomUUID().toString());

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        // Bind Flyway explicitly too: external Flyway credentials must not select another database.
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("spring.flyway.enabled", () -> true);
        properties.add("spring.flyway.clean-disabled", () -> true);
        properties.add("spring.flyway.locations", () -> "classpath:db/migration");
        properties.add("matriz.demo-seed.enabled", () -> false);
    }

    private static final String PASSWORD = "IntegrationOnlyPassword123!";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    private long managerId;
    private long memberId;
    private long sharedProject;
    private long privateProject;
    private String managerEmail;
    private String memberEmail;

    private record Login(MockHttpSession session, String csrf) {}

    @BeforeEach
    void fixtures() {
        // Every invocation owns its rows; no truncation, sequence reset or shared demo seed.
        String suffix = UUID.randomUUID().toString();
        managerEmail = "manager-" + suffix + "@example.org";
        memberEmail = "member-" + suffix + "@example.org";
        String hash = passwords.encode(PASSWORD);
        managerId = jdbc.queryForObject("INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?) RETURNING id",
            Long.class, "Integration Manager", managerEmail, hash);
        memberId = jdbc.queryForObject("INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?) RETURNING id",
            Long.class, "Integration Member", memberEmail, hash);
        sharedProject = project("Shared integration project");
        privateProject = project("Private integration project");
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'membro')",
            sharedProject, memberId);
    }

    private long project(String name) {
        long id = jdbc.queryForObject("INSERT INTO projects(name,created_by_user_id) VALUES (?,?) RETURNING id",
            Long.class, name, managerId);
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'gestor')", id, managerId);
        return id;
    }

    private Login login(String email) throws Exception {
        MvcResult bootstrap = mvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) bootstrap.getRequest().getSession();
        String token = json.readTree(bootstrap.getResponse().getContentAsString()).get("token").asText();
        MvcResult authenticated = mvc.perform(post("/api/v1/auth/login").session(session)
                .header("X-CSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", email, "password", PASSWORD))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email)).andReturn();
        session = (MockHttpSession) authenticated.getRequest().getSession();
        MvcResult refreshed = mvc.perform(get("/api/v1/auth/csrf").session(session))
            .andExpect(status().isOk()).andReturn();
        return new Login(session, json.readTree(refreshed.getResponse().getContentAsString()).get("token").asText());
    }

    private MockHttpServletRequestBuilder mutation(MockHttpServletRequestBuilder request, Login login, String body) {
        return request.session(login.session()).header("X-CSRF-TOKEN", login.csrf())
            .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private JsonNode create(Login login, long project, String body) throws Exception {
        MvcResult result = mvc.perform(mutation(post("/api/v1/projects/{id}/tasks", project), login, body))
            .andExpect(status().isCreated()).andReturn();
        JsonNode task = json.readTree(result.getResponse().getContentAsString());
        assertTrue(task.get("id").asLong() > 0);
        assertEquals("/api/v1/tasks/" + task.get("id").asLong(), result.getResponse().getHeader("Location"));
        return task;
    }

    /** A fresh connection, independent of Spring's request transaction, sees only committed writes. */
    private JsonNode committedTask(long id) throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.prepareStatement(
                 "SELECT row_to_json(t)::text FROM tasks t WHERE id=?")) {
            statement.setLong(1, id);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next(), "Task must be committed and visible on a separate connection");
                JsonNode task = json.readTree(rows.getString(1));
                assertFalse(rows.next());
                return task;
            }
        }
    }

    private List<Long> assignments(long task) {
        return jdbc.query("SELECT user_id FROM task_assignments WHERE task_id=? ORDER BY user_id",
            (row, n) -> row.getLong(1), task);
    }

    private void assertNoTasks(long project) {
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE project_id=?", Long.class, project));
        assertEquals(0L, jdbc.queryForObject(
            "SELECT COUNT(*) FROM task_assignments a JOIN tasks t ON t.id=a.task_id WHERE t.project_id=?",
            Long.class, project));
    }

    @Test
    void managerCreatesRetrievesAndCommitsTaskWithEligibleAssignment() throws Exception {
        Login manager = login(managerEmail);
        Instant before = Instant.now();
        JsonNode created = create(manager, sharedProject, json.writeValueAsString(Map.of(
            "title", "  Persisted task  ", "description", "  Real PostgreSQL  ",
            "responsibleUserIds", List.of(memberId))));
        long id = created.get("id").asLong();
        assertEquals(sharedProject, created.get("projectId").asLong());
        assertEquals("Persisted task", created.get("title").asText());
        assertEquals("Real PostgreSQL", created.get("description").asText());
        assertEquals("TODO", created.get("status").asText());
        assertEquals("MEDIUM", created.get("priority").asText());
        assertEquals(1, created.get("responsibleUserIds").size());
        assertEquals(memberId, created.get("responsibleUserIds").get(0).asLong());
        Instant timestamp = Instant.parse(created.get("createdAt").asText());
        assertFalse(timestamp.isBefore(before));
        assertFalse(timestamp.isAfter(Instant.now()));
        MvcResult retrieved = mvc.perform(get("/api/v1/tasks/{id}", id).session(manager.session()))
            .andExpect(status().isOk()).andReturn();
        assertEquals(created, json.readTree(retrieved.getResponse().getContentAsString()));
        JsonNode persisted = committedTask(id);
        assertEquals(id, persisted.get("id").asLong());
        assertEquals(sharedProject, persisted.get("project_id").asLong());
        assertEquals("Persisted task", persisted.get("title").asText());
        assertEquals("Real PostgreSQL", persisted.get("description").asText());
        assertEquals("TODO", persisted.get("status").asText());
        assertEquals("MEDIUM", persisted.get("priority").asText());
        assertEquals(timestamp, Instant.parse(persisted.get("created_at").asText()));
        assertEquals(List.of(memberId), assignments(id));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE project_id=?", Long.class, sharedProject));
        assertEquals(2L, jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success AND version IN ('1','2')", Long.class));
    }

    @Test
    void memberCreatesCommentWithAuthorTimestampAndCommittedPersistence() throws Exception {
        Login manager = login(managerEmail);
        long taskId = create(manager, sharedProject, "{\"title\":\"Comment integration task\"}").get("id").asLong();
        Login member = login(memberEmail);
        Instant before = Instant.now();
        MvcResult result = mvc.perform(mutation(post("/api/v1/tasks/{id}/comments", taskId), member,
                "{\"content\":\"  Persisted member comment  \"}"))
            .andExpect(status().isCreated()).andReturn();
        Instant after = Instant.now();
        JsonNode comment = json.readTree(result.getResponse().getContentAsString());
        long commentId = comment.get("id").asLong();
        assertTrue(commentId > 0);
        assertEquals("/api/v1/tasks/" + taskId + "/comments/" + commentId,
            result.getResponse().getHeader("Location"));
        assertEquals(taskId, comment.get("taskId").asLong());
        assertEquals(memberId, comment.get("authorId").asLong());
        assertEquals("Persisted member comment", comment.get("content").asText());
        assertTrue(comment.get("createdAt").asText().endsWith("Z"));
        Instant timestamp = Instant.parse(comment.get("createdAt").asText());
        assertFalse(timestamp.isBefore(before));
        assertFalse(timestamp.isAfter(after));

        // No enclosing test transaction: a separate connection proves the request committed.
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.prepareStatement(
                 "SELECT task_id,author_id,content,created_at FROM task_comments WHERE id=?")) {
            statement.setLong(1, commentId);
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next(), "Comment must be committed on a separate connection");
                assertEquals(taskId, rows.getLong("task_id"));
                assertEquals(memberId, rows.getLong("author_id"));
                assertEquals("Persisted member comment", rows.getString("content"));
                assertEquals(timestamp, rows.getTimestamp("created_at").toInstant());
                assertFalse(rows.next());
            }
        }
        MvcResult listed = mvc.perform(get("/api/v1/tasks/{id}/comments", taskId).session(manager.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1)).andReturn();
        JsonNode items = json.readTree(listed.getResponse().getContentAsString()).get("items");
        assertEquals(1, items.size());
        assertEquals(comment, items.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"title\":\"Member task\"}",
        "{\"title\":\"Member task\",\"description\":null,\"responsibleUserIds\":[]}"})
    void memberCreatesWithoutAssignments(String body) throws Exception {
        Login member = login(memberEmail);
        JsonNode created = create(member, sharedProject, body);
        long id = created.get("id").asLong();
        assertTrue(created.get("description").isNull());
        assertEquals(json.createArrayNode(), created.get("responsibleUserIds"));
        mvc.perform(get("/api/v1/tasks/{id}", id).session(member.session()))
            .andExpect(status().isOk()).andExpect(content().json(created.toString(), true));
        assertTrue(committedTask(id).get("description").isNull());
        assertEquals(List.of(), assignments(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "blank", "short", "long"})
    void invalidTitlesAreRejectedWithoutWrites(String scenario) throws Exception {
        Login manager = login(managerEmail);
        String body = switch (scenario) {
            case "missing" -> "{}";
            case "blank" -> "{\"title\":\"   \"}";
            case "short" -> "{\"title\":\"ab\"}";
            default -> json.writeValueAsString(Map.of("title", "a".repeat(151)));
        };
        mvc.perform(mutation(post("/api/v1/projects/{id}/tasks", sharedProject), manager, body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors[0].field").value("title"));
        assertNoTasks(sharedProject);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 150})
    void trimmedTitleBoundariesAreAccepted(int length) throws Exception {
        JsonNode created = create(login(managerEmail), sharedProject,
            json.writeValueAsString(Map.of("title", "  " + "a".repeat(length) + "  ")));
        assertEquals("a".repeat(length), created.get("title").asText());
        assertEquals("a".repeat(length), committedTask(created.get("id").asLong()).get("title").asText());
    }

    @Test
    void unsupportedCreationFieldIsBadRequestWithoutWrites() throws Exception {
        mvc.perform(mutation(post("/api/v1/projects/{id}/tasks", sharedProject), login(managerEmail),
                "{\"title\":\"Valid title\",\"status\":\"DONE\"}"))
            .andExpect(status().isBadRequest());
        assertNoTasks(sharedProject);
    }

    @Test
    void nonexistentTaskReturnsNotFound() throws Exception {
        // Generated IDs are positive; zero is absent in this disposable database.
        mvc.perform(get("/api/v1/tasks/0").session(login(managerEmail).session()))
            .andExpect(status().isNotFound());
        assertNoTasks(sharedProject);
    }

    @Test
    void memberCannotCreateReadOrModifyPrivateProjectTask() throws Exception {
        JsonNode created = create(login(managerEmail), privateProject, "{\"title\":\"Private task\"}");
        long id = created.get("id").asLong();
        JsonNode before = committedTask(id);
        Login member = login(memberEmail);
        mvc.perform(mutation(post("/api/v1/projects/{id}/tasks", privateProject), member,
                "{\"title\":\"Forbidden creation\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks/{id}", id).session(member.session()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.projectId").doesNotExist());
        mvc.perform(mutation(patch("/api/v1/tasks/{id}", id), member, "{\"title\":\"Forbidden edit\"}"))
            .andExpect(status().isNotFound());
        assertEquals(before, committedTask(id));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE project_id=?", Long.class, privateProject));
        assertEquals(List.of(), assignments(id));
    }

    @Test
    void memberCannotSelfAssignDuringCreation() throws Exception {
        mvc.perform(mutation(post("/api/v1/projects/{id}/tasks", sharedProject), login(memberEmail),
                json.writeValueAsString(Map.of("title", "Forbidden assignment", "responsibleUserIds", List.of(memberId)))))
            .andExpect(status().isForbidden());
        assertNoTasks(sharedProject);
    }

    @Test
    void concurrentTitleOnlyAndDescriptionOnlyPatchesPreserveBothChanges() throws Exception {
        Login titleEditor = login(managerEmail);
        Login descriptionEditor = login(managerEmail);
        assertNotSame(titleEditor.session(), descriptionEditor.session());
        long id = create(titleEditor, sharedProject,
            "{\"title\":\"Original title\",\"description\":\"Original description\"}").get("id").asLong();
        var executor = Executors.newFixedThreadPool(2);
        try {
            try (var lock = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                 var observer = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
                lock.setAutoCommit(false);
                int blockerPid;
                try (var statement = lock.prepareStatement("SELECT pg_backend_pid() FROM tasks WHERE id=? FOR UPDATE")) {
                    statement.setLong(1, id);
                    try (var rows = statement.executeQuery()) {
                        assertTrue(rows.next());
                        blockerPid = rows.getInt(1);
                    }
                }

                // Follow the blocking chain: the second UPDATE may queue behind the first,
                // rather than directly behind our transaction. Only count our own lock's waiters.
                try (var waiting = observer.prepareStatement("""
                        WITH RECURSIVE blocked(pid) AS (
                            SELECT CAST(? AS integer)
                            UNION
                            SELECT a.pid FROM pg_stat_activity a
                            JOIN blocked b ON b.pid = ANY(pg_blocking_pids(a.pid))
                        )
                        SELECT COUNT(*) FROM pg_stat_activity a
                        JOIN blocked b ON b.pid = a.pid
                        WHERE a.datname = current_database() AND a.wait_event_type = 'Lock'
                          AND a.query ~* '^\\s*UPDATE\\s+tasks\\s'
                        """)) {
                    waiting.setInt(1, blockerPid);
                    waiting.setQueryTimeout(1);
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                    var titlePatch = executor.submit(() -> mvc.perform(mutation(patch("/api/v1/tasks/{id}", id),
                            titleEditor, "{\"title\":\"Concurrent title\"}"))
                        .andExpect(status().isOk()).andReturn());
                    var descriptionPatch = executor.submit(() -> mvc.perform(mutation(patch("/api/v1/tasks/{id}", id),
                            descriptionEditor, "{\"description\":\"Concurrent description\"}"))
                        .andExpect(status().isOk()).andReturn());
                    int waiters = 0;
                    while (System.nanoTime() < deadline) {
                        try (var rows = waiting.executeQuery()) {
                            assertTrue(rows.next());
                            waiters = rows.getInt(1);
                        }
                        if (waiters == 2) break;
                        TimeUnit.MILLISECONDS.sleep(10);
                    }
                    // Release before asserting or waiting on futures: application JDBC timeout is 3s.
                    lock.commit();
                    assertEquals(2, waiters, "Both PATCH requests must overlap while waiting at UPDATE");
                    titlePatch.get(5, TimeUnit.SECONDS);
                    descriptionPatch.get(5, TimeUnit.SECONDS);
                }
            }
            JsonNode persisted = committedTask(id);
            assertEquals("Concurrent title", persisted.get("title").asText());
            assertEquals("Concurrent description", persisted.get("description").asText());
            mvc.perform(get("/api/v1/tasks/{id}", id).session(titleEditor.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Concurrent title"))
                .andExpect(jsonPath("$.description").value("Concurrent description"));
        } finally {
            // Connections close (releasing the lock even on failure) before workers are stopped.
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "PATCH workers must terminate");
        }
    }

    @Test
    void unassignedMemberCanReadButCannotMutateSharedTask() throws Exception {
        JsonNode created = create(login(managerEmail), sharedProject, "{\"title\":\"Shared task\"}");
        long id = created.get("id").asLong();
        JsonNode before = committedTask(id);
        Login member = login(memberEmail);
        mvc.perform(get("/api/v1/tasks/{id}", id).session(member.session()))
            .andExpect(status().isOk()).andExpect(content().json(created.toString(), true));
        mvc.perform(mutation(patch("/api/v1/tasks/{id}", id), member, "{\"title\":\"Forbidden edit\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(mutation(patch("/api/v1/tasks/{id}/status", id), member, "{\"status\":\"DONE\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(mutation(put("/api/v1/tasks/{id}/responsibles", id), member,
                json.writeValueAsString(Map.of("responsibleUserIds", List.of(memberId)))))
            .andExpect(status().isForbidden());
        assertEquals(before, committedTask(id));
        assertEquals(List.of(), assignments(id));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM task_history WHERE task_id=?", Long.class, id));
    }

    /** Snapshot only this invocation's projects and dependents on an independent connection. */
    private Map<String, JsonNode> committedProjectState() throws Exception {
        return committedProjectState(sharedProject, privateProject);
    }

    private Map<String, JsonNode> committedProjectState(long firstProject, long secondProject) throws Exception {
        Map<String, String> scopes = Map.of(
            "projects", "id IN (?,?)",
            "project_memberships", "project_id IN (?,?)",
            "tasks", "project_id IN (?,?)",
            "task_assignments", "task_id IN (SELECT id FROM tasks WHERE project_id IN (?,?))",
            "task_comments", "task_id IN (SELECT id FROM tasks WHERE project_id IN (?,?))",
            "task_history", "task_id IN (SELECT id FROM tasks WHERE project_id IN (?,?))");
        Map<String, JsonNode> snapshot = new java.util.HashMap<>();
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            for (var scope : scopes.entrySet()) {
                // Table names/predicates are fixed test constants, never request input.
                try (var statement = connection.prepareStatement(
                        "SELECT COALESCE(json_agg(row_to_json(t) ORDER BY row_to_json(t)::text), '[]'::json)::text "
                        + "FROM " + scope.getKey() + " t WHERE " + scope.getValue())) {
                    statement.setLong(1, firstProject);
                    statement.setLong(2, secondProject);
                    try (var rows = statement.executeQuery()) {
                        assertTrue(rows.next());
                        snapshot.put(scope.getKey(), json.readTree(rows.getString(1)));
                    }
                }
            }
        }
        return snapshot;
    }

    @ParameterizedTest(name = "cross-project {0} {1}")
    @CsvSource(value = {
        "GET|/projects/{id}|project|{}",
        "PATCH|/projects/{id}|project|{\"name\":\"Authorized rename\"}",
        "DELETE|/projects/{id}|project|{}",
        "GET|/projects/{id}/eligible-users|project|{}",
        "GET|/projects/{id}/tasks|project|{}",
        "DELETE|/tasks/{id}|task|{}",
        "PATCH|/tasks/{id}/status|task|{\"status\":\"DONE\"}",
        "PATCH|/tasks/{id}/priority|task|{\"priority\":\"HIGH\"}",
        "PUT|/tasks/{id}/responsibles|task|{\"responsibleUserIds\":[]}",
        "GET|/tasks/{id}/comments|task|{}",
        "POST|/tasks/{id}/comments|task|{\"content\":\"Authorized comment\"}",
        "GET|/tasks/{id}/history|task|{}"
    }, delimiter = '|')
    void crossProjectIdentifierSwappingIsHiddenAndCannotChangeCommittedState(
            String method, String path, String resource, String body) throws Exception {
        Login manager = login(managerEmail);
        long taskId = create(manager, privateProject, json.writeValueAsString(Map.of(
            "title", "Private authorization task", "responsibleUserIds", List.of(managerId)))).get("id").asLong();
        // Nonempty dependents make destructive endpoint checks meaningful.
        mvc.perform(mutation(post("/api/v1/tasks/{id}/comments", taskId), manager,
                "{\"content\":\"Private sentinel comment\"}"))
            .andExpect(status().isCreated());
        mvc.perform(mutation(patch("/api/v1/tasks/{id}/status", taskId), manager,
                "{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isOk());
        create(manager, sharedProject, "{\"title\":\"Unrelated shared sentinel\"}");
        Login member = login(memberEmail);
        mvc.perform(get("/api/v1/projects/{id}", sharedProject).session(member.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(sharedProject));
        mvc.perform(get("/api/v1/tasks/{id}", taskId).session(manager.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(taskId));

        Map<String, JsonNode> before = committedProjectState();
        long target = resource.equals("project") ? privateProject : taskId;
        var httpMethod = org.springframework.http.HttpMethod.valueOf(method);
        var forbidden = request(httpMethod, "/api/v1" + path, target);
        if (method.equals("GET")) forbidden.session(member.session());
        else mutation(forbidden, member, body); // Valid CSRF/payload isolates authorization.
        mvc.perform(forbidden).andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Not Found"))
            .andExpect(jsonPath("$.items").doesNotExist())
            .andExpect(jsonPath("$.projectId").doesNotExist())
            .andExpect(jsonPath("$.content").doesNotExist());
        assertEquals(before, committedProjectState(), "Rejected identifier swap must preserve all owned rows");

        // The identical endpoint/payload succeeds for the actual project manager.
        var allowed = request(httpMethod, "/api/v1" + path, target);
        if (method.equals("GET")) allowed.session(manager.session());
        else mutation(allowed, manager, body);
        int expected = method.equals("DELETE") ? 204 : method.equals("POST") ? 201 : 200;
        mvc.perform(allowed).andExpect(status().is(expected));
    }

    @Test
    void realInvalidPasswordDoesNotAuthenticateOrChangeAccount() throws Exception {
        String hashBefore = jdbc.queryForObject("SELECT password_hash FROM app_users WHERE id=?", String.class, memberId);
        Map<String, JsonNode> before = committedProjectState();
        MvcResult bootstrap = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) bootstrap.getRequest().getSession();
        String token = json.readTree(bootstrap.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/v1/auth/login").session(session).header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", memberEmail, "password", PASSWORD + "wrong"))))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/projects/{id}", sharedProject).session(session))
            .andExpect(status().isUnauthorized());
        assertEquals(hashBefore, jdbc.queryForObject("SELECT password_hash FROM app_users WHERE id=?", String.class, memberId));
        assertEquals(before, committedProjectState());
        Login valid = login(memberEmail);
        mvc.perform(get("/api/v1/users/me").session(valid.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(memberId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"omitted", "null", "text"})
    void authenticatedProjectCreationCommitsCreatorMembershipAndListEntry(String descriptionMode) throws Exception {
        // A user who is only a member elsewhere must become manager of their new project.
        Login member = login(memberEmail);
        var body = json.createObjectNode().put("name", "  API project  ");
        if (descriptionMode.equals("null")) body.putNull("description");
        if (descriptionMode.equals("text")) body.put("description", "  Persisted description  ");
        MvcResult result = mvc.perform(mutation(post("/api/v1/projects"), member, body.toString()))
            .andExpect(status().isCreated()).andReturn();
        JsonNode created = json.readTree(result.getResponse().getContentAsString());
        long id = created.get("id").asLong();
        Map<String, JsonNode> committed = committedProjectState(id, id);
        assertEquals(1, committed.get("projects").size());
        JsonNode row = committed.get("projects").get(0);
        assertEquals("API project", row.get("name").asText());
        assertEquals(memberId, row.get("created_by_user_id").asLong());
        JsonNode expectedDescription = descriptionMode.equals("text")
            ? json.getNodeFactory().textNode("Persisted description") : json.getNodeFactory().nullNode();
        assertEquals(expectedDescription, row.get("description"));
        assertEquals(expectedDescription, created.get("description"));
        assertEquals(memberId, created.get("createdByUserId").asLong());
        JsonNode memberships = committed.get("project_memberships");
        assertEquals(1, memberships.size(), "New project must contain only the creator membership");
        assertEquals(memberId, memberships.get(0).get("user_id").asLong());
        assertEquals("gestor", memberships.get(0).get("role").asText());
        mvc.perform(get("/api/v1/projects/{id}", id).session(member.session()))
            .andExpect(status().isOk()).andExpect(content().json(created.toString(), true));
        MvcResult listed = mvc.perform(get("/api/v1/projects").session(member.session()))
            .andExpect(status().isOk()).andReturn();
        JsonNode list = json.readTree(listed.getResponse().getContentAsString());
        assertEquals(2, list.get("totalItems").asInt()); // Shared project + newly created project.
        assertEquals(created, list.get("items").get(0));
        assertEquals(sharedProject, list.get("items").get(1).get("id").asLong());
        assertEquals(committed, committedProjectState(id, id));
    }

    @Test
    void projectSearchMatchesCaseAndLiteralWildcardsInPostgres() throws Exception {
        Login manager = login(managerEmail);
        Login member = login(memberEmail);
        String prefix = "Search-" + UUID.randomUUID() + "-";
        List<String> names = List.of("Alpha", "aLPHa extra", "Café", "Cafe", "rate%", "rateX", "under_score", "underXscore", "bang!", "bangX");
        Map<String, Long> ids = new java.util.HashMap<>();
        for (String name : names) ids.put(name, project(prefix + name));
        Map<String, List<Long>> cases = Map.ofEntries(
            Map.entry("  " + prefix.toUpperCase(java.util.Locale.ROOT) + "ALpH  ", List.of(ids.get("aLPHa extra"), ids.get("Alpha"))),
            Map.entry(prefix + "Café", List.of(ids.get("Café"))),
            Map.entry(prefix + "Cafe", List.of(ids.get("Cafe"))),
            Map.entry("%", List.of(ids.get("rate%"))),
            Map.entry("_", List.of(ids.get("under_score"))),
            Map.entry("!", List.of(ids.get("bang!"))),
            Map.entry(prefix + "absent", List.of()));
        for (var search : cases.entrySet()) {
            MvcResult result = mvc.perform(get("/api/v1/projects").session(manager.session()).param("search", search.getKey()))
                .andExpect(status().isOk()).andReturn();
            JsonNode response = json.readTree(result.getResponse().getContentAsString());
            List<Long> actual = new java.util.ArrayList<>();
            response.get("items").forEach(item -> actual.add(item.get("id").asLong()));
            assertEquals(search.getValue(), actual, "Search must match literal substring: " + search.getKey());
            assertEquals(actual.size(), response.get("totalItems").asInt());
        }
        List<Long> expectedAll = new java.util.ArrayList<>(ids.values());
        expectedAll.add(sharedProject);
        expectedAll.add(privateProject);
        expectedAll.sort(java.util.Comparator.reverseOrder());
        for (String search : List.of("", "   ")) {
            MvcResult result = mvc.perform(get("/api/v1/projects").session(manager.session()).param("search", search))
                .andExpect(status().isOk()).andReturn();
            JsonNode response = json.readTree(result.getResponse().getContentAsString());
            List<Long> actual = new java.util.ArrayList<>();
            response.get("items").forEach(item -> actual.add(item.get("id").asLong()));
            assertEquals(expectedAll, actual);
            assertEquals(expectedAll.size(), response.get("totalItems").asInt());
        }
        mvc.perform(get("/api/v1/projects").session(member.session()).param("search", prefix))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void projectEditingAndDeletionCommitCleanupWithoutChangingUnrelatedProject() throws Exception {
        Login manager = login(managerEmail);
        long targetTask = create(manager, sharedProject, json.writeValueAsString(Map.of(
            "title", "Project deletion target", "responsibleUserIds", List.of(memberId)))).get("id").asLong();
        long sentinelTask = create(manager, privateProject, json.writeValueAsString(Map.of(
            "title", "Unrelated project sentinel", "responsibleUserIds", List.of(managerId)))).get("id").asLong();
        for (long taskId : List.of(targetTask, sentinelTask)) {
            mvc.perform(mutation(post("/api/v1/tasks/{id}/comments", taskId), manager, "{\"content\":\"Keep with owner\"}"))
                .andExpect(status().isCreated());
            mvc.perform(mutation(patch("/api/v1/tasks/{id}/status", taskId), manager, "{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        }
        Map<String, JsonNode> unrelated = committedProjectState(privateProject, privateProject);
        Map<String, JsonNode> original = committedProjectState(sharedProject, sharedProject);
        for (String table : List.of("projects", "tasks", "task_assignments", "task_comments", "task_history"))
            assertEquals(1, original.get(table).size(), "Cleanup precondition: " + table);
        assertEquals(2, original.get("project_memberships").size());

        MvcResult edited = mvc.perform(mutation(patch("/api/v1/projects/{id}", sharedProject), manager,
                "{\"name\":\"  Committed rename  \",\"description\":\"  Committed details  \"}"))
            .andExpect(status().isOk()).andReturn();
        JsonNode updated = json.readTree(edited.getResponse().getContentAsString());
        mvc.perform(get("/api/v1/projects/{id}", sharedProject).session(manager.session()))
            .andExpect(status().isOk()).andExpect(content().json(updated.toString(), true));
        Map<String, JsonNode> persisted = committedProjectState(sharedProject, sharedProject);
        var expectedProject = original.get("projects").get(0).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) expectedProject).put("name", "Committed rename").put("description", "Committed details");
        assertEquals(expectedProject, persisted.get("projects").get(0));
        for (String table : List.of("project_memberships", "tasks", "task_assignments", "task_comments", "task_history"))
            assertEquals(original.get(table), persisted.get(table), "Project edit must not alter dependents: " + table);
        mvc.perform(get("/api/v1/projects").session(manager.session()).param("search", "Committed rename"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(sharedProject));
        assertEquals(unrelated, committedProjectState(privateProject, privateProject));

        mvc.perform(mutation(delete("/api/v1/projects/{id}", sharedProject), manager, "{}"))
            .andExpect(status().isNoContent());
        for (var table : committedProjectState(sharedProject, sharedProject).entrySet())
            assertTrue(table.getValue().isEmpty(), "Deleted project rows must be gone: " + table.getKey());
        // Query captured task IDs directly: a project-scoped join alone could conceal orphan rows.
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            for (String table : List.of("task_assignments", "task_comments", "task_history")) {
                try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE task_id=?")) {
                    statement.setLong(1, targetTask);
                    try (var rows = statement.executeQuery()) {
                        assertTrue(rows.next());
                        assertEquals(0L, rows.getLong(1), "No orphan rows in " + table);
                    }
                }
            }
        }
        assertEquals(unrelated, committedProjectState(privateProject, privateProject));
        mvc.perform(get("/api/v1/projects/{id}", sharedProject).session(manager.session())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/tasks/{id}", targetTask).session(manager.session())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/projects").session(manager.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].id").value(privateProject));
    }

    private record ExpectedTask(long id, long projectId, String status, String priority) {}

    private JsonNode expectedCounts(List<ExpectedTask> tasks, String priority) {
        var counts = json.createObjectNode();
        int total = 0;
        for (String status : List.of("TODO", "IN_PROGRESS", "DONE")) {
            int count = (int) tasks.stream().filter(t -> t.status().equals(status)
                && (priority == null || t.priority().equals(priority))).count();
            counts.put(status, count);
            total += count;
        }
        return counts.put("total", total);
    }

    private void assertTaskFiltersAndCounters(Login reader, List<ExpectedTask> tasks,
            List<Long> accessibleProjects) throws Exception {
        List<ExpectedTask> shared = tasks.stream().filter(t -> t.projectId() == sharedProject).toList();
        for (String priority : java.util.Arrays.asList(null, "LOW", "MEDIUM", "HIGH")) {
            for (String status : java.util.Arrays.asList(null, "TODO", "IN_PROGRESS", "DONE")) {
                var request = get("/api/v1/projects/{id}/tasks", sharedProject).session(reader.session());
                if (status != null) request.param("status", status);
                if (priority != null) request.param("priority", priority);
                JsonNode result = json.readTree(mvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
                String selectedStatus = status, selectedPriority = priority;
                List<ExpectedTask> matching = shared.stream()
                    .filter(t -> selectedStatus == null || t.status().equals(selectedStatus))
                    .filter(t -> selectedPriority == null || t.priority().equals(selectedPriority))
                    .sorted(java.util.Comparator.comparingLong(ExpectedTask::id).reversed()).toList();
                List<Long> actualIds = new java.util.ArrayList<>();
                for (JsonNode item : result.get("items")) {
                    actualIds.add(item.get("id").asLong());
                    ExpectedTask expected = matching.stream().filter(t -> t.id() == item.get("id").asLong()).findFirst().orElseThrow();
                    assertEquals(expected.status(), item.get("status").asText());
                    assertEquals(expected.priority(), item.get("priority").asText());
                }
                assertEquals(matching.stream().map(ExpectedTask::id).toList(), actualIds,
                    "Exact filtered IDs for " + status + "/" + priority);
                assertEquals(matching.size(), result.get("totalItems").asInt());
                // Counts intentionally ignore status, even when the filtered items are empty.
                assertEquals(expectedCounts(shared, priority), result.get("counts"));
            }
            JsonNode baseline = null;
            for (String status : java.util.Arrays.asList(null, "DONE")) {
                var request = get("/api/v1/dashboard").session(reader.session());
                if (priority != null) request.param("priority", priority);
                if (status != null) request.param("status", status);
                JsonNode dashboard = json.readTree(mvc.perform(request).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
                List<Long> actualProjects = new java.util.ArrayList<>();
                for (JsonNode entry : dashboard.get("projects")) {
                    long projectId = entry.get("project").get("id").asLong();
                    actualProjects.add(projectId);
                    assertEquals(expectedCounts(tasks.stream().filter(t -> t.projectId() == projectId).toList(), priority),
                        entry.get("counts"));
                }
                assertEquals(accessibleProjects, actualProjects, "Filters must not remove accessible empty projects");
                assertEquals(accessibleProjects.size(), dashboard.get("totalProjects").asInt());
                assertEquals(expectedCounts(tasks.stream().filter(t -> accessibleProjects.contains(t.projectId())).toList(), priority),
                    dashboard.get("counts"));
                if (baseline == null) baseline = dashboard;
                else assertEquals(baseline, dashboard, "Task status must not constrain dashboard universe or counts");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager", "assigned-member"})
    void authorizedTaskMutationsCommitAndRefreshExactFiltersAndCounters(String role) throws Exception {
        // Explicit fixture values, not API results/production SQL, define the expected answers.
        List<ExpectedTask> expected = new java.util.ArrayList<>();
        for (String[] values : List.of(new String[]{"TODO", "MEDIUM"}, new String[]{"DONE", "HIGH"},
                new String[]{"IN_PROGRESS", "LOW"}, new String[]{"TODO", "HIGH"})) {
            long id = jdbc.queryForObject("INSERT INTO tasks(project_id,title,status,priority,created_at) VALUES (?,?,?,?,?) RETURNING id",
                Long.class, sharedProject, "Counter fixture " + expected.size(), values[0], values[1],
                java.sql.Timestamp.from(Instant.parse("2026-11-01T00:00:00Z")));
            expected.add(new ExpectedTask(id, sharedProject, values[0], values[1]));
        }
        long target = expected.get(0).id();
        jdbc.update("INSERT INTO task_assignments(task_id,user_id) VALUES (?,?)", target, memberId);
        long privateTask = jdbc.queryForObject("INSERT INTO tasks(project_id,title,status,priority) VALUES (?,?,'DONE','HIGH') RETURNING id",
            Long.class, privateProject, "Private counter sentinel");
        expected.add(new ExpectedTask(privateTask, privateProject, "DONE", "HIGH"));
        long emptyProject = project("Empty counter project");
        Login manager = login(managerEmail);
        Login member = login(memberEmail);
        Login editor = role.equals("manager") ? manager : member;
        Map<String, JsonNode> privateBefore = committedProjectState(privateProject, emptyProject);
        JsonNode targetBefore = committedTask(target);
        assertTaskFiltersAndCounters(manager, expected, List.of(emptyProject, privateProject, sharedProject));
        assertTaskFiltersAndCounters(member, expected, List.of(sharedProject));

        for (String field : List.of("status", "priority")) {
            String value = field.equals("status") ? "DONE" : "HIGH";
            MvcResult changed = mvc.perform(mutation(patch("/api/v1/tasks/{id}/" + field, target), editor,
                    json.writeValueAsString(Map.of(field, value))))
                .andExpect(status().isOk()).andExpect(jsonPath("$." + field).value(value)).andReturn();
            ((com.fasterxml.jackson.databind.node.ObjectNode) targetBefore).put(field, value);
            assertEquals(targetBefore, committedTask(target), "Only selected task column must change and commit");
            mvc.perform(get("/api/v1/tasks/{id}", target).session(editor.session()))
                .andExpect(status().isOk()).andExpect(content().json(changed.getResponse().getContentAsString(), true));
            ExpectedTask old = expected.get(0);
            expected.set(0, new ExpectedTask(target, sharedProject,
                field.equals("status") ? value : old.status(), field.equals("priority") ? value : old.priority()));
            assertTaskFiltersAndCounters(manager, expected, List.of(emptyProject, privateProject, sharedProject));
            assertTaskFiltersAndCounters(member, expected, List.of(sharedProject));
            assertEquals(privateBefore, committedProjectState(privateProject, emptyProject));
            for (ExpectedTask sentinel : expected.subList(1, expected.size())) {
                JsonNode committed = committedTask(sentinel.id());
                assertEquals(sentinel.status(), committed.get("status").asText());
                assertEquals(sentinel.priority(), committed.get("priority").asText());
            }
        }
    }

    @Test
    void managerAssignmentLifecycleCommitsAndRejectsInvalidSetsWithoutPartialWrites() throws Exception {
        Login manager = login(managerEmail);
        long taskId = create(manager, sharedProject, "{\"title\":\"Assignment lifecycle\"}").get("id").asLong();
        create(manager, privateProject, json.writeValueAsString(Map.of(
            "title", "Unrelated assignment sentinel", "responsibleUserIds", List.of(managerId))));
        // Existing user, deliberately not a participant in the target project.
        long outsider = jdbc.queryForObject("INSERT INTO app_users(name,email,password_hash) "
                + "SELECT 'Ineligible assignment user',?,password_hash FROM app_users WHERE id=? RETURNING id",
            Long.class, "assignment-outsider-" + UUID.randomUUID() + "@example.org", managerId);
        Map<String, JsonNode> unrelated = committedProjectState(privateProject, privateProject);
        Map<String, JsonNode> initial = committedProjectState(sharedProject, sharedProject);
        assertTrue(initial.get("task_assignments").isEmpty());
        List<Long> previous = List.of();
        int historyCount = initial.get("task_history").size();
        // Add, multiple, reordered idempotent PUT, remove one, replace, clear, repeat clear.
        for (List<Long> requested : List.of(List.of(managerId), List.of(managerId, memberId),
                List.of(memberId, managerId), List.of(memberId), List.of(managerId),
                List.<Long>of(), List.<Long>of())) {
            Map<String, JsonNode> before = committedProjectState(sharedProject, sharedProject);
            List<Long> expected = requested.stream().sorted().toList();
            String body = json.writeValueAsString(Map.of("responsibleUserIds", requested));
            mvc.perform(mutation(put("/api/v1/tasks/{id}/responsibles", taskId), manager, body))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals(json.readTree(json.writeValueAsString(expected)),
                    json.readTree(result.getResponse().getContentAsString()).get("responsibleUserIds")));
            Map<String, JsonNode> after = committedProjectState(sharedProject, sharedProject);
            List<Long> committedIds = new java.util.ArrayList<>();
            for (JsonNode row : after.get("task_assignments")) {
                assertEquals(taskId, row.get("task_id").asLong());
                committedIds.add(row.get("user_id").asLong());
            }
            committedIds.sort(Long::compareTo);
            assertEquals(expected, committedIds, "Exact committed assignments, without duplicates");
            mvc.perform(get("/api/v1/tasks/{id}", taskId).session(manager.session()))
                .andExpect(status().isOk()).andExpect(result -> assertEquals(json.readTree(json.writeValueAsString(expected)),
                    json.readTree(result.getResponse().getContentAsString()).get("responsibleUserIds")));
            if (previous.equals(expected)) assertEquals(before, after, "Idempotent PUT must not write history or data");
            else historyCount++;
            assertEquals(historyCount, after.get("task_history").size());
            for (String table : List.of("projects", "project_memberships", "tasks", "task_comments"))
                assertEquals(initial.get(table), after.get(table), "Assignments must not change " + table);
            assertEquals(unrelated, committedProjectState(privateProject, privateProject));

            if (expected.equals(List.of(managerId, memberId).stream().sorted().toList())) {
                for (List<Long> invalid : List.of(List.of(managerId, managerId), List.of(managerId, outsider))) {
                    mvc.perform(mutation(put("/api/v1/tasks/{id}/responsibles", taskId), manager,
                            json.writeValueAsString(Map.of("responsibleUserIds", invalid))))
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.errors[0].field").value("responsibleUserIds"));
                    assertEquals(after, committedProjectState(sharedProject, sharedProject),
                        "Invalid replacement must preserve all rows, including assignments/history");
                    assertEquals(unrelated, committedProjectState(privateProject, privateProject));
                }
            }
            previous = expected;
        }
    }

    @Test
    void anonymousRequestsAreUnauthorizedWithoutWrites() throws Exception {
        JsonNode created = create(login(managerEmail), sharedProject, "{\"title\":\"Existing task\"}");
        long id = created.get("id").asLong();
        JsonNode before = committedTask(id);
        mvc.perform(get("/api/v1/tasks/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/projects/{id}/tasks", privateProject)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Anonymous task\"}"))
            .andExpect(status().isUnauthorized());
        assertEquals(before, committedTask(id));
        assertNoTasks(privateProject);
    }
}
