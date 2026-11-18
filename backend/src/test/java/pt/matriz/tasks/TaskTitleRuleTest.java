package pt.matriz.tasks;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Isolated business-rule tests: direct public call, no Spring context, HTTP or database. */
class TaskTitleRuleTest {
    static Stream<Arguments> unicodeDescriptions() {
        return Stream.of(
            Arguments.of("😀", true),
            Arguments.of("😀".repeat(2000), true),
            Arguments.of("a" + "😀".repeat(1999), true),
            Arguments.of("😀".repeat(2001), false),
            Arguments.of("a".repeat(2001), false),
            Arguments.of("", false), Arguments.of(" \t ", false), Arguments.of(null, true));
    }

    @ParameterizedTest @MethodSource("unicodeDescriptions")
    void unicodeDescriptionsAndComments(String input, boolean valid) throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("unit@example.test");
        when(jdbc.query(contains("FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString()))
            .thenReturn(List.of(1L));
        when(jdbc.query(contains("FROM project_memberships"), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(10L), eq(1L)))
            .thenReturn(List.of("gestor"));
        when(jdbc.query(contains("SELECT t.project_id, pm.role"), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(), eq(20L), eq(1L)))
            .thenAnswer(call -> {
                RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(10L); when(row.getString(2)).thenReturn("gestor");
                return List.of(mapper.mapRow(row, 0));
            });
        when(jdbc.query(startsWith("SELECT id,project_id,title"), org.mockito.ArgumentMatchers.<RowMapper<TaskController.Task>>any(), eq(20L)))
            .thenReturn(List.of(new TaskController.Task(20L, 10L, "Original", "Retained", "TODO", "MEDIUM", Instant.EPOCH, List.of())));
        when(jdbc.queryForObject(startsWith("INSERT INTO tasks"), eq(Long.class), eq(10L), eq("Valid title"), nullable(String.class))).thenReturn(20L);
        when(jdbc.queryForObject(startsWith("INSERT INTO task_comments"), eq(Long.class), eq(20L), eq(1L), anyString())).thenReturn(30L);
        TaskController controller = new TaskController(jdbc);
        String padded = input == null ? null : "  " + input + "  ";
        String expected = input == null ? null : input.strip();
        var body = new HashMap<String, Object>();
        body.put("title", "Valid title"); body.put("description", padded);
        assertEquals(valid ? 201 : 422, controller.create(10L, body, auth).getStatusCode().value());
        if (valid) verify(jdbc).queryForObject(startsWith("INSERT INTO tasks"), eq(Long.class), eq(10L), eq("Valid title"), eq(expected));
        else assertNoWrites(jdbc);
        clearInvocations(jdbc);
        body.remove("title");
        assertEquals(valid ? 200 : 422, controller.update(20L, body, auth).getStatusCode().value());
        if (valid) verify(jdbc).update("UPDATE tasks SET title=CASE WHEN ? THEN ? ELSE title END, description=CASE WHEN ? THEN ? ELSE description END WHERE id=?", false, null, true, expected, 20L);
        else assertNoWrites(jdbc);
        clearInvocations(jdbc);
        controller.update(20L, Map.of("title", "Renamed"), auth);
        verify(jdbc).update("UPDATE tasks SET title=CASE WHEN ? THEN ? ELSE title END, description=CASE WHEN ? THEN ? ELSE description END WHERE id=?", true, "Renamed", false, null, 20L);
        clearInvocations(jdbc);
        body.clear(); body.put("content", padded);
        boolean validComment = valid && input != null;
        assertEquals(validComment ? 201 : 422, controller.addComment(20L, body, auth).getStatusCode().value());
        if (validComment) verify(jdbc).queryForObject(startsWith("INSERT INTO task_comments"), eq(Long.class), eq(20L), eq(1L), eq(expected));
        else assertNoWrites(jdbc);
    }

    private void assertNoWrites(JdbcTemplate jdbc) {
        assertTrue(mockingDetails(jdbc).getInvocations().stream().noneMatch(call -> {
            Object[] args = call.getArguments();
            return args.length > 0 && args[0] instanceof String sql && sql.stripLeading().matches("(?s)(INSERT|UPDATE|DELETE).*" );
        }), "Rejected input must not write through JDBC");
    }

    static Stream<Arguments> titles() {
        return Stream.of(
            Arguments.of("supplementary below minimum", " \t" + "\uD83D\uDE00".repeat(2) + "\n ", null),
            Arguments.of("supplementary minimum", " \t" + "\uD83D\uDE00".repeat(3) + "\n ", "\uD83D\uDE00".repeat(3)),
            Arguments.of("supplementary maximum", " \t" + "\uD83D\uDE00".repeat(150) + "\n ", "\uD83D\uDE00".repeat(150)),
            Arguments.of("supplementary above maximum", " \t" + "\uD83D\uDE00".repeat(151) + "\n ", null),
            Arguments.of("ordinary valid title", "A valid task title", "A valid task title"),
            Arguments.of("minimum", "a".repeat(3), "a".repeat(3)),
            Arguments.of("maximum", "a".repeat(150), "a".repeat(150)),
            Arguments.of("trimmed minimum", " \t" + "a".repeat(3) + "\n ", "a".repeat(3)),
            Arguments.of("trimmed maximum", " \t" + "a".repeat(150) + "\n ", "a".repeat(150)),
            Arguments.of("below minimum", "a".repeat(2), null),
            Arguments.of("above maximum", "a".repeat(151), null),
            Arguments.of("padding cannot reach minimum", "  ab  ", null),
            Arguments.of("padding cannot hide excess", "  " + "a".repeat(151) + "  ", null),
            Arguments.of("empty", "", null),
            Arguments.of("blank", " \t\n ", null),
            Arguments.of("explicit null", null, null)
        );
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {2, 3, 150, 151})
    void unicodeTitleBoundariesOnUpdate(int count) throws Exception {
        String title = "\uD83D\uDE00".repeat(count);
        boolean valid = count >= 3 && count <= 150;
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("unit@example.test");
        when(jdbc.query(contains("FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq("unit@example.test")))
            .thenReturn(List.of(1L));
        when(jdbc.query(contains("SELECT t.project_id, pm.role"), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(), eq(20L), eq(1L)))
            .thenAnswer(call -> {
                RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong(1)).thenReturn(10L);
                when(row.getString(2)).thenReturn("gestor");
                return List.of(mapper.mapRow(row, 0));
            });
        when(jdbc.query(startsWith("SELECT id,project_id,title"), org.mockito.ArgumentMatchers.<RowMapper<TaskController.Task>>any(), eq(20L)))
            .thenReturn(List.of(new TaskController.Task(20L, 10L, "Original", "Retained", "TODO", "MEDIUM", Instant.EPOCH, List.of())));
        var response = new TaskController(jdbc).update(20L, Map.of("title", "  " + title + "  "), auth);
        if (valid) {
            assertEquals(200, response.getStatusCode().value());
            verify(jdbc).update("UPDATE tasks SET title=CASE WHEN ? THEN ? ELSE title END, description=CASE WHEN ? THEN ? ELSE description END WHERE id=?", true, title, false, null, 20L);
        } else {
            assertEquals(422, response.getStatusCode().value());
            ProblemDetail problem = assertInstanceOf(ProblemDetail.class, response.getBody());
            assertEquals("title", ((Map<?, ?>)((List<?>)problem.getProperties().get("errors")).get(0)).get("field"));
            verify(jdbc).query(contains("FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq("unit@example.test"));
            verify(jdbc).query(contains("SELECT t.project_id, pm.role"), org.mockito.ArgumentMatchers.<RowMapper<Object>>any(), eq(20L), eq(1L));
            verifyNoMoreInteractions(jdbc);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("titles")
    void creationAppliesTrimmedTitleLimits(String scenario, String input, String normalized) {
        // Reuse TaskControllerTest's authenticated-user and direct-membership JDBC pattern.
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("unit@example.test");
        when(jdbc.query(contains("FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq("unit@example.test")))
            .thenReturn(List.of(1L));
        when(jdbc.query(contains("FROM project_memberships"), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(10L), eq(1L)))
            .thenReturn(List.of("gestor"));
        var body = new HashMap<String, Object>();
        body.put("title", input);
        if (normalized != null) {
            when(jdbc.queryForObject(startsWith("INSERT INTO tasks"), eq(Long.class), eq(10L), eq(normalized), isNull()))
                .thenReturn(20L);
            when(jdbc.query(startsWith("SELECT id,project_id,title"), org.mockito.ArgumentMatchers.<RowMapper<TaskController.Task>>any(), eq(20L)))
                .thenReturn(List.of(new TaskController.Task(20L, 10L, normalized, null, "TODO", "MEDIUM", Instant.EPOCH, List.of())));
        }

        var response = new TaskController(jdbc).create(10L, body, auth);
        if (normalized != null) {
            assertEquals(201, response.getStatusCode().value());
            assertEquals("/api/v1/tasks/20", response.getHeaders().getLocation().toString());
            assertEquals(normalized, assertInstanceOf(TaskController.Task.class, response.getBody()).title());
            verify(jdbc).queryForObject("INSERT INTO tasks(project_id,title,description) VALUES (?,?,?) RETURNING id",
                Long.class, 10L, normalized, null);
            verify(jdbc).update("DELETE FROM task_assignments WHERE task_id=?", 20L);
        } else {
            assertEquals(422, response.getStatusCode().value());
            ProblemDetail problem = assertInstanceOf(ProblemDetail.class, response.getBody());
            assertEquals(List.of(Map.of("field", "title", "message", "Invalid title or description.")), problem.getProperties().get("errors"));
            // Only identity/access reads are allowed: no INSERT (queryForObject) or dependent writes.
            verify(jdbc).query(contains("FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq("unit@example.test"));
            verify(jdbc).query(contains("FROM project_memberships"), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), eq(10L), eq(1L));
            verifyNoMoreInteractions(jdbc);
        }
    }
}
