#!/usr/bin/env python3
"""Local evidence floors. Never use these as PR CI gates."""

from __future__ import annotations

import json
import pathlib
import sys


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit("usage: perf-gate.py SESSION_DIR")
    root = pathlib.Path(__file__).resolve().parents[1]
    floors_path = root / "tests/load/baselines/v2-floors.json"
    session = pathlib.Path(sys.argv[1])
    if not floors_path.exists():
        raise SystemExit(f"missing {floors_path}")
    floors = json.loads(floors_path.read_text(encoding="utf-8")).get("floors") or {}
    failures: list[str] = []
    session_floors = session / "floors.json"
    if session_floors.exists():
        measured = json.loads(session_floors.read_text(encoding="utf-8")).get("floors") or {}
        for key, floor in floors.items():
            value = measured.get(key)
            if value is None:
                continue
            # floors.json already has slack applied; compare session floors to committed floors
            if float(value) + 1e-9 < float(floor) * 0.98:
                failures.append(f"{key} session_floor={value} committed={floor}")
    if failures:
        raise SystemExit("local evidence floor regression: " + "; ".join(failures))
    print("local evidence floors ok")


if __name__ == "__main__":
    main()
