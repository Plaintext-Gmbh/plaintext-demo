#!/usr/bin/env bash
# ci/release.sh — Release der Demo als Jar am GitHub-Release (Karte 2412, vorher GitHub Actions).
#
# Ausloeser ist der Git-Tag vX.Y.Z (Woodpecker-Ereignis `tag`, .woodpecker/release.yml). Die
# POM-Version wird nur im Arbeitsbaum gesetzt, nie committet.
#
#   ci/release.sh             Release (braucht CI_COMMIT_TAG und GITHUB_RELEASE_TOKEN)
#   ci/release.sh --trocken   bauen, pruefen und anzeigen, was veroeffentlicht wuerde
#
# Umgebung: CI_COMMIT_TAG (Woodpecker), GITHUB_RELEASE_TOKEN (Secret github_release_token),
#           GITHUB_REPO (Standard Plaintext-Gmbh/plaintext-demo)
set -euo pipefail

TROCKEN=0; [ "${1:-}" = "--trocken" ] && TROCKEN=1
cd "$(dirname "$0")/.."
REPO="${GITHUB_REPO:-Plaintext-Gmbh/plaintext-demo}"
API="https://api.github.com/repos/$REPO"
sag() { echo "release: $*"; }
fehler() { echo "release: FEHLER: $*" >&2; exit 1; }

TAG="${CI_COMMIT_TAG:-}"
[[ "$TAG" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || fehler "Tag '$TAG' ist keine Version vX.Y.Z"
V="${TAG#v}"
[ "$TROCKEN" = 1 ] || [ -n "${GITHUB_RELEASE_TOKEN:-}" ] || fehler "GITHUB_RELEASE_TOKEN fehlt"

# Kopfdatei statt Argument: das Token steht so nie in der Prozessliste.
KOPF="$(mktemp)"; trap 'rm -f "$KOPF"' EXIT
printf 'Authorization: Bearer %s\nAccept: application/vnd.github+json\n' "${GITHUB_RELEASE_TOKEN:-}" > "$KOPF"
gh_api() { curl -sS --max-time 60 -H "@$KOPF" "$@"; }

# Kollision: gibt es das Release schon, wird nichts ueberschrieben (Tag neu vergeben).
c="$(curl -s -o /dev/null -w '%{http_code}' --max-time 30 "https://api.github.com/repos/$REPO/releases/tags/$TAG" || true)"
case "$c" in
  404) sag "Release $TAG ist frei" ;;
  200) fehler "Release $TAG gibt es schon: https://github.com/$REPO/releases/tag/$TAG" ;;
  *)   fehler "GitHub antwortet $c auf releases/tags/$TAG" ;;
esac

mvn -B -Dmaven.build.cache.enabled=false versions:set -DnewVersion="$V" -DgenerateBackupPoms=false -DprocessAllModules=true
mvn -B -Dmaven.build.cache.enabled=false clean verify
rm -rf dist; mkdir dist
cp "plaintext-demo-webapp/target/plaintext-demo-webapp-$V.jar" "dist/plaintext-demo-$V.jar"
(cd dist && sha256sum "plaintext-demo-$V.jar" > "plaintext-demo-$V.jar.sha256")
sag "gebaut: $(ls dist | tr '\n' ' ')"

if [ "$TROCKEN" = 1 ]; then sag "--trocken: nichts veroeffentlicht"; exit 0; fi

ANTWORT="$(gh_api -X POST "$API/releases" \
  -d "{\"tag_name\":\"$TAG\",\"name\":\"plaintext-demo $V\",\"generate_release_notes\":true}")"
# Das erste "id" der Antwort ist die des Release (author/assets folgen danach).
ID="$(printf '%s' "$ANTWORT" | sed -n 's/^  "id": *\([0-9]*\),*$/\1/p' | head -1)"
[ -n "$ID" ] || fehler "Release nicht angelegt: $(printf '%s' "$ANTWORT" | head -c 300)"
sag "Release $TAG angelegt (id $ID)"

for f in dist/*; do
  n="$(basename "$f")"
  code="$(gh_api -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/octet-stream' \
    --data-binary "@$f" "https://uploads.github.com/repos/$REPO/releases/$ID/assets?name=$n")"
  [ "$code" = 201 ] || fehler "Upload $n: HTTP $code"
  sag "hochgeladen: $n"
done

# Nachpruefung wie start.sh: latest muss auf das neue Jar zeigen.
curl -fsS --max-time 30 "$API/releases/latest" | grep -q "plaintext-demo-$V.jar\"" \
  || fehler "releases/latest zeigt nicht auf plaintext-demo-$V.jar"
sag "fertig: https://github.com/$REPO/releases/tag/$TAG"
