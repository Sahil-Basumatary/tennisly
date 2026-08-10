# Security

Living audit notes for Phase 7 Week 30. Treat this as the checklist we close before calling the stack production-ready.

## Controls already in place

| Control | Status | Notes |
|---|---|---|
| Clerk JWT validation at gateway | Done | Issuer + JWKS; roles forwarded only from trusted claims |
| Public API key auth | Done | `X-Api-Key` → user-service validate; spoofable identity headers stripped |
| Tier rate limits | Done | Redis fixed window; local default fail-open; **prod profile fail-closed** |
| CORS allowlist | Done | `CORS_ALLOWED_ORIGINS` CSV → `CorsWebFilter` (no Spring Cloud globalcors dual stack) |
| Actuator lockdown | Done | Only `/actuator/health(**)` public; other actuator paths deny; gateway endpoint disabled |
| Webhook HMAC | Done | `t=,v1=` HMAC-SHA256 + skew window (`WebhookSignature`) |
| Webhook secret storage | Done | SHA-256 lookup hash + AES-GCM ciphertext |
| Webhook SSRF guard | Done | `WebhookUrlValidator` blocks private/link-local by default; prod forces off |
| Security response headers (gateway) | Done | nosniff, DENY frames, HSTS, referrer, permissions-policy |
| Security response headers (web) | Done | CSP + nosniff + frame deny + referrer + permissions-policy |
| Dependency review on PRs | Done | GitHub `dependency-review-action` fails on high+ |
| Dependabot | Done | Maven / npm / Actions weekly |
| OWASP ZAP API scan | Done | OpenAPI-driven `zap-api-scan` against `/api/v1`; optional CI via `RUN_ZAP` |

## Production knobs

Activate Spring prod profile on gateway / user / notification (`SPRING_PROFILES_ACTIVE=prod`).

```bash
# Required in prod — comma-separated browser origins (no wildcard with credentials)
CORS_ALLOWED_ORIGINS=https://app.example.com,https://www.example.com

# Fail closed when Redis is down (prod profile default; override only if you accept open limits)
RATE_LIMIT_FAIL_OPEN=false

# Keep private/link-local webhook targets blocked (prod profile default)
WEBHOOK_ALLOW_PRIVATE_TARGETS=false

# Never ship logging-only webhook/email secrets to prod without providers configured
WEBHOOK_ENCRYPTION_KEY=<32-byte-base64>
FCM_PROJECT_ID=...
FCM_ACCESS_TOKEN=...
```

Local defaults stay developer-friendly: CORS `http://localhost:3000`, rate-limit fail-open, webhook private targets allowed for Testcontainers stubs.

## OWASP ZAP (public API)

Baseline **active + passive** scan of the gateway public surface via OpenAPI — not a full pen-test.

```bash
# Gateway + deps up; use a disposable key you can revoke after the run
export API_KEY=tly_live_...
# Mac Docker default (Linux loopback: http://127.0.0.1:8080)
export TARGET_URL=http://host.docker.internal:8080
make zap-api
# reports → .run/zap/zap-api-*.{html,json,md}
```

- Contract: `tests/security/public-api.openapi.yaml` (gateway `/api/v1/**` paths).
- Rule overrides: `tests/security/zap-api-rules.conf` (browser CSP noise IGNORE; SQLi/XSS/RCE FAIL).
- Soft local only: `ZAP_IGNORE_WARN=true` (never set in CI).
- Optional CI: repo var `RUN_ZAP=true` + secrets `ZAP_TARGET_URL`, `ZAP_API_KEY`.

## Open items (next milestones)

1. Tighten CSP (`unsafe-inline` / `unsafe-eval` still required by Next + Clerk — document residual risk).
2. SQL injection / XSS spot-check on admin forms (prefer parameterized JPA — already default).
3. Secrets scan in CI (gitleaks) before open-sourcing.
4. Expand Pact beyond players list (matches, webhooks) and consider a Pact Broker when multi-repo consumers appear.
5. Triage first real ZAP report and ratchet rules (WARN → FAIL) as findings clear.

## Threat notes worth remembering

- Match events fan out to **all** orgs subscribed to that event type — webhook config is an authorization surface.
- Internal service routes must never be reachable from the public internet without network policy / mesh auth.
- API key plaintext is shown once; revoke + rotate path must stay admin-audited.
- Empty `CORS_ALLOWED_ORIGINS` fails gateway startup on purpose — never deploy prod without it set.
