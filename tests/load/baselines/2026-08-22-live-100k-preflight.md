# 2026-08-22 live 100k cluster preflight

Read-only inspection before any 100k workload. No cluster objects were created.

## Control plane on this Mac

- kubectl 1.36.4 installed via Homebrew (`kubernetes-cli`)
- `~/.kube` is absent; `KUBECONFIG` is unset; `kubectl config current-context` has no context
- docker / kind / minikube / colima / podman / gcloud / aws / az are not installed
- no Docker-compatible GUI app in `/Applications`

There is no reachable Kubernetes API from this laptop. The previously selected “existing cluster” cannot be inspected, sized, or billed until a kubeconfig is provided.

## Laptop ceiling (not a 100k host)

- Apple M1 Pro, 10 cores, 16 GiB RAM (`hw.memsize=17179869184`)
- Local Postgres 16 and Redis 7 were used for 20–40 and 100-client evidence
- k6 v2.1.0 is present
- This machine cannot hold 100,000 client VUs plus 8 service replicas without swapping. It is a staging laptop, not the proof environment.

## Required cluster shape (when credentials exist)

Do not apply manifests until this is confirmed on the real cluster.

| Workload | Intent | Requested resources |
|---|---|---|
| match-service × 8 | ~12.5k sockets/pod | 8 CPU / 16 Gi request; 32 CPU / 24 Gi limit |
| k6 workers × 10 | 10k VUs/worker at 100k | 40 CPU / 160 Gi request (16 Gi/worker) |
| Redis × 1 | Pub/Sub bus | 0.5–2 CPU / 0.5–2 Gi |
| Prometheus × 1 | global k6 histograms + JVM scrape | 1 CPU / 2 Gi |
| Postgres | outside the bundle | pool 20/replica → 160 connections plus admin |

Rough incremental cost if the cluster autoscale on public cloud: **£4–8 per hour** for ~6–8 memory-heavy nodes, plus the existing Postgres bill. Confirm the cloud invoice before `APPROVE_SCALE_PROVISION=true`.

## Image

`infrastructure/kubernetes/live-100k/match-service.yaml` still requires an immutable digest in `MATCH_SERVICE_IMAGE`. A placeholder tag is not a reproducible run.

## Approval gate

Provisioning is blocked until:

1. a kubeconfig context works (`kubectl cluster-info`)
2. `MATCH_SERVICE_IMAGE` is a digest the cluster can pull
3. secret `tennisly-live/tennisly-live-db` exists
4. `APPROVE_SCALE_PROVISION=true` is set after reviewing the cost table above
