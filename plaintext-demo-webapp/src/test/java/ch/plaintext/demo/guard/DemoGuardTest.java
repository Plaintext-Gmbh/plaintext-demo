package ch.plaintext.demo.guard;

import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoGuardTest {

    // Stand-ins: the guard matches entities by simple class name.
    static class OidcConfig { }
    static class I18nTranslation { }
    static class MyUserEntity {
        final String username;
        MyUserEntity(String username) { this.username = username; }
        public String getUsername() { return username; }
    }

    private final DemoWriteGuard guard = new DemoWriteGuard();

    @BeforeEach
    void inRequest() {
        DemoWriteGuard.enabled = true;
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void readOnlyEntityCannotBeCreatedOrChangedFromTheWeb() {
        assertThrows(DemoWriteGuard.DemoReadOnlyException.class, () -> guard.onPreInsert(insert(new OidcConfig())));
        assertThrows(DemoWriteGuard.DemoReadOnlyException.class,
                () -> guard.onPreUpdate(update(new OidcConfig(), new String[]{"issuerUrl"}, new Object[]{"a"}, new Object[]{"b"})));
    }

    @Test
    void outsideARequestEverythingIsAllowed() {
        RequestContextHolder.resetRequestAttributes();
        assertDoesNotThrow(() -> guard.onPreInsert(insert(new OidcConfig())));
    }

    @Test
    void guardOffAllowsEverything() {
        DemoWriteGuard.enabled = false;
        assertDoesNotThrow(() -> guard.onPreInsert(insert(new OidcConfig())));
    }

    @Test
    void translationPlaceholdersMayBeCreatedButNotEdited() {
        assertDoesNotThrow(() -> guard.onPreInsert(insert(new I18nTranslation())));
        assertThrows(DemoWriteGuard.DemoReadOnlyException.class,
                () -> guard.onPreUpdate(update(new I18nTranslation(), new String[]{"text"}, new Object[]{"X_a"}, new Object[]{"b"})));
    }

    @Test
    void demoAccountKeepsPasswordButMayGetItsOidcLink() {
        String[] names = {"username", "password", "oidcSubject"};
        assertThrows(DemoWriteGuard.DemoReadOnlyException.class, () -> guard.onPreUpdate(update(new MyUserEntity("root"), names,
                new Object[]{"root", "h1", null}, new Object[]{"root", "h2", null})));
        assertDoesNotThrow(() -> guard.onPreUpdate(update(new MyUserEntity("root"), names,
                new Object[]{"root", "h1", null}, new Object[]{"root", "h1", "sub-1"})));
        assertDoesNotThrow(() -> guard.onPreUpdate(update(new MyUserEntity("someone"), names,
                new Object[]{"someone", "h1", null}, new Object[]{"someone", "h2", null})), "own users may change");
    }

    @Test
    void demoAccountCannotBeDeleted() {
        PreDeleteEvent delete = mock(PreDeleteEvent.class);
        EntityPersister persister = mock(EntityPersister.class);
        when(delete.getEntity()).thenReturn(new MyUserEntity("extra"));
        when(delete.getPersister()).thenReturn(persister);
        when(persister.getPropertyNames()).thenReturn(new String[]{"username"});
        when(delete.getDeletedState()).thenReturn(new Object[]{"extra"});
        assertThrows(DemoWriteGuard.DemoReadOnlyException.class, () -> guard.onPreDelete(delete));
    }

    @Test
    void blockedPagesAnswerWithANotice() throws Exception {
        assertTrue(DemoBlockedViewsFilter.isBlocked("/root-api-token.html"));
        assertTrue(DemoBlockedViewsFilter.isBlocked("/sessions.xhtml"));
        assertFalse(DemoBlockedViewsFilter.isBlocked("/demo-table.html"));
        assertFalse(DemoBlockedViewsFilter.isBlocked(null));

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        MockFilterChain chainBlocked = new MockFilterChain();
        new DemoBlockedViewsFilter().doFilter(new MockHttpServletRequest("GET", "/sessions.html"), blocked, chainBlocked);
        assertEquals(403, blocked.getStatus());
        assertTrue(blocked.getContentAsString().contains("Not in the public demo"));
        assertNull(chainBlocked.getRequest(), "the page itself is not rendered");

        MockFilterChain chainOpen = new MockFilterChain();
        new DemoBlockedViewsFilter().doFilter(new MockHttpServletRequest("GET", "/demo-table.html"), new MockHttpServletResponse(), chainOpen);
        assertNotNull(chainOpen.getRequest());
    }

    @Test
    void refusalIsFoundInTheCauseChain() {
        Exception wrapped = new RuntimeException("commit failed", new IllegalStateException(
                new DemoWriteGuard.DemoReadOnlyException("Not possible in the public demo: x")));
        assertEquals("Not possible in the public demo: x", DemoExceptionHandlerFactory.refusal(wrapped));
        assertNull(DemoExceptionHandlerFactory.refusal(new RuntimeException("other")));
    }

    private static PreInsertEvent insert(Object entity) {
        PreInsertEvent event = mock(PreInsertEvent.class);
        when(event.getEntity()).thenReturn(entity);
        return event;
    }

    private static PreUpdateEvent update(Object entity, String[] names, Object[] old, Object[] now) {
        PreUpdateEvent event = mock(PreUpdateEvent.class);
        EntityPersister persister = mock(EntityPersister.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getPersister()).thenReturn(persister);
        when(persister.getPropertyNames()).thenReturn(names);
        when(event.getOldState()).thenReturn(old);
        when(event.getState()).thenReturn(now);
        return event;
    }
}
