package ch.plaintext.demo.tabelle;

import ch.plaintext.boot.plugins.security.PlaintextSecurityHolder;
import ch.plaintext.boot.table.TableColumn;
import ch.plaintext.boot.table.TableSettings;
import ch.plaintext.boot.table.TableStateStore;
import ch.plaintext.i18n.web.I18nEL;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * The demo table: every variant of pt:tableSettings on one page — many columns, widths, order,
 * show/hide, profiles, paging, sorting, filters — stored per user and mandate, so it survives a
 * reload. This is the workbench page for changes to the building block.
 */
@Component
@Scope("session")
public class DemoTableBackingBean implements Serializable {

    /** Key, German label (i18n key), default width, visible by default, required (cannot be hidden). */
    static final List<TableColumn> COLUMNS = List.of(
            new TableColumn("sku", "Artikelnummer", 130, true, true),
            new TableColumn("name", "Produktname", 240, true, true),
            new TableColumn("category", "Kategorie", 140),
            new TableColumn("supplier", "Lieferant", 180),
            new TableColumn("warehouse", "Lager", 120),
            new TableColumn("quantity", "Menge", 90),
            new TableColumn("unit", "Einheit", 90),
            new TableColumn("price", "Preis", 110),
            new TableColumn("currency", "Waehrung", 90, false),
            new TableColumn("vatRate", "MWST %", 90, false),
            new TableColumn("weightKg", "Gewicht kg", 110, false),
            new TableColumn("color", "Farbe", 110),
            new TableColumn("status", "Status", 120),
            new TableColumn("active", "Aktiv", 80),
            new TableColumn("releaseDate", "Lanciert", 120),
            new TableColumn("lastDelivery", "Letzte Lieferung", 140, false),
            new TableColumn("owner", "Verantwortlich", 160),
            new TableColumn("rating", "Bewertung", 100),
            new TableColumn("country", "Herkunft", 120, false),
            new TableColumn("ean", "EAN", 140, false),
            new TableColumn("notes", "Bemerkung", 260, false));

    @Autowired
    private transient DemoProductRepository repository;
    @Autowired(required = false)
    private transient TableStateStore tableStateStore;
    @Autowired(required = false)
    private transient I18nEL i18n;

    @Getter
    private final TableSettings anzeige = new TableSettings("demo-table", true, 25);
    @Getter
    private List<DemoProduct> products = List.of();
    @Getter
    @Setter
    private List<DemoProduct> filteredProducts;
    @Getter
    @Setter
    private DemoProduct selected;

    @PostConstruct
    void init() {
        anzeige.init(tableStateStore, COLUMNS.stream()
                .map(c -> new TableColumn(c.getKey(), t(c.getLabel()), c.getDefaultWidth(), c.isDefaultVisible(), c.isRequired()))
                .toList());
    }

    /** preRenderView: the rows of the current mandate (root switches the mandate within a session). */
    public void load() {
        products = repository.findByMandatAndDeletedFalseOrderBySku(PlaintextSecurityHolder.getMandat());
    }

    public String getMandat() {
        return PlaintextSecurityHolder.getMandat();
    }

    /** Status codes are stable keys; the label is German text, translated by i18n. */
    public String statusText(String code) {
        return t(switch (code == null ? "" : code) {
            case "AVAILABLE" -> "Verfuegbar";
            case "LOW_STOCK" -> "Knapp";
            case "ORDERED" -> "Bestellt";
            case "DISCONTINUED" -> "Ausgelaufen";
            default -> code == null ? "" : code;
        });
    }

    private String t(String german) {
        return i18n != null ? i18n.t(german) : german;
    }
}
