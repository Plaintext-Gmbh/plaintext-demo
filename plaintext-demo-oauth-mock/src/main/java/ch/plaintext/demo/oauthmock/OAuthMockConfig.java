package ch.plaintext.demo.oauthmock;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import static ch.plaintext.demo.oauthmock.OAuthMockProperties.PATH;

/**
 * A complete OIDC provider inside the demo application, so the demo needs no Keycloak.
 *
 * <p>The endpoints follow Keycloak's layout ({@code <issuer>/protocol/openid-connect/…}), because
 * plaintext-root derives them from the issuer URL that way. Everything lives below
 * {@code /oauth-mock}; the filter chain matches only there and keeps its own login in its own
 * session attributes, so signing in at the mock is not yet a login to the application — that
 * happens when root's OIDC client completes the code flow.</p>
 */
@Configuration
@EnableConfigurationProperties(OAuthMockProperties.class)
public class OAuthMockConfig {

    static final String LOGIN = PATH + "/login";
    static final String ENDPOINTS = PATH + "/protocol/openid-connect";
    /** Registration id root derives from the OIDC configuration name "Demo IdP". */
    public static final String REGISTRATION_ID = "demo-idp";

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain oauthMockFilterChain(HttpSecurity http, OAuthMockProperties props,
                                             JWKSource<SecurityContext> oauthMockJwkSource) throws Exception {
        HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
        contexts.setSpringSecurityContextKey("DEMO_OAUTH_MOCK_SECURITY_CONTEXT");
        HttpSessionRequestCache requests = new HttpSessionRequestCache();
        requests.setSessionAttrName("DEMO_OAUTH_MOCK_SAVED_REQUEST");
        DaoAuthenticationProvider users = new DaoAuthenticationProvider(users(props));
        users.setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());

        http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher(PATH + "/**"))
                .oauth2AuthorizationServer(s -> s.oidc(Customizer.withDefaults()))
                // userinfo checks the access token; the decoder stays in this chain (no global JwtDecoder bean for root)
                .oauth2ResourceServer(rs -> rs.jwt(j -> j.decoder(NimbusJwtDecoder.withJwkSource(oauthMockJwkSource).build())))
                .authenticationProvider(users)
                .securityContext(c -> c.securityContextRepository(contexts))
                .requestCache(c -> c.requestCache(requests))
                .authorizeHttpRequests(a -> a.requestMatchers(LOGIN).permitAll().anyRequest().authenticated())
                .formLogin(f -> f.loginPage(LOGIN).permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN)));
        return http.build();
    }

    private static UserDetailsService users(OAuthMockProperties props) {
        return new InMemoryUserDetailsManager(props.getUsers().stream()
                .map(u -> User.withUsername(u.username()).password("{noop}" + u.password()).roles("DEMO").build())
                .toList());
    }

    @Bean
    AuthorizationServerSettings oauthMockServerSettings(OAuthMockProperties props) {
        return AuthorizationServerSettings.builder()
                .issuer(props.issuer())
                .authorizationEndpoint(ENDPOINTS + "/auth")
                .tokenEndpoint(ENDPOINTS + "/token")
                .jwkSetEndpoint(ENDPOINTS + "/certs")
                .oidcUserInfoEndpoint(ENDPOINTS + "/userinfo")
                .oidcLogoutEndpoint(ENDPOINTS + "/logout")
                .tokenRevocationEndpoint(ENDPOINTS + "/revoke")
                .tokenIntrospectionEndpoint(ENDPOINTS + "/token/introspect")
                .deviceAuthorizationEndpoint(ENDPOINTS + "/auth/device")
                .deviceVerificationEndpoint(PATH + "/device")
                .oidcClientRegistrationEndpoint(PATH + "/clients-registrations")
                .pushedAuthorizationRequestEndpoint(ENDPOINTS + "/ext/par/request")
                .build();
    }

    /** The application's client. The secret is encoded with the application's own encoder, which the server checks it with. */
    @Bean
    RegisteredClientRepository oauthMockClients(OAuthMockProperties props, PasswordEncoder passwordEncoder) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(props.getClientId())
                .clientSecret(passwordEncoder.encode(props.getClientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(props.getBaseUrl() + "/login/oauth2/code/" + REGISTRATION_ID)
                .postLogoutRedirectUri(props.getBaseUrl() + "/login.html?logout")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    /** ID token claims root reads: preferred_username (user mapping), and a verified e-mail. */
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> oauthMockClaims(OAuthMockProperties props) {
        return context -> {
            if (!OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                return;
            }
            String name = context.getPrincipal().getName();
            props.getUsers().stream().filter(u -> u.username().equals(name)).findFirst().ifPresent(u -> context.getClaims()
                    .claim("preferred_username", u.username())
                    .claim("name", u.name())
                    .claim("email", u.email())
                    .claim("email_verified", true));
        };
    }

    /** Signing key, new on every start: tokens never outlive the instance, nothing to store. */
    @Bean
    JWKSource<SecurityContext> oauthMockJwkSource() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAKey key = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }
}
