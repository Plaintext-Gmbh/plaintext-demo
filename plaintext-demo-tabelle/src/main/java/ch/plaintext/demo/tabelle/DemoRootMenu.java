package ch.plaintext.demo.tabelle;

import ch.plaintext.boot.menu.MenuAnnotation;

@MenuAnnotation(
        title = "Demo",
        link = "demo-table.html",
        order = 1,
        icon = "pi pi-star",
        roles = {"USER", "ADMIN", "ROOT"},
        moduleId = "demo"
)
public class DemoRootMenu {
}
