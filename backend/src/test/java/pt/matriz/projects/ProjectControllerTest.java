package pt.matriz.projects;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.matriz.auth.SecurityConfig;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "manager@example.org")
class ProjectControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate jdbc;
    private final ProjectController.Project project = new ProjectController.Project(10L, "Original", "Existing description", 7L);

    private void user() {
        when(jdbc.query(contains("SELECT id FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString()))
            .thenReturn(List.of(1L));
    }
    private void access(boolean member, boolean manager) {
        user();
        when(jdbc.queryForObject(contains("FROM project_memberships"), eq(Integer.class), eq(10L), eq(1L)))
            .thenAnswer(call -> call.getArgument(0, String.class).contains("role = 'gestor'") ? (manager ? 1 : 0) : (member ? 1 : 0));
    }
    private void patchResult(ProjectController.Project result) {
        when(jdbc.query(contains("UPDATE projects"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            anyBoolean(), nullable(String.class), anyBoolean(), nullable(String.class), eq(10L))).thenReturn(List.of(result));
    }
    private org.springframework.test.web.servlet.ResultActions patchBody(String body) throws Exception {
        return mvc.perform(patch("/api/v1/projects/10").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond())
            .with(csrf()).contentType("application/json").content(body));
    }

    @Test void listsProjectsWithExactEnvelopeAndSchema() throws Exception {
        user();
        when(jdbc.query(contains("FROM projects p"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(), eq(1L), anyString()))
            .thenReturn(List.of(project));
        mvc.perform(get("/api/v1/projects").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk()).andExpect(content().json("""
                {"items":[{"id":10,"name":"Original","description":"Existing description","createdByUserId":7}],"totalItems":1}
                """, true));
        verify(jdbc).query(argThat(sql -> sql.contains("ORDER BY p.id DESC") && !sql.contains("created_at")),
            org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(), eq(1L), eq("%%"));
    }
    @Test void emptyListHasEnvelope() throws Exception {
        user();
        mvc.perform(get("/api/v1/projects").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk()).andExpect(content().json("{\"items\":[],\"totalItems\":0}", true));
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource(value = {
        "  Matriz  |%Matriz%", "%|%!%%", "_|%!_%", "!|%!!%", "\\|%\\%", "  a!%_\\b  |%a!!!%!_\\b%"
    }, delimiter = '|', ignoreLeadingAndTrailingWhitespace = false)
    void searchUsesTrimmedLiteralSubstring(String search, String pattern) throws Exception {
        user();
        mvc.perform(get("/api/v1/projects").param("search", search)
                .sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk());
        verify(jdbc).query(argThat(sql -> sql.contains("p.name ILIKE ? ESCAPE '!'") && sql.contains("pm.user_id = ?")),
            org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(), eq(1L), eq(pattern));
    }
    @ParameterizedTest @ValueSource(strings = {"", "   "})
    void emptySearchDoesNotFilter(String search) throws Exception {
        user();
        mvc.perform(get("/api/v1/projects").param("search", search)
                .sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk());
        verify(jdbc).query(contains("FROM projects p"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(), eq(1L), eq("%%"));
    }
    @Test void searchLimitAppliesAfterTrimAndBeforeEscaping() throws Exception {
        user();
        String search = "%".repeat(100);
        mvc.perform(get("/api/v1/projects").param("search", "  " + search + "  ")
                .sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk());
        verify(jdbc).query(contains("FROM projects p"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            eq(1L), eq("%" + "!%".repeat(100) + "%"));
    }
    @Test void overlongSearchIsRejectedBeforeProjectQuery() throws Exception {
        user();
        mvc.perform(get("/api/v1/projects").param("search", "  " + "x".repeat(101) + "  ")
                .sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isBadRequest());
        verify(jdbc, never()).query(contains("FROM projects p"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            anyLong(), anyString());
    }
    @Test void searchLimitCountsUnicodeCharactersRatherThanUtf16Units() throws Exception {
        user();
        String search = "\uD83D\uDE00".repeat(100);
        mvc.perform(get("/api/v1/projects").param("search", search)
                .sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk());
        verify(jdbc).query(contains("FROM projects p"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            eq(1L), eq("%" + search + "%"));
    }
    @Test void readsExactProjectSchema() throws Exception {
        user();
        when(jdbc.query(contains("FROM projects p"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(), eq(10L), eq(1L)))
            .thenReturn(List.of(project));
        mvc.perform(get("/api/v1/projects/10").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk()).andExpect(content().json("{\"id\":10,\"name\":\"Original\",\"description\":\"Existing description\",\"createdByUserId\":7}", true));
    }
    @ParameterizedTest @ValueSource(ints = {2, 3, 100, 101})
    void unicodeNameBoundariesOnCreateAndPatch(int count) throws Exception {
        String name = "\uD83D\uDE00".repeat(count);
        String body = "{\"name\":\"  " + name + "  \"}";
        boolean valid = count >= 3 && count <= 100;
        user();
        if (valid) when(jdbc.queryForObject(contains("INSERT INTO projects"), eq(Long.class), eq(name), isNull(), eq(1L)))
            .thenReturn(10L);
        var creation = mvc.perform(post("/api/v1/projects").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond())
            .with(csrf()).contentType("application/json").content(body));
        if (valid) {
            creation.andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(name));
            verify(jdbc).queryForObject(contains("INSERT INTO projects"), eq(Long.class), eq(name), isNull(), eq(1L));
        } else {
            creation.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors[0].field").value("name"));
            verify(jdbc).query(contains("SELECT id FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString());
            verifyNoMoreInteractions(jdbc);
        }
        clearInvocations(jdbc);
        access(true, true);
        if (valid) patchResult(new ProjectController.Project(10L, name, project.description(), 7L));
        var patch = patchBody(body);
        if (valid) {
            patch.andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name));
            verify(jdbc).query(contains("UPDATE projects"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
                eq(true), eq(name), eq(false), isNull(), eq(10L));
        } else {
            patch.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors[0].field").value("name"));
            verify(jdbc).query(contains("SELECT id FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString());
            verify(jdbc, times(2)).queryForObject(contains("FROM project_memberships"), eq(Integer.class), eq(10L), eq(1L));
            verifyNoMoreInteractions(jdbc); // In particular no UPDATE query or other write.
        }
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> unicodeDescriptions() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("😀", true),
            org.junit.jupiter.params.provider.Arguments.of("😀".repeat(1000), true),
            org.junit.jupiter.params.provider.Arguments.of("a" + "😀".repeat(999), true),
            org.junit.jupiter.params.provider.Arguments.of("😀".repeat(1001), false),
            org.junit.jupiter.params.provider.Arguments.of("a".repeat(1001), false),
            org.junit.jupiter.params.provider.Arguments.of("", false),
            org.junit.jupiter.params.provider.Arguments.of(" \t ", false),
            org.junit.jupiter.params.provider.Arguments.of(null, true));
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("unicodeDescriptions")
    void unicodeDescriptionCreateAndPatch(String input, boolean valid) throws Exception {
        user();
        String expected = input == null ? null : input.strip();
        String padded = input == null ? null : "  " + input + "  ";
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var body = new java.util.HashMap<String, Object>();
        body.put("name", "Project"); body.put("description", padded);
        when(jdbc.queryForObject(contains("INSERT INTO projects"), eq(Long.class), eq("Project"), nullable(String.class), eq(1L))).thenReturn(10L);
        var creation = mvc.perform(post("/api/v1/projects").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond())
            .with(csrf()).contentType("application/json").content(json.writeValueAsString(body)));
        if (valid) {
            creation.andExpect(status().isCreated());
            verify(jdbc).queryForObject(contains("INSERT INTO projects"), eq(Long.class), eq("Project"), eq(expected), eq(1L));
        } else {
            creation.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors[0].field").value("description"));
            verify(jdbc).query(contains("SELECT id FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString());
            verifyNoMoreInteractions(jdbc);
        }
        clearInvocations(jdbc);
        access(true, true);
        patchResult(new ProjectController.Project(10L, "Original", expected, 7L));
        body.remove("name");
        var result = patchBody(json.writeValueAsString(body));
        if (valid) {
            result.andExpect(status().isOk());
            verify(jdbc).query(contains("UPDATE projects"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
                eq(false), isNull(), eq(true), eq(expected), eq(10L));
        } else {
            result.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.errors[0].field").value("description"));
            verify(jdbc).query(contains("SELECT id FROM app_users"), org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString());
            verify(jdbc, times(2)).queryForObject(contains("FROM project_memberships"), eq(Integer.class), eq(10L), eq(1L));
            verifyNoMoreInteractions(jdbc);
        }
    }

    @Test void createsProjectWithSessionCreatorAndMembership() throws Exception {
        user();
        when(jdbc.queryForObject(contains("INSERT INTO projects"), eq(Long.class), eq("New project"), isNull(), eq(1L))).thenReturn(10L);
        mvc.perform(post("/api/v1/projects").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond())
            .with(csrf()).contentType("application/json").content("{\"name\":\" New project \"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/projects/10"))
            .andExpect(content().json("{\"id\":10,\"name\":\"New project\",\"description\":null,\"createdByUserId\":1}", true));
        verify(jdbc).update(contains("INSERT INTO project_memberships"), eq(10L), eq(1L));
    }
    @Test void nameOnlyPatchPreservesDescriptionAndOriginalCreator() throws Exception {
        access(true, true);
        patchResult(new ProjectController.Project(10, "Renamed", project.description(), 7L));
        patchBody("{\"name\":\" Renamed \"}").andExpect(status().isOk())
            .andExpect(content().json("{\"id\":10,\"name\":\"Renamed\",\"description\":\"Existing description\",\"createdByUserId\":7}", true));
        verify(jdbc).query(contains("ELSE description END"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            eq(true), eq("Renamed"), eq(false), isNull(), eq(10L));
    }
    @Test void descriptionOnlyPatchPreservesName() throws Exception {
        access(true, true);
        patchResult(new ProjectController.Project(10, "Original", "Updated", 7L));
        patchBody("{\"description\":\" Updated \"}").andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Original"));
        verify(jdbc).query(contains("ELSE name END"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            eq(false), isNull(), eq(true), eq("Updated"), eq(10L));
    }
    @Test void explicitNullClearsDescription() throws Exception {
        access(true, true);
        patchResult(new ProjectController.Project(10, "Original", null, 7L));
        patchBody("{\"description\":null}").andExpect(status().isOk())
            .andExpect(content().json("{\"id\":10,\"name\":\"Original\",\"description\":null,\"createdByUserId\":7}", true));
        verify(jdbc).query(contains("UPDATE projects"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            eq(false), isNull(), eq(true), isNull(), eq(10L));
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"  \"}", "{\"description\":\"  \"}",
        "{\"description\":3}", "{\"createdByUserId\":2}", "{\"role\":\"gestor\"}"})
    void invalidPatchDoesNotWrite(String body) throws Exception {
        access(true, true);
        int expected = body.equals("{}") || body.contains("createdByUserId") || body.contains("role") ? 400 : 422;
        patchBody(body).andExpect(status().is(expected))
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(expected));
        verify(jdbc, never()).query(contains("UPDATE projects"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            anyBoolean(), nullable(String.class), anyBoolean(), nullable(String.class), anyLong());
    }
    @Test void patchesBothFieldsAndRejectsOverLimitValues() throws Exception {
        access(true, true);
        patchResult(new ProjectController.Project(10, "Changed", "Updated", 7L));
        patchBody("{\"name\":\" Changed \",\"description\":\" Updated \"}").andExpect(status().isOk());
        verify(jdbc).query(contains("UPDATE projects"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(),
            eq(true), eq("Changed"), eq(true), eq("Updated"), eq(10L));
        patchBody("{\"name\":\"" + "x".repeat(101) + "\"}").andExpect(status().isUnprocessableEntity());
        patchBody("{\"description\":\"" + "x".repeat(1001) + "\"}").andExpect(status().isUnprocessableEntity());
    }
    @Test void legacyCreatorRemainsNullRatherThanBeingInferredFromMembership() throws Exception {
        user();
        when(jdbc.query(contains("FROM projects p"), org.mockito.ArgumentMatchers.<RowMapper<ProjectController.Project>>any(), eq(10L), eq(1L)))
            .thenAnswer(call -> {
                RowMapper<?> mapper = call.getArgument(1);
                var row = mock(java.sql.ResultSet.class);
                when(row.getLong("id")).thenReturn(10L);
                when(row.getString("name")).thenReturn("Legacy project");
                return List.of(mapper.mapRow(row, 0));
            });
        mvc.perform(get("/api/v1/projects/10").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isOk()).andExpect(content().json("{\"id\":10,\"name\":\"Legacy project\",\"description\":null,\"createdByUserId\":null}", true));
    }
    @Test void memberCannotPatchOrDelete() throws Exception {
        access(true, false);
        patchBody("{\"name\":\"Forbidden\"}").andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403)).andExpect(jsonPath("$.detail").isString());
        mvc.perform(delete("/api/v1/projects/10").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()).with(csrf()))
            .andExpect(status().isForbidden());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
    @Test void inaccessibleProjectIsHidden() throws Exception {
        access(false, false);
        patchBody("{\"name\":\"Hidden\"}").andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.detail").value("Not Found"));
        mvc.perform(get("/api/v1/projects/10").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond()))
            .andExpect(status().isNotFound());
    }
    @Test void oldPutMethodIsNotSupported() throws Exception {
        mvc.perform(put("/api/v1/projects/10").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond())
            .with(csrf()).contentType("application/json").content("{\"name\":\"Changed\"}"))
            .andExpect(status().isMethodNotAllowed());
    }
    @Test void patchRequiresCsrf() throws Exception {
        mvc.perform(patch("/api/v1/projects/10").sessionAttr("MATRIZ_SESSION_START", Instant.now().getEpochSecond())
            .contentType("application/json").content("{\"name\":\"Changed\"}"))
            .andExpect(status().isForbidden());
    }
}
