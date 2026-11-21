package ch.plaintext.demo.oauthmock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the mock's properties even with the mock switched off: the demo data take the accounts from them (Karte 2418). */
@Configuration
@EnableConfigurationProperties(OAuthMockProperties.class)
public class OAuthMockPropertiesConfig {
}
