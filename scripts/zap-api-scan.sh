#!/usr/bin/env bash
# OWASP ZAP API scan against the gateway public OpenAPI surface.
#
# Prereqs: Docker, stack reachable at TARGET_URL, disposable API_KEY.
#
#   export API_KEY=tly_live_...
#   export TARGET_URL=http://host.docker.internal:8080   # Mac Docker default
#   ./scripts/zap-api-scan.sh
#
# Reports land in .run/zap/ (gitignored). Exit non-zero on WARN/FAIL unless
# ZAP_IGNORE_WARN=true (local soft mode only — never for CI).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SEC_DIR="$ROOT/tests/security"
REPORT_DIR="${ZAP_REPORT_DIR:-$ROOT/.run/zap}"
IMAGE="${ZAP_IMAGE:-ghcr.io/zaproxy/zaproxy:stable}"
OPENAPI_SRC="$SEC_DIR/public-api.openapi.yaml"
RULES="$SEC_DIR/zap-api-rules.conf"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"

if [[ "$(uname -s)" == "Darwin" ]]; then
  DEFAULT_TARGET="http://host.docker.internal:8080"
else
  DEFAULT_TARGET="http://127.0.0.1:8080"
fi

TARGET_URL="${TARGET_URL:-$DEFAULT_TARGET}"
API_KEY="${API_KEY:-}"
DOCKER_NET=()

# Linux loopback targets need host networking; remote staging URLs do not.
if [[ "$(uname -s)" != "Darwin" ]]; then
  case "$TARGET_URL" in
    http://127.0.0.1:*|http://localhost:*|https://127.0.0.1:*|https://localhost:*)
      DOCKER_NET=(--network host)
      ;;
  esac
fi

if [[ "${1:-}" == "--gen-rules" ]]; then
  mkdir -p "$REPORT_DIR"
  # -S skips active scan so the template is written after a short passive pass.
  docker run --rm -v "${REPORT_DIR}:/zap/wrk:rw" -u zap "${IMAGE}" \
    zap-api-scan.py -t https://example.com -f openapi -g gen.conf -S -I -T 5
  echo "wrote ${REPORT_DIR}/gen.conf — merge into tests/security/zap-api-rules.conf"
  exit 0
fi

if [[ -z "$API_KEY" ]]; then
  echo "set API_KEY to a disposable public API key (X-Api-Key)" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required to run ZAP" >&2
  exit 1
fi

mkdir -p "$REPORT_DIR"
OPENAPI_RUN="$REPORT_DIR/public-api.openapi.yaml"
# Rewrite servers.url so ZAP hits the live gateway from inside the container.
python3 - "$OPENAPI_SRC" "$OPENAPI_RUN" "$TARGET_URL" <<'PY'
import sys
from pathlib import Path
src, dst, target = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3].rstrip("/")
text = src.read_text()
# Keep YAML simple: replace the first servers url line after "servers:"
out = []
replaced = False
for line in text.splitlines():
    if not replaced and line.strip().startswith("- url:"):
        out.append(f"  - url: {target}")
        replaced = True
    else:
        out.append(line)
if not replaced:
    raise SystemExit("could not patch servers.url in OpenAPI")
dst.write_text("\n".join(out) + "\n")
PY

HTML_REPORT="zap-api-${STAMP}.html"
JSON_REPORT="zap-api-${STAMP}.json"
MD_REPORT="zap-api-${STAMP}.md"

ZAP_ARGS=(
  zap-api-scan.py
  -t "/zap/wrk/public-api.openapi.yaml"
  -f openapi
  -c "/zap/wrk/zap-api-rules.conf"
  -r "$HTML_REPORT"
  -J "$JSON_REPORT"
  -w "$MD_REPORT"
  -s
)

if [[ "${ZAP_IGNORE_WARN:-false}" == "true" ]]; then
  ZAP_ARGS+=(-I)
fi

# Inject X-Api-Key on every request ZAP issues.
ZAP_OPTS=(
  -config "replacer.full_list(0).description=tennisly-api-key"
  -config "replacer.full_list(0).enabled=true"
  -config "replacer.full_list(0).matchtype=REQ_HEADER"
  -config "replacer.full_list(0).matchstr=X-Api-Key"
  -config "replacer.full_list(0).replacement=${API_KEY}"
  -config "replacer.full_list(0).regex=false"
)
ZAP_ARGS+=(-z "${ZAP_OPTS[*]}")

cp "$RULES" "$REPORT_DIR/zap-api-rules.conf"

echo "ZAP api-scan target=${TARGET_URL} image=${IMAGE}"
echo "reports → ${REPORT_DIR}"

set +e
docker run --rm \
  "${DOCKER_NET[@]}" \
  -v "${REPORT_DIR}:/zap/wrk:rw" \
  -u zap \
  "${IMAGE}" \
  "${ZAP_ARGS[@]}"
status=$?
set -e

echo "ZAP exit=${status} (0=ok, 1=failures, 2=warnings)"
echo "  html  ${REPORT_DIR}/${HTML_REPORT}"
echo "  json  ${REPORT_DIR}/${JSON_REPORT}"
echo "  md    ${REPORT_DIR}/${MD_REPORT}"
exit "$status"
