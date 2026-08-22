# 100k live-delivery manifests

Provider-neutral Kubernetes objects for kind, k3s, EKS, GKE, or AKS. They are executable only after cluster preflight, an immutable `MATCH_SERVICE_IMAGE` digest, and `APPROVE_SCALE_PROVISION=true`. They are not a completed 100k measurement by themselves.

Topology:

- 8 match-service replicas sharing Redis Pub/Sub and one Postgres
- Prometheus with remote-write receiver for **global** k6 native histograms
- Indexed k6 jobs, invoked one profile at a time
- `k6-realistic.yaml`: 8 match topics
- `k6-hot.yaml`: 1 match topic

Never apply both k6 jobs together.

## Preflight

Read [tests/load/baselines/2026-08-22-live-100k-preflight.md](../../../tests/load/baselines/2026-08-22-live-100k-preflight.md). Confirm allocatable CPU/memory, autoscaling, and the Postgres `max_connections` budget (20 per replica × 8 = 160 plus admin).

## Apply

```bash
kubectl apply -f infrastructure/kubernetes/live-100k/namespace.yaml
kubectl -n tennisly-live create secret generic tennisly-live-db \
  --from-literal=host=HOST \
  --from-literal=port=5432 \
  --from-literal=username=USER \
  --from-literal=password=PASS \
  --from-literal=database=tennisly_matches
export MATCH_SERVICE_IMAGE=registry.example/match-service@sha256:REPLACE
export APPROVE_SCALE_PROVISION=true
PROFILE=realistic make load-live-scale
```

`scripts/match-live-scale.sh` waits for readiness, runs 100 → 1k → 10k → 25k → 50k → 100k, and stops at the first failed gate. Hot-match is a separate `PROFILE=hot` invocation after realistic 100k passes.

Worker 0 creates benchmark matches; other workers join `GET /api/matches/external/{id}` after HTTP 409. k6 `--execution-segment` splits subscriber VUs. Global p99 must come from Prometheus (`K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true`), not by averaging worker summaries.

```promql
histogram_quantile(0.99, sum by (le) (rate(k6_live_ws_delivery_ms{client="normal"}[10m])))
sum(match_live_session_active)
sum(match_live_subscribe_active)
```

Publish a 100k baseline only when the run records connected clients, successful subscriptions, **global** p50/p95/p99, disconnects, replay gaps, duplicates, server CPU/memory/GC, network throughput, load-generator saturation, image digest, and cluster shape. Local laptops cannot host this job.
