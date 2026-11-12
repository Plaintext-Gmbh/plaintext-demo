package ch.plaintext.demo.guard;

import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;
import java.util.Set;

/**
 * The public demo is signed in as root by anyone. This keeps what would lock others out or reach
 * beyond the demo read-only — for writes inside an HTTP request only; startup, the demo-data loader
 * and crons are not affected. Entities are matched by simple class name, so the guard needs no
 * compile-time dependency on the modules it protects.
 */
public class DemoWriteGuard implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener,
        PreCollectionUpdateEventListener, PreCollectionRemoveEventListener, PreCollectionRecreateEventListener {

    /** No writes at all from the web. */
    static final Set<String> READ_ONLY = Set.of(
            "OidcConfig", "OidcModuleConfiguration", "I18nConfig", "Setting", "SetupConfig", "BrandingLogo",
            "MailTemplate", "MailTemplateModuleConfiguration", "CronConfigEntity", "ModuleConfig",
            "ModulesModuleConfiguration", "SettingsModuleConfiguration", "Rollenzuteilung", "ApiToken",
            "ApiTokenModuleConfiguration", "FreigabeLink", "FreigabeLinkModuleConfiguration",
            "RegistrationToken", "MagicLinkToken", "PasswordResetToken");
    /** root creates missing translations as X_ placeholders while rendering; editing them is blocked. */
    static final Set<String> NO_CHANGE = Set.of("I18nTranslation");
    static final Set<String> DEMO_ACCOUNTS = Set.of("root", "admin", "user", "extra");
    /** What may change on a demo account: the OIDC link made at its first IdP login, and the start page. */
    static final Set<String> DEMO_ACCOUNT_MAY_CHANGE = Set.of("oidcSubject", "startpage");

    static volatile boolean enabled = true;

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        check(event.getEntity(), "create", READ_ONLY);
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        Object entity = event.getEntity();
        check(entity, "change", READ_ONLY, NO_CHANGE);
        if (isDemoAccount(entity, event.getOldState(), event.getPersister().getPropertyNames())) {
            String[] names = event.getPersister().getPropertyNames();
            for (int i = 0; i < names.length; i++) {
                if (!DEMO_ACCOUNT_MAY_CHANGE.contains(names[i])
                        && !Objects.equals(event.getOldState()[i], event.getState()[i])) {
                    refuse("change '" + names[i] + "' of the demo account");
                }
            }
        }
        return false;
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        check(event.getEntity(), "delete", READ_ONLY, NO_CHANGE);
        if (isDemoAccount(event.getEntity(), event.getDeletedState(), event.getPersister().getPropertyNames())) {
            refuse("delete a demo account");
        }
        return false;
    }

    @Override
    public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
        collection(event);
    }

    @Override
    public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
        collection(event);
    }

    @Override
    public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
        collection(event);
    }

    /** The roles of a demo account are an element collection. */
    private void collection(AbstractCollectionEvent event) {
        Object owner = event.getAffectedOwnerOrNull();
        if (owner != null && active()) {
            check(owner, "change", READ_ONLY);
            if ("MyUserEntity".equals(owner.getClass().getSimpleName()) && DEMO_ACCOUNTS.contains(username(owner))) {
                refuse("change the roles of a demo account");
            }
        }
    }

    @SafeVarargs
    private static void check(Object entity, String what, Set<String>... sets) {
        if (entity == null || !active()) {
            return;
        }
        String type = entity.getClass().getSimpleName();
        for (Set<String> set : sets) {
            if (set.contains(type)) {
                refuse(what + " " + type);
            }
        }
    }

    private static boolean isDemoAccount(Object entity, Object[] state, String[] names) {
        if (entity == null || !active() || !"MyUserEntity".equals(entity.getClass().getSimpleName())) {
            return false;
        }
        for (int i = 0; i < names.length; i++) {
            if ("username".equals(names[i]) && state != null && DEMO_ACCOUNTS.contains(String.valueOf(state[i]))) {
                return true;
            }
        }
        return DEMO_ACCOUNTS.contains(username(entity));
    }

    private static String username(Object user) {
        try {
            return String.valueOf(user.getClass().getMethod("getUsername").invoke(user));
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    static boolean active() {
        return enabled && RequestContextHolder.getRequestAttributes() != null;
    }

    private static void refuse(String what) {
        throw new DemoReadOnlyException(what);
    }

    /** Thrown when the demo refuses a write. The page shows {@link #KEY} and {@link #HINT} translated, plus the detail. */
    public static class DemoReadOnlyException extends RuntimeException {
        public static final String KEY = "In der öffentlichen Demo nicht möglich";
        public static final String HINT = "Die Demo wird jede Nacht zurückgesetzt. Lokal gestartet lässt sich alles ausprobieren.";
        private final String detail;

        public DemoReadOnlyException(String detail) {
            super(KEY + ": " + detail);
            this.detail = detail;
        }

        public String getDetail() {
            return detail;
        }
    }
}
