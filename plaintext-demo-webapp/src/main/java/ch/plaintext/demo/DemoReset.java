package ch.plaintext.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Nightly reset of the public demo. The scheduled {@link DemoResetJob} leaves a marker in the data
 * directory and ends the application; the container restarts it, and before Spring starts,
 * {@link #beforeStart} deletes the H2 files, so Flyway and the demo-data loader build a fresh
 * database. No cron outside the application, and the reset only ever touches the demo's own
 * data directory.
 */
public final class DemoReset {

    static final String MARKER = ".reset-requested";

    private DemoReset() {
    }

    /** Data directory as the datasource sees it (DEMO_DATA_DIR, default ./data). */
    public static Path dataDir() {
        String dir = System.getenv("DEMO_DATA_DIR");
        return Path.of(dir == null || dir.isBlank() ? "data" : dir);
    }

    /** @return true if a reset was carried out */
    public static boolean beforeStart(Path dataDir) {
        if (!Files.exists(dataDir.resolve(MARKER))) {
            return false;
        }
        try (Stream<Path> files = Files.list(dataDir)) {
            for (Path f : files.toList()) {
                String name = f.getFileName().toString();
                if (name.endsWith(".mv.db") || name.endsWith(".trace.db") || name.equals(MARKER)) {
                    Files.delete(f);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("plaintext-demo: reset failed in " + dataDir, e);
        }
        return true;
    }

    static void request(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        Files.writeString(dataDir.resolve(MARKER), "requested\n");
    }
}
