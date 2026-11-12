package ch.plaintext.demo.h2;

import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Flyway resources = every {@code classpath*:db/migration/*.sql}, passed through {@link H2MigrationRewriter}. */
public class H2MigrationResources implements ResourceProvider {

    private final List<LoadableResource> resources = new ArrayList<>();

    public H2MigrationResources() {
        try {
            for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*.sql")) {
                String name = r.getFilename();
                String sql = H2MigrationRewriter.rewrite(r.getContentAsString(StandardCharsets.UTF_8));
                resources.add(new Loaded(name, r.getURL().toString(), sql));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Flyway-Migrationen nicht lesbar", e);
        }
    }

    @Override
    public LoadableResource getResource(String name) {
        return resources.stream().filter(r -> r.getRelativePath().equals(name) || r.getFilename().equals(name))
                .findFirst().orElse(null);
    }

    @Override
    public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
        return resources.stream()
                .filter(r -> r.getFilename().startsWith(prefix) && Arrays.stream(suffixes).anyMatch(r.getFilename()::endsWith))
                .toList();
    }

    private static final class Loaded extends LoadableResource {
        private final String name;
        private final String url;
        private final String sql;

        Loaded(String name, String url, String sql) {
            this.name = name;
            this.url = url;
            this.sql = sql;
        }

        @Override public Reader read() { return new StringReader(sql); }
        @Override public String getAbsolutePath() { return url; }
        @Override public String getAbsolutePathOnDisk() { return url; }
        @Override public String getFilename() { return name; }
        @Override public String getRelativePath() { return name; }
    }
}
