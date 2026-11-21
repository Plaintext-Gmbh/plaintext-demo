package ch.plaintext.demo.guard;

import jakarta.servlet.DispatcherType;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.List;

/**
 * Wires the demo guard: Hibernate write listeners and the blocked-pages filter. DEMO_GUARD=false turns both off,
 * except under the profile prod (demo.plaintext.ch, never start.sh): there the guard is always on (Karte 2418).
 */
@Configuration
public class DemoGuardConfig {

    public DemoGuardConfig(@Value("${demo.guard:true}") boolean enabled, Environment env) {
        DemoWriteGuard.enabled = enabled(enabled, env);
    }

    static boolean enabled(boolean setting, Environment env) {
        if (!setting && env.acceptsProfiles(Profiles.of("prod"))) {
            org.slf4j.LoggerFactory.getLogger(DemoGuardConfig.class).warn("plaintext-demo: DEMO_GUARD=false ignored, profile prod keeps the guard on");
            return true;
        }
        return setting;
    }

    @Bean
    HibernatePropertiesCustomizer demoWriteGuard() {
        DemoWriteGuard guard = new DemoWriteGuard();
        Integrator integrator = new Integrator() {
            @Override
            public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
                EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
                registry.appendListeners(EventType.PRE_INSERT, guard);
                registry.appendListeners(EventType.PRE_UPDATE, guard);
                registry.appendListeners(EventType.PRE_DELETE, guard);
                registry.appendListeners(EventType.PRE_COLLECTION_UPDATE, guard);
                registry.appendListeners(EventType.PRE_COLLECTION_REMOVE, guard);
                registry.appendListeners(EventType.PRE_COLLECTION_RECREATE, guard);
            }
        };
        return properties -> properties.put("hibernate.integrator_provider", (IntegratorProvider) () -> List.of(integrator));
    }

    @Bean
    FilterRegistrationBean<DemoBlockedViewsFilter> demoBlockedViews(
            org.springframework.beans.factory.ObjectProvider<ch.plaintext.i18n.web.I18nEL> i18n,
            org.springframework.beans.factory.ObjectProvider<ch.plaintext.boot.plugins.jsf.userprofile.UserPreferencesBackingBean> prefs) {
        FilterRegistrationBean<DemoBlockedViewsFilter> registration = new FilterRegistrationBean<>(new DemoBlockedViewsFilter(
                text -> { var el = i18n.getIfAvailable(); return el != null ? el.t(text) : text; },
                () -> { try { var p = prefs.getIfAvailable(); return p != null && p.getLanguage() != null ? p.getLanguage() : "en"; }
                        catch (RuntimeException e) { return "en"; } }));
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
        return registration;
    }
}
