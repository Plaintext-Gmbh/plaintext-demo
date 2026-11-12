package ch.plaintext.demo.oauthmock;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * The demo identity provider. Everything here is demo data, published on purpose: the client
 * secret only protects the token endpoint of a mock that knows nothing but these users.
 *
 * <p>A class, not a record: plaintext-root's timing aspect proxies every bean below
 * {@code ch.plaintext}, and a record is final.</p>
 */
@ConfigurationProperties("demo.oauth-mock")
public class OAuthMockProperties {

    public static final String PATH = "/oauth-mock";

    /** Public base URL of the application (issuer = baseUrl + /oauth-mock). */
    private String baseUrl = "http://localhost:8080";
    /** Client the application registers in its OIDC configuration. */
    private String clientId = "plaintext-demo";
    private String clientSecret = "demo-only-not-a-secret";
    /** The accounts the mock knows (username = preferred_username claim). */
    private List<User> users = new ArrayList<>(List.of(
            new User("root", "root", "Rita Root", "root@demo.plaintext.ch"),
            new User("admin", "admin", "Adam Admin", "admin@demo.plaintext.ch"),
            new User("user", "user", "Uma User", "user@demo.plaintext.ch"),
            new User("extra", "extra", "Erik Extra", "extra@demo.plaintext.ch")));

    public String issuer() {
        return getBaseUrl() + PATH;
    }

    public String getBaseUrl() {
        return baseUrl.replaceAll("/+$", "");
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    /** One account of the mock. */
    public record User(String username, String password, String name, String email) {
    }
}
