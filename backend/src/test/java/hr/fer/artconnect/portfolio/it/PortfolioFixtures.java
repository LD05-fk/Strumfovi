package hr.fer.artconnect.portfolio.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.mock.web.MockMultipartFile;

/** Testni korisnici, područja, izložbe, djela i stvarne sesije za testove portfolija. */
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
                VALUES (?, ?, ?, true, ?)
                """, id, "pt-" + id, "Testno područje " + id, 99 + artFields.size());
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

    public UUID pastExhibition(UUID artistId) {
        // Izložba pripada profilu, pa za test prava pristupa pripremamo oba zapisa.
        jdbc.update("INSERT INTO artist_profiles (user_id) VALUES (?)", artistId);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO past_exhibitions (id, artist_id, title, year, place)
                VALUES (?, ?, 'Gradski motivi', 2024, 'Zagreb')
                """, id, artistId);
        return id;
    }

    public UUID artwork(UUID artistId, OffsetDateTime createdAt) {
        // Različita vremena nastanka omogućuju pouzdanu provjeru redoslijeda bez čekanja.
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO artworks (id, artist_id, title, year, technique, visibility, created_at)
                VALUES (?, ?, 'Gradski motivi', 2024, 'Akril', 'PUBLIC', ?)
                """, id, artistId, createdAt);
        return id;
    }

    public void cleanup() {
        // Brisanje korisnika kaskadno briše profil, izložbe, djela, odabire područja i poveznice.
        for (UUID id : users) {
            jdbc.update("DELETE FROM spring_session WHERE principal_name = ?", id.toString());
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
        for (UUID id : artFields) {
            jdbc.update("DELETE FROM art_fields WHERE id = ?", id);
        }
    }

    // Datoteku šaljemo kao preglednik: multipart tijelo, stvarna sesija i CSRF token.
    // Rezultat vraćamo testu kako bi on provjerio očekivani uspjeh ili grešku.
    public ResultActions uploadImage(UUID artworkId, UserSession user, String name, byte[] bytes)
            throws Exception {
        return mockMvc.perform(multipart("/api/v1/artworks/{id}/images", artworkId)
                .file(new MockMultipartFile("file", name, "application/octet-stream", bytes))
                .cookie(user.session(), user.csrf())
                .header("X-XSRF-TOKEN", user.csrf().getValue()));
    }

    public record UserSession(Cookie session, Cookie csrf) {
    }
}
