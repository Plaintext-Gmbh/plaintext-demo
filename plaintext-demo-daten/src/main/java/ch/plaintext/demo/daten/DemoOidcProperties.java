package ch.plaintext.demo.daten;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Karte 2418: an own OIDC server for a self-hosted demo (profile login-oidc, e.g. Keycloak).
 * With an issuer the demo data keep the entry «Own IdP» (registration id own-idp, redirect URI
 * {@code <base>/login/oauth2/code/own-idp}) enabled; without one it stays switched off.
 * A class, not a record: plaintext-root's timing aspect proxies every bean below ch.plaintext.
 */
@Component
@ConfigurationProperties("demo.oidc")
public class DemoOidcProperties {

    private String issuerUrl = "";
    private String clientId = "";
    private String clientSecret = "";
    private String buttonLabel = "Sign in";
    private String usernameAttribute = "preferred_username";
    private boolean autoCreateUsers;

    public boolean isConfigured() {
        return issuerUrl != null && !issuerUrl.isBlank();
    }

    public String getIssuerUrl() { return issuerUrl; }
    public void setIssuerUrl(String issuerUrl) { this.issuerUrl = issuerUrl; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getButtonLabel() { return buttonLabel; }
    public void setButtonLabel(String buttonLabel) { this.buttonLabel = buttonLabel; }
    public String getUsernameAttribute() { return usernameAttribute; }
    public void setUsernameAttribute(String usernameAttribute) { this.usernameAttribute = usernameAttribute; }
    public boolean isAutoCreateUsers() { return autoCreateUsers; }
    public void setAutoCreateUsers(boolean autoCreateUsers) { this.autoCreateUsers = autoCreateUsers; }
}
