# Modul-Demo module-demo.plaintext.ch — Sollstand & nächtlicher Reset (Karte 2478, 2518)

**Welche Instanz?** Diese Dateien gehören zur Hüllen-/Lader-Instanz `plaintext-demo` auf dem NAS
(Port 1171, **module-demo.plaintext.ch**, Postgres). `demo.plaintext.ch` ist eine andere Instanz:
`plaintext-demo-public` (Port 1172) mit dem Release-Jar dieses Repos, H2 und eigenem Reset
(`DEMO_RESET_CRON`, 03:30). Seit Karte 2378 zeigt der Tunnel so (Karte 2518).

Auftrag Daniel (Karte 2478, Punkt 3): Root-Zugang der Demo öffentlich, **mit nächtlichem Reset**
(Daten und Modulauswahl jede Nacht zurück). Dieses Verzeichnis versioniert den Sollstand und
das Reset-Skript; die laufende Instanz liegt auf dem NAS unter `/volume1/docker/plaintext-demo/`.

## Dateien

- `sollstand.sql.gz` — pg_dump der Demo-DB (Postgres `plaintext_demo`, 51 Tabellen). Zustand vom
  23.11.2026 (Hülle 3.77.0, Auswahl files/notizen/demo). **Nach jeder gewollten Daten-Änderung der
  Demo neu erzeugen** (siehe unten).
- `auswahl.soll.yaml` — Sollstand der Modulauswahl (Laufzeit-Module, Seite «Laufzeit-Module»). Seit Karte 2541
  über den Kunden-Index `registry kunde` (Schlüssel demo, Abos intern + Demo). Ändert sich die Auswahl auf dem NAS,
  diese Datei mitziehen, sonst setzt der Reset sie nachts zurück.
- `reset-nachts.sh` — Reset-Skript: Modulauswahl zurück, App stoppen, DB auf den Sollstand, App starten.

## Cron

Der Cron läuft als root auf dem NAS (User-crontab ist gesperrt), Eintrag in `/etc/crontab` seit
Karte 2518, 03:17 NAS-Zeit. Das NAS läuft auf `Africa/Windhoek` (MEZ+1), also 02:17 MEZ:

```
17 3 * * * root sh /volume1/docker/plaintext-demo/reset/reset-nachts.sh >> /volume1/docker/plaintext-demo/reset/reset.log 2>&1
```

Log der Probeläufe: `/volume1/docker/plaintext-demo/reset/reset.log`.

## Sollstand neu erzeugen (nach gewollten Demo-Daten-Änderungen)

```bash
ssh nas
sudo -n docker exec plaintext-demo-db pg_dump -U plaintext plaintext_demo | gzip > /volume1/docker/plaintext-demo/reset/sollstand.sql.gz
cp /volume1/docker/plaintext-demo/module/auswahl.yaml /volume1/docker/plaintext-demo/module/auswahl.soll.yaml
```

Danach die beiden Dateien hierher zurückspielen und committen.

## Probe (23.11.2026, kimi1)

Vorher: Marker-Tabelle `reset_probe` eingefügt, Auswahl auf `version: 1.0.0` manipuliert.
Skriptlauf: Marker weg (`relation "reset_probe" does not exist`), Auswahl wieder `latest`,
Container healthy, status.txt ergebnis=OK.

## Zugänge und Hinweis (Karte 2518)

Die Hashes von `root@root.root` und `demo@demo.demo` waren im ersten Sollstand unbrauchbar (die Shell
hatte `$…` im BCrypt-Wert ersetzt, Länge 63/54 statt 60), deshalb ging keine Passwort-Anmeldung.
Seit 2518 sind sie gültig (BCrypt, Kosten 12). Die Werte sind bewusst öffentlich und stehen auf der
Login-Seite: `plaintext.login.demo-hinweis` in `config/application.yml` der Instanz (root-v2 PR 91).
