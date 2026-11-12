package ch.plaintext.demo.guard;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/** Pages that do not belong in a public demo where anyone is root: they answer with a short notice. */
public class DemoBlockedViewsFilter implements Filter {

    // German keys, translated through root's i18n (i18n/plaintext-demo.csv), like every demo text.
    static final String TITLE = "In der öffentlichen Demo nicht verfügbar";
    static final String TEXT = "Hier sind alle mit denselben Demo-Konten angemeldet, darum ist diese Seite ausgeschaltet. "
            + "Starten Sie plaintext-demo auf Ihrem eigenen Rechner, um sie auszuprobieren.";
    static final String BACK = "Zurück zur Demo";
    static final String ABOUT = "Über plaintext-demo";

    private final java.util.function.UnaryOperator<String> t;
    private final java.util.function.Supplier<String> language;

    /** @param t translation of a German text into the visitor's language; @param language its code (de, en, fr, it) */
    public DemoBlockedViewsFilter(java.util.function.UnaryOperator<String> t, java.util.function.Supplier<String> language) {
        this.t = t;
        this.language = language;
    }

    static final Set<String> BLOCKED = Set.of(
            "api-token", "admin-api-token", "root-api-token",   // issue access tokens
            "sessions", "sessioninsights",                     // other visitors' IPs and browsers
            "freigabe-links",                                  // public links to content
            "modulauswahl",                                    // runtime module selection
            "adminentities", "rootentities",                   // generic editor for every entity
            "debug");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (req instanceof HttpServletRequest request && res instanceof HttpServletResponse response
                && DemoWriteGuard.enabled && isBlocked(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/html;charset=UTF-8");
            String lang = language.get();
            String about = "de".equals(lang) ? "https://plaintext.ch/demo/" : "https://plaintext.ch/en/demo/";
            String title = esc(t.apply(TITLE));
            response.getWriter().write("<!DOCTYPE html><html lang=\"" + esc(lang) + "\"><head><meta charset=\"utf-8\"><title>"
                    + title + "</title></head><body style=\"font-family:system-ui,sans-serif;max-width:560px;margin:80px auto;line-height:1.5\">"
                    + "<h1>" + title + "</h1><p>" + esc(t.apply(TEXT)) + "</p><p><a href=\"index.html\">" + esc(t.apply(BACK))
                    + "</a> &middot; <a href=\"" + about + "\">" + esc(t.apply(ABOUT)) + "</a></p></body></html>");
            return;
        }
        chain.doFilter(req, res);
    }

    private static String esc(String s) {
        return org.springframework.web.util.HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    static boolean isBlocked(String uri) {
        if (uri == null) {
            return false;
        }
        String page = uri.substring(uri.lastIndexOf('/') + 1).replaceFirst("\\.(x?html)$", "");
        return BLOCKED.contains(page);
    }
}
