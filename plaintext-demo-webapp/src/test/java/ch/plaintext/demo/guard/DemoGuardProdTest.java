package ch.plaintext.demo.guard;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Karte 2418: the online profile prod keeps the locks on even with DEMO_GUARD=false. */
class DemoGuardProdTest {

    private static MockEnvironment profile(String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    @Test
    void prodErzwingtDieSperren() {
        assertTrue(DemoGuardConfig.enabled(false, profile("prod")));
        assertTrue(DemoGuardConfig.enabled(false, profile("prod", "login-form")));
        assertTrue(DemoGuardConfig.enabled(true, profile("prod")));
    }

    @Test
    void ohneProdGiltDieEinstellung() {
        assertFalse(DemoGuardConfig.enabled(false, profile()));
        assertFalse(DemoGuardConfig.enabled(false, profile("login-oidc")));
        assertTrue(DemoGuardConfig.enabled(true, profile()));
    }
}
