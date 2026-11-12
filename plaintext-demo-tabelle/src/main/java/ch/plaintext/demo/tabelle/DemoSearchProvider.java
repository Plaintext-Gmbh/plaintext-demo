package ch.plaintext.demo.tabelle;

import ch.plaintext.boot.plugins.security.PlaintextSecurityHolder;
import ch.plaintext.boot.search.SearchProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Global search (Cmd+K): the demo page itself, and the products of the current tenant by SKU, name or category. */
@Component
public class DemoSearchProvider implements SearchProvider {

    private static final List<String> PAGE_WORDS = List.of("demo", "table", "tabelle", "configurable", "konfigurierbar",
            "tableau", "tabella", "tablesettings");

    private final DemoProductRepository products;

    public DemoSearchProvider(DemoProductRepository products) {
        this.products = products;
    }

    @Override
    public String providerId() {
        return "demo";
    }

    @Override
    public String moduleTitle() {
        return "Demo";
    }

    @Override
    public List<SearchHit> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) {
            return List.of();
        }
        List<SearchHit> hits = new ArrayList<>();
        if (PAGE_WORDS.stream().anyMatch(w -> w.contains(q) || q.contains(w))) {
            hits.add(new Hit("Configurable table", "Demo – pt:tableSettings", "demo-table.html", "pi pi-table", 100));
        }
        products.findByMandatAndDeletedFalseOrderBySku(PlaintextSecurityHolder.getMandat()).stream()
                .filter(p -> Stream.of(p.getSku(), p.getName(), p.getCategory())
                        .anyMatch(v -> v != null && v.toLowerCase(Locale.ROOT).contains(q)))
                .limit(Math.max(0, limit - hits.size()))
                .forEach(p -> hits.add(new Hit(p.getSku() + " – " + p.getName(), p.getCategory() + " · " + p.getSupplier(),
                        "demo-table.html", "pi pi-box", 50)));
        return hits;
    }

    record Hit(String getTitle, String getSubtitle, String getLink, String getIcon, int getScore) implements SearchHit {
    }
}
