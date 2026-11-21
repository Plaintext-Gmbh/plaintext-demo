package ch.plaintext.demo.oauthmock;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Karte 2417: the mock's sign-in form sent twice must start the sign-in over, not end in a 403. */
@SpringJUnitWebConfig(OAuthMockDoppelAbsendenTest.Beans.class)
class OAuthMockDoppelAbsendenTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({OAuthMockPropertiesConfig.class, OAuthMockConfig.class, OAuthMockLoginController.class})
    static class Beans {
        @Bean
        PasswordEncoder passwordEncoder() {
            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }
    }

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
    }

    @Test
    void zweitesAbsendenDesFormularsStartetDenLoginNeu() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String html = mvc.perform(get(OAuthMockConfig.LOGIN).session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher m = CSRF.matcher(html);
        assertTrue(m.find(), "CSRF-Feld im Formular");
        String token = m.group(1);

        // first click: signed in at the mock
        mvc.perform(post(OAuthMockConfig.LOGIN).session(session)
                        .param("username", "root").param("password", "root").param("_csrf", token))
                .andExpect(status().is3xxRedirection());
        // second click with the same (now rotated) token: sign-in starts over
        mvc.perform(post(OAuthMockConfig.LOGIN).session(session)
                        .param("username", "root").param("password", "root").param("_csrf", token))
                .andExpect(redirectedUrl("/oauth2/authorization/" + OAuthMockConfig.REGISTRATION_ID));
    }

    @Test
    void andereAblehnungenBleiben403() throws Exception {
        var handler = OAuthMockConfig.signInAgainOnStaleForm();
        var csrf = new InvalidCsrfTokenException(new DefaultCsrfToken("X", "_csrf", "a"), "b");

        MockHttpServletResponse andererPfad = new MockHttpServletResponse();
        handler.handle(new MockHttpServletRequest("POST", OAuthMockProperties.PATH + "/protocol/openid-connect/logout"),
                andererPfad, csrf);
        assertEquals(403, andererPfad.getStatus());

        MockHttpServletResponse keinCsrf = new MockHttpServletResponse();
        handler.handle(new MockHttpServletRequest("POST", OAuthMockConfig.LOGIN), keinCsrf, new AccessDeniedException("nein"));
        assertEquals(403, keinCsrf.getStatus());
    }
}
