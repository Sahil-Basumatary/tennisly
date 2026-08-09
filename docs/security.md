# Security

Living audit notes for Phase 7 Week 30. Treat this as the checklist we close before calling the stack production-ready.

## Controls already in place

| Control | Status | Notes |
|---|---|---|
| Clerk JWT validation at gateway | Done | Issuer + JWKS; roles forwarded only from trusted claims |
| Public API key auth | Done | `X-Api-Key` → user-service validate; spoofable identity headers stripped |
| Tier rate limits | Done | Redis fixed window; `RATE_LIMIT_FAIL_OPEN` (default `true` local, set `false` in prod) |
| Webhook HMAC | Done | `t=,v1=` HMAC-SHA256 + skew window (`WebhookSignature`) |
| Webhook secret storage | Done | SHA-256 lookup hash + AES-GCM ciphertext |
| Webhook SSRF guard | Done | `WebhookUrlValidator` blocks private/link-local by default |
| Security response headers (gateway) | Done | nosniff, DENY frames, HSTS, referrer, permissions-policy |
| Security response headers (web) | Done | CSP + nosniff + frame deny + referrer + permissions-policy |
| Dependency review on PRs | Done | GitHub `dependency-review-action` fails on high+ |
| Dependabot | Done | Maven / npm / Actions weekly |

## Production knobs

```bash
# Fail closed when Redis is down — prefer in staging/prod
RATE_LIMIT_FAIL_OPEN=false

# Never ship logging-only webhook/email secrets to prod without providers configured
WEBHOOK_ENCRYPTION_KEY=<32-byte-base64>
FCM_PROJECT_ID=...
FCM_ACCESS_TOKEN=...
```

## Open items (next milestones)

1. Tighten CSP (`unsafe-inline` / `unsafe-eval` still required by Next + Clerk — document residual risk).
2. CORS allowlist beyond `localhost:3000` for real web origins.
3. Actuator exposure review (gateway currently exposes `gateway` endpoint — lock down in prod profile).
4. OWASP ZAP baseline against `/api/v1` with a disposable key.
5. SQL injection / XSS spot-check on admin forms (prefer parameterized JPA — already default).
6. Secrets scan in CI (gitleaks) before open-sourcing.

## Threat notes worth remembering

- Match events fan out to **all** orgs subscribed to that event type — webhook config is an authorization surface.
- Internal service routes must never be reachable from the public internet without network policy / mesh auth.
- API key plaintext is shown once; revoke + rotate path must stay admin-audited.
