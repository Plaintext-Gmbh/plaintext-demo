package ch.plaintext.demo.tabelle;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
        title = "Konfigurierbare Tabelle",
        link = "demo-table.html",
        parent = "Demo",
        order = 1,
        icon = "pi pi-table",
        roles = {"USER", "ADMIN", "ROOT"}
)
public class DemoTableMenu {
}
