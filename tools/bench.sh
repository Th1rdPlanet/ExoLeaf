#!/usr/bin/env bash
# One load-test run: start the test server, connect bots, sample MSPT, stop.
#   bench.sh <label> <bots> <seconds>
set -e
TOOLS="$(cd "$(dirname "$0")" && pwd)"
DIR="$TOOLS/../loadtest/exo"
LABEL="${1:-run}"; BOTS="${2:-200}"; SECONDS_TOTAL="${3:-100}"
"$TOOLS/testserver.sh" start
for _ in $(seq 1 120); do grep -q "Done (" "$DIR/server.log" 2>/dev/null && break; sleep 1; done
# Four bot processes: one Python process cannot decode chunk data for hundreds of clients.
PIDS=()
for part in 0 1 2 3; do
  python3 -I "$TOOLS/loadbots.py" --port 25570 --bots $(( BOTS / 4 )) --rate 5 --duration "$SECONDS_TOTAL" \
    --prefix "B${part}_" > "$DIR/bots-$LABEL-$part.log" 2>&1 &
  PIDS+=($!)
done
sleep $(( SECONDS_TOTAL / 2 ))
"$TOOLS/testserver.sh" cmd "mspt"
sleep $(( SECONDS_TOTAL / 2 - 10 ))
"$TOOLS/testserver.sh" cmd "mspt"
"$TOOLS/testserver.sh" cmd "tps"
sleep 3
wait "${PIDS[@]}" || true
echo "== $LABEL: bots =="
grep -h FINAL "$DIR"/bots-$LABEL-*.log || tail -n 2 "$DIR"/bots-$LABEL-*.log
echo "== $LABEL: server =="
grep -A4 -E "Server tick times|TPS from last" "$DIR/server.log" | sed 's/\x1b\[[0-9;]*m//g' | grep -vE "^--$" | tail -12
"$TOOLS/testserver.sh" stop
for _ in $(seq 1 60); do pgrep -f "exo.*server.jar|java -Xms2G -Xmx2G -jar server.jar" >/dev/null || break; sleep 1; done
TAILPID=$(cat "$DIR/tail.pid" 2>/dev/null || true); [ -n "$TAILPID" ] && kill "$TAILPID" 2>/dev/null || true
cp "$DIR/server.log" "$DIR/server-$LABEL.log"
