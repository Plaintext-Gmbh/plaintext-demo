package ch.plaintext.demo.h2;

import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** On the H2 datasource, Flyway reads root's migrations through {@link H2MigrationResources}. */
@Configuration
public class H2FlywayConfig {

    @Bean
    FlywayConfigurationCustomizer h2Migrations(Environment env) {
        return configuration -> {
            if (env.getProperty("spring.datasource.url", "").startsWith("jdbc:h2:")) {
                configuration.resourceProvider(new H2MigrationResources());
            }
        };
    }
}
