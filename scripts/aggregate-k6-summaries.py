#!/usr/bin/env python3
import json
import sys
from pathlib import Path

def metric(summary, name):
    return summary.get("metrics", {}).get(name, {})

def count_of(summary, name):
    return float(metric(summary, name).get("count", 0) or 0)

def rate_of(summary, name):
    row = metric(summary, name)
    return float(row.get("value", row.get("rate", 0)) or 0)

def percentile(summary, name, key):
    return float(metric(summary, name).get(key, 0) or 0)

def main():
    if len(sys.argv) < 2:
        print("usage: aggregate-k6-summaries.py FILE [FILE...]", file=sys.stderr)
        return 2
    paths = [Path(item) for item in sys.argv[1:]]
    summaries = []
    for path in paths:
        summaries.append(json.loads(path.read_text(encoding="utf-8")))
    messages = sum(count_of(item, "live_ws_messages") for item in summaries)
    subscribed = sum(count_of(item, "live_ws_subscribed") for item in summaries)
    failed = sum(count_of(item, "live_ws_connect_failed") for item in summaries)
    unrecovered = sum(count_of(item, "live_ws_unrecovered_gaps") for item in summaries)
    duplicates = sum(count_of(item, "live_ws_duplicates") for item in summaries)
    malformed = sum(count_of(item, "live_ws_malformed_frames") for item in summaries)
    recovered = sum(count_of(item, "live_ws_recovered_events") for item in summaries)
    worst_p99 = max((percentile(item, "live_ws_delivery_ms", "p(99)") for item in summaries), default=0)
    worst_p95 = max((percentile(item, "live_ws_delivery_ms", "p(95)") for item in summaries), default=0)
    worst_p50 = max((percentile(item, "live_ws_delivery_ms", "med") for item in summaries), default=0)
    print(
        "aggregate "
        f"workers={len(summaries)} "
        f"messages={messages:.0f} "
        f"subscribed={subscribed:.0f} "
        f"connect_failed={failed:.0f} "
        f"unrecovered={unrecovered:.0f} "
        f"duplicates={duplicates:.0f} "
        f"malformed={malformed:.0f} "
        f"recovered={recovered:.0f} "
        f"worst_worker_p50={worst_p50:.2f}ms "
        f"worst_worker_p95={worst_p95:.2f}ms "
        f"worst_worker_p99={worst_p99:.2f}ms"
    )
    print(
        "note worst_worker_p99 is not a global percentile; use Prometheus native histograms for global p50/p95/p99"
    )
    if failed > 0 or unrecovered > 0 or duplicates > 0 or malformed > 0:
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
