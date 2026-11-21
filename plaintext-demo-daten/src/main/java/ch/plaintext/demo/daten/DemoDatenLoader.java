package ch.plaintext.demo.daten;

import ch.plaintext.boot.plugins.jsf.userprofile.UserPreference;
import ch.plaintext.boot.plugins.jsf.userprofile.UserPrefsSimpleStorage;
import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import ch.plaintext.demo.oauthmock.OAuthMockProperties;
import ch.plaintext.demo.tabelle.DemoProduct;
import ch.plaintext.demo.tabelle.DemoProductRepository;
import ch.plaintext.oidc.entity.OidcConfig;
import ch.plaintext.oidc.repository.OidcConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fills an empty demo database (called by {@link DemoDatenStart} before the web server takes requests): the four demo users, the two mandates (they come from the users'
 * mandate), the OIDC configuration pointing at the built-in IdP, and the demo rows. Idempotent —
 * runs on every start and only adds what is missing; the OIDC issuer is refreshed, because it
 * depends on the base URL the instance runs under.
 */
@Slf4j
@Component
public class DemoDatenLoader {

    static final String MANDAT_DEFAULT = "default";
    static final String MANDAT_EXTRA = "extra";
    static final String OIDC_NAME = "Demo IdP";
    /** Karte 2418: the own OIDC server of a self-hosted demo (profile login-oidc). */
    static final String OWN_OIDC_NAME = "Own IdP";

    /** username -> roles and mandate. root sees everything, extra is admin of the second mandate. */
    static final Map<String, DemoUser> USERS = Map.of(
            "root", new DemoUser(Set.of("root", "admin", "user"), MANDAT_DEFAULT),
            "admin", new DemoUser(Set.of("admin", "user"), MANDAT_DEFAULT),
            "user", new DemoUser(Set.of("user"), MANDAT_DEFAULT),
            "extra", new DemoUser(Set.of("admin", "user"), MANDAT_EXTRA));

    private final MyUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OidcConfigRepository oidcConfigs;
    private final DemoProductRepository products;
    private final UserPrefsSimpleStorage preferences;
    private final OAuthMockProperties oauthMock;
    private final DemoOidcProperties ownOidc;

    public DemoDatenLoader(MyUserRepository users, PasswordEncoder passwordEncoder, OidcConfigRepository oidcConfigs,
                           DemoProductRepository products, UserPrefsSimpleStorage preferences, OAuthMockProperties oauthMock,
                           DemoOidcProperties ownOidc) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.oidcConfigs = oidcConfigs;
        this.products = products;
        this.preferences = preferences;
        this.oauthMock = oauthMock;
        this.ownOidc = ownOidc;
    }

    @Transactional
    public void fill() {
        int newUsers = 0;
        for (OAuthMockProperties.User u : oauthMock.getUsers()) {
            DemoUser spec = USERS.get(u.username());
            if (spec != null && users.findByUsername(u.username()) == null) {
                createUser(u, spec);
                newUsers++;
            }
        }
        oidcConfig();
        ownOidcConfig();
        int newRows = fillProducts(MANDAT_DEFAULT, DemoProducts.forDefault()) + fillProducts(MANDAT_EXTRA, DemoProducts.forExtra());
        log.info("plaintext-demo: {} demo user(s) and {} demo row(s) created, IdP issuer {}", newUsers, newRows, oauthMock.issuer());
    }

    private void createUser(OAuthMockProperties.User u, DemoUser spec) {
        MyUserEntity user = new MyUserEntity();
        user.setUsername(u.username());
        user.setPassword(passwordEncoder.encode(u.password()));
        spec.roles().forEach(user::addRole);
        user.setMandat(spec.mandat());
        String[] name = u.name().split(" ", 2);
        user.setVorname(name[0]);
        user.setNachname(name.length > 1 ? name[1] : "");
        users.save(user);
        try {
            UserPreference prefs = new UserPreference();
            prefs.setUniqueId(u.username());
            prefs.setLanguage("en");
            preferences.save(prefs);
        } catch (RuntimeException e) {
            log.warn("plaintext-demo: language preference for {} not stored: {}", u.username(), e.toString());
        }
    }

    private OidcConfig named(String name) {
        return oidcConfigs.findAll().stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }

    /** The built-in IdP; switched off (not deleted) when the mock is off, so the login shows no dead button. */
    private void oidcConfig() {
        OidcConfig config = named(OIDC_NAME);
        if (config == null) {
            config = new OidcConfig();
        }
        config.setName(OIDC_NAME);
        config.setEnabled(oauthMock.isEnabled());
        config.setIssuerUrl(oauthMock.issuer());
        config.setClientId(oauthMock.getClientId());
        config.setClientSecret(oauthMock.getClientSecret());
        config.setScopes("openid,profile,email");
        config.setButtonLabel("Sign in with the demo IdP");
        config.setButtonIcon("pi pi-sign-in");
        config.setUsernameAttribute("preferred_username");
        config.setAutoCreateUsers(false);
        oidcConfigs.save(config);
    }

    /** Own IdP from demo.oidc.*: refreshed on every start; without an issuer an existing entry is switched off. */
    private void ownOidcConfig() {
        OidcConfig config = named(OWN_OIDC_NAME);
        if (!ownOidc.isConfigured()) {
            if (config != null && config.isEnabled()) {
                config.setEnabled(false);
                oidcConfigs.save(config);
            }
            return;
        }
        if (config == null) {
            config = new OidcConfig();
        }
        config.setName(OWN_OIDC_NAME);
        config.setEnabled(true);
        config.setIssuerUrl(ownOidc.getIssuerUrl().replaceAll("/+$", ""));
        config.setClientId(ownOidc.getClientId());
        config.setClientSecret(ownOidc.getClientSecret());
        config.setScopes("openid,profile,email");
        config.setButtonLabel(ownOidc.getButtonLabel());
        config.setButtonIcon("pi pi-sign-in");
        config.setUsernameAttribute(ownOidc.getUsernameAttribute());
        config.setAutoCreateUsers(ownOidc.isAutoCreateUsers());
        oidcConfigs.save(config);
    }

    private int fillProducts(String mandat, List<DemoProduct> rows) {
        if (products.countByMandat(mandat) > 0) {
            return 0;
        }
        rows.forEach(p -> p.setMandat(mandat));
        products.saveAll(rows);
        return rows.size();
    }

    record DemoUser(Set<String> roles, String mandat) {
    }
}
