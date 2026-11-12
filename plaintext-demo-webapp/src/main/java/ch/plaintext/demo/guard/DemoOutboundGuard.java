package ch.plaintext.demo.guard;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Calls that would make the public demo fetch an arbitrary URL. The OIDC "Test Connection" GETs
 * {@code <issuer>/.well-known/openid-configuration} for the issuer typed into the form, saved or not.
 */
@Aspect
@Component
public class DemoOutboundGuard {

    @Before("execution(* ch.plaintext.oidc.service.OidcConfigService.testConnection(..))")
    public void refuseOidcConnectionTest() {
        if (DemoWriteGuard.enabled) {
            throw new DemoWriteGuard.DemoReadOnlyException(
                    "Not possible in the public demo: connection test to an identity provider (outgoing request).");
        }
    }
}
