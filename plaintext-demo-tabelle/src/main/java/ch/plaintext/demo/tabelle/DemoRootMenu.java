package ch.plaintext.demo.tabelle;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
        title = "Demo",
        link = "demo-table.html",
        order = 10000, // Daniel 12.11.: the menu "Demo" stands at the very bottom
        icon = "pi pi-star",
        roles = {"USER", "ADMIN", "ROOT"},
        moduleId = "demo"
)
public class DemoRootMenu {
}
