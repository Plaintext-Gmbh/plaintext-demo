package ch.plaintext.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/** Karte 2541: /nosec/version shows the demo's own Maven version, filled in at build time. */
class VersionTest {

    @Test
    void eigeneVersionIstEingesetzt() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("config/application.yml"));
        assertThat(yaml.getObject().getProperty("plaintext.version"))
                .as("Maven resource filter must replace @project.version@")
                .matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?");
    }
}
