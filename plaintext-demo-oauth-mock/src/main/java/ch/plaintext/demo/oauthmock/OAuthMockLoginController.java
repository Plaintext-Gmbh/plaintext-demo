package ch.plaintext.demo.oauthmock;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

/** Sign-in page of the demo identity provider. It lists the demo accounts on purpose. */
@Controller
public class OAuthMockLoginController {

    private final OAuthMockProperties props;
    private final org.springframework.beans.factory.ObjectProvider<ch.plaintext.i18n.web.I18nEL> i18n;
    private final org.springframework.beans.factory.ObjectProvider<ch.plaintext.boot.plugins.jsf.userprofile.UserPreferencesBackingBean> prefs;

    public OAuthMockLoginController(OAuthMockProperties props,
                                    org.springframework.beans.factory.ObjectProvider<ch.plaintext.i18n.web.I18nEL> i18n,
                                    org.springframework.beans.factory.ObjectProvider<ch.plaintext.boot.plugins.jsf.userprofile.UserPreferencesBackingBean> prefs) {
        this.props = props;
        this.i18n = i18n;
        this.prefs = prefs;
    }

    /** German text in the visitor's language (root's i18n, i18n/plaintext-demo.csv). */
    private String t(String german) {
        var el = i18n.getIfAvailable();
        return esc(el != null ? el.t(german) : german);
    }

    private String language() {
        try {
            var p = prefs.getIfAvailable();
            return p != null && p.getLanguage() != null ? p.getLanguage() : "en";
        } catch (RuntimeException e) {
            return "en";
        }
    }

    @GetMapping(value = OAuthMockConfig.LOGIN, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String login(HttpServletRequest request) {
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        StringBuilder accounts = new StringBuilder();
        for (OAuthMockProperties.User u : props.getUsers()) {
            accounts.append("<li><code>").append(esc(u.username())).append(" / ").append(esc(u.password()))
                    .append("</code> &ndash; ").append(esc(u.name())).append("</li>");
        }
        String error = request.getParameter("error") != null
                ? "<p class=\"error\">" + t("Ungültiger Benutzername oder Passwort") + "</p>" : "";
        String lang = language();
        String about = "de".equals(lang) ? "https://plaintext.ch/demo/" : "https://plaintext.ch/en/demo/";
        return """
                <!DOCTYPE html>
                <html lang="%s"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s &ndash; plaintext-demo</title>
                <style>
                body{font-family:system-ui,sans-serif;background:#f4f6f8;margin:0;display:flex;min-height:100vh;align-items:center;justify-content:center}
                main{background:#fff;padding:32px;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,.08);width:min(380px,90vw)}
                h1{font-size:20px;margin:0 0 4px}p.sub{color:#555;margin:0 0 20px;font-size:14px}
                label{display:block;font-size:13px;margin:12px 0 4px}input{width:100%%;box-sizing:border-box;padding:10px;border:1px solid #ccd;border-radius:6px;font-size:15px}
                button{margin-top:18px;width:100%%;padding:11px;border:0;border-radius:6px;background:#2563eb;color:#fff;font-size:15px;cursor:pointer}
                ul{padding-left:18px;font-size:14px}.error{color:#b91c1c;font-size:14px}.hint{font-size:13px;color:#555;margin-top:18px}
                </style></head><body><main>
                <h1>%s</h1>
                <p class="sub">%s</p>
                %s
                <form method="post" action="%s">
                <label for="username">%s</label><input id="username" name="username" autofocus autocomplete="username">
                <label for="password">%s</label><input id="password" name="password" type="password" autocomplete="current-password">
                <input type="hidden" name="%s" value="%s">
                <button type="submit">%s</button>
                </form>
                <div class="hint">%s<ul>%s</ul>
                <a href="%s">%s</a></div>
                </main></body></html>
                """.formatted(esc(lang), t("Demo-Identitätsanbieter"), t("Demo-Identitätsanbieter"),
                t("Ein eingebauter OAuth2/OIDC-Server, er ersetzt Keycloak in plaintext-demo."), error,
                request.getContextPath() + OAuthMockConfig.LOGIN, t("Benutzername"), t("Passwort"),
                csrf != null ? esc(csrf.getParameterName()) : "_csrf", csrf != null ? esc(csrf.getToken()) : "",
                t("Anmelden"), t("Demo-Konten:"), accounts, about, t("Über plaintext-demo"));
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }
}
