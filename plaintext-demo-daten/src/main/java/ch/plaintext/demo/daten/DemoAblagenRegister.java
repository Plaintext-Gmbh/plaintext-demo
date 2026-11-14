package ch.plaintext.demo.daten;

import ch.plaintext.ablagen.DateiAblage;
import ch.plaintext.ablagen.DateiAblagenRegister;
import ch.plaintext.boot.ablage.LokaleDateiAblage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The demo's only file store (card 2096): «Lokale Datei» from plaintext-root, a folder in the demo's data
 * directory. The public demo leaves out plaintext-admin-sidecars (where stores are normally set up under
 * Root → Sidecars), so it registers this one store itself; the notes module finds it through
 * {@link DateiAblagenRegister} and, as the only store, uses it without any setting.
 */
@Component
public class DemoAblagenRegister implements DateiAblagenRegister {

    private static final Logger log = LoggerFactory.getLogger(DemoAblagenRegister.class);

    static final String NOTIZEN = "demo-notizen";

    private final Path basis;

    public DemoAblagenRegister(@Value("${demo.ablagen.basis:./data/ablagen}") String basis) {
        this.basis = Path.of(basis);
    }

    @Override
    public List<String> namen() {
        return List.of(NOTIZEN);
    }

    @Override
    public Optional<DateiAblage> ablage(String name) {
        if (!NOTIZEN.equals(name == null ? null : name.strip())) {
            return Optional.empty();
        }
        try {
            return Optional.of(LokaleDateiAblage.unter(NOTIZEN, basis, "notizen"));
        } catch (IOException e) {
            log.warn("plaintext-demo: file store {} not usable: {}", NOTIZEN, e.getMessage());
            return Optional.empty();
        }
    }
}
