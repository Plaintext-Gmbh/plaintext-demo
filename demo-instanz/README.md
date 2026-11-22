# Demo-Instanz demo.plaintext.ch — Sollstand & nächtlicher Reset (Karte 2478)

Auftrag Daniel (Karte 2478, Punkt 3): Root-Zugang der Demo öffentlich, **mit nächtlichem Reset**
(Daten und Modulauswahl jede Nacht zurück). Dieses Verzeichnis versioniert den Sollstand und
das Reset-Skript; die laufende Instanz liegt auf dem NAS unter `/volume1/docker/plaintext-demo/`.

## Dateien

- `sollstand.sql.gz` — pg_dump der Demo-DB (Postgres `plaintext_demo`, 51 Tabellen). Zustand vom
  23.11.2026 (Hülle 3.77.0, Auswahl files/notizen/demo). **Nach jeder gewollten Daten-Änderung der
  Demo neu erzeugen** (siehe unten).
- `auswahl.soll.yaml` — Sollstand der Modulauswahl (Laufzeit-Module, Seite «Laufzeit-Module»).
- `reset-nachts.sh` — Reset-Skript: Modulauswahl zurück, App stoppen, DB auf den Sollstand, App starten.

## Cron

Der Cron läuft als root auf dem NAS (User-crontab ist gesperrt), 03:17 Uhr nachts:

```
17 3 * * * sh /volume1/docker/plaintext-demo/reset/reset-nachts.sh >> /volume1/docker/plaintext-demo/reset/reset.log 2>&1
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
