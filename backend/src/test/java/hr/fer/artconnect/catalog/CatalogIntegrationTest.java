package hr.fer.artconnect.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class CatalogIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @BeforeEach
    void pripremiSifrarnike() {
        // Svaki test dobiva isti mali skup; transakcija na kraju vraća početne podatke.
        // Veze na šifrarnike (npr. područja iz dev seeda profila umjetnika) brišu se prve
        // jer su FK-ovi prema šifrarnicima RESTRICT.
        jdbc.update("DELETE FROM artist_profile_art_fields");
        jdbc.update("DELETE FROM opportunity_art_fields");
        for (String table : new String[]{"art_fields", "opportunity_types"}) {
            jdbc.update("DELETE FROM " + table);
            insertItem(table, "beta", "Beta", true, 2);
            insertItem(table, "zeta", "Zeta", true, 1);
            insertItem(table, "alfa", "Alfa", true, 1);
            insertItem(table, "skriveno", "Skriveno", false, 0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void javnoVracaSamoAktivneStavkePoPozicijiINazivu(String path) throws Exception {
        mvc.perform(get("/api/v1/" + path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").isString())
                .andExpect(jsonPath("$.data[0].code").value("alfa"))
                .andExpect(jsonPath("$.data[0].name").value("Alfa"))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[0].position").value(1))
                .andExpect(jsonPath("$.data[1].name").value("Zeta"))
                .andExpect(jsonPath("$.data[2].name").value("Beta"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.total").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void straniciAktivneStavkeIVracaPraznuStranicuIzaKraja(String path) throws Exception {
        mvc.perform(get("/api/v1/" + path).param("page", "2").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Beta"))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(2))
                .andExpect(jsonPath("$.meta.total").value(3));

        mvc.perform(get("/api/v1/" + path).param("page", "3").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.page").value(3))
                .andExpect(jsonPath("$.meta.total").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"art-fields", "opportunity-types"})
    void odbijaNeispravneParametreStranicenja(String path) throws Exception {
        var invalidParams = Map.of("page", new String[]{"0", "abc"},
                "pageSize", new String[]{"0", "101", "abc"});
        for (var param : invalidParams.entrySet()) {
            for (String value : param.getValue()) {
                mvc.perform(get("/api/v1/" + path).param(param.getKey(), value))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                        .andExpect(jsonPath("$.error.details[0].field").value(param.getKey()));
            }
        }
    }

    @Test
    void javnePostavkeCitaIzBazePriSvakomDohvatu() throws Exception {
        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.artworkMaxImageBytes").value(5242880))
                .andExpect(jsonPath("$.artworkMaxImages").value(3))
                .andExpect(jsonPath("$.applicationMaxImageBytes").value(5242880))
                .andExpect(jsonPath("$.applicationMaxPdfBytes").value(10485760))
                .andExpect(jsonPath("$.applicationMaxFiles").value(10));

        jdbc.update("UPDATE system_settings SET value = ? WHERE key = ?",
                "7", "upload.application.maxFiles");
        // Test dijeli transakciju između zahtjeva; uklanjamo entitete učitane prije SQL izmjene.
        entityManager.clear();

        mvc.perform(get("/api/v1/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationMaxFiles").value(7));
    }

    private void insertItem(String table, String code, String name, boolean active, int position) {
        jdbc.update("INSERT INTO " + table + " (id, code, name, active, position) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), code, name, active, position);
    }
}
