package ch.plaintext.demo.daten;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * Fills the database once all beans exist and before the web server accepts requests — with
 * ApplicationReadyEvent the login page answered for a few seconds while the OIDC configuration
 * was still missing ("Invalid Client Registration").
 */
@Component
public class DemoDatenStart implements SmartInitializingSingleton {

    private final DemoDatenLoader loader;
    private final DemoNotizen notizen;

    public DemoDatenStart(DemoDatenLoader loader, DemoNotizen notizen) {
        this.loader = loader;
        this.notizen = notizen;
    }

    @Override
    public void afterSingletonsInstantiated() {
        loader.fill();
        java.util.Map<String, String> mandate = new java.util.LinkedHashMap<>();
        DemoDatenLoader.USERS.forEach((name, user) -> mandate.put(name, user.mandat()));
        org.slf4j.LoggerFactory.getLogger(DemoDatenStart.class)
                .info("plaintext-demo: {} sample note(s) written", notizen.beispielnotizen(mandate));
    }
}
