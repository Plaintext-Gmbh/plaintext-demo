package ch.plaintext.demo.tabelle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** root's seed import stops at the first malformed line, so every data line needs exactly three fields. */
class DemoTranslationsTest {

    @Test
    void everyLineHasLabelLanguageText() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/i18n/plaintext-demo.csv")) {
            assertNotNull(in);
            List<String> bad = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(l -> !l.isBlank() && !l.startsWith("#") && !l.startsWith("defaultLabel;"))
                    .filter(l -> l.split(";", -1).length != 3 || l.split(";", -1)[1].length() > 10)
                    .toList();
            assertEquals(List.of(), bad, "Zeilen ohne genau label;sprache;text (ein ';' im Text bricht den Import ab)");
        }
    }
}
