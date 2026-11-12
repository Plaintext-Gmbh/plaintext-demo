package ch.plaintext.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoResetTest {

    @Test
    void withoutMarkerNothingIsDeleted(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("plaintext-demo.mv.db"), "x");
        assertFalse(DemoReset.beforeStart(dir));
        assertTrue(Files.exists(dir.resolve("plaintext-demo.mv.db")));
    }

    @Test
    void markerDeletesOnlyTheH2FilesAndItself(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("plaintext-demo.mv.db"), "x");
        Files.writeString(dir.resolve("plaintext-demo.trace.db"), "x");
        Files.writeString(dir.resolve("keep.txt"), "x");
        DemoReset.request(dir);

        assertTrue(DemoReset.beforeStart(dir));

        assertFalse(Files.exists(dir.resolve("plaintext-demo.mv.db")));
        assertFalse(Files.exists(dir.resolve("plaintext-demo.trace.db")));
        assertFalse(Files.exists(dir.resolve(DemoReset.MARKER)));
        assertTrue(Files.exists(dir.resolve("keep.txt")), "only the database goes");
    }

    @Test
    void missingDataDirectoryIsNoReset(@TempDir Path dir) {
        assertFalse(DemoReset.beforeStart(dir.resolve("noch-nicht-da")));
    }
}
