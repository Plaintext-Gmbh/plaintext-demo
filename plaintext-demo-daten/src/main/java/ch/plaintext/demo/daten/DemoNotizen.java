package ch.plaintext.demo.daten;

import ch.plaintext.notizen.NotizSpeicher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Sample note for every demo account (card 2096): written at start when the account has no note yet. After
 * the nightly reset the notes folder is gone (see DemoReset), so the sample is back every morning.
 */
@Component
public class DemoNotizen {

    private static final Logger log = LoggerFactory.getLogger(DemoNotizen.class);

    static final String BEISPIEL = """
            # Welcome to Notes

            Everything you type here is saved on its own - the status next to the toolbar shows **Saved** with the time.

            - **Bold**, *italic*, lists and headings: use the toolbar or Ctrl+B / Ctrl+I. The note stays plain Markdown.
            - **Open in its own window** keeps the notes next to your other work.
            - The note is a Markdown file in a plaintext-root file store - here the store type «Local file».

            This is a public demo: everyone who signs in with the same account sees the same note.
            The demo resets every night at 03:30 (Europe/Zurich), then this sample note is back.
            """;

    private final ObjectProvider<NotizSpeicher> speicher;

    public DemoNotizen(ObjectProvider<NotizSpeicher> speicher) {
        this.speicher = speicher;
    }

    /** @return how many sample notes were written */
    public int beispielnotizen(Map<String, String> mandatJeBenutzer) {
        NotizSpeicher s = speicher.getIfAvailable();
        if (s == null) {
            return 0;
        }
        int neu = 0;
        for (Map.Entry<String, String> e : mandatJeBenutzer.entrySet()) {
            try {
                if (s.ladeFuer(e.getValue(), e.getKey()).isEmpty()) {
                    s.speichereFuer(e.getValue(), e.getKey(), BEISPIEL);
                    neu++;
                }
            } catch (IOException | RuntimeException ex) {
                log.warn("plaintext-demo: sample note for {} not written: {}", e.getKey(), ex.getMessage());
            }
        }
        return neu;
    }
}
