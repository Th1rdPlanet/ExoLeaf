#!/usr/bin/env bash
# Test server control for load tests (offline mode, port 25570, in loadtest/exo).
#   testserver.sh start | stop | cmd "<console command>" | log [lines]
set -e
DIR="$(cd "$(dirname "$0")/../loadtest/exo" && pwd)"
case "$1" in
  start)
    rm -f "$DIR/console"
    mkfifo "$DIR/console"
    (cd "$DIR" && nohup bash -c 'tail -f console & echo $! > tail.pid; wait' 2>/dev/null | (cd "$DIR" && exec java -Xms2G -Xmx2G -jar server.jar --nogui) > "$DIR/server.log" 2>&1 &)
    ;;
  stop)
    echo stop > "$DIR/console"
    ;;
  cmd)
    echo "$2" > "$DIR/console"
    ;;
  log)
    tail -n "${2:-20}" "$DIR/server.log"
    ;;
  *)
    echo "usage: $0 start|stop|cmd <command>|log [lines]" >&2
    exit 1
    ;;
esac
