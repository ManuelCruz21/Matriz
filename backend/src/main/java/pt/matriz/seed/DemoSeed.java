package pt.matriz.seed;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** One-time optional fictitious data; refuses existing demo accounts and never deletes data. */
@Component
@ConditionalOnProperty(name = "matriz.demo-seed.enabled", havingValue = "true")
public class DemoSeed implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DemoSeed(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String firstPassword = requireEnv("DEMO_MANAGER_PASSWORD");
        String secondPassword = requireEnv("DEMO_MEMBER_PASSWORD");
        long manager = ensureUser("gestor@example.org", "Gestor Demo", firstPassword);
        long member = ensureUser("membro@example.org", "Membro Demo", secondPassword);
        ensureProject("Projeto privado de demonstracao", manager, null);
        ensureProject("Projeto partilhado de demonstracao", manager, member);
    }

    private String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.length() < 12) {
            throw new IllegalStateException(key + " deve ter pelo menos 12 caracteres para ativar o seed demo");
        }
        return value;
    }

    private long ensureUser(String email, String name, String password) {
        List<Long> ids = jdbc.query("SELECT id FROM app_users WHERE email = ?", (rs, i) -> rs.getLong(1), email);
        if (!ids.isEmpty()) {
            throw new IllegalStateException(
                    "Demo seed refused: an existing demo-email account has no verified seed ownership. "
                    + "Disable DEMO_SEED_ENABLED; existing accounts must not be reused or reset.");
        }
        return jdbc.queryForObject("INSERT INTO app_users (name, email, password_hash) VALUES (?, ?, ?) RETURNING id",
                Long.class, name, email, encoder.encode(password));
    }

    private void ensureProject(String name, long manager, Long member) {
        // O seed nao modifica projetos existentes com o mesmo nome.
        List<Long> ids = jdbc.query("SELECT p.id FROM projects p JOIN project_memberships m ON m.project_id = p.id WHERE p.name = ? AND m.user_id = ? AND m.role = 'gestor'",
                (rs, i) -> rs.getLong(1), name, manager);
        if (!ids.isEmpty()) return;
        Long project = jdbc.queryForObject("INSERT INTO projects (name, created_by_user_id) VALUES (?, ?) RETURNING id", Long.class, name, manager);
        jdbc.update("INSERT INTO project_memberships (project_id, user_id, role) VALUES (?, ?, 'gestor')", project, manager);
        if (member != null) jdbc.update("INSERT INTO project_memberships (project_id, user_id, role) VALUES (?, ?, 'membro')", project, member);
    }
}
