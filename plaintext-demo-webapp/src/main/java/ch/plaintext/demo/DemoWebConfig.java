package ch.plaintext.demo;

import ch.plaintext.boot.plugins.jsf.userprofile.UserPreferencesBackingBean;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoWebConfig {

    /** FORWARD as well: root forwards /login.html to /login.xhtml inside its rewrite filter. */
    @Bean
    FilterRegistrationBean<DemoDefaultLanguageFilter> demoDefaultLanguage(ObjectProvider<UserPreferencesBackingBean> preferences,
                                                                          @Value("${demo.default-language:en}") String language) {
        FilterRegistrationBean<DemoDefaultLanguageFilter> registration =
                new FilterRegistrationBean<>(new DemoDefaultLanguageFilter(preferences, language));
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
        return registration;
    }
}
