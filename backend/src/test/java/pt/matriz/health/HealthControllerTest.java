
package pt.matriz.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(pt.matriz.auth.SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JdbcTemplate jdbc;

    @Test
    void reportsDatabaseAvailable() throws Exception {
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test
    void hidesDatabaseFailureDetails() throws Exception {
        when(jdbc.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new DataAccessResourceFailureException("private connection details"));

        mvc.perform(get("/api/health"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().json("{\"status\":\"DOWN\"}"));
    }

    // @Test
    // void doesNotImplementBusinessEndpoints() throws Exception {
    //     mvc.perform(get("/api/v1/projects"))
    //         .andExpect(status().isNotFound());
    // }
}
