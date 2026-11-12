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

    public DemoDatenStart(DemoDatenLoader loader) {
        this.loader = loader;
    }

    @Override
    public void afterSingletonsInstantiated() {
        loader.fill();
    }
}
