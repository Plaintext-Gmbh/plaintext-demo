package ch.plaintext.demo.daten;

import ch.plaintext.boot.plugins.jsf.userprofile.UserPrefsSimpleStorage;
import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import ch.plaintext.demo.oauthmock.OAuthMockProperties;
import ch.plaintext.demo.tabelle.DemoProduct;
import ch.plaintext.demo.tabelle.DemoProductRepository;
import ch.plaintext.oidc.entity.OidcConfig;
import ch.plaintext.oidc.repository.OidcConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The demo data: four accounts in two mandates, the demo IdP and the product rows - idempotent. */
class DemoDatenLoaderTest {

    @Test
    void productsAreDeterministicPerMandate() {
        List<DemoProduct> standard = DemoProducts.forDefault();
        List<DemoProduct> extra = DemoProducts.forExtra();
        assertThat(standard).hasSize(80);
        assertThat(extra).hasSize(30);
        assertThat(standard.get(0).getSku()).startsWith("DP-");
        assertThat(extra.get(0).getSku()).startsWith("EX-");
        assertThat(DemoProducts.forDefault().get(5).getSku()).isEqualTo(standard.get(5).getSku());
    }

    @Test
    void emptyDatabaseIsFilledOnce() {
        MyUserRepository users = mock(MyUserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("{hash}");
        OidcConfigRepository oidc = mock(OidcConfigRepository.class);
        when(oidc.findAll()).thenReturn(List.of());
        DemoProductRepository products = mock(DemoProductRepository.class);
        UserPrefsSimpleStorage prefs = mock(UserPrefsSimpleStorage.class);
        OAuthMockProperties mock = new OAuthMockProperties();

        new DemoDatenLoader(users, encoder, oidc, products, prefs, mock, new DemoOidcProperties()).fill();
        verify(users, times(4)).save(any(MyUserEntity.class));
        verify(oidc).save(any(OidcConfig.class));
        verify(products, times(2)).saveAll(anyIterable());

        // second start: everything there already
        when(users.findByUsername(anyString())).thenReturn(new MyUserEntity());
        when(products.countByMandat(anyString())).thenReturn(1L);
        DemoDatenLoader wieder = new DemoDatenLoader(users, encoder, oidc, products, prefs, mock, new DemoOidcProperties());
        wieder.fill();
        verify(users, times(4)).save(any(MyUserEntity.class));
        verify(products, times(2)).saveAll(anyIterable());
    }

    /** Karte 2418: which OIDC entries the loader saves, per login mode. */
    private static List<OidcConfig> gespeichert(OAuthMockProperties mock, DemoOidcProperties own, List<OidcConfig> vorhanden) {
        MyUserRepository users = mock(MyUserRepository.class);
        when(users.findByUsername(anyString())).thenReturn(new MyUserEntity());
        OidcConfigRepository oidc = mock(OidcConfigRepository.class);
        when(oidc.findAll()).thenReturn(vorhanden);
        DemoProductRepository products = mock(DemoProductRepository.class);
        when(products.countByMandat(anyString())).thenReturn(1L);
        new DemoDatenLoader(users, mock(PasswordEncoder.class), oidc, products, mock(UserPrefsSimpleStorage.class), mock, own).fill();
        org.mockito.ArgumentCaptor<OidcConfig> saved = org.mockito.ArgumentCaptor.forClass(OidcConfig.class);
        verify(oidc, org.mockito.Mockito.atLeast(0)).save(saved.capture());
        return saved.getAllValues();
    }

    @Test
    void builtinLoginKeepsTheDemoIdpAndNoOwnIdp() {
        List<OidcConfig> s = gespeichert(new OAuthMockProperties(), new DemoOidcProperties(), List.of());
        assertThat(s).extracting(OidcConfig::getName).containsExactly(DemoDatenLoader.OIDC_NAME);
        assertThat(s.get(0).isEnabled()).isTrue();
    }

    @Test
    void formLoginSwitchesTheDemoIdpOff() {
        OAuthMockProperties aus = new OAuthMockProperties();
        aus.setEnabled(false);
        List<OidcConfig> s = gespeichert(aus, new DemoOidcProperties(), List.of());
        assertThat(s).hasSize(1);
        assertThat(s.get(0).isEnabled()).isFalse();
    }

    @Test
    void ownIdpIsStoredFromTheSettings() {
        OAuthMockProperties aus = new OAuthMockProperties();
        aus.setEnabled(false);
        DemoOidcProperties own = new DemoOidcProperties();
        own.setIssuerUrl("https://kc.example.org/realms/demo/");
        own.setClientId("demo");
        own.setClientSecret("s");
        List<OidcConfig> s = gespeichert(aus, own, List.of());
        OidcConfig eigen = s.stream().filter(c -> DemoDatenLoader.OWN_OIDC_NAME.equals(c.getName())).findFirst().orElseThrow();
        assertThat(eigen.isEnabled()).isTrue();
        assertThat(eigen.getIssuerUrl()).isEqualTo("https://kc.example.org/realms/demo");
        assertThat(eigen.getClientId()).isEqualTo("demo");
        assertThat(eigen.isAutoCreateUsers()).isFalse();
    }

    @Test
    void ownIdpIsSwitchedOffWhenNoLongerConfigured() {
        OidcConfig alt = new OidcConfig();
        alt.setName(DemoDatenLoader.OWN_OIDC_NAME);
        alt.setEnabled(true);
        gespeichert(new OAuthMockProperties(), new DemoOidcProperties(), List.of(alt));
        assertThat(alt.isEnabled()).isFalse();
    }

    @Test
    void everyDemoAccountHasAMandate() {
        Map<String, DemoDatenLoader.DemoUser> u = DemoDatenLoader.USERS;
        assertThat(u).containsOnlyKeys("root", "admin", "user", "extra");
        assertThat(u.get("extra").mandat()).isEqualTo(DemoDatenLoader.MANDAT_EXTRA);
        assertThat(u.get("user").mandat()).isEqualTo(DemoDatenLoader.MANDAT_DEFAULT);
    }
}
