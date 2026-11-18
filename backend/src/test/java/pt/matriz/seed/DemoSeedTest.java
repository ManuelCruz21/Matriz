package pt.matriz.seed;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Isolated account-seeding regressions: no Spring context, environment or database access. */
class DemoSeedTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DemoSeed seed = new DemoSeed(jdbc);
    private static final String TEST_PASSWORD = "IsolatedTestOnlyPassword!";

    @ParameterizedTest
    @ValueSource(strings = {"gestor@example.org", "membro@example.org"})
    void refusesExistingDemoAccountWithoutAnyWrites(String email) {
        when(jdbc.query(eq("SELECT id FROM app_users WHERE email = ?"),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq(email)))
                .thenReturn(List.of(42L));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(seed, "ensureUser", email, "Demo", TEST_PASSWORD));

        assertTrue(error.getMessage().contains("no verified seed ownership"));
        assertTrue(error.getMessage().contains("Disable DEMO_SEED_ENABLED"));
        assertFalse(error.getMessage().contains(TEST_PASSWORD));
        verify(jdbc).query(eq("SELECT id FROM app_users WHERE email = ?"),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq(email));
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void createsFreshPrivateAndSharedProjectsWithExpectedMemberships() {
        when(jdbc.query(contains("SELECT p.id FROM projects"),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), anyString(), eq(17L)))
                .thenReturn(List.of());
        when(jdbc.queryForObject(startsWith("INSERT INTO projects"), eq(Long.class),
                anyString(), eq(17L))).thenReturn(31L, 32L);

        ReflectionTestUtils.invokeMethod(seed, "ensureProject", "Private demo", 17L, null);
        ReflectionTestUtils.invokeMethod(seed, "ensureProject", "Shared demo", 17L, 18L);

        verify(jdbc).update(contains("'gestor'"), eq(31L), eq(17L));
        verify(jdbc).update(contains("'gestor'"), eq(32L), eq(17L));
        verify(jdbc).update(contains("'membro'"), eq(32L), eq(18L));
        verify(jdbc, times(3)).update(anyString(), any(Long.class), any(Long.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gestor@example.org", "membro@example.org"})
    void createsFreshDemoAccountWithBcryptHash(String email) {
        when(jdbc.query(eq("SELECT id FROM app_users WHERE email = ?"),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(), eq(email)))
                .thenReturn(List.of());
        when(jdbc.queryForObject(startsWith("INSERT INTO app_users"), eq(Long.class),
                eq("Demo"), eq(email), anyString())).thenReturn(17L);

        Long id = ReflectionTestUtils.invokeMethod(seed, "ensureUser", email, "Demo", TEST_PASSWORD);

        assertEquals(17L, id);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(
                eq("INSERT INTO app_users (name, email, password_hash) VALUES (?, ?, ?) RETURNING id"),
                eq(Long.class), eq("Demo"), eq(email), hash.capture());
        assertNotEquals(TEST_PASSWORD, hash.getValue());
        assertTrue(new BCryptPasswordEncoder().matches(TEST_PASSWORD, hash.getValue()));
    }
}
