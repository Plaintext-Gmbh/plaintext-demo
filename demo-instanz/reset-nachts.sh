#!/bin/sh
# Karte 2478: naechtlicher Demo-Reset — DB + Modulauswahl auf den Sollstand.
# Sollstand: sollstand.sql.gz (pg_dump der Demo-DB) und auswahl.soll.yaml, beide im Repo
# Plaintext-Gmbh/plaintext-demo unter demo-instanz/ versioniert (dorthin zurueckspielen nach Aenderung).
# Aufruf: cron (mad auf dem NAS, ~03:17) oder von Hand als Probe. Log: reset/reset.log.
set -eu
D=/volume1/docker/plaintext-demo
echo "== Demo-Reset $(date '+%F %T') =="

cp "$D/module/auswahl.soll.yaml" "$D/module/auswahl.yaml"
echo "Modulauswahl zurueck (files/notizen/demo, registry kunde)"

sudo -n docker stop plaintext-demo-app >/dev/null
echo "App gestoppt"

echo "drop schema public cascade; create schema public;" | sudo -n docker exec -i plaintext-demo-db psql -q -U plaintext -d plaintext_demo
zcat "$D/reset/sollstand.sql.gz" | sudo -n docker exec -i plaintext-demo-db psql -q -U plaintext -d plaintext_demo
echo "DB auf Sollstand (51 Tabellen, Flyway-Stand des Dumps)"

sudo -n docker start plaintext-demo-app >/dev/null
echo "Reset fertig $(date '+%F %T')"
