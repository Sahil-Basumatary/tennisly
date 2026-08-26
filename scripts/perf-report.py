#!/usr/bin/env python3
"""Turn a local evidence session directory into committed markdown. No hand-copied units."""

from __future__ import annotations

import json
import pathlib
import statistics
import sys
from typing import Any


CLAIM_LABELS = [
    "in-process latency",
    "replay frames/s",
    "atomic commit TPS",
    "transactional batch points/s",
    "staging/promote rows/s",
]


def load_json(path: pathlib.Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def jmh_rows(path: pathlib.Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    return load_json(path)


def percentile_ns(row: dict[str, Any], key: str) -> float | None:
    metric = row.get("primaryMetric") or {}
    pct = metric.get("scorePercentiles") or {}
    value = pct.get(key)
    if value is None:
        return None
    unit = metric.get("scoreUnit") or ""
    if "us" in unit or "μs" in unit or "µs" in unit:
        return float(value) * 1_000.0
    if "ms" in unit:
        return float(value) * 1_000_000.0
    if unit.endswith("s/op") and "n" not in unit and "m" not in unit:
        return float(value) * 1_000_000_000.0
    return float(value)


def fmt_ns(value: float | None) -> str:
    if value is None:
        return ""
    if value >= 1_000_000:
        return f"{value / 1_000_000:.3f} ms"
    if value >= 1_000:
        return f"{value / 1_000:.3f} µs"
    return f"{value:.1f} ns"


def fmt_ms(value: Any) -> str:
    if value == "" or value is None:
        return ""
    return f"{float(value):.2f}"


def render_jmh(title: str, claim: str, rows: list[dict[str, Any]], point_frames: int, match_frames: int) -> list[str]:
    lines = [f"### {title}", "", f"Claim label: **{claim}**.", ""]
    if not rows:
        lines.append("_No JMH JSON in this session._")
        lines.append("")
        return lines
    lines.append("| Benchmark | mode | score | p50 | p95 | p99 | unit | derived |")
    lines.append("|---|---|---:|---:|---:|---:|---|---|")
    for row in rows:
        name = row.get("benchmark", "").rsplit(".", 1)[-1]
        mode = row.get("mode", "")
        metric = row.get("primaryMetric") or {}
        score = metric.get("score")
        unit = metric.get("scoreUnit", "")
        p50 = percentile_ns(row, "50.0")
        p95 = percentile_ns(row, "95.0")
        p99 = percentile_ns(row, "99.0")
        derived = ""
        if mode == "thrpt" and score is not None:
            if name in {"fullPointPipeline", "assemblerOnly"}:
                kind = "full-pipeline" if name == "fullPointPipeline" else "assembler-only"
                derived = f"{score * point_frames:,.1f} {kind} frames/s"
            elif name == "fullMatchPipeline":
                derived = f"{score * match_frames:,.1f} full-pipeline frames/s"
            elif name == "processMillionEvents":
                derived = f"{score * 1_000_000:,.0f} events/s"
        score_s = f"{score:.4f}" if isinstance(score, (int, float)) else ""
        if mode == "thrpt":
            p50_s = p95_s = p99_s = ""
        else:
            p50_s = fmt_ns(p50)
            p95_s = fmt_ns(p95)
            p99_s = fmt_ns(p99)
        params = row.get("params") or {}
        if params:
            name = name + "[" + ",".join(f"{key}={params[key]}" for key in sorted(params)) + "]"
        lines.append(
            f"| {name} | {mode} | {score_s} | {p50_s} | {p95_s} | {p99_s} | {unit} | {derived} |"
        )
    lines.append("")
    return lines


def median_metric(values: list[float]) -> float | None:
    if not values:
        return None
    return statistics.median(values)


def render_http_runs(title: str, claim: str, files: list[pathlib.Path]) -> tuple[list[str], list[tuple[str, float]]]:
    lines = [f"### {title}", "", f"Claim label: **{claim}**.", ""]
    samples: list[tuple[str, float]] = []
    if not files:
        lines.append("_No service-run JSON in this session._")
        lines.append("")
        return lines, samples
    lines.append("| run | phase | metric | value | p50 ms | p95 ms | p99 ms |")
    lines.append("|---|---|---|---:|---:|---:|---:|")
    by_op: dict[str, list[float]] = {}
    for path in files:
        data = load_json(path)
        phase = data.get("phase", path.stem)
        for row in data.get("operations", []):
            op = str(row.get("operation", ""))
            rate = float(row.get("rate", 0))
            lines.append(
                "| {run} | {phase} | {op} | {value:.2f} | {p50} | {p95} | {p99} |".format(
                    run=path.stem,
                    phase=phase,
                    op=op,
                    value=rate,
                    p50=fmt_ms(row.get("p50_ms", "")),
                    p95=fmt_ms(row.get("p95_ms", "")),
                    p99=fmt_ms(row.get("p99_ms", "")),
                )
            )
            if op in {
                "atomic_commit_tps",
                "transactional_batch_points",
                "staging_copy_rows",
                "promote_rows",
            }:
                samples.append((op, rate))
                by_op.setdefault(op, []).append(rate)
    if by_op:
        lines.append("")
        for op, values in by_op.items():
            med = median_metric(values)
            if med is None:
                continue
            floor = med * 0.80
            lines.append(
                f"Multi-run median **{op}** {med:.2f}. Local regression floor (20% slack): **{floor:.2f}**."
            )
    lines.append("")
    return lines, samples


def replay_ladder(rows: list[dict[str, Any]], point_frames: int, match_frames: int) -> list[str]:
    lines = ["### Replay frames/s ladders", "", "Claim label: **replay frames/s**. Full-pipeline only. Assembler-only is a different claim.", ""]
    targets = [50_000, 100_000, 250_000]
    measured = []
    for row in rows:
        name = row.get("benchmark", "").rsplit(".", 1)[-1]
        if row.get("mode") != "thrpt":
            continue
        score = (row.get("primaryMetric") or {}).get("score")
        if score is None:
            continue
        if name == "fullPointPipeline":
            frames = score * point_frames
            measured.append(("fullPointPipeline", frames))
        elif name == "fullMatchPipeline":
            frames = score * match_frames
            measured.append(("fullMatchPipeline", frames))
        elif name == "assemblerOnly":
            lines.append(f"Assembler-only: **{score * point_frames:,.0f} frames/s** (not a full-pipeline claim).")
            lines.append("")
    if not measured:
        lines.append("_No full-pipeline throughput in this session._")
        lines.append("")
        return lines
    ceiling = max(frames for _, frames in measured)
    lines.append("| source | full-pipeline frames/s | 50k | 100k | 250k |")
    lines.append("|---|---:|---|---|---|")
    for name, frames in measured:
        marks = " | ".join("hit" if frames >= target else "miss" for target in targets)
        lines.append(f"| {name} | {frames:,.1f} | {marks} |")
    lines.append("")
    if ceiling < 50_000:
        lines.append(
            f"Measured ceiling **{ceiling:,.0f} full-pipeline frames/s**. "
            "50k/100k/250k ladders were missed. Do not relabel match parallelism or assembler throughput as this number."
        )
    elif ceiling < 100_000:
        lines.append(
            f"Measured ceiling **{ceiling:,.0f} full-pipeline frames/s**. Hit 50k, missed 100k and 250k."
        )
    elif ceiling < 250_000:
        lines.append(
            f"Measured ceiling **{ceiling:,.0f} full-pipeline frames/s**. Hit 50k and 100k, missed 250k."
        )
    else:
        lines.append(f"Measured ceiling **{ceiling:,.0f} full-pipeline frames/s**. Hit 50k, 100k, and 250k.")
    lines.append("")
    return lines


def write_floors(session: pathlib.Path, durable: list[float], bulk: list[tuple[str, float]], replay_frames: float | None) -> None:
    floors: dict[str, Any] = {
        "schema": "tennisly.perf.floors.v1",
        "slack": 0.20,
        "note": "Local evidence suite only. PR CI keeps correctness and non-noisy canaries.",
        "floors": {},
    }
    if durable:
        floors["floors"]["atomic_commit_tps"] = round(statistics.median(durable) * 0.80, 2)
    by_op: dict[str, list[float]] = {}
    for op, rate in bulk:
        by_op.setdefault(op, []).append(rate)
    for op, values in by_op.items():
        floors["floors"][op] = round(statistics.median(values) * 0.80, 2)
    if replay_frames is not None:
        floors["floors"]["replay_full_pipeline_frames_per_s"] = round(replay_frames * 0.80, 1)
    if not floors["floors"]:
        return
    path = session / "floors.json"
    path.write_text(json.dumps(floors, indent=2) + "\n", encoding="utf-8")
    committed = pathlib.Path(__file__).resolve().parents[1] / "tests/load/baselines/v2-floors.json"
    committed.write_text(json.dumps(floors, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    if len(sys.argv) < 2:
        raise SystemExit("usage: perf-report.py SESSION_DIR [OUT.md]")
    session = pathlib.Path(sys.argv[1])
    out = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else session / "SUMMARY.md"
    manifest = {}
    man_path = session / "session.json"
    if man_path.exists():
        manifest = load_json(man_path)
    golden = pathlib.Path(__file__).resolve().parents[1] / "tests/load/baselines/replay-golden-sha256.txt"
    counts: dict[str, int] = {}
    for raw in golden.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line.startswith("point_frames:"):
            counts["point_frames"] = int(line.split(":", 1)[1].strip())
        elif line.startswith("match_frames:"):
            counts["match_frames"] = int(line.split(":", 1)[1].strip())
    if "point_frames" not in counts or "match_frames" not in counts:
        raise SystemExit(f"missing frame counts in {golden}")
    lines = [
        f"# {manifest.get('title', 'Credible local performance evidence')}",
        "",
        "Generated from session JSON. Do not mix the five claim labels:",
        "",
    ]
    for label in CLAIM_LABELS:
        lines.append(f"- {label}")
    lines.extend(
        [
            "",
            "## Environment",
            "",
            f"- date: {manifest.get('date', '')}",
            f"- git: {manifest.get('gitSha', '')}",
            f"- hardware: {manifest.get('hardware', '')}",
            f"- jvm: {manifest.get('jvm', '')}",
            f"- postgres: {manifest.get('postgres', '')}",
            f"- forks: {manifest.get('jmhForks', '')}",
            f"- limitations: {manifest.get('limitations', '')}",
            "",
            "## Results",
            "",
        ]
    )
    lines.extend(
        render_jmh(
            "In-process CPU latency",
            "in-process latency",
            jmh_rows(session / "jmh-hotpath.json"),
            counts["point_frames"],
            counts["match_frames"],
        )
    )
    replay_rows = jmh_rows(session / "jmh-replay.json")
    lines.extend(
        render_jmh(
            "Replay physics",
            "replay frames/s",
            replay_rows,
            counts["point_frames"],
            counts["match_frames"],
        )
    )
    lines.extend(replay_ladder(replay_rows, counts["point_frames"], counts["match_frames"]))
    lines.extend(
        render_jmh(
            "In-memory archive tape",
            "in-memory archive events/s (not staging/promote rows/s)",
            jmh_rows(session / "jmh-archive.json"),
            counts["point_frames"],
            counts["match_frames"],
        )
    )
    lines.append("Archive tape throughput is **not** staging/promote rows/s.")
    lines.append("")
    durable_lines, durable_samples = render_http_runs(
        "Atomic match commits",
        "atomic commit TPS",
        sorted(session.glob("durable-*.json")),
    )
    bulk_lines, bulk_samples = render_http_runs(
        "Bulk ingest",
        "transactional batch points/s and staging/promote rows/s",
        sorted(session.glob("bulk-*.json")),
    )
    lines.extend(durable_lines)
    lines.extend(bulk_lines)
    replay_frames = None
    for row in replay_rows:
        name = row.get("benchmark", "").rsplit(".", 1)[-1]
        if name == "fullPointPipeline" and row.get("mode") == "thrpt":
            score = (row.get("primaryMetric") or {}).get("score")
            if score is not None:
                replay_frames = score * counts["point_frames"]
    write_floors(
        session,
        [rate for op, rate in durable_samples if op == "atomic_commit_tps"],
        bulk_samples,
        replay_frames,
    )
    lines.extend(
        [
            "## Historical single-run rows",
            "",
            "Keep [2026-08-25-replay-physics.md](2026-08-25-replay-physics.md), "
            "[2026-08-25-archive-tape.md](2026-08-25-archive-tape.md), "
            "[2026-08-25-durable-match-write.md](2026-08-25-durable-match-write.md), and "
            "[2026-08-25-bulk-ingest.md](2026-08-25-bulk-ingest.md) as historical one-fork / one-run evidence. "
            "Do not replace them in place.",
            "",
        ]
    )
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
