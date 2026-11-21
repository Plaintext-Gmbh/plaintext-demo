package ch.plaintext.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** Karte 2418: the login profiles switch the mock off and pick root's login the way the README says. */
class LoginProfileTest {

    private static Properties profil(String name) {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("config/application-" + name + ".yml"));
        return yaml.getObject();
    }

    @Test
    void formularLogin() {
        Properties p = profil("login-form");
        assertThat(p).containsEntry("demo.oauth-mock.enabled", false)
                .containsEntry("plaintext.login.form", true)
                .containsEntry("plaintext.login.auto-redirect-single-idp", false);
    }

    @Test
    void eigenerOidcServer() {
        Properties p = profil("login-oidc");
        assertThat(p).containsEntry("demo.oauth-mock.enabled", false)
                .containsEntry("plaintext.login.form", false)
                .containsEntry("demo.oidc.issuer-url", "${DEMO_OIDC_ISSUER}")
                .containsEntry("demo.oidc.client-id", "${DEMO_OIDC_CLIENT_ID}")
                .containsEntry("demo.oidc.client-secret", "${DEMO_OIDC_CLIENT_SECRET}");
    }
}
