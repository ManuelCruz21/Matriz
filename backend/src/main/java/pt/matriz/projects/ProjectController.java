
package pt.matriz.projects;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final JdbcTemplate jdbc;

    public ProjectController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Project(
        long id,
        String name,
        String description,
        Long createdByUserId
    ) {}

    public record ProjectRequest(
        String name,
        String description
    ) {}

    private Long currentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        var ids = jdbc.query(
            "SELECT id FROM app_users WHERE lower(email) = lower(?)",
            (rs, row) -> rs.getLong("id"),
            auth.getName()
        );

        return ids.isEmpty() ? null : ids.get(0);
    }

    // Listar apenas os projetos do utilizador autenticado.
    @GetMapping
    public ResponseEntity<?> list(
        Authentication auth,
        @RequestParam(defaultValue = "") String search
    ) {
        Long userId = currentUserId(auth);

        if (userId == null) {
            return pt.matriz.ApiProblems.response(401);
        }

        String searchText = search.strip();
        if (searchText.codePointCount(0, searchText.length()) > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search exceeds 100 characters"));
        }
        String term = "%" + searchText.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";

        List<Project> projects = jdbc.query("""
            SELECT p.id, p.name, p.description, p.created_by_user_id
            FROM projects p
            JOIN project_memberships pm ON pm.project_id = p.id
            WHERE pm.user_id = ?
              AND p.name ILIKE ? ESCAPE '!'
            ORDER BY p.id DESC
            """,
            (rs, row) -> new Project(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("created_by_user_id", Long.class)
            ),
            userId,
            term
        );

        return ResponseEntity.ok(Map.of("items", projects, "totalItems", projects.size()));
    }

    // Consultar um projeto específico.
    @GetMapping("/{id}")
    public ResponseEntity<?> get(
        @PathVariable long id,
        Authentication auth
    ) {
        Long userId = currentUserId(auth);

        if (userId == null) {
            return pt.matriz.ApiProblems.response(401);
        }

        var projects = jdbc.query("""
            SELECT p.id, p.name, p.description, p.created_by_user_id
            FROM projects p
            JOIN project_memberships pm ON pm.project_id = p.id
            WHERE p.id = ? AND pm.user_id = ?
            """,
            (rs, row) -> new Project(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("created_by_user_id", Long.class)
            ),
            id,
            userId
        );

        if (projects.isEmpty()) {
            return pt.matriz.ApiProblems.response(404);
        }

        return ResponseEntity.ok(projects.get(0));
    }

    // Criar um projeto e atribuir o papel de gestor ao criador.
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(
        @RequestBody ProjectRequest body,
        Authentication auth
    ) {
        Long userId = currentUserId(auth);

        if (userId == null) {
            return pt.matriz.ApiProblems.response(401);
        }

        if (!valid(body)) {
            return pt.matriz.ApiProblems.validation(body == null || body.name() == null
                || !validName(body.name()) ? "name" : "description",
                "Invalid project name or description.");
        }

        Long id = jdbc.queryForObject("""
            INSERT INTO projects (name, description, created_by_user_id)
            VALUES (?, ?, ?)
            RETURNING id
            """,
            Long.class,
            body.name().strip(),
            normalizeDescription(body.description()),
            userId
        );

        jdbc.update("""
            INSERT INTO project_memberships (project_id, user_id, role)
            VALUES (?, ?, 'gestor')
            """,
            id,
            userId
        );

        return ResponseEntity.created(java.net.URI.create("/api/v1/projects/" + id)).body(
            new Project(
                id,
                body.name().strip(),
                normalizeDescription(body.description()),
                userId
            )
        );
    }

    // Editar: apenas gestores do projeto.
    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(
        @PathVariable long id,
        @RequestBody Map<String, Object> body,
        Authentication auth
    ) {
        Long userId = currentUserId(auth);
        if (userId == null) return pt.matriz.ApiProblems.response(401);
        if (!isMember(id, userId)) return pt.matriz.ApiProblems.response(404);
        if (!isManager(id, userId)) return pt.matriz.ApiProblems.response(403);

        if (body == null || body.isEmpty()
                || body.keySet().stream().anyMatch(k -> !k.equals("name") && !k.equals("description"))) {
            return pt.matriz.ApiProblems.response(400);
        }
        if ((body.containsKey("name") && (!(body.get("name") instanceof String name)
                    || !validName(name)))
                || (body.containsKey("description") && body.get("description") != null
                    && (!(body.get("description") instanceof String description)
                        || !validDescription(description)))) {
            return pt.matriz.ApiProblems.validation(body.containsKey("name") && (!(body.get("name") instanceof String n)
                || !validName(n)) ? "name" : "description", "Invalid project data.");
        }

        // Presence flags preserve omitted fields without a read/replace race.
        var projects = jdbc.query("""
            UPDATE projects
            SET name = CASE WHEN ? THEN ? ELSE name END,
                description = CASE WHEN ? THEN ? ELSE description END
            WHERE id = ?
            RETURNING id, name, description, created_by_user_id
            """,
            (rs, row) -> new Project(rs.getLong("id"), rs.getString("name"),
                rs.getString("description"), rs.getObject("created_by_user_id", Long.class)),
            body.containsKey("name"), body.containsKey("name") ? ((String) body.get("name")).strip() : null,
            body.containsKey("description"), normalizeDescription((String) body.get("description")), id
        );
        return projects.isEmpty() ? pt.matriz.ApiProblems.response(404) : ResponseEntity.ok(projects.get(0));
    }

    // Eliminar: apenas gestores do projeto.
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(
        @PathVariable long id,
        Authentication auth
    ) {
        Long userId = currentUserId(auth);

        if (userId == null) {
            return pt.matriz.ApiProblems.response(401);
        }

        if (!isMember(id, userId)) {
            return pt.matriz.ApiProblems.response(404);
        }

        if (!isManager(id, userId)) {
            return pt.matriz.ApiProblems.response(403);
        }

        jdbc.update("DELETE FROM projects WHERE id = ?", id);

        return ResponseEntity.noContent().build();
    }

    private boolean isManager(long projectId, long userId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM project_memberships
            WHERE project_id = ?
              AND user_id = ?
              AND role = 'gestor'
            """,
            Integer.class,
            projectId,
            userId
        );

        return count != null && count > 0;
    }

    private boolean validName(String name) {
        if (name == null) return false;
        String value = name.strip();
        int length = value.codePointCount(0, value.length());
        return length >= 3 && length <= 100;
    }

    private boolean valid(ProjectRequest body) {
        if (body == null || !validName(body.name())) {
            return false;
        }

        return validDescription(body.description());
    }

    private boolean validDescription(String description) {
        if (description == null) return true;
        String value = description.strip();
        int length = value.codePointCount(0, value.length());
        return length >= 1 && length <= 1000;
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.strip();
    }

    private boolean isMember(long projectId, long userId) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM project_memberships
            WHERE project_id = ?
            AND user_id = ?
            """,
            Integer.class,
            projectId,
            userId
        );

        return count != null && count > 0;
    }
}
