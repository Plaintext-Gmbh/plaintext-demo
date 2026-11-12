package ch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** plaintext-demo: the plaintext-root platform with demo modules, on an embedded H2 database. */
@SpringBootApplication
@ComponentScan(basePackages = {"ch.plaintext"})
@EntityScan(basePackages = {"ch.plaintext"})
@EnableJpaRepositories(basePackages = {"ch.plaintext"})
@EnableScheduling
@EnableAsync
@EnableMethodSecurity
public class DemoBootApplication {

    public static void main(String[] args) {
        if (ch.plaintext.demo.DemoReset.beforeStart(ch.plaintext.demo.DemoReset.dataDir())) {
            System.out.println("plaintext-demo: database reset, starting empty");
        }
        SpringApplication.run(DemoBootApplication.class, args);
    }
}
