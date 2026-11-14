# plaintext-demo

A public demo application on [plaintext-root](https://github.com/Plaintext-Gmbh/plaintext-root):
the platform with its tables, tenants, roles and admin pages, running on an embedded H2 database
with a built-in OAuth2/OIDC server, so it starts with nothing but Java.

**Live:** https://demo.plaintext.ch (coming soon, reset every night) · **About:** https://plaintext.ch/en/demo/

## Start it — one command

You need **Java 25** or newer.

```sh
curl -fsSL https://raw.githubusercontent.com/Plaintext-Gmbh/plaintext-demo/main/start.sh | sh
```

This downloads the latest release into `./plaintext-demo`, starts it on http://localhost:8080 and
opens the login in your browser. The H2 database is created next to the jar (`plaintext-demo/data`).

From a clone (builds with Maven first): `./start.sh` — options: `--port 8080`, `--reset` (start with
an empty database), `--no-browser`.

## Sign in

| User | Password | Sees |
|------|----------|------|
| `root` | `root` | everything, can switch between the tenants `default` and `extra` |
| `admin` | `admin` | administrator of tenant `default` |
| `user` | `user` | regular user of tenant `default` |
| `extra` | `extra` | administrator of tenant `extra` — different data |

Use the form, or **Sign in with the demo IdP**: that is the built-in OAuth2/OIDC server
(module `plaintext-demo-oauth-mock`), standing in for Keycloak. The UI is English; switch to German
in the user settings.

## What is inside

| Module | Purpose |
|--------|---------|
| `plaintext-demo-webapp` | The application: plaintext-root on H2 (`config/application.yml`), plus the H2 adapter for root's PostgreSQL migrations |
| `plaintext-demo-oauth-mock` | Built-in OAuth2/OIDC server (Spring Authorization Server) with configurable users and Keycloak-shaped endpoints below `/oauth-mock` |
| `plaintext-demo-daten` | Fills an empty database: users, tenants, the OIDC configuration, sample rows, a sample note per account; registers the demo's file store |
| `plaintext-demo-tabelle` | The demo table — every variant of root's configurable table `pt:tableSettings` |

**H2 instead of PostgreSQL.** plaintext-root writes its Flyway migrations for PostgreSQL. The demo
reads them through `H2MigrationRewriter` at load time (only on H2; the files stay unchanged), and
`H2MigrationRewriterTest` runs every migration on the classpath into a fresh H2 — it fails as soon
as root ships syntax the adapter does not cover.

**Notes module.** The demo includes [plaintext-modul-notizen](https://github.com/Plaintext-Gmbh/plaintext-modul-notizen)
as an example of an installable module (menu, dashboard tile, settings page, four languages): notes with autosave,
a mini editor and their own window. The notes are Markdown files in a plaintext-root file store of type «Local file»
(`LokaleDateiAblage`) below `${DEMO_DATA_DIR}/ablagen`; the nightly reset empties that folder and every account gets
its sample note back.

## Workbench for root

The demo is where building blocks such as the configurable table are improved and shown. To try a
root change before it is released:

```sh
cd ../plaintext-root && mvn install -DskipTests     # e.g. 3.20.0-SNAPSHOT into ~/.m2
cd ../plaintext-demo && ./start.sh --root 3.20.0-SNAPSHOT --reset
```

Building needs the root artifacts from GitHub Packages, i.e. a `settings.xml` with a token
(`read:packages`) for the server id `plaintext`.

## Configuration

| Variable | Default | |
|----------|---------|--|
| `DEMO_PORT` | `8080` | HTTP port |
| `DEMO_DATA_DIR` | `./data` | where the H2 file lives |
| `DEMO_BASE_URL` | `http://localhost:<port>` | public URL; the OIDC issuer and redirect URI derive from it |
| `DEMO_RESET_CRON` | off | e.g. `0 30 3 * * *`: at that time the demo ends with exit code 75 and, on its next start, begins with an empty database that the demo data fill again — for an instance whose container restarts it |

All passwords and the OAuth client secret in this repository are demo values, published on purpose.

## License

[MPL-2.0](LICENSE), like plaintext-root.
