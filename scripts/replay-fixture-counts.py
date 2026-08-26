#!/usr/bin/env python3
"""Read replay frame counts from the golden fixture file, not from shell literals."""

from __future__ import annotations

import pathlib
import sys


def load(path: pathlib.Path) -> dict[str, int]:
    counts: dict[str, int] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line.startswith("point_frames:"):
            counts["point_frames"] = int(line.split(":", 1)[1].strip())
        elif line.startswith("match_frames:"):
            counts["match_frames"] = int(line.split(":", 1)[1].strip())
    if "point_frames" not in counts or "match_frames" not in counts:
        raise SystemExit(f"missing frame counts in {path}")
    return counts


def main() -> None:
    root = pathlib.Path(__file__).resolve().parents[1]
    path = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else root / "tests/load/baselines/replay-golden-sha256.txt"
    counts = load(path)
    print(f"{counts['point_frames']} {counts['match_frames']}")


if __name__ == "__main__":
    main()
