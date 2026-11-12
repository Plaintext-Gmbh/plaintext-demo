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

    public OAuthMockLoginController(OAuthMockProperties props) {
        this.props = props;
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
                ? "<p class=\"error\">Unknown user or wrong password.</p>" : "";
        return """
                <!DOCTYPE html>
                <html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Demo IdP &ndash; plaintext-demo</title>
                <style>
                body{font-family:system-ui,sans-serif;background:#f4f6f8;margin:0;display:flex;min-height:100vh;align-items:center;justify-content:center}
                main{background:#fff;padding:32px;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,.08);width:min(380px,90vw)}
                h1{font-size:20px;margin:0 0 4px}p.sub{color:#555;margin:0 0 20px;font-size:14px}
                label{display:block;font-size:13px;margin:12px 0 4px}input{width:100%%;box-sizing:border-box;padding:10px;border:1px solid #ccd;border-radius:6px;font-size:15px}
                button{margin-top:18px;width:100%%;padding:11px;border:0;border-radius:6px;background:#2563eb;color:#fff;font-size:15px;cursor:pointer}
                ul{padding-left:18px;font-size:14px}.error{color:#b91c1c;font-size:14px}.hint{font-size:13px;color:#555;margin-top:18px}
                </style></head><body><main>
                <h1>Demo identity provider</h1>
                <p class="sub">A built-in OAuth2/OIDC server &ndash; stands in for Keycloak in plaintext-demo.</p>
                %s
                <form method="post" action="%s">
                <label for="username">Username</label><input id="username" name="username" autofocus autocomplete="username">
                <label for="password">Password</label><input id="password" name="password" type="password" autocomplete="current-password">
                <input type="hidden" name="%s" value="%s">
                <button type="submit">Sign in</button>
                </form>
                <div class="hint">Demo accounts:<ul>%s</ul>
                <a href="https://plaintext.ch/en/demo/">About plaintext-demo</a></div>
                </main></body></html>
                """.formatted(error, request.getContextPath() + OAuthMockConfig.LOGIN,
                csrf != null ? esc(csrf.getParameterName()) : "_csrf", csrf != null ? esc(csrf.getToken()) : "",
                accounts);
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }
}
