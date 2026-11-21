package ch.plaintext.demo.oauthmock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** Karte 2418: demo.oauth-mock.enabled=false leaves the mock out, the accounts stay available. */
class OAuthMockSchalterTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(OAuthMockPropertiesConfig.class, OAuthMockConfig.class, OAuthMockLoginController.class);

    @Test
    void ausgeschaltetOhneMockAberMitKonten() {
        runner.withPropertyValues("demo.oauth-mock.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(OAuthMockConfig.class).doesNotHaveBean(OAuthMockLoginController.class);
            assertThat(ctx.getBean(OAuthMockProperties.class).isEnabled()).isFalse();
            assertThat(ctx.getBean(OAuthMockProperties.class).getUsers()).hasSize(4);
        });
    }

    @Test
    void ohneEinstellungBleibtDerMockAn() {
        // Nur die Bedingung: die Beans des Mocks selbst brauchen eine Web-Umgebung (OAuthMockDoppelAbsendenTest).
        new ApplicationContextRunner().withUserConfiguration(OAuthMockPropertiesConfig.class).run(ctx ->
                assertThat(ctx.getBean(OAuthMockProperties.class).isEnabled()).isTrue());
    }
}
