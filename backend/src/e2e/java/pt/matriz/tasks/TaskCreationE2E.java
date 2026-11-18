package pt.matriz.tasks;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Browser -> private Vite proxy -> real HTTP backend -> disposable PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TaskCreationE2E {
    private static final String SECURITY_ADMIN_EMAIL = "security-admin-e2e@example.org";
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11-bookworm")
        .withDatabaseName("matriz_e2e_test").withUsername("matriz_e2e")
        .withPassword(UUID.randomUUID().toString());

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("spring.flyway.enabled", () -> true);
        properties.add("spring.flyway.clean-disabled", () -> true);
        properties.add("spring.flyway.locations", () -> "classpath:db/migration");
        properties.add("matriz.demo-seed.enabled", () -> false);
        properties.add("server.address", () -> "127.0.0.1");
        properties.add("server.servlet.session.cookie.secure", () -> false);
        properties.add("MATRIZ_SECURITY_ADMIN_EMAIL", () -> SECURITY_ADMIN_EMAIL);
    }

    @LocalServerPort int backendPort;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;

    @Test
    void securityAdminCanUseSecurityCenter() throws Exception {
        String password = "SecurityAdminE2EPassword123!";

        jdbc.update(
            "INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?)",
            "Security E2E Admin",
            SECURITY_ADMIN_EMAIL,
            passwords.encode(password)
        );

        withBrowser("security-admin", (page, baseUrl) -> {
            page.navigate(baseUrl);

            page.getByLabel(
                "Username",
                new Page.GetByLabelOptions().setExact(true)
            ).fill(SECURITY_ADMIN_EMAIL);

            page.getByLabel(
                "Password",
                new Page.GetByLabelOptions().setExact(true)
            ).fill(password);

            button(page, "Sign in").click();

            assertThat(
                page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                        .setName("Dashboard")
                        .setExact(true)
                )
            ).isVisible();

            verifySecurityCenter(page);
        });
    }


    @Test
    void loginOpenProjectCreateTaskAndRemainInItsTaskList() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "browser-" + suffix + "@example.org";
        String password = "BrowserOnlyPassword123!";
        String projectName = "Browser project " + suffix;
        String taskTitle = "Browser task " + suffix;
        long user = jdbc.queryForObject("INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?) RETURNING id",
            Long.class, "Browser User", email, passwords.encode(password));
        long project = jdbc.queryForObject("INSERT INTO projects(name,created_by_user_id) VALUES (?,?) RETURNING id",
            Long.class, projectName, user);
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'gestor')", project, user);

        withBrowser("task-creation", (page, baseUrl) -> {
                    page.navigate(baseUrl);
                    page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true)).fill(email);
                    page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(password);
                    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true)).click();
                    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
                    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Projects").setExact(true)).click();
                    Locator card = page.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText(projectName));
                    card.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Open project: " + projectName).setExact(true)).click();
                    Locator projectHeading = page.getByRole(AriaRole.HEADING,
                        new Page.GetByRoleOptions().setName(projectName).setExact(true));
                    assertThat(projectHeading).isVisible();
                    // Prove that creation clears filters which would otherwise hide TODO/MEDIUM.
                    page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Status"))).selectOption("DONE");
                    page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Priority"))).selectOption("HIGH");
                    page.getByLabel("Title", new Page.GetByLabelOptions().setExact(true)).fill(taskTitle);
                    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save task").setExact(true)).click();
                    assertThat(page.getByRole(AriaRole.STATUS).filter(
                        new Locator.FilterOptions().setHasText("Task created successfully."))).isVisible();
                    assertThat(projectHeading).isVisible();
                    assertThat(page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Status")))).hasValue("");
                    assertThat(page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Priority")))).hasValue("");
                    Locator taskCard = page.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText(taskTitle));
                    assertThat(taskCard).hasCount(1);
                    assertThat(taskCard.getByRole(AriaRole.HEADING)).hasText(taskTitle);
                    assertThat(taskCard).containsText("To Do · Medium");
                    assertThat(taskCard.getByRole(AriaRole.LINK,
                        new Locator.GetByRoleOptions().setName("Open task: " + taskTitle).setExact(true))).isVisible();
                    assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE project_id=? AND title=? AND status='TODO' AND priority='MEDIUM'",
                        Long.class, project, taskTitle));
                    page.reload();
                    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Projects").setExact(true)).click();
                    card.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Open project: " + projectName).setExact(true)).click();
                    assertThat(projectHeading).isVisible();
                    assertThat(taskCard).hasCount(1);
                    assertThat(button(page, "Security Center")).hasCount(0);
        });
    }

    @Test
    void authenticationAndPermissionBoundaries() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String password = "BoundaryOnlyPassword123!";
        long[] ids = new long[3];
        String[] emails = new String[3];
        for (int i = 0; i < ids.length; i++) {
            emails[i] = "boundary-" + i + "-" + suffix + "@example.org";
            ids[i] = jdbc.queryForObject("INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?) RETURNING id",
                Long.class, "Boundary User " + i, emails[i], passwords.encode(password));
        }
        String projectName = "Boundary project " + suffix;
        long project = jdbc.queryForObject("INSERT INTO projects(name,created_by_user_id) VALUES (?,?) RETURNING id",
            Long.class, projectName, ids[0]);
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'gestor'),(?,?,'membro')",
            project, ids[0], project, ids[1]);
        long task = jdbc.queryForObject("INSERT INTO tasks(project_id,title) VALUES (?,?) RETURNING id",
            Long.class, project, "Boundary task");
        // Nonempty dependents make denied deletion checks meaningful.
        jdbc.update("INSERT INTO task_assignments(task_id,user_id) VALUES (?,?)", task, ids[0]);
        jdbc.update("INSERT INTO task_comments(task_id,author_id,content) VALUES (?,?,'Sentinel comment')", task, ids[0]);
        jdbc.update("INSERT INTO task_history(task_id,actor_id,field,previous_value,new_value) VALUES (?,?,'status','IN_PROGRESS','TODO')",
            task, ids[0]);
        var before = committedBoundaryState();

        withBrowser("authentication-boundaries", (manager, baseUrl) -> {
            manager.navigate(baseUrl);
            manager.getByLabel("Username", new Page.GetByLabelOptions().setExact(true)).fill(emails[0]);
            manager.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill("WrongPassword123!");
            Response rejected = manager.waitForResponse(r -> r.url().endsWith("/auth/login") && r.request().method().equals("POST"),
                () -> button(manager, "Sign in").click());
            assertEquals(401, rejected.status());
            assertThat(manager.getByRole(AriaRole.ALERT)).hasText("Não foi possível iniciar sessão. Verifica os dados e tenta novamente.");
            assertThat(manager.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Log in"))).isVisible();
            assertThat(manager.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).hasCount(0);
            assertEquals(401, manager.context().request().get(baseUrl + "/api/v1/users/me").status());
            assertEquals(before, committedBoundaryState());
            // Login changes the session version and remounts the form: re-enter both credentials.
            manager.getByLabel("Username", new Page.GetByLabelOptions().setExact(true)).fill(emails[0]);
            manager.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(password);
            button(manager, "Sign in").click();
            assertThat(manager.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
            assertThat(manager.getByRole(AriaRole.ALERT)).hasCount(0);
            openBoundaryProject(manager, projectName);
            assertThat(manager.getByText("Role: GESTOR", new Page.GetByTextOptions().setExact(true))).isVisible();
            assertThat(manager.getByText("Edit or delete project", new Page.GetByTextOptions().setExact(true))).isVisible();
            assertThat(button(manager, "Save task")).isVisible();
            link(manager, "Open task: Boundary task").click();
            assertThat(button(manager, "Save task")).isVisible();
            assertThat(button(manager, "Save assignees")).isVisible();
            assertThat(button(manager, "Delete task")).isVisible();

            try (BrowserContext memberContext = manager.context().browser().newContext();
                 BrowserContext outsiderContext = manager.context().browser().newContext();
                 BrowserContext anonymous = manager.context().browser().newContext()) {
                Page member = memberContext.newPage();
                loginBoundaryUser(member, baseUrl, emails[1], password);
                openBoundaryProject(member, projectName);
                assertThat(member.getByText("Role: MEMBRO", new Page.GetByTextOptions().setExact(true))).isVisible();
                assertThat(member.getByText("Edit or delete project", new Page.GetByTextOptions().setExact(true))).hasCount(0);
                assertThat(button(member, "Delete project")).hasCount(0);
                assertThat(button(member, "Save task")).isVisible();
                link(member, "Open task: Boundary task").click();
                assertThat(button(member, "Save task")).hasCount(0);
                assertThat(button(member, "Save assignees")).hasCount(0);
                assertThat(button(member, "Delete task")).hasCount(0);
                assertThat(member.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Status").setExact(true))).hasCount(0);
                assertThat(member.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Priority").setExact(true))).hasCount(0);
                assertThat(button(member, "Post comment")).isVisible();
                assertEquals(200, memberContext.request().get(baseUrl + "/api/v1/projects/" + project).status());
                assertEquals(200, memberContext.request().get(baseUrl + "/api/v1/tasks/" + task).status());
                assertEquals(403, boundaryMutation(memberContext, baseUrl, "PATCH", "/projects/" + project,
                    java.util.Map.of("name", "Forbidden change")));
                assertEquals(before, committedBoundaryState());
                assertEquals(403, boundaryMutation(memberContext, baseUrl, "DELETE", "/projects/" + project, null));
                assertEquals(before, committedBoundaryState());

                Page outsider = outsiderContext.newPage();
                loginBoundaryUser(outsider, baseUrl, emails[2], password);
                assertDashboard(outsider, 0, 0, 0, 0);
                link(outsider, "Projects").click();
                assertThat(outsider.getByText("No projects match your search.", new Page.GetByTextOptions().setExact(true))).isVisible();
                assertThat(outsider.getByRole(AriaRole.ARTICLE)).hasCount(0);
                assertThat(button(outsider, "Delete project")).hasCount(0);
                assertThat(link(outsider, "Open task: Boundary task")).hasCount(0);
                // Representative identifier swaps, not the existing full PostgreSQL matrix.
                assertEquals(404, outsiderContext.request().get(baseUrl + "/api/v1/projects/" + project).status());
                assertEquals(404, outsiderContext.request().get(baseUrl + "/api/v1/tasks/" + task).status());
                assertEquals(404, boundaryMutation(outsiderContext, baseUrl, "PATCH", "/tasks/" + task,
                    java.util.Map.of("title", "Forbidden outsider change")));
                assertEquals(before, committedBoundaryState());
                assertEquals(401, anonymous.request().get(baseUrl + "/api/v1/projects/" + project).status());
                assertEquals(401, anonymous.request().get(baseUrl + "/api/v1/tasks/" + task).status());
                assertEquals(401, boundaryMutation(anonymous, baseUrl, "DELETE", "/projects/" + project, null));
                assertEquals(before, committedBoundaryState());
                assertEquals(200, manager.context().request().get(baseUrl + "/api/v1/projects/" + project).status());
            }
        });
    }

    @Test
    void projectLifecycleValidationSearchAndDeletion() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "project-browser-" + suffix + "@example.org";
        String password = "ProjectBrowserOnly123!";
        long user = jdbc.queryForObject("INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?) RETURNING id",
            Long.class, "Project Browser Manager", email, passwords.encode(password));
        String original = "Café MiXeD%_ " + suffix;
        String edited = "Café Edited%_ " + suffix;
        // Near matches distinguish accent and literal wildcard behavior without reproducing the API matrix.
        for (String name : java.util.List.of("Cafe MiXeD%_ " + suffix, "Café MiXeDXY " + suffix)) {
            long sentinel = jdbc.queryForObject("INSERT INTO projects(name,created_by_user_id) VALUES (?,?) RETURNING id",
                Long.class, name, user);
            jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'gestor')", sentinel, user);
            seedProjectDependents(sentinel, user);
        }
        var unrelated = committedBoundaryState();
        withBrowser("project-lifecycle", (page, baseUrl) -> {
            loginBoundaryUser(page, baseUrl, email, password);
            link(page, "Projects").click();
            Locator name = page.getByLabel("Name", new Page.GetByLabelOptions().setExact(true));
            Locator description = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Description")));
            java.util.List<String> writes = new java.util.ArrayList<>();
            page.onRequest(request -> {
                if (request.url().contains("/api/v1/projects") && !request.method().equals("GET"))
                    writes.add(request.method());
            });
            description.fill("Draft retained after validation");
            button(page, "Save project").click();
            assertEquals(true, name.evaluate("element => element.validity.valueMissing"));
            assertEquals(0, writes.size(), "Empty required name must not submit");
            assertEquals(unrelated, committedBoundaryState());
            name.fill("   ");
            button(page, "Save project").click();
            assertEquals(true, name.evaluate("element => element.validity.customError"));
            assertEquals(0, writes.size());
            // Bypass native validation to retain coverage of the server's 422 and draft handling.
            Response invalid = page.waitForResponse(r -> r.url().endsWith("/api/v1/projects") && r.request().method().equals("POST"),
                () -> button(page, "Save project").evaluate("button => button.form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}))"));
            assertEquals(422, invalid.status());
            assertThat(page.getByRole(AriaRole.ALERT)).isVisible();
            assertThat(name).hasValue("   ");
            assertThat(description).hasValue("Draft retained after validation");
            assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Project created successfully."))).hasCount(0);
            assertEquals(unrelated, committedBoundaryState());

            name.fill(original);
            description.fill("Created through the browser");
            Response created = page.waitForResponse(r -> r.url().endsWith("/api/v1/projects") && r.request().method().equals("POST"),
                () -> button(page, "Save project").click());
            assertEquals(201, created.status());
            long project = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created.text()).get("id").asLong();
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(original).setExact(true))).isVisible();
            assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Project created successfully."))).hasText("Project created successfully.");
            assertThat(page.getByRole(AriaRole.PARAGRAPH).filter(new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("^Created through the browser$")))).isVisible();
            assertEquals(user, jdbc.queryForObject("SELECT created_by_user_id FROM projects WHERE id=?", Long.class, project));
            assertEquals(java.util.List.of(java.util.Map.of("user_id", user, "role", "gestor")),
                jdbc.queryForList("SELECT user_id,role FROM project_memberships WHERE project_id=?", project));
            page.reload();
            button(page, "Dashboard").click();
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
            assertDashboard(page, 3, 2, 0, 0);
            openBoundaryProject(page, original);
            assertThat(page.getByRole(AriaRole.PARAGRAPH).filter(new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("^Created through the browser$")))).isVisible();
            link(page, "Projects").click();
            String[][] searches = {{"  CAFÉ mixed%_  ", original}, {"cafe mixed%_", "Cafe MiXeD%_ " + suffix}, {"%_", ""}};
            for (String[] search : searches) {
                page.getByPlaceholder("Search by name", new Page.GetByPlaceholderOptions().setExact(true)).fill(search[0]);
                Response found = page.waitForResponse(r -> r.url().contains("/api/v1/projects?search=") && r.request().method().equals("GET"),
                    () -> button(page, "Search").click());
                assertEquals(200, found.status());
                assertThat(page.getByRole(AriaRole.ARTICLE)).hasCount(search[1].isEmpty() ? 2 : 1);
                if (!search[1].isEmpty()) assertThat(page.getByRole(AriaRole.ARTICLE).getByRole(AriaRole.HEADING)).hasText(search[1]);
                else assertThat(page.getByRole(AriaRole.ARTICLE).getByRole(AriaRole.HEADING)).hasText(new String[]{original, "Cafe MiXeD%_ " + suffix});
            }
            // Reset the search so deletion refresh checks exercise the complete project list.
            page.getByPlaceholder("Search by name").fill("");
            page.waitForResponse(r -> r.url().contains("/api/v1/projects?search=") && r.request().method().equals("GET"),
                () -> button(page, "Search").click());
            assertThat(page.getByRole(AriaRole.ARTICLE)).hasCount(3);
            openBoundaryProject(page, original);
            page.getByText("Edit or delete project", new Page.GetByTextOptions().setExact(true)).click();
            page.getByLabel("Name", new Page.GetByLabelOptions().setExact(true)).fill(edited);
            page.locator("details").getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Description"))).fill("Edited through the browser");
            Response updated = page.waitForResponse(r -> r.url().endsWith("/api/v1/projects/" + project) && r.request().method().equals("PATCH"),
                () -> button(page, "Save project").click());
            assertEquals(200, updated.status());
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(edited).setExact(true))).isVisible();
            assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Project updated successfully."))).hasText("Project updated successfully.");
            assertEquals(edited, jdbc.queryForObject("SELECT name FROM projects WHERE id=?", String.class, project));
            assertEquals("Edited through the browser", jdbc.queryForObject("SELECT description FROM projects WHERE id=?", String.class, project));
            page.reload();
            openBoundaryProject(page, edited);
            assertThat(page.getByRole(AriaRole.PARAGRAPH).filter(new Locator.FilterOptions().setHasText(java.util.regex.Pattern.compile("^Edited through the browser$")))).isVisible();
            seedProjectDependents(project, user);
            page.getByText("Edit or delete project", new Page.GetByTextOptions().setExact(true)).click();
            var beforeCancel = committedBoundaryState();
            int beforeWrites = writes.size();
            page.onceDialog(dialog -> {
                assertEquals("confirm", dialog.type());
                assertEquals("Delete this project and all associated data?", dialog.message());
                dialog.dismiss();
            });
            button(page, "Delete project").click();
            assertThat(button(page, "Delete project")).isEnabled();
            assertEquals(beforeWrites, writes.size(), "Cancellation must not send DELETE");
            assertEquals(beforeCancel, committedBoundaryState());
            page.onceDialog(Dialog::accept);
            Response deleted = page.waitForResponse(r -> r.url().endsWith("/api/v1/projects/" + project) && r.request().method().equals("DELETE"),
                () -> button(page, "Delete project").click());
            assertEquals(204, deleted.status());
            assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Project deleted successfully."))).hasText("Project deleted successfully.");
            assertThat(page.getByRole(AriaRole.ARTICLE)).hasCount(2);
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(edited).setExact(true))).hasCount(0);
            // Exact full-table equality proves all target dependents are gone and unrelated populated projects unchanged.
            assertEquals(unrelated, committedBoundaryState());
            page.reload();
            button(page, "Dashboard").click();
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
            assertDashboard(page, 2, 2, 0, 0);
            assertThat(page.locator(".cards").getByRole(AriaRole.PARAGRAPH)).hasText(new String[]{
                "To Do: 1 · In Progress: 0 · Completed: 0", "To Do: 1 · In Progress: 0 · Completed: 0"});
            link(page, "Projects").click();
            assertThat(page.getByRole(AriaRole.ARTICLE)).hasCount(2);
            assertThat(page.getByRole(AriaRole.ARTICLE).getByRole(AriaRole.HEADING)).hasText(
                new String[]{"Café MiXeDXY " + suffix, "Cafe MiXeD%_ " + suffix});
            assertEquals(unrelated, committedBoundaryState());
        });
    }

    private void seedProjectDependents(long project, long user) {
        long task = jdbc.queryForObject("INSERT INTO tasks(project_id,title) VALUES (?,'Project cleanup sentinel') RETURNING id", Long.class, project);
        jdbc.update("INSERT INTO task_assignments(task_id,user_id) VALUES (?,?)", task, user);
        jdbc.update("INSERT INTO task_comments(task_id,author_id,content) VALUES (?,?,'Cleanup comment')", task, user);
        jdbc.update("INSERT INTO task_history(task_id,actor_id,field,previous_value,new_value) VALUES (?,?,'status','IN_PROGRESS','TODO')", task, user);
    }

    @Test
    void taskCollaborationFiltersAssignmentsCommentsAndReload() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String password = "CollaborationOnly123!";
        String[] names = {"Collaboration Manager", "Collaboration Member"};
        String[] emails = {"collab-manager-" + suffix + "@example.org", "collab-member-" + suffix + "@example.org"};
        long[] users = new long[2];
        for (int i = 0; i < users.length; i++) users[i] = jdbc.queryForObject(
            "INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?) RETURNING id",
            Long.class, names[i], emails[i], passwords.encode(password));
        String projectName = "Collaboration " + suffix;
        long project = jdbc.queryForObject("INSERT INTO projects(name,created_by_user_id) VALUES (?,?) RETURNING id",
            Long.class, projectName, users[0]);
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'gestor'),(?,?,'membro')",
            project, users[0], project, users[1]);
        long task = jdbc.queryForObject("INSERT INTO tasks(project_id,title) VALUES (?,'Collaboration target') RETURNING id", Long.class, project);
        jdbc.update("INSERT INTO tasks(project_id,title,status,priority) VALUES (?,'Done sentinel','DONE','HIGH'),(?,'Todo sentinel','TODO','MEDIUM')",
            project, project);
        withBrowser("task-collaboration", (manager, baseUrl) -> {
            loginBoundaryUser(manager, baseUrl, emails[0], password);
            openBoundaryProject(manager, projectName);
            assertThat(manager.getByRole(AriaRole.ARTICLE)).hasCount(3);
            assertThat(manager.getByText("To Do: 2 · In Progress: 0 · Completed: 1", new Page.GetByTextOptions().setExact(true))).isVisible();
            openCollaborationTask(manager);
            awaitCollaborationMutation(manager, "/tasks/" + task + "/status", "PATCH", 200,
                () -> manager.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Status").setExact(true)).selectOption("DONE"));
            awaitCollaborationMutation(manager, "/tasks/" + task + "/priority", "PATCH", 200,
                () -> manager.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Priority").setExact(true)).selectOption("HIGH"));
            assertThat(manager.getByText("Status: Completed · Priority: High", new Page.GetByTextOptions().setExact(true))).isVisible();
            assertEquals(java.util.Map.of("status", "DONE", "priority", "HIGH"),
                jdbc.queryForMap("SELECT status,priority FROM tasks WHERE id=?", task));

            // Add two, repeat without duplicates, remove one, clear, then assign the member for authorized edits.
            for (int[] selected : new int[][]{{0, 1}, {0, 1}, {1}, {}, {1}}) {
                for (int i = 0; i < users.length; i++) {
                    final int index = i;
                    boolean checked = java.util.Arrays.stream(selected).anyMatch(value -> value == index);
                    manager.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(names[i]).setExact(true)).setChecked(checked);
                }
                awaitCollaborationMutation(manager, "/tasks/" + task + "/responsibles", "PUT", 200,
                    () -> button(manager, "Save assignees").click());
                var expected = java.util.Arrays.stream(selected).mapToObj(index -> users[index]).sorted().toList();
                assertEquals(expected, jdbc.queryForList("SELECT user_id FROM task_assignments WHERE task_id=? ORDER BY user_id", Long.class, task));
                for (int i = 0; i < users.length; i++) {
                    final int index = i;
                    boolean checked = java.util.Arrays.stream(selected).anyMatch(value -> value == index);
                    assertThat(manager.getByRole(AriaRole.PARAGRAPH).filter(new Locator.FilterOptions()
                        .setHasText(java.util.regex.Pattern.compile("^" + names[i] + "$")))).hasCount(checked ? 1 : 0);
                }
            }
            manager.reload();
            openBoundaryProject(manager, projectName);
            openCollaborationTask(manager);
            assertThat(manager.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(names[1]).setExact(true))).isChecked();
            assertThat(manager.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName(names[0]).setExact(true))).not().isChecked();
            assertThat(manager.getByText("Status: Completed · Priority: High", new Page.GetByTextOptions().setExact(true))).isVisible();

            try (BrowserContext memberContext = manager.context().browser().newContext()) {
                Page member = memberContext.newPage();
                loginBoundaryUser(member, baseUrl, emails[1], password);
                openBoundaryProject(member, projectName);
                openCollaborationTask(member);
                awaitCollaborationMutation(member, "/tasks/" + task + "/status", "PATCH", 200,
                    () -> member.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Status").setExact(true)).selectOption("IN_PROGRESS"));
                awaitCollaborationMutation(member, "/tasks/" + task + "/priority", "PATCH", 200,
                    () -> member.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Priority").setExact(true)).selectOption("LOW"));
                assertEquals(java.util.Map.of("status", "IN_PROGRESS", "priority", "LOW"),
                    jdbc.queryForMap("SELECT status,priority FROM tasks WHERE id=?", task));
                String comment = "Persisted collaboration comment " + suffix;
                member.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^New comment"))).fill(comment);
                java.time.Instant beforeComment = java.time.Instant.now();
                awaitCollaborationMutation(member, "/tasks/" + task + "/comments", "POST", 201,
                    () -> button(member, "Post comment").click());
                java.time.Instant afterComment = java.time.Instant.now();
                var row = jdbc.queryForMap("SELECT content,author_id,created_at FROM task_comments WHERE task_id=?", task);
                assertEquals(comment, row.get("content"));
                assertEquals(users[1], row.get("author_id"));
                java.time.Instant createdAt = ((java.sql.Timestamp) row.get("created_at")).toInstant();
                org.junit.jupiter.api.Assertions.assertFalse(createdAt.isBefore(beforeComment) || createdAt.isAfter(afterComment));
                String displayedDate = (String) member.evaluate("value => new Date(value).toLocaleString('pt-PT')", createdAt.toString());
                Locator commentCard = member.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText(comment));
                assertThat(commentCard).hasCount(1);
                assertThat(commentCard.locator("small")).hasText("Autor #" + users[1] + " · " + displayedDate);
                assertThat(member.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^New comment")))).hasValue("");
                member.reload();
                openBoundaryProject(member, projectName);
                openCollaborationTask(member);
                assertThat(member.getByText("Status: In Progress · Priority: Low", new Page.GetByTextOptions().setExact(true))).isVisible();
                assertThat(member.getByRole(AriaRole.PARAGRAPH).filter(new Locator.FilterOptions().setHasText("Collaboration Member"))).hasCount(1);
                assertThat(commentCard.locator("small")).hasText("Autor #" + users[1] + " · " + displayedDate);
                assertThat(commentCard).containsText(comment);
                link(member, "Back to project: " + projectName).click();
                assertThat(button(member, "Log out")).isEnabled();
                // Representative unfiltered, individual, combined and empty results; counts ignore status but respect priority.
                String[][] filters = {{"", "", "Todo sentinel|Done sentinel|Collaboration target", "1|1|1"},
                    {"IN_PROGRESS", "", "Collaboration target", "1|1|1"},
                    {"IN_PROGRESS", "HIGH", "", "0|0|1"},
                    {"", "LOW", "Collaboration target", "0|1|0"},
                    {"IN_PROGRESS", "LOW", "Collaboration target", "0|1|0"}};
                for (String[] filter : filters) {
                    member.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Status").setExact(true)).selectOption(filter[0]);
                    assertThat(button(member, "Log out")).isEnabled();
                    member.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Priority").setExact(true)).selectOption(filter[1]);
                    assertThat(button(member, "Log out")).isEnabled();
                    String[] titles = filter[2].isEmpty() ? new String[0] : filter[2].split("\\|");
                    assertThat(member.getByRole(AriaRole.ARTICLE)).hasCount(titles.length);
                    if (titles.length > 0) assertThat(member.getByRole(AriaRole.ARTICLE).getByRole(AriaRole.HEADING)).hasText(titles);
                    else assertThat(member.getByText("No tasks match the selected filters.", new Page.GetByTextOptions().setExact(true))).isVisible();
                    String[] counts = filter[3].split("\\|");
                    assertThat(member.getByText("To Do: " + counts[0] + " · In Progress: " + counts[1] + " · Completed: " + counts[2],
                        new Page.GetByTextOptions().setExact(true))).isVisible();
                }
                member.reload();
                button(member, "Dashboard").click();
                assertThat(member.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
                assertDashboard(member, 1, 1, 1, 1);
                assertThat(member.getByText("To Do: 1 · In Progress: 1 · Completed: 1", new Page.GetByTextOptions().setExact(true))).hasCount(1);
                assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM task_comments WHERE task_id=?", Long.class, task));
                assertEquals(java.util.List.of(users[1]), jdbc.queryForList("SELECT user_id FROM task_assignments WHERE task_id=?", Long.class, task));
            }
        });
    }

    private static void openCollaborationTask(Page page) {
        page.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText("Collaboration target"))
            .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Open task: Collaboration target").setExact(true)).click();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Collaboration target").setExact(true).setLevel(2))).isVisible();
        assertThat(button(page, "Log out")).isEnabled();
    }

    @Test
    void taskValidationDraftsBusyAndDeletion() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String email = "resilience-" + suffix + "@example.org", password = "ResilienceOnly123!";
        String projectName = "Resilience " + suffix, title = "Resilient task " + suffix;
        long user = jdbc.queryForObject("INSERT INTO app_users(name,email,password_hash) VALUES (?,?,?) RETURNING id",
            Long.class, "Resilience Manager", email, passwords.encode(password));
        long project = jdbc.queryForObject("INSERT INTO projects(name,created_by_user_id) VALUES (?,?) RETURNING id",
            Long.class, projectName, user);
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'gestor')", project, user);
        seedProjectDependents(project, user); // Populated same-project sentinel must survive task deletion.
        long otherProject = jdbc.queryForObject("INSERT INTO projects(name,created_by_user_id) VALUES (?,?) RETURNING id",
            Long.class, "Unrelated " + suffix, user);
        jdbc.update("INSERT INTO project_memberships(project_id,user_id,role) VALUES (?,?,'gestor')", otherProject, user);
        seedProjectDependents(otherProject, user);
        var baseline = committedBoundaryState();
        withBrowser("task-validation-deletion", (page, baseUrl) -> {
            loginBoundaryUser(page, baseUrl, email, password);
            openBoundaryProject(page, projectName);
            Locator taskTitle = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Title").setExact(true));
            Locator description = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Description"))).last();
            java.util.List<String> writes = new java.util.ArrayList<>();
            page.onRequest(request -> {
                if (request.url().contains("/api/v1/") && !request.method().equals("GET"))
                    writes.add(request.method() + " " + request.url());
            });
            description.fill("Retained create draft");
            button(page, "Save task").click();
            assertEquals(true, taskTitle.evaluate("element => element.validity.valueMissing"));
            assertEquals(0, writes.size());
            assertEquals(baseline, committedBoundaryState());
            taskTitle.fill("   ");
            button(page, "Save task").click();
            assertEquals(true, taskTitle.evaluate("element => element.validity.customError"));
            assertEquals(0, writes.size());
            // Bypass native validation to exercise server validation without weakening the form.
            Response invalid = page.waitForResponse(r -> r.url().endsWith("/projects/" + project + "/tasks") && r.request().method().equals("POST"),
                () -> button(page, "Save task").evaluate("button => button.form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}))"));
            assertEquals(422, invalid.status());
            assertThat(button(page, "Save task")).isEnabled();
            assertThat(page.getByRole(AriaRole.ALERT)).containsText("title: Invalid title or description.");
            assertThat(taskTitle).hasValue("   ");
            assertThat(description).hasValue("Retained create draft");
            assertThat(page.getByRole(AriaRole.STATUS)).hasCount(0);
            assertEquals(baseline, committedBoundaryState());

            // Hold only this request, then forward it unchanged to the real backend.
            String createUrl = "**/api/v1/projects/" + project + "/tasks";
            var held = new java.util.concurrent.atomic.AtomicReference<Route>();
            java.util.function.Consumer<Route> hold = route -> {
                if (route.request().method().equals("POST")) held.set(route);
                else route.resume();
            };
            page.route(createUrl, hold);
            taskTitle.fill(title);
            int beforeCreate = writes.size();
            // Two submissions in the same event turn exercise singleFlight, not just disabled HTML.
            button(page, "Save task").evaluate("button => { button.form.requestSubmit(); button.form.requestSubmit(); }");
            page.waitForCondition(() -> held.get() != null);
            assertThat(button(page, "Save task")).isDisabled();
            assertThat(taskTitle).isDisabled();
            assertThat(description).isDisabled();
            assertThat(button(page, "Log out")).isEnabled(); // Logout must remain available during pending writes.
            assertThat(page.getByRole(AriaRole.STATUS)).hasText("Processing…");
            button(page, "Save task").evaluate("button => button.form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}))");
            assertEquals(beforeCreate + 1, writes.size(), "Only one POST while pending");
            assertEquals(baseline, committedBoundaryState());
            Response created = page.waitForResponse(r -> r.url().endsWith("/projects/" + project + "/tasks") && r.request().method().equals("POST"),
                () -> held.get().resume());
            page.unroute(createUrl, hold);
            assertEquals(201, created.status());
            long task = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created.text()).get("id").asLong();
            assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Task created successfully."))).hasText("Task created successfully.");
            assertThat(button(page, "Save task")).isEnabled();
            assertThat(page.getByRole(AriaRole.ALERT)).hasCount(0);
            assertEquals(beforeCreate + 1, writes.size());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tasks WHERE title=?", Integer.class, title));
            page.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText(title))
                .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^Open task: "))).click();
            assertThat(button(page, "Delete task")).isEnabled();
            var beforeEdit = committedBoundaryState();
            taskTitle.fill("   ");
            description.fill("Retained edit draft");
            Response editInvalid = page.waitForResponse(r -> r.url().endsWith("/tasks/" + task) && r.request().method().equals("PATCH"),
                () -> button(page, "Save task").evaluate("button => button.form.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}))"));
            assertEquals(422, editInvalid.status());
            assertThat(button(page, "Save task")).isEnabled();
            assertThat(page.getByRole(AriaRole.ALERT)).containsText("title: Title must contain 3 to 150 characters.");
            assertThat(taskTitle).hasValue("   ");
            assertThat(description).hasValue("Retained edit draft");
            assertThat(page.getByRole(AriaRole.STATUS)).hasCount(0);
            assertEquals(beforeEdit, committedBoundaryState());
            taskTitle.fill(title);
            Response edited = page.waitForResponse(r -> r.url().endsWith("/tasks/" + task) && r.request().method().equals("PATCH"),
                () -> button(page, "Save task").click());
            assertEquals(200, edited.status());
            assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Task updated successfully."))).hasText("Task updated successfully.");
            assertThat(page.getByRole(AriaRole.ALERT)).hasCount(0);
            assertEquals("Retained edit draft", jdbc.queryForObject("SELECT description FROM tasks WHERE id=?", String.class, task));

            Locator comment = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile("^New comment")));
            comment.fill("Comment draft survives service failure");
            var beforeFailure = committedBoundaryState();
            String commentUrl = "**/api/v1/tasks/" + task + "/comments";
            java.util.function.Consumer<Route> unavailable = route -> {
                if (route.request().method().equals("POST")) route.fulfill(new Route.FulfillOptions().setStatus(503)
                    .setContentType("application/problem+json").setBody("{\"detail\":\"E2E service temporarily unavailable\"}"));
                else route.resume();
            };
            page.route(commentUrl, unavailable);
            Response failed = page.waitForResponse(r -> r.url().endsWith("/tasks/" + task + "/comments") && r.request().method().equals("POST"),
                () -> button(page, "Post comment").click());
            assertEquals(503, failed.status());
            assertThat(button(page, "Post comment")).isEnabled();
            assertThat(page.getByRole(AriaRole.ALERT)).hasText("E2E service temporarily unavailable");
            assertThat(comment).hasValue("Comment draft survives service failure");
            assertThat(page.getByRole(AriaRole.STATUS)).hasCount(0);
            assertEquals(beforeFailure, committedBoundaryState());
            page.unroute(commentUrl, unavailable);
            awaitCollaborationMutation(page, "/tasks/" + task + "/comments", "POST", 201, () -> button(page, "Post comment").click());
            assertThat(comment).hasValue("");
            assertThat(page.getByRole(AriaRole.ALERT)).hasCount(0);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM task_comments WHERE task_id=? AND content=?", Integer.class,
                task, "Comment draft survives service failure"));
            awaitCollaborationMutation(page, "/tasks/" + task + "/responsibles", "PUT", 200, () -> {
                page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("Resilience Manager")).check();
                button(page, "Save assignees").click();
            });
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM task_assignments WHERE task_id=?", Integer.class, task));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM task_history WHERE task_id=?", Integer.class, task));
            var beforeCancel = committedBoundaryState();
            int beforeDelete = writes.size();
            page.onceDialog(dialog -> {
                assertEquals("confirm", dialog.type());
                assertEquals("Delete this task?", dialog.message());
                dialog.dismiss();
            });
            button(page, "Delete task").click();
            assertThat(button(page, "Delete task")).isEnabled();
            assertEquals(beforeDelete, writes.size());
            assertEquals(beforeCancel, committedBoundaryState());
            page.onceDialog(Dialog::accept);
            Response deleted = page.waitForResponse(r -> r.url().endsWith("/tasks/" + task) && r.request().method().equals("DELETE"),
                () -> button(page, "Delete task").click());
            assertEquals(204, deleted.status());
            assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Task deleted successfully."))).hasText("Task deleted successfully.");
            assertThat(button(page, "Save task")).isEnabled();
            assertEquals(beforeDelete + 1, writes.size());
            assertThat(page.getByRole(AriaRole.ARTICLE)).hasCount(1);
            assertThat(page.getByRole(AriaRole.ARTICLE).getByRole(AriaRole.HEADING)).hasText("Project cleanup sentinel");
            // Exact seven-table equality: deleted task + all dependents gone, both sentinels untouched.
            assertEquals(baseline, committedBoundaryState());
            assertEquals(404, page.context().request().get(baseUrl + "/api/v1/tasks/" + task).status());
            page.reload();
            button(page, "Dashboard").click();
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
            assertDashboard(page, 2, 2, 0, 0);
            assertThat(page.locator(".cards").getByRole(AriaRole.PARAGRAPH)).hasText(new String[]{
                "To Do: 1 · In Progress: 0 · Completed: 0", "To Do: 1 · In Progress: 0 · Completed: 0"});
            openBoundaryProject(page, projectName);
            assertThat(page.getByRole(AriaRole.ARTICLE)).hasCount(1);
            assertThat(page.getByRole(AriaRole.ARTICLE).getByRole(AriaRole.HEADING)).hasText("Project cleanup sentinel");
            assertThat(page.getByText("To Do: 1 · In Progress: 0 · Completed: 0", new Page.GetByTextOptions().setExact(true))).isVisible();
            assertEquals(baseline, committedBoundaryState());
        });
    }

    private static void awaitCollaborationMutation(Page page, String path, String method, int status, Runnable action) {
        Response response = page.waitForResponse(r -> r.url().endsWith("/api/v1" + path) && r.request().method().equals(method), action);
        assertEquals(status, response.status());
        assertThat(button(page, "Log out")).isEnabled();
        assertThat(page.getByRole(AriaRole.STATUS).filter(new Locator.FilterOptions().setHasText("Changes saved successfully."))).isVisible();
    }

    private java.util.Map<String, String> committedBoundaryState() {
        var state = new java.util.LinkedHashMap<String, String>();
        for (String table : java.util.List.of("app_users", "projects", "project_memberships", "tasks",
                "task_assignments", "task_comments", "task_history")) {
            state.put(table, jdbc.queryForObject("SELECT COALESCE(jsonb_agg(to_jsonb(r) ORDER BY to_jsonb(r)::text), '[]'::jsonb)::text FROM "
                + table + " r", String.class));
        }
        return state;
    }

    private static void verifySecurityCenter(Page page) throws Exception {
        button(page, "Dashboard").click();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
        button(page, "Security Center").click();
        Locator security = page.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName("Security Center").setExact(true));
        assertThat(security.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Security Center"))).isVisible();
        button(page, "Daily Report Agent").click();
        button(page, "Generate daily report").click();
        assertThat(security.getByRole(AriaRole.STATUS)).hasText("Report generated successfully.");
        assertThat(security.locator("pre")).containsText("Source: Sample events (demo)");
        assertThat(security.locator("pre")).containsText("Total events: 9");
        Download download = page.waitForDownload(() -> button(page, "Download .txt report").click());
        assertEquals("matriz-security-report.txt", download.suggestedFilename());
        String report = Files.readString(download.path());
        org.junit.jupiter.api.Assertions.assertTrue(report.contains("Total events: 9"));
        org.junit.jupiter.api.Assertions.assertTrue(report.contains("Repeated authentication failures"));
        button(page, "Event data").click();
        page.getByLabel("Import JSON events").fill("{}");
        button(page, "Analyze JSON").click();
        assertThat(security.getByRole(AriaRole.STATUS)).hasText(
            "Import failed: Expected an array (max 500) with time, type, source and detail.");
        assertThat(security.getByRole(AriaRole.ROW)).hasCount(10);
        page.getByLabel("Import JSON events").fill(
            "[{\"time\":\"12:00\",\"type\":\"api_error\",\"source\":\"Browser regression\",\"detail\":\"Sanitized test event\"}]");
        button(page, "Analyze JSON").click();
        assertThat(security.getByRole(AriaRole.STATUS)).hasText("Events loaded locally. No data was sent to a server.");
        assertThat(security.getByRole(AriaRole.ROW)).hasCount(2);
        assertThat(security.getByRole(AriaRole.CELL, new Locator.GetByRoleOptions().setName("Sanitized test event").setExact(true))).isVisible();
        button(page, "Daily Report Agent").click();
        assertThat(security.locator("pre")).hasCount(0);
        button(page, "Generate daily report").click();
        assertThat(security.locator("pre")).containsText("Source: User-provided events (not independently verified)");
        assertThat(security.locator("pre")).containsText("Total events: 1");
        // Navigation must dismiss the local panel, including when returning with browser Back.
        link(page, "Projects").click();
        assertThat(security).hasCount(0);
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Projects").setExact(true))).isVisible();
        page.goBack();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
        assertThat(security).hasCount(0);
    }

    private static void assertDashboard(Page page, int projects, int todo, int inProgress, int completed) {
        String[] labels = {"Projects", "To Do", "In Progress", "Completed"};
        int[] totals = {projects, todo, inProgress, completed};
        for (int i = 0; i < labels.length; i++) {
            Locator stat = page.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions()
                .setHas(page.getByText(labels[i], new Page.GetByTextOptions().setExact(true))));
            assertThat(stat.locator("strong")).hasText(Integer.toString(totals[i]));
        }
        assertThat(page.locator(".cards").getByRole(AriaRole.ARTICLE)).hasCount(projects);
    }

    private static Locator link(Page page, String name) {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private static Locator button(Page page, String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private static void loginBoundaryUser(Page page, String baseUrl, String email, String password) {
        page.navigate(baseUrl);
        page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true)).fill(email);
        page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true)).fill(password);
        button(page, "Sign in").click();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true))).isVisible();
    }

    private static void openBoundaryProject(Page page, String name) {
        link(page, "Projects").click();
        page.getByRole(AriaRole.ARTICLE).filter(new Locator.FilterOptions().setHasText(name))
            .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Open project: " + name).setExact(true)).click();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name).setExact(true).setLevel(2))).isVisible();
        assertThat(button(page, "Log out")).isEnabled();
    }

    private static int boundaryMutation(BrowserContext context, String baseUrl, String method, String path, Object body) throws Exception {
        APIResponse csrf = context.request().get(baseUrl + "/api/v1/auth/csrf");
        assertEquals(200, csrf.status());
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(csrf.text()).get("token").asText();
        var options = com.microsoft.playwright.options.RequestOptions.create().setMethod(method).setHeader("X-CSRF-TOKEN", token);
        if (body != null) options.setData(body);
        return context.request().fetch(baseUrl + "/api/v1" + path, options).status();
    }

    @FunctionalInterface
    private interface BrowserFlow { void run(Page page, String baseUrl) throws Exception; }

    private void withBrowser(String artifactName, BrowserFlow flow) throws Exception {
        Path frontend = Path.of("../frontend").toAbsolutePath().normalize();
        Path artifacts = Path.of("target/e2e-artifacts", artifactName);
        Files.createDirectories(artifacts);
        Process vite = new ProcessBuilder(System.getProperty("e2e.node", "node"), "e2e/server.mjs", Integer.toString(backendPort))
            .directory(frontend.toFile()).redirectErrorStream(true).start();
        CompletableFuture<String> ready = new CompletableFuture<>();
        Thread output = new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(vite.getInputStream()));
                 var log = Files.newBufferedWriter(artifacts.resolve("vite.log"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.write(line); log.newLine(); log.flush();
                    if (line.startsWith("MATRIZ_E2E_URL=")) ready.complete(line.substring("MATRIZ_E2E_URL=".length()));
                }
                ready.completeExceptionally(new IllegalStateException("Vite exited; see target/e2e-artifacts/vite.log"));
            } catch (Exception error) { ready.completeExceptionally(error); }
        }, "e2e-vite-output");
        output.setDaemon(true);
        output.start();
        try {
            String baseUrl = ready.get(60, TimeUnit.SECONDS);
            try (Playwright playwright = Playwright.create();
                 Browser browser = playwright.chromium().launch();
                 BrowserContext context = browser.newContext()) {
                context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
                Page page = context.newPage();
                page.setDefaultTimeout(Duration.ofSeconds(15).toMillis());
                try {
                    flow.run(page, baseUrl);
                } catch (Throwable failure) {
                    page.screenshot(new Page.ScreenshotOptions().setPath(artifacts.resolve("failure.png")));
                    context.tracing().stop(new Tracing.StopOptions().setPath(artifacts.resolve("failure-trace.zip")));
                    throw failure;
                }
            }
        } finally {
            vite.destroy();
            if (!vite.waitFor(10, TimeUnit.SECONDS)) vite.destroyForcibly();
            output.join(5000);
        }
    }
}
