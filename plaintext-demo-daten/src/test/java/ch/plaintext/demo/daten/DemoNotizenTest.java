package ch.plaintext.demo.daten;

import ch.plaintext.ablagen.DateiAblage;
import ch.plaintext.notizen.NotizSpeicher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Card 2096: the demo's file store and the sample notes. */
class DemoNotizenTest {

    @Test
    void registerOffersOneLocalStoreBelowTheBase(@TempDir Path tmp) throws Exception {
        DemoAblagenRegister register = new DemoAblagenRegister(tmp.resolve("ablagen").toString());
        assertThat(register.namen()).containsExactly(DemoAblagenRegister.NOTIZEN);
        assertThat(register.ablage("andere")).isEmpty();

        DateiAblage a = register.ablage(DemoAblagenRegister.NOTIZEN).orElseThrow();
        a.schreibe("notizen/default/user.md", "x".getBytes(StandardCharsets.UTF_8), "text/markdown");
        assertThat(Files.readString(tmp.resolve("ablagen/notizen/notizen/default/user.md"))).isEqualTo("x");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sampleNoteOnlyForAccountsWithoutNote() throws Exception {
        NotizSpeicher speicher = mock(NotizSpeicher.class);
        when(speicher.ladeFuer("default", "root")).thenReturn(Optional.of("eigene Notiz"));
        when(speicher.ladeFuer("default", "user")).thenReturn(Optional.empty());
        when(speicher.ladeFuer("extra", "extra")).thenReturn(Optional.empty());
        ObjectProvider<NotizSpeicher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(speicher);

        Map<String, String> konten = new LinkedHashMap<>();
        konten.put("root", "default");
        konten.put("user", "default");
        konten.put("extra", "extra");
        assertThat(new DemoNotizen(provider).beispielnotizen(konten)).isEqualTo(2);
        verify(speicher).speichereFuer("default", "user", DemoNotizen.BEISPIEL);
        verify(speicher).speichereFuer("extra", "extra", DemoNotizen.BEISPIEL);
        verify(speicher, never()).speichereFuer("default", "root", DemoNotizen.BEISPIEL);
    }

    @Test
    @SuppressWarnings("unchecked")
    void withoutNotesModuleNothingHappens() throws Exception {
        ObjectProvider<NotizSpeicher> provider = mock(ObjectProvider.class);
        assertThat(new DemoNotizen(provider).beispielnotizen(Map.of("user", "default"))).isZero();
        assertThat(DemoNotizen.BEISPIEL).doesNotContain(";").contains("03:30");
    }
}
