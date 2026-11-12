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
            response.getWriter().write("""
                    <!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><title>Not in the public demo</title></head>
                    <body style="font-family:system-ui,sans-serif;max-width:560px;margin:80px auto;line-height:1.5">
                    <h1>Not in the public demo</h1>
                    <p>Everyone is signed in with the same demo accounts here, so this page is switched off.
                    Start plaintext-demo on your own computer to try it.</p>
                    <p><a href="index.html">Back to the demo</a> &middot; <a href="https://plaintext.ch/en/demo/">About plaintext-demo</a></p>
                    </body></html>
                    """);
            return;
        }
        chain.doFilter(req, res);
    }

    static boolean isBlocked(String uri) {
        if (uri == null) {
            return false;
        }
        String page = uri.substring(uri.lastIndexOf('/') + 1).replaceFirst("\\.(x?html)$", "");
        return BLOCKED.contains(page);
    }
}
