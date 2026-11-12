package ch.plaintext.demo;

import ch.plaintext.boot.plugins.jsf.userprofile.UserPreferencesBackingBean;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

/**
 * The demo speaks English before login, too. plaintext-root falls back to German for anonymous
 * visitors; this sets the session's (unsaved) anonymous preference to {@code demo.default-language}
 * on page requests. Signed-in users keep the language stored in their preferences.
 */

public class DemoDefaultLanguageFilter implements Filter {

    private final ObjectProvider<UserPreferencesBackingBean> preferences;
    private final String language;

    public DemoDefaultLanguageFilter(ObjectProvider<UserPreferencesBackingBean> preferences, String language) {
        this.preferences = preferences;
        this.language = language;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest http && isPage(http) && isAnonymous()) {
            UserPreferencesBackingBean prefs = preferences.getIfAvailable();
            if (prefs != null && !language.equals(prefs.getLanguage())) {
                prefs.setLanguage(language);
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isPage(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "GET".equals(request.getMethod()) && (uri.endsWith(".html") || uri.endsWith(".xhtml") || uri.endsWith("/"));
    }

    private static boolean isAnonymous() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated();
    }
}
