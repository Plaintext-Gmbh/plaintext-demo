package ch.plaintext.demo.tabelle;

import ch.plaintext.boot.dashboard.DashboardTile;

@DashboardTile(
        id = "demo-table",
        title = "Konfigurierbare Tabelle",
        icon = "pi pi-table",
        link = "demo-table.html",
        menuTitle = "Demo",
        order = 1,
        roles = {"USER", "ADMIN", "ROOT"}
)
public class DemoDashboardTile {
}
