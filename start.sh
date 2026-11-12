#!/bin/sh
# plaintext-demo — one command: get the demo, start it, open the login in the browser.
#
#   From a clone:          ./start.sh                 (builds with Maven, then starts)
#   Without a clone:       curl -fsSL https://raw.githubusercontent.com/Plaintext-Gmbh/plaintext-demo/main/start.sh | sh
#                          (downloads the latest release jar into ./plaintext-demo)
#   Workbench:             ./start.sh --root 3.20.0-SNAPSHOT   (root from your local ~/.m2, after
#                          'mvn install' in plaintext-root)
#   Options:               --port 8080   --reset (start with an empty database)   --no-browser
#
# Needs Java 25 or newer. The H2 database is created next to the jar (data/).
set -eu

PORT="${DEMO_PORT:-8080}"
ROOT_VERSION=""
RESET=0
BROWSER=1
REPO="Plaintext-Gmbh/plaintext-demo"

while [ $# -gt 0 ]; do
  case "$1" in
    --port) PORT="$2"; shift 2 ;;
    --root) ROOT_VERSION="$2"; shift 2 ;;
    --reset) RESET=1; shift ;;
    --no-browser) BROWSER=0; shift ;;
    -h|--help) sed -n '2,13p' "$0" 2>/dev/null || true; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

fail() { echo "plaintext-demo: $*" >&2; exit 1; }

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
(cd "$DIR" && DEMO_PORT="$PORT" DEMO_DATA_DIR="$DIR/data" exec java -jar "$JAR" > "$DIR/plaintext-demo.log" 2>&1) &
PID=$!
trap 'kill $PID 2>/dev/null' INT TERM

i=0
until curl -fs -o /dev/null "$LOGIN"; do
  kill -0 "$PID" 2>/dev/null || fail "the application stopped, see $DIR/plaintext-demo.log"
  i=$((i + 1)); [ $i -gt 180 ] && fail "no answer after 3 minutes, see $DIR/plaintext-demo.log"
  sleep 1
done

cat <<INFO

  plaintext-demo is running:  $LOGIN

  Sign in with  root / root   (all roles, can switch tenants)
                admin / admin (administrator, tenant default)
                user / user   (user, tenant default)
                extra / extra (administrator, tenant extra)
  or use "Sign in with the demo IdP" (built-in OAuth2/OIDC server).

  Stop with Ctrl+C.
INFO

if [ "$BROWSER" = 1 ]; then
  if command -v open >/dev/null 2>&1; then open "$LOGIN"
  elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$LOGIN" >/dev/null 2>&1
  elif command -v cmd.exe >/dev/null 2>&1; then cmd.exe /c start "$LOGIN"
  fi
fi
wait $PID
