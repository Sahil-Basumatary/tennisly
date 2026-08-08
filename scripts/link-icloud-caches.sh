#!/usr/bin/env bash
# Keep heavy build dirs as *.nosync so iCloud Drive skips them, with plain
# symlinks so Node/pnpm/Next still resolve the usual paths. Safe to re-run.
#
# Workflow after `pnpm install` (pnpm recreates a real node_modules):
#   ./scripts/link-icloud-caches.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

relink() {
  local dir="$1"
  local base parent nosync
  base="$(basename "$dir")"
  parent="$(dirname "$dir")"
  nosync="$parent/${base}.nosync"

  if [ -L "$dir" ]; then
    local target
    target="$(readlink "$dir")"
    if [ "$target" = "${base}.nosync" ] && [ -d "$nosync" ]; then
      echo "ok  $dir -> ${base}.nosync"
      return 0
    fi
    rm -f "$dir"
  fi

  if [ -d "$dir" ] && [ ! -L "$dir" ]; then
    if [ -e "$nosync" ]; then
      echo "replace stale ${base}.nosync with fresh $base"
      rm -rf "$nosync"
    fi
    echo "move $dir -> ${base}.nosync"
    mv "$dir" "$nosync"
  fi

  if [ ! -e "$nosync" ]; then
    echo "skip $dir (no ${base}.nosync yet — run install/dev first)"
    return 0
  fi

  ln -s "${base}.nosync" "$dir"
  echo "link $dir -> ${base}.nosync"
}

relink "$ROOT/node_modules"
relink "$ROOT/apps/web/node_modules"
relink "$ROOT/apps/web/.next"
relink "$ROOT/.turbo"
echo "done"
