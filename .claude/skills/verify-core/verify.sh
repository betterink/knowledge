#!/usr/bin/env bash
# Drive the core Ktor service against a disposable vault.
set -euo pipefail

CORE="$(cd "$(dirname "$0")/../../../core" && pwd)"
BIN="$CORE/build/install/knowledge-backend/bin/knowledge-backend"
BASE="${TMPDIR:-/tmp}/verify-core"

die() { echo "error: $*" >&2; exit 1; }
need_run() { [[ -n "${VERIFY_RUN:-}" && -d "$VERIFY_RUN" ]] || die "set VERIFY_RUN to the dir printed by launch"; }
port() { cat "$VERIFY_RUN/port"; }
pid() { cat "$VERIFY_RUN/pid"; }

write() { mkdir -p "$(dirname "$1")"; printf '%b' "$2" > "$1"; }

seed() {
  local v="$1" out="$2"
  write "$v/Home.md" '---\ntitle: x\n# not a title\n---\n# Home Page\n[[Notes.base#Table]] [[Missing]] [[#Local]]\n'
  write "$v/Projects/Projects.md" '[[Home|Start]] ![[Diagram.excalidraw]] ![[image.png|300]]\n```\n[[Ignored]]\n```\n| [[Home\\|Escaped]] |\n'
  write "$v/Projects/Diagram.excalidraw.md" '## Text Elements\n[[Home]]\n'
  write "$v/Projects/image.png" 'png'
  write "$v/Notes.base" 'views: []\n'
  write "$v/A/Same.md" 'a'
  write "$v/A/Other.md" '[[Same]]'
  write "$v/B/Same.md" 'b'
  write "$v/.obsidian/app.json" '{}'
  write "$v/notes.txt" 'unknown format'
  mkdir -p "$v/Empty"
  write "$out/secret.md" 'outside the vault'
  ln -s "$out/secret.md" "$v/Escape.md"
}

cmd_launch() {
  (cd "$CORE" && ./gradlew installDist -q)
  local id run p
  id="$(date +%Y%m%d-%H%M%S)-$$"
  run="$BASE/$id"
  mkdir -p "$run/instance" "$run/evidence"
  seed "$run/instance/vault-$id" "$run/instance/outside"
  p="$(python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1])')"
  echo "$p" > "$run/port"
  echo "vault-$id" > "$run/vault"
  PORT="$p" VAULT_ROOT="$run/instance/vault-$id" nohup "$BIN" > "$run/instance/server.log" 2>&1 &
  echo $! > "$run/pid"
  for _ in $(seq 1 60); do
    if curl -sf "http://127.0.0.1:$p/health" > /dev/null; then echo "$run"; return; fi
    kill -0 "$(cat "$run/pid")" 2>/dev/null || { cat "$run/instance/server.log" >&2; die "server exited"; }
    sleep 0.5
  done
  die "not ready after 30s; see $run/instance/server.log"
}

cmd_doctor() {
  need_run
  local p; p="$(port)"
  kill -0 "$(pid)" 2>/dev/null || die "pid $(pid) not running"
  lsof -nP -a -p "$(pid)" -iTCP:"$p" -sTCP:LISTEN > /dev/null || die "port $p not owned by pid $(pid)"
  [[ "$(curl -sf "http://127.0.0.1:$p/health")" == ok ]] || die "/health not ok"
  local name; name="$(curl -sf "http://127.0.0.1:$p/v1/workspace" | jq -r .name)"
  [[ "$name" == "$(cat "$VERIFY_RUN/vault")" ]] || die "serving vault '$name', expected '$(cat "$VERIFY_RUN/vault")'"
  echo "ok pid=$(pid) port=$p vault=$name"
}

# req <label> <path>: saves request, headers, body, status under evidence/.
cmd_req() {
  need_run
  local label="$1" path="$2" dir="$VERIFY_RUN/evidence"
  local url="http://127.0.0.1:$(port)$path"
  echo "GET $url" > "$dir/$label.request"
  curl -s --path-as-is -D "$dir/$label.headers" -o "$dir/$label.body" -w '%{http_code}' "$url" > "$dir/$label.status"
  echo "$(cat "$dir/$label.status") $dir/$label.body"
}

cmd_stop() {
  need_run
  if [[ -f "$VERIFY_RUN/pid" ]] && kill -0 "$(pid)" 2>/dev/null; then
    kill "$(pid)"
    for _ in $(seq 1 20); do kill -0 "$(pid)" 2>/dev/null || break; sleep 0.25; done
  fi
  [[ -d "$VERIFY_RUN/instance" ]] && cp "$VERIFY_RUN/instance/server.log" "$VERIFY_RUN/evidence/server.log"
  rm -rf "$VERIFY_RUN/instance" "$VERIFY_RUN/pid"
  echo "stopped; evidence kept at $VERIFY_RUN/evidence"
}

case "${1:-}" in
  launch) cmd_launch ;;
  doctor) cmd_doctor ;;
  req) shift; cmd_req "$@" ;;
  stop) cmd_stop ;;
  *) die "usage: verify.sh launch | doctor | req <label> <path> | stop" ;;
esac
