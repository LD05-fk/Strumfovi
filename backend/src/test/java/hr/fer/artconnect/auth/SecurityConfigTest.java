package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.fer.artconnect.auth.web.CsrfController;
import jakarta.servlet.http.Cookie;

// Testni kontroler potvrđuje prolazak kroz filtre i za rute koje još nisu implementirane.
@WebMvcTest(SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, CsrfController.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    ArtConnectOidcUserService oidcUserService;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/opportunities", "/api/v1/opportunities/123",
            "/api/v1/artists", "/api/v1/artists/123",
            "/api/v1/artworks", "/api/v1/artworks/123",
            "/api/v1/files", "/api/v1/files/123",
            "/api/v1/art-fields", "/api/v1/opportunity-types",
            "/api/v1/settings/public", "/actuator/health"
    })
    void javniGetDostupanJeBezPrijave(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string("OK"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/me", "/api/v1/admin/users", "/api/v1/settings",
            "/api/v1/art-fields/123", "/api/v1/settings/public/private", "/actuator/env"
    })
    void ostaleRuteTrazePrijavu(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.message").value("Potrebno je prijaviti se."))
                .andExpect(jsonPath("$.error.details").doesNotExist());
    }

    @Test
    void javnaPutanjaNeDopustaAnonimniPost() throws Exception {
        // Valjan CSRF token omogućuje da test provjeri baš pravilo prijave.
        Cookie token = mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        mvc.perform(post("/api/v1/opportunities").cookie(token)
                        .header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ARTIST")
    void prijavljeniKorisnikMozeNaZasticenuRutu() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ARTIST")
    void umjetnikNemaPravaOrganizatora() throws Exception {
        mvc.perform(get("/api/v1/test/organizer")).andExpect(status().isForbidden());
    }

    @Test
    void anonimniKorisnikNaJavnojRutiSProvjeromUlogeDobiva401() throws Exception {
        // Regresija iz komentara na issueu: MVC mora vratiti 401, kao i filtri.
        mvc.perform(get("/api/v1/artists/organizer-only"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @WithMockUser(roles = "ARTIST")
    void prijavljeniKorisnikBezUlogeNaJavnojRutiDobiva403() throws Exception {
        mvc.perform(get("/api/v1/artists/organizer-only"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("Nemate dopuštenje za ovu radnju."));
    }

    @Test
    void anonimniPostBezTokenaDobiva403() throws Exception {
        mvc.perform(post("/api/v1/opportunities"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ARTIST")
    void csrfKolacicIZaglavljeMorajuSePodudarati() throws Exception {
        // Dohvaćamo pravi token iz aplikacije, bez testnog with(csrf()).
        var response = mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn().getResponse();
        Cookie token = response.getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();
        assertThat(token.isHttpOnly()).isFalse();
        assertThat(token.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(response.getContentAsString()).contains(token.getValue());

        mvc.perform(post("/api/v1/opportunities").cookie(token)
                        .header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/opportunities").cookie(token)
                        .header("X-XSRF-TOKEN", "pogresan-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mvc.perform(post("/api/v1/opportunities").cookie(token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser
    void devRutaBezDevProfilaTraziCsrf() throws Exception {
        mvc.perform(post("/api/v1/dev/login-as"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void organizatorMozePozvatiSvojuMetodu() throws Exception {
        mvc.perform(get("/api/v1/test/organizer"))
                .andExpect(status().isOk()).andExpect(content().string("ORGANIZER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorNeNasljedujePravaOrganizatora() throws Exception {
        mvc.perform(get("/api/v1/test/organizer"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @RestController
    public static class TestController {
        @GetMapping("/**")
        public String get() {
            return "OK";
        }

        @PostMapping({"/api/v1/opportunities", "/api/v1/dev/login-as"})
        public String create() {
            return "OK";
        }

        @GetMapping({"/api/v1/test/organizer", "/api/v1/artists/organizer-only"})
        @PreAuthorize("hasRole('ORGANIZER')")
        public String organizer() {
            return "ORGANIZER";
        }
    }
}
