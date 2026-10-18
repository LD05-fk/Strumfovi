package hr.fer.artconnect.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.security.test.context.support.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;

@WebMvcTest(SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestController.class})
@ActiveProfiles("dev")
class DevCsrfTest {
    @Autowired
    MockMvc mvc;

    @Test
    @WithMockUser
    void samoDevRutePreskacuCsrf() throws Exception {
        mvc.perform(post("/api/v1/dev/login-as")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/opportunities")).andExpect(status().isForbidden());
    }

    @Test
    void izuzeceCsrfNeZaobilaziPrijavu() throws Exception {
        mvc.perform(post("/api/v1/dev/login-as")).andExpect(status().isUnauthorized());
    }
}
