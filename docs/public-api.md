# Tennisly Public API (v1)

Machine-readable tennis data for integrations. All public endpoints live under `/api/v1` and require an organization API key.

## Authentication

Send your API key on every request:

```http
X-Api-Key: tly_live_<secret>
```

Keys are issued once at creation and stored only as a hash server-side. Do not commit keys to source control or expose them in client-side code.

Clerk JWT routes (for example `/api/users/**`) are unchanged and continue to use `Authorization: Bearer <jwt>`.

## Base URL

Through the API gateway:

```text
https://<gateway-host>/api/v1
```

Local default gateway port: `8080`.

## Endpoints

| Public path | Upstream service | Internal path |
|-------------|------------------|---------------|
| `GET /api/v1/players/**` | tennis-data-service | `/api/tennis/players/**` |
| `GET /api/v1/rankings/**` | tennis-data-service | `/api/tennis/rankings/**` |
| `GET /api/v1/tournaments/**` | tennis-data-service | `/api/tennis/tournaments/**` |
| `GET /api/v1/matches/**` | match-service | `/api/matches/**` |

Exact query parameters and response shapes match the internal read APIs after path rewrite.

## Creating an API key

Platform admins create keys in user-service:

```http
POST /api/users/admin/api-keys
Authorization: Bearer <clerk-jwt-with-admin-role>
Content-Type: application/json

{
  "organizationId": "<uuid>",
  "name": "Partner feed",
  "scopes": ["read"],
  "expiresAt": null
}
```

The response includes `plaintextKey` once. Store it securely; it cannot be retrieved again.

List and revoke keys via `/api/users/admin/api-keys` and `/api/users/admin/api-keys/{id}/revoke`.

## Rate limits

Public API traffic is limited **per organization** using a fixed one-minute window keyed in Redis (`apikey-rl:{orgId}:{yyyyMMddHHmm}` UTC). Limits depend on the organization plan tier returned during API key validation:

| Plan tier | Requests per minute |
|-----------|---------------------|
| FREE | 30 |
| BASIC | 120 |
| PRO | 600 |
| ENTERPRISE | 3,000 |

Unknown or missing tiers are treated as FREE. A separate IP-based gateway limiter (`10/s replenish, burst 20`) still applies to all routes as a coarse safety net.

Successful responses include:

- `X-RateLimit-Limit` — tier limit for the current window
- `X-RateLimit-Remaining` — requests left in the current window

When the org limit is exceeded:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: <seconds until next minute boundary>
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 0
Content-Type: application/json

{"error":"rate_limit_exceeded","planTier":"FREE"}
```

## Errors

| HTTP | Body | Meaning |
|------|------|---------|
| 401 | `{"error":"missing_api_key"}` | No `X-Api-Key` header |
| 401 | `{"error":"invalid_api_key"}` | Unknown, revoked, expired, or inactive key |
| 429 | `{"error":"rate_limit_exceeded","planTier":"<tier>"}` | Organization tier limit exceeded for the current minute |
| 429 | (gateway IP limiter) | IP-based default limiter on all gateway routes |

Downstream services may return their own 4xx/5xx responses after the gateway accepts the key.

## Trusted downstream headers

After validation the gateway sets (and strips client spoof attempts for):

- `X-Org-Id`
- `X-Api-Key-Id`
- `X-Api-Key-Scopes` (comma-separated)
- `X-Plan-Tier` (normalized uppercase tier used for rate limiting)

The raw `X-Api-Key` is not forwarded downstream.

## Webhooks

Organizations can register webhook endpoints to receive real-time notifications for platform events. Secrets are shown once at creation and used for HMAC-SHA256 signature verification.

### Event types

| Event type | Scope | Description |
|------------|-------|-------------|
| `match.completed` | Platform (fan-out to all subscribed orgs) | A match reached completed status |
| `match.point_recorded` | Platform (fan-out to all subscribed orgs) | A point was recorded in a live match |
| `api_key.revoked` | Org-scoped | An API key belonging to this org was revoked |
| `webhook.test` | Org-scoped | Manual test ping from the dashboard |

### Webhook endpoints

| Public path | Method | Description |
|-------------|--------|-------------|
| `/api/v1/webhooks` | GET | List webhook endpoints for the org |
| `/api/v1/webhooks` | POST | Create a new webhook endpoint |
| `/api/v1/webhooks/{id}/revoke` | POST | Revoke an endpoint |
| `/api/v1/webhooks/{id}/rotate-secret` | POST | Rotate the signing secret |
| `/api/v1/webhooks/{id}/test` | POST | Send a test event |

All webhook endpoints require `X-Api-Key` (or JWT with `X-Org-Id` header).

### Signature verification

Every delivery includes a `Tennisly-Webhook-Signature` header:

```text
t=1700000000,v1=5257a869e7ecebeda32affa62cdca3fa51cad7e77a0e56ff536d0ce8e108d8f9
```

Verify by computing HMAC-SHA256 of `"{timestamp}.{rawBody}"` using your endpoint secret. Reject if the timestamp is more than 5 minutes old.

### Create webhook request

```http
POST /api/v1/webhooks
X-Api-Key: tly_live_<secret>
Content-Type: application/json

{
  "name": "Match events",
  "targetUrl": "https://your-server.com/webhooks/tennisly",
  "eventTypes": ["match.completed", "match.point_recorded"],
  "description": "Receive match completion events"
}
```

The response includes `plaintextSecret` once. Store it securely; it cannot be retrieved again.

## Not in v1 slice 1

- Write endpoints or scoped authorization beyond gateway validation
