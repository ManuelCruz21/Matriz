package pt.matriz.tasks;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.matriz.auth.SecurityConfig;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Testes de controlador com JDBC simulado. Não comprovam persistência real. */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class TaskControllerTest {
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;

    @Test void applicationMapperPreservesUntypedDecimalPrecision() throws Exception {
        var body = json.readValue("{\"id\":2.0000000000000000001}", java.util.Map.class);
        org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("2.0000000000000000001"),
            org.junit.jupiter.api.Assertions.assertInstanceOf(java.math.BigDecimal.class, body.get("id")));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "2.0000000000000000001", "1.9999999999999999999", "9223372036854775808.0",
        "9223372036854775807.0000000000000000001", "1.9", "0.0", "-1.0", "null", "true", "\"2\""
    })
    @WithMockUser(username="gestor@example.org")
    void exactDecimalRejectionIsAtomicOnBothEndpoints(String number) throws Exception {
        user(1L);
        when(jdbc.query(contains("FROM project_memberships"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq(10L), eq(1L)))
            .thenReturn(List.of("gestor"));
        when(jdbc.query(contains("SELECT t.project_id, pm.role"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), eq(20L), eq(1L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(10L); when(row.getString(2)).thenReturn("gestor");
                return List.of(mapper.mapRow(row, 0));
            });
        for (boolean creation : new boolean[]{true, false}) {
            clearInvocations(jdbc);
            var request = creation ? post("/api/v1/projects/10/tasks") : put("/api/v1/tasks/20/responsibles");
            mvc.perform(request.sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + (creation ? "\"title\":\"Valid title\"," : "") + "\"responsibleUserIds\":[2," + number + "]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errors[0].field").value("responsibleUserIds"));
            verify(jdbc).query(contains("FROM app_users"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any(), any());
            if (creation) verify(jdbc).query(contains("FROM project_memberships"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq(10L), eq(1L));
            else verify(jdbc).query(contains("SELECT t.project_id, pm.role"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), eq(20L), eq(1L));
            verifyNoMoreInteractions(jdbc); // No eligibility, task, assignment or history writes after a valid first ID.
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "2.0,2", "2.000,2", "2e0,2", "9007199254740993.0,9007199254740993",
        "9223372036854775807.0,9223372036854775807"
    })
    @WithMockUser(username="gestor@example.org")
    void exactIntegralDecimalsPreservedOnBothEndpoints(String number, long expected) throws Exception {
        user(1L);
        when(jdbc.query(contains("FROM project_memberships"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq(10L), eq(1L)))
            .thenReturn(List.of("gestor"));
        when(jdbc.query(contains("SELECT t.project_id, pm.role"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), eq(20L), eq(1L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(10L); when(row.getString(2)).thenReturn("gestor");
                return List.of(mapper.mapRow(row, 0));
            });
        when(jdbc.queryForObject(startsWith("SELECT COUNT(*) FROM project_memberships"), eq(Integer.class), any(Object[].class)))
            .thenReturn(1);
        when(jdbc.queryForObject(startsWith("INSERT INTO tasks"), eq(Long.class), eq(10L), eq("Valid title"), isNull()))
            .thenReturn(20L);
        for (boolean creation : new boolean[]{true, false}) {
            clearInvocations(jdbc);
            var request = creation ? post("/api/v1/projects/10/tasks") : put("/api/v1/tasks/20/responsibles");
            mvc.perform(request.sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + (creation ? "\"title\":\"Valid title\"," : "") + "\"responsibleUserIds\":[" + number + "]}"))
                .andExpect(status().is(creation ? 201 : 200));
            verify(jdbc).queryForObject(startsWith("SELECT COUNT(*) FROM project_memberships"), eq(Integer.class), eq(10L), eq(expected));
            verify(jdbc).update("INSERT INTO task_assignments(task_id,user_id) VALUES (?,?)", 20L, expected);
        }
    }
    @MockitoBean JdbcTemplate jdbc;

    private void user(long id) {
        when(jdbc.query(contains("FROM app_users"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any(),
                any())).thenReturn(List.of(id));
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> taskFilters() {
        return java.util.stream.Stream.of(null, "TODO", "IN_PROGRESS", "DONE").flatMap(status ->
            java.util.stream.Stream.of(null, "LOW", "MEDIUM", "HIGH").map(priority ->
                org.junit.jupiter.params.provider.Arguments.of(status, priority)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("taskFilters")
    @WithMockUser(username="membro@example.org")
    void taskListUsesOnlySuppliedFiltersAndBoundValues(String statusFilter, String priority) throws Exception {
        user(2L);
        when(jdbc.query(contains("FROM project_memberships"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq(10L), eq(2L)))
            .thenReturn(List.of("membro"));
        var expectedArgs = new java.util.ArrayList<Object>();
        expectedArgs.add(10L);
        if (statusFilter != null) expectedArgs.add(statusFilter);
        if (priority != null) expectedArgs.add(priority);
        String expectedSql = "SELECT id FROM tasks WHERE project_id=?"
            + (statusFilter == null ? "" : " AND status=?")
            + (priority == null ? "" : " AND priority=?") + " ORDER BY created_at DESC,id DESC";
        var task = new TaskController.Task(99L,10L,"Task",null,
            statusFilter == null ? "TODO" : statusFilter, priority == null ? "MEDIUM" : priority,
            java.time.Instant.parse("2026-11-13T10:00:00Z"),List.of());
        when(jdbc.query(startsWith("SELECT id FROM tasks WHERE"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<TaskController.Task>>any(), any(Object[].class)))
            .thenAnswer(call -> {
                org.junit.jupiter.api.Assertions.assertEquals(expectedSql, call.getArgument(0));
                org.junit.jupiter.api.Assertions.assertArrayEquals(expectedArgs.toArray(),
                    java.util.Arrays.copyOfRange(call.getArguments(), 2, call.getArguments().length));
                return List.of(task);
            });
        when(jdbc.query(startsWith("SELECT status,COUNT(*)"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<java.util.Map.Entry<String,Long>>>any(), any(Object[].class)))
            .thenReturn(List.of(java.util.Map.entry("TODO",2L),java.util.Map.entry("DONE",1L)));
        var request = get("/api/v1/projects/10/tasks")
            .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond());
        if (statusFilter != null) request.param("status",statusFilter);
        if (priority != null) request.param("priority",priority);
        mvc.perform(request).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(99))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.counts.TODO").value(2))
            .andExpect(jsonPath("$.counts.DONE").value(1))
            .andExpect(jsonPath("$.counts.total").value(3));
        verify(jdbc).query(eq(expectedSql),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<TaskController.Task>>any(), eq(expectedArgs.toArray()));
        String countSql = "SELECT status,COUNT(*) AS total FROM tasks WHERE project_id=?"
            + (priority == null ? "" : " AND priority=?") + " GROUP BY status";
        Object[] countArgs = priority == null ? new Object[]{10L} : new Object[]{10L,priority};
        verify(jdbc).query(eq(countSql),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<java.util.Map.Entry<String,Long>>>any(), eq(countArgs));
    }

    @Test @WithMockUser(username="gestor@example.org")
    void invalidTaskTitleIsRejected() throws Exception {
        user(1L);
        when(jdbc.query(contains("FROM project_memberships"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(),
                eq(10L), eq(1L))).thenReturn(List.of("gestor"));
        mvc.perform(post("/api/v1/projects/10/tasks")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  \"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.errors[0].field").value("title"));
        verify(jdbc, never()).update(startsWith("INSERT INTO tasks"), any(Object[].class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"unexpected", "status", "priority", "projectId", "createdByUserId"})
    @WithMockUser(username="gestor@example.org")
    void taskCreationRejectsEveryUnknownFieldWithoutWriting(String field) throws Exception {
        user(1L);
        when(jdbc.query(contains("FROM project_memberships"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(),
                eq(10L), eq(1L))).thenReturn(List.of("gestor"));
        mvc.perform(post("/api/v1/projects/10/tasks")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Valid title\",\"description\":null,\"responsibleUserIds\":[],\"" + field + "\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400));
        verify(jdbc, never()).queryForObject(contains("INSERT INTO tasks"), eq(Long.class), anyLong(), anyString(), nullable(String.class));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"1.9", "2.5", "-1.5", "0", "9223372036854775808", "9223372036854775808.0"})
    @WithMockUser(username="gestor@example.org")
    void responsibleIdsRejectInvalidNumbersWithoutWrites(String number) throws Exception {
        user(1L);
        when(jdbc.query(contains("FROM project_memberships"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq(10L), eq(1L)))
            .thenReturn(List.of("gestor"));
        mvc.perform(post("/api/v1/projects/10/tasks")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Valid title\",\"responsibleUserIds\":[2," + number + "]}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].field").value("responsibleUserIds"));
        verify(jdbc).query(contains("FROM app_users"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any(), any());
        verify(jdbc).query(contains("FROM project_memberships"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq(10L), eq(1L));
        verifyNoMoreInteractions(jdbc); // No eligibility/INSERT/dependent writes, even after a valid first ID.
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"1.9", "2.5", "9223372036854775808.0"})
    @WithMockUser(username="gestor@example.org")
    void responsibleReplacementRejectsInvalidNumbersWithoutWrites(String number) throws Exception {
        user(1L);
        when(jdbc.query(contains("SELECT t.project_id, pm.role"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), eq(20L), eq(1L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(10L);
                when(row.getString(2)).thenReturn("gestor");
                return List.of(mapper.mapRow(row, 0));
            });
        mvc.perform(put("/api/v1/tasks/20/responsibles")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"responsibleUserIds\":[2," + number + "]}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errors[0].field").value("responsibleUserIds"));
        verify(jdbc).query(contains("FROM app_users"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any(), any());
        verify(jdbc).query(contains("SELECT t.project_id, pm.role"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), eq(20L), eq(1L));
        verifyNoMoreInteractions(jdbc);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"2,2", "2147483648,2147483648", "9007199254740993,9007199254740993", "9223372036854775807,9223372036854775807", "2.0,2"})
    @WithMockUser(username="gestor@example.org")
    void responsibleIdsPreserveValidIntegers(String number, long expected) throws Exception {
        user(1L);
        when(jdbc.query(contains("FROM project_memberships"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(), eq(10L), eq(1L)))
            .thenReturn(List.of("gestor"));
        when(jdbc.queryForObject(startsWith("SELECT COUNT(*) FROM project_memberships"), eq(Integer.class), any(Object[].class)))
            .thenReturn(1);
        when(jdbc.queryForObject(startsWith("INSERT INTO tasks"), eq(Long.class), eq(10L), eq("Valid title"), isNull()))
            .thenReturn(20L);
        mvc.perform(post("/api/v1/projects/10/tasks")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Valid title\",\"responsibleUserIds\":[" + number + "]}"))
            .andExpect(status().isCreated());
        verify(jdbc).queryForObject(startsWith("SELECT COUNT(*) FROM project_memberships"), eq(Integer.class), eq(10L), eq(expected));
        verify(jdbc).update("INSERT INTO task_assignments(task_id,user_id) VALUES (?,?)", 20L, expected);
    }

    @Test @WithMockUser(username="membro@example.org")
    void userWithoutProjectMembershipCannotCreateTask() throws Exception {
        user(2L);
        mvc.perform(post("/api/v1/projects/99/tasks")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Tarefa válida\"}"))
            .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(username="membro@example.org")
    void userWithoutTaskMembershipCannotReadTask() throws Exception {
        user(2L);
        mvc.perform(get("/api/v1/tasks/99")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()))
            .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(username="membro@example.org")
    void eligibleUsersUseNestedUserAndProjectRole() throws Exception {
        user(2L);
        when(jdbc.query(contains("FROM project_memberships"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(),
                eq(10L), eq(2L))).thenReturn(List.of("membro"));
        when(jdbc.query(contains("SELECT u.id,u.name,u.email,m.role"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<java.util.Map<String,Object>>>any(), eq(10L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(2L);
                when(row.getString(2)).thenReturn("Member");
                when(row.getString(3)).thenReturn("member@example.org");
                when(row.getString(4)).thenReturn("membro");
                return List.of(mapper.mapRow(row, 0));
            });
        mvc.perform(get("/api/v1/projects/10/eligible-users")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()))
            .andExpect(status().isOk()).andExpect(content().json("""
                {"items":[{"user":{"id":2,"name":"Member","email":"member@example.org"},"projectRole":"MEMBRO"}],"totalItems":1}
                """, true));
    }

    @Test @WithMockUser(username="membro@example.org")
    void eligibleUsersHideInaccessibleProjects() throws Exception {
        user(2L);
        mvc.perform(get("/api/v1/projects/99/eligible-users")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()))
            .andExpect(status().isNotFound());
        verify(jdbc, never()).query(contains("SELECT u.id,u.name,u.email,m.role"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), anyLong());
    }

    @Test @WithMockUser(username="membro@example.org")
    void dashboardUsesExactProjectSchemaIncludingNullDescriptionAndLegacyCreator() throws Exception {
        user(2L);
        when(jdbc.query(contains("SELECT p.id,p.name,p.description,p.created_by_user_id"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<pt.matriz.projects.ProjectController.Project>>any(), eq(2L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(10L);
                when(row.getString(2)).thenReturn("Legacy project");
                return List.of(mapper.mapRow(row, 0));
            });
        mvc.perform(get("/api/v1/dashboard")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()))
            .andExpect(status().isOk()).andExpect(content().json("""
                {"projects":[{"project":{"id":10,"name":"Legacy project","description":null,"createdByUserId":null},
                "counts":{"TODO":0,"IN_PROGRESS":0,"DONE":0,"total":0}}],"totalProjects":1,
                "counts":{"TODO":0,"IN_PROGRESS":0,"DONE":0,"total":0}}
                """, true));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"LOW", "MEDIUM", "HIGH"})
    @WithMockUser(username="membro@example.org")
    void dashboardFiltersAllCountsByPriorityWithoutRemovingProjects(String priority) throws Exception {
        user(2L);
        when(jdbc.query(contains("SELECT p.id,p.name,p.description,p.created_by_user_id"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<pt.matriz.projects.ProjectController.Project>>any(), eq(2L)))
            .thenReturn(List.of(
                new pt.matriz.projects.ProjectController.Project(10L,"First",null,1L),
                new pt.matriz.projects.ProjectController.Project(9L,"Second",null,1L),
                new pt.matriz.projects.ProjectController.Project(8L,"Empty",null,1L)));
        when(jdbc.query(contains("SELECT status,COUNT(*)"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<java.util.Map.Entry<String,Long>>>any(),
                any(Object[].class)))
            .thenAnswer(call -> {
                String expectedSql = "SELECT status,COUNT(*) AS total FROM tasks WHERE project_id=?"
                    + (priority == null ? "" : " AND priority=?") + " GROUP BY status";
                org.junit.jupiter.api.Assertions.assertEquals(expectedSql, call.getArgument(0));
                org.junit.jupiter.api.Assertions.assertEquals(priority == null ? 3 : 4, call.getArguments().length);
                if (priority != null) org.junit.jupiter.api.Assertions.assertEquals(priority, call.getArgument(3));
                long pid = call.getArgument(2);
                if (pid == 8L) return List.of();
                return pid == 10L ? List.of(java.util.Map.entry("TODO",2L),java.util.Map.entry("DONE",1L))
                    : List.of(java.util.Map.entry("IN_PROGRESS",4L));
            });
        var request = get("/api/v1/dashboard").param("status", "DONE")
            .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond());
        if (priority != null) request.param("priority", priority);
        mvc.perform(request).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalProjects").value(3))
            .andExpect(jsonPath("$.projects[0].counts.TODO").value(2))
            .andExpect(jsonPath("$.projects[0].counts.total").value(3))
            .andExpect(jsonPath("$.projects[1].counts.IN_PROGRESS").value(4))
            .andExpect(jsonPath("$.projects[2].counts.total").value(0))
            .andExpect(jsonPath("$.counts.TODO").value(2))
            .andExpect(jsonPath("$.counts.IN_PROGRESS").value(4))
            .andExpect(jsonPath("$.counts.DONE").value(1))
            .andExpect(jsonPath("$.counts.total").value(7));
        for (long pid : new long[]{10L,9L,8L}) {
            if (priority == null) {
                verify(jdbc).query(eq("SELECT status,COUNT(*) AS total FROM tasks WHERE project_id=? GROUP BY status"),
                    org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<java.util.Map.Entry<String,Long>>>any(), eq(pid));
            } else {
                verify(jdbc).query(eq("SELECT status,COUNT(*) AS total FROM tasks WHERE project_id=? AND priority=? GROUP BY status"),
                    org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<java.util.Map.Entry<String,Long>>>any(), eq(pid), eq(priority));
            }
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", "high", "URGENT", " HIGH "})
    @WithMockUser(username="membro@example.org")
    void dashboardRejectsInvalidPriorityBeforeReadingProjects(String priority) throws Exception {
        user(2L);
        mvc.perform(get("/api/v1/dashboard").param("priority",priority)
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()))
            .andExpect(status().isBadRequest());
        verify(jdbc, never()).query(contains("FROM projects p"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), anyLong());
        verify(jdbc, never()).query(contains("SELECT status,COUNT(*)"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), any(Object[].class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
        "status|TODO|DONE|status|\"TODO\"|\"DONE\"",
        "priority|LOW|HIGH|priority|\"LOW\"|\"HIGH\"",
        "assignees|[]|[2, 9007199254740993]|responsibleUserIds|[]|[2,9007199254740993]",
        "assignees|[2, 3]|[]|responsibleUserIds|[2,3]|[]"
    }, delimiter = '|')
    @WithMockUser(username="membro@example.org")
    void historyMatchesContract(String storedField, String before, String after,
                                String publicField, String jsonBefore, String jsonAfter) throws Exception {
        user(2L);
        when(jdbc.query(contains("SELECT t.project_id, pm.role"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(), eq(99L), eq(2L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(10L);
                when(row.getString(2)).thenReturn("membro");
                return List.of(mapper.mapRow(row, 0));
            });
        when(jdbc.query(contains("FROM task_history"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<TaskController.History>>any(), eq(99L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(5L);
                when(row.getLong(2)).thenReturn(99L);
                when(row.getLong(3)).thenReturn(7L);
                when(row.getString(4)).thenReturn(storedField);
                when(row.getString(5)).thenReturn(before);
                when(row.getString(6)).thenReturn(after);
                when(row.getTimestamp(7)).thenReturn(java.sql.Timestamp.from(java.time.Instant.parse("2026-11-13T10:00:00Z")));
                return List.of(mapper.mapRow(row, 0));
            });
        mvc.perform(get("/api/v1/tasks/99/history")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()))
            .andExpect(status().isOk()).andExpect(content().json("""
                {"items":[{"id":5,"taskId":99,"changedByUserId":7,"field":"%s",
                "previousValue":%s,"newValue":%s,"changedAt":"2026-11-13T10:00:00Z"}],"totalItems":1}
                """.formatted(publicField, jsonBefore, jsonAfter), true));
        verify(jdbc).query(contains("ORDER BY changed_at,id"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<TaskController.History>>any(), eq(99L));
    }

    @Test @WithMockUser(username="membro@example.org")
    void historyHidesInaccessibleTasks() throws Exception {
        user(2L);
        mvc.perform(get("/api/v1/tasks/99/history")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond()))
            .andExpect(status().isNotFound());
        verify(jdbc, never()).query(contains("FROM task_history"),
            org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<TaskController.History>>any(), anyLong());
    }

    @Test
    void anonymousUserCannotReadHistory() throws Exception {
        mvc.perform(get("/api/v1/tasks/99/history")).andExpect(status().isUnauthorized());
        verifyNoInteractions(jdbc);
    }

    @Test
    void anonymousUserCannotReadTask() throws Exception {
        mvc.perform(get("/api/v1/tasks/99"))
            .andExpect(status().isUnauthorized());
    }


    
    // Simulate a project member who is not assigned to the task.
    private void unassignedMember() {
        user(2L);
        when(jdbc.queryForObject("SELECT COUNT(*) FROM task_assignments WHERE task_id=? AND user_id=?",
                Integer.class, 99L, 2L)).thenReturn(0);

        when(jdbc.query(
                contains("SELECT t.project_id, pm.role"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any(),
                eq(99L), eq(2L)))
            .thenAnswer(call -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);

                when(row.getLong(1)).thenReturn(10L);
                when(row.getString(2)).thenReturn("membro");

                return List.of(mapper.mapRow(row, 0));
            });
    }


    @Test
    @WithMockUser(username = "membro@example.org")
    void unassignedMemberCannotEditTask() throws Exception {
        unassignedMember();

        mvc.perform(patch("/api/v1/tasks/99")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Unauthorized change\"}"))
            .andExpect(status().isForbidden());

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @WithMockUser(username = "membro@example.org")
    void unassignedMemberCannotChangeTaskStatus() throws Exception {
        unassignedMember();

        mvc.perform(patch("/api/v1/tasks/99/status")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
            .andExpect(status().isForbidden());

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @WithMockUser(username = "membro@example.org")
    void memberCannotDeleteTask() throws Exception {
        unassignedMember();

        mvc.perform(delete("/api/v1/tasks/99")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond())
                .with(csrf()))
            .andExpect(status().isForbidden());

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @WithMockUser(username = "membro@example.org")
    void memberCannotChangeTaskResponsibles() throws Exception {
        unassignedMember();

        mvc.perform(put("/api/v1/tasks/99/responsibles")
                .sessionAttr("MATRIZ_SESSION_START", java.time.Instant.now().getEpochSecond())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"responsibleUserIds\":[2]}"))
            .andExpect(status().isForbidden());

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

}
