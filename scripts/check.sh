#!/usr/bin/env bash
# Offline model + JSON codec build/test/ownership gate. No Maven or downloads.
set -euo pipefail
root="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
for tool in python3 javac java jar jdeps; do
  command -v "$tool" >/dev/null || { printf 'Missing build tool: %s\n' "$tool" >&2; exit 2; }
done
exec python3 -B "$root/scripts/harness/architecture.py" --offline-build
