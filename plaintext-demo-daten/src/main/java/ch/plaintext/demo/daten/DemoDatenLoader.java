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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fills an empty demo database: the four demo users, the two mandates (they come from the users'
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

    public DemoDatenLoader(MyUserRepository users, PasswordEncoder passwordEncoder, OidcConfigRepository oidcConfigs,
                           DemoProductRepository products, UserPrefsSimpleStorage preferences, OAuthMockProperties oauthMock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.oidcConfigs = oidcConfigs;
        this.products = products;
        this.preferences = preferences;
        this.oauthMock = oauthMock;
    }

    @EventListener(ApplicationReadyEvent.class)
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

    private void oidcConfig() {
        OidcConfig config = oidcConfigs.findAll().stream().filter(c -> OIDC_NAME.equals(c.getName())).findFirst()
                .orElseGet(OidcConfig::new);
        config.setName(OIDC_NAME);
        config.setEnabled(true);
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
