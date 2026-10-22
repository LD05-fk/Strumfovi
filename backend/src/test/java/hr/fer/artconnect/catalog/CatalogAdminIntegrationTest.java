package hr.fer.artconnect.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@WithMockUser(roles = "ADMIN")
class CatalogAdminIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void pripremiPodatke() {
        jdbc.update("DELETE FROM opportunity_art_fields");
        jdbc.update("DELETE FROM opportunities");
        jdbc.update("DELETE FROM artist_profile_art_fields");
        jdbc.update("DELETE FROM art_fields");
        jdbc.update("DELETE FROM opportunity_types");
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void dodajeUredujeIDeaktiviraPaBriseNekoristenuStavku(String path) throws Exception {
        String location = create(path, input("keramika", "Keramika", true, 999));
        mvc.perform(put(location).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(input("nova-keramika", "Nova keramika", false, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("nova-keramika"))
                .andExpect(jsonPath("$.name").value("Nova keramika"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.position").value(0));

        mvc.perform(get("/api/v1/" + path).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get(adminPath(path)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].active").value(false));

        mvc.perform(delete(location).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table(path), Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void vracaSveStavkePoPozicijiINazivuIStranici(String path) throws Exception {
        create(path, input("beta", "Beta", true, 2));
        create(path, input("zeta", "Zeta", false, 1));
        create(path, input("alfa", "Alfa", true, 1));

        mvc.perform(get(adminPath(path)).param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].code").value("alfa"))
                .andExpect(jsonPath("$.data[1].code").value("zeta"))
                .andExpect(jsonPath("$.data[1].active").value(false))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(2))
                .andExpect(jsonPath("$.meta.total").value(3));
        mvc.perform(get(adminPath(path)).param("pageSize", "2").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("beta"));
        mvc.perform(get(adminPath(path)).param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(3));
        mvc.perform(get(adminPath(path)).param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("pageSize"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void odbijaDuplikateSifreINazivaPriDodavanjuIUredivanju(String path) throws Exception {
        create(path, input("keramika", "Keramika", false, 0));
        String second = create(path, input("slikarstvo", "Slikarstvo", true, 0));

        for (String body : new String[]{
                input("keramika", "Drugi naziv", true, 1),
                input("druga-sifra", "Keramika", true, 1)}) {
            for (var request : new MockHttpServletRequestBuilder[]{post(adminPath(path)), put(second)}) {
                mvc.perform(request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.error.code").value("DUPLICATE_CODE"));
            }
        }
        // Vlastita šifra i naziv nisu duplikati.
        mvc.perform(put(second).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(input("slikarstvo", "Slikarstvo", true, 0)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void validiraObaveznaPoljaDuljineFormatIGranice(String path) throws Exception {
        String location = create(path, input("valjano", "Valjano", true, 0));
        String[][] invalid = {
                {"{}", "code"},
                {input("A!", "Valjano", true, 0), "code"},
                {input("a", "Valjano", true, 0), "code"},
                {input("a".repeat(41), "Valjano", true, 0), "code"},
                {input("valjano", " ", true, 0), "name"},
                {input("valjano", "a".repeat(81), true, 0), "name"},
                {input("valjano", "Valjano", true, -1), "position"},
                {input("valjano", "Valjano", true, 1000), "position"},
                {"{\"code\":\"ab\",\"name\":\"AB\",\"position\":0}", "active"},
                {"{\"code\":\"ab\",\"name\":\"AB\",\"active\":true}", "position"}
        };
        for (String[] example : invalid) {
            for (var request : new MockHttpServletRequestBuilder[]{post(adminPath(path)), put(location)}) {
                mvc.perform(request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(example[0]))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                        .andExpect(jsonPath("$.error.details[*].field", hasItem(example[1])));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void neSkracujeDecimalnuPozicijuUCijeliBroj(String path) throws Exception {
        String location = create(path, input("keramika", "Keramika", true, 6));
        for (String position : new String[]{"1.9", "-0.5", "999.9"}) {
            String body = """
                    {"code":"keramika","name":"Keramika","active":true,"position":%s}
                    """.formatted(position);
            for (var request : new MockHttpServletRequestBuilder[]{post(adminPath(path)), put(location)}) {
                mvc.perform(request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
            }
        }
        mvc.perform(get(adminPath(path)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].position").value(6));
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void sveOperacijeTrazeAdministratoraAPromjeneICsrf(String path) throws Exception {
        String location = adminPath(path) + "/" + UUID.randomUUID();
        for (String role : new String[]{"ARTIST", "ORGANIZER"}) {
            for (var request : operations(path, location)) {
                mvc.perform(request.with(user("korisnik").roles(role)).with(csrf()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
            }
        }
        for (var request : operations(path, location)) {
            mvc.perform(request.with(anonymous()).with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        }
        for (var request : new MockHttpServletRequestBuilder[]{
                post(adminPath(path)), put(location), delete(location)}) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                            .content(input("keramika", "Keramika", true, 0)))
                    .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void nepostojecaStavkaVraca404(String path) throws Exception {
        String location = adminPath(path) + "/" + UUID.randomUUID();
        mvc.perform(put(location).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(input("keramika", "Keramika", true, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(delete(location).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void neBrisePodrucjeKojegKoristiProfilUmjetnika() throws Exception {
        String location = create("art-fields", input("keramika", "Keramika", true, 0));
        UUID artist = insertUser("ARTIST");
        jdbc.update("INSERT INTO artist_profiles (user_id) VALUES (?)", artist);
        jdbc.update("INSERT INTO artist_profile_art_fields (user_id, art_field_id) VALUES (?, ?)",
                artist, itemId(location));
        expectInUse(location);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artist_profile_art_fields", Long.class)).isEqualTo(1);
    }

    @Test
    void neBriseKoristeneStavkeADeaktivacijaCuvaVezePrilike() throws Exception {
        String field = create("art-fields", input("keramika", "Keramika", true, 0));
        String type = create("opportunity-types", input("izlozba", "Izložba", true, 0));
        UUID opportunity = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO opportunities (id, organizer_id, type_id, title, description,
                    deadline, location_type, online_url)
                VALUES (?, ?, ?, 'Izložba keramike', 'Opis', now() + interval '30 days',
                    'ONLINE', 'https://example.test')
                """, opportunity, insertUser("ORGANIZER"), itemId(type));
        jdbc.update("INSERT INTO opportunity_art_fields (opportunity_id, art_field_id) VALUES (?, ?)",
                opportunity, itemId(field));
        expectInUse(field);
        expectInUse(type);

        for (var data : new String[][]{
                {field, input("keramika", "Keramika", false, 0)},
                {type, input("izlozba", "Izložba", false, 0)}}) {
            mvc.perform(put(data[0]).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(data[1]))
                    .andExpect(status().isOk());
        }
        for (String path : new String[]{"art-fields", "opportunity-types"}) {
            mvc.perform(get("/api/v1/" + path).with(anonymous()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
        // Dohvat prilike još nije implementiran; SQL spajanje provjerava da stare veze ostaju čitljive.
        assertThat(jdbc.queryForObject("""
                SELECT t.name || ' / ' || f.name
                FROM opportunities o JOIN opportunity_types t ON t.id = o.type_id
                JOIN opportunity_art_fields ofl ON ofl.opportunity_id = o.id
                JOIN art_fields f ON f.id = ofl.art_field_id WHERE o.id = ?
                """, String.class, opportunity)).isEqualTo("Izložba / Keramika");
        expectInUse(field);
        expectInUse(type);
    }

    private String create(String path, String body) throws Exception {
        var response = mvc.perform(post(adminPath(path)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(adminPath(path) + "/")))
                .andExpect(jsonPath("$.id").isString())
                .andReturn().getResponse();
        String location = response.getHeader("Location");
        mvc.perform(get(adminPath(path)))
                .andExpect(jsonPath("$.data[*].id", hasItem(itemId(location).toString())));
        return location;
    }

    private void expectInUse(String location) throws Exception {
        mvc.perform(delete(location).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IN_USE"));
    }

    private UUID insertUser(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, display_name, role, status, organizer_approval)
                VALUES (?, 'Test', ?, 'ACTIVE', ?)
                """, id, role, "ORGANIZER".equals(role) ? "APPROVED" : null);
        return id;
    }

    private static MockHttpServletRequestBuilder[] operations(String path, String location) {
        String body = input("keramika", "Keramika", true, 0);
        return new MockHttpServletRequestBuilder[]{
                get(adminPath(path)),
                post(adminPath(path)).contentType(MediaType.APPLICATION_JSON).content(body),
                put(location).contentType(MediaType.APPLICATION_JSON).content(body),
                delete(location)};
    }

    private static String input(String code, String name, boolean active, int position) {
        return """
                {"code":"%s","name":"%s","active":%s,"position":%d}
                """.formatted(code, name, active, position);
    }

    private static UUID itemId(String location) {
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private static String adminPath(String path) {
        return "/api/v1/admin/" + path;
    }

    private static String table(String path) {
        return path.replace('-', '_');
    }
}
