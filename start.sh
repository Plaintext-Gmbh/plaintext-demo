#!/bin/sh
# plaintext-demo — one command: get the demo, start it, open the login in the browser.
#
#   From a clone:          ./start.sh                 (builds with Maven, then starts)
#   Without a clone:       curl -fsSL https://raw.githubusercontent.com/Plaintext-Gmbh/plaintext-demo/main/start.sh | sh
#                          (downloads the latest release jar into ./plaintext-demo)
#   Workbench:             ./start.sh --root 3.20.0-SNAPSHOT   (root from your local ~/.m2, after
#                          'mvn install' in plaintext-root)
#   Options:               --port 8080   --reset (start with an empty database)   --no-browser
#                          --login builtin|form|oidc   (sign-in: built-in IdP, root's own form, own OIDC server
#                          from DEMO_OIDC_ISSUER/_CLIENT_ID/_CLIENT_SECRET)   --guard (keep the online demo's locks)
#
# Needs Java 25 or newer. The H2 database is created next to the jar (data/).
set -eu

PORT="${DEMO_PORT:-8080}"
ROOT_VERSION=""
RESET=0
BROWSER=1
LOGIN_MODE="${DEMO_LOGIN:-builtin}"
# Karte 2418: self-hosted means everything open; the online demo keeps the code default (locks on).
GUARD="${DEMO_GUARD:-false}"
REPO="Plaintext-Gmbh/plaintext-demo"

while [ $# -gt 0 ]; do
  case "$1" in
    --port) PORT="$2"; shift 2 ;;
    --root) ROOT_VERSION="$2"; shift 2 ;;
    --reset) RESET=1; shift ;;
    --no-browser) BROWSER=0; shift ;;
    --login) LOGIN_MODE="$2"; shift 2 ;;
    --guard) GUARD=true; shift ;;
    -h|--help) sed -n '2,15p' "$0" 2>/dev/null || true; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

fail() { echo "plaintext-demo: $*" >&2; exit 1; }

case "$LOGIN_MODE" in
  builtin) PROFILES="${SPRING_PROFILES_ACTIVE:-}" ;;
  form|oidc) PROFILES="${SPRING_PROFILES_ACTIVE:+$SPRING_PROFILES_ACTIVE,}login-$LOGIN_MODE" ;;
  *) fail "--login must be builtin, form or oidc, not: $LOGIN_MODE" ;;
esac
if [ "$LOGIN_MODE" = oidc ]; then
  [ -n "${DEMO_OIDC_ISSUER:-}" ] && [ -n "${DEMO_OIDC_CLIENT_ID:-}" ] && [ -n "${DEMO_OIDC_CLIENT_SECRET:-}" ] \
    || fail "--login oidc needs DEMO_OIDC_ISSUER, DEMO_OIDC_CLIENT_ID and DEMO_OIDC_CLIENT_SECRET"
fi

# Java 25+
command -v java >/dev/null 2>&1 || fail "Java 25 or newer is required (e.g. https://adoptium.net or 'sdk install java 25-open')."
JAVA_MAJOR=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1)
[ "${JAVA_MAJOR:-0}" -ge 25 ] 2>/dev/null || fail "Java 25 or newer is required, found: $(java -version 2>&1 | head -n 1)"

HERE=""
[ -f "${0:-}" ] && HERE=$(cd "$(dirname "$0")" && pwd)

if [ -n "$HERE" ] && [ -f "$HERE/pom.xml" ]; then
  # From a clone: build.
  command -v mvn >/dev/null 2>&1 || fail "Maven is needed to build from a clone (or use the curl one-liner, which downloads the release)."
  ARGS="-B -q -DskipTests"
  [ -n "$ROOT_VERSION" ] && ARGS="$ARGS -Dplaintext-root.version=$ROOT_VERSION"
  echo "plaintext-demo: building${ROOT_VERSION:+ against root $ROOT_VERSION} ..."
  (cd "$HERE" && mvn $ARGS package) || fail "build failed"
  JAR=$(ls "$HERE"/plaintext-demo-webapp/target/plaintext-demo-webapp-*.jar | grep -v -e '-sources' -e '\.original' | head -n 1)
  DIR="$HERE/plaintext-demo-webapp/target"
else
  # Without a clone: download the latest release into ./plaintext-demo.
  [ -n "$ROOT_VERSION" ] && fail "--root needs a clone of the repository"
  DIR="$(pwd)/plaintext-demo"
  mkdir -p "$DIR"
  URL=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" | sed -n 's/.*"browser_download_url": *"\([^"]*\.jar\)".*/\1/p' | head -n 1)
  [ -n "$URL" ] || fail "no release found at https://github.com/$REPO/releases"
  JAR="$DIR/$(basename "$URL")"
  if [ ! -f "$JAR" ]; then
    echo "plaintext-demo: downloading $(basename "$URL") ..."
    curl -fL --progress-bar -o "$JAR.part" "$URL" && mv "$JAR.part" "$JAR"
  fi
fi

[ "$RESET" = 1 ] && rm -rf "$DIR/data"

LOGIN="http://localhost:$PORT/login.html"
echo "plaintext-demo: starting on $LOGIN (database: $DIR/data, log: $DIR/plaintext-demo.log) ..."
(cd "$DIR" && DEMO_PORT="$PORT" DEMO_DATA_DIR="$DIR/data" DEMO_GUARD="$GUARD" SPRING_PROFILES_ACTIVE="$PROFILES" \
  exec java -jar "$JAR" > "$DIR/plaintext-demo.log" 2>&1) &
PID=$!
trap 'kill $PID 2>/dev/null' INT TERM

i=0
until curl -fs -o /dev/null "$LOGIN"; do
  kill -0 "$PID" 2>/dev/null || fail "the application stopped, see $DIR/plaintext-demo.log"
  i=$((i + 1)); [ $i -gt 180 ] && fail "no answer after 3 minutes, see $DIR/plaintext-demo.log"
  sleep 1
done

case "$LOGIN_MODE" in
  builtin) HOW="on the page of the built-in demo IdP (OAuth2/OIDC server inside the demo)" ;;
  form) HOW="in the sign-in form (built-in IdP switched off)" ;;
  oidc) HOW="at your OIDC server $DEMO_OIDC_ISSUER (accounts there; same usernames match these)" ;;
esac
[ "$GUARD" = true ] && LOCKS="on, as on demo.plaintext.ch" || LOCKS="off, everything open"
cat <<INFO

  plaintext-demo is running:  $LOGIN   (locks: $LOCKS)

  Sign in $HOW with
                root / root   (all roles, can switch tenants)
                admin / admin (administrator, tenant default)
                user / user   (user, tenant default)
                extra / extra (administrator, tenant extra)

  Stop with Ctrl+C.
INFO

if [ "$BROWSER" = 1 ]; then
  if command -v open >/dev/null 2>&1; then open "$LOGIN"
  elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$LOGIN" >/dev/null 2>&1
  elif command -v cmd.exe >/dev/null 2>&1; then cmd.exe /c start "$LOGIN"
  fi
fi
wait $PID
