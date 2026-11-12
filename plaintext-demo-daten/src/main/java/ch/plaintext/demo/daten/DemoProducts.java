package ch.plaintext.demo.daten;

import ch.plaintext.demo.tabelle.DemoProduct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Sample rows, the same on every reset (fixed seed). The two mandates differ visibly. */
final class DemoProducts {

    private static final String[] COLORS = {"Black", "White", "Red", "Blue", "Green", "Grey", "Yellow", "Orange"};
    private static final String[] STATUS = {"AVAILABLE", "AVAILABLE", "AVAILABLE", "LOW_STOCK", "ORDERED", "DISCONTINUED"};
    private static final String[] OWNERS = {"Rita Root", "Adam Admin", "Uma User", "Lena Lager", "Marco Meier", "Sofia Rossi"};
    private static final String[] COUNTRIES = {"Switzerland", "Germany", "Italy", "France", "Austria", "Netherlands", "Japan"};
    private static final String[] UNITS = {"pcs", "box", "set", "kg", "m"};

    private DemoProducts() {
    }

    static List<DemoProduct> forDefault() {
        return generate("DP-", 80, 4711,
                new String[]{"Office", "Electronics", "Furniture", "Tools", "Garden"},
                new String[]{"Muster AG", "Alpenbedarf GmbH", "Helvetica Supply", "Bern Trading", "Zuri Parts"},
                new String[]{"Bern", "Zurich", "Basel"},
                new String[]{"Desk lamp", "Monitor arm", "Ergonomic chair", "Cordless drill", "Garden hose", "USB-C hub",
                        "Notebook stand", "Filing cabinet", "Hedge trimmer", "Label printer", "Whiteboard", "Toolbox"});
    }

    static List<DemoProduct> forExtra() {
        return generate("EX-", 30, 815,
                new String[]{"Outdoor", "Sports", "Camping"},
                new String[]{"Extra Outdoor Ltd", "Gipfel Sport", "Nordwand Gear"},
                new String[]{"Lucerne", "Chur"},
                new String[]{"Trekking pole", "Tent 2P", "Sleeping bag", "Head lamp", "Climbing rope", "Backpack 40l",
                        "Water filter", "Camping stove"});
    }

    private static List<DemoProduct> generate(String prefix, int count, long seed, String[] categories, String[] suppliers,
                                              String[] warehouses, String[] names) {
        Random r = new Random(seed);
        List<DemoProduct> rows = new ArrayList<>();
        LocalDate base = LocalDate.of(2024, 1, 15);
        for (int i = 1; i <= count; i++) {
            DemoProduct p = new DemoProduct();
            p.setSku(prefix + String.format("%04d", 1000 + i * 7));
            p.setName(names[r.nextInt(names.length)] + " " + (char) ('A' + r.nextInt(6)) + (10 + r.nextInt(90)));
            p.setCategory(categories[r.nextInt(categories.length)]);
            p.setSupplier(suppliers[r.nextInt(suppliers.length)]);
            p.setWarehouse(warehouses[r.nextInt(warehouses.length)]);
            p.setQuantity(r.nextInt(500));
            p.setUnit(UNITS[r.nextInt(UNITS.length)]);
            p.setPrice(BigDecimal.valueOf(5 + r.nextInt(150000) / 100.0).setScale(2, RoundingMode.HALF_UP));
            p.setCurrency("CHF");
            p.setVatRate(new BigDecimal(r.nextInt(4) == 0 ? "2.60" : "8.10"));
            p.setWeightKg(BigDecimal.valueOf(r.nextInt(25000) / 1000.0).setScale(3, RoundingMode.HALF_UP));
            p.setColor(COLORS[r.nextInt(COLORS.length)]);
            p.setStatus(STATUS[r.nextInt(STATUS.length)]);
            p.setActive(!"DISCONTINUED".equals(p.getStatus()));
            p.setReleaseDate(base.plusDays(r.nextInt(900)));
            p.setLastDelivery(p.getReleaseDate().plusDays(r.nextInt(120)));
            p.setOwner(OWNERS[r.nextInt(OWNERS.length)]);
            p.setRating(1 + r.nextInt(5));
            p.setCountry(COUNTRIES[r.nextInt(COUNTRIES.length)]);
            p.setEan("76" + String.format("%011d", Math.abs(r.nextLong()) % 100_000_000_000L));
            p.setNotes(r.nextInt(3) == 0 ? "Sample row " + i + " of the demo data, reset every night." : "");
            rows.add(p);
        }
        return rows;
    }
}
