package hr.fer.artconnect.portfolio.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Testni korisnici, područja i stvarne sesije za testove portfolija. */
public class PortfolioFixtures {
    private final JdbcTemplate jdbc;
    private final MockMvc mockMvc;
    private final List<UUID> users = new ArrayList<>();
    private final List<UUID> artFields = new ArrayList<>();

    public PortfolioFixtures(JdbcTemplate jdbc, MockMvc mockMvc) {
        this.jdbc = jdbc;
        this.mockMvc = mockMvc;
    }

    public UUID user(String role, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, display_name, role, status, organizer_approval)
                VALUES (?, ?, 'Portfolio test', ?, ?, ?)
                """, id, id + "@artconnect.test", role, status,
                "ORGANIZER".equals(role) ? "APPROVED" : null);
        users.add(id);
        return id;
    }

    public UUID artField() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO art_fields (id, code, name, active, position)
                VALUES (?, ?, ?, true, 99)
                """, id, "pt-" + id, "Testno područje " + id);
        artFields.add(id);
        return id;
    }

    public UserSession login(UUID userId) throws Exception {
        Cookie session = mockMvc.perform(post("/api/v1/dev/login-as")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + userId + "@artconnect.test\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION");
        Cookie csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        return new UserSession(session, csrf);
    }

    public void cleanup() {
        // Brisanje korisnika kaskadno briše njegov profil, područja i poveznice.
        for (UUID id : users) {
            jdbc.update("DELETE FROM spring_session WHERE principal_name = ?", id.toString());
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        for (UUID id : artFields) {
            jdbc.update("DELETE FROM art_fields WHERE id = ?", id);
        }
    }

    public record UserSession(Cookie session, Cookie csrf) {
    }
}
