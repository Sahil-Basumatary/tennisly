# Webhooks

Tennisly delivers platform events to external HTTP endpoints configured by
organizations via the user-service Webhook settings.

## Event types

| Type | Trigger |
|------|---------|
| `match.completed` | A match transitions to `COMPLETED` |
| `match.point_recorded` | A point is scored during a live match |
| `api_key.revoked` | An API key belonging to the organization is revoked |
| `webhook.test` | Manual test ping from the Webhook settings page |

## Delivery envelope

Every POST to the configured `targetUrl` carries a JSON body:

```json
{
  "id": "<eventId>",
  "type": "match.completed",
  "createdAt": "2026-08-08T12:00:00Z",
  "data": { ... }
}
```

## Headers

| Header | Value |
|--------|-------|
| `Content-Type` | `application/json` |
| `X-Tennisly-Event` | The event type, e.g. `match.completed` |
| `X-Tennisly-Delivery` | UUID of this individual delivery attempt |
| `X-Tennisly-Signature` | HMAC signature (see below) |
| `User-Agent` | `Tennisly-Webhooks/1.0` |

## Signature verification

Signatures use HMAC-SHA256 with the endpoint\'s signing secret.

Format: `t=<unix_epoch_seconds>,v1=<hex_hmac>`

The signed content is `<timestamp>.<raw_body>`. To verify:

1. Extract `t` and `v1` from the header.
2. Compute `HMAC-SHA256(signing_secret, "<t>.<raw_body>")`.
3. Compare with `v1` using a timing-safe comparison.
4. Optionally reject if `t` is more than 5 minutes old (replay protection).

## Retry policy

Failed deliveries (non-2xx or network error) are retried with exponential backoff:

| Attempt | Delay |
|---------|-------|
| 1 | 30 seconds |
| 2 | 2 minutes |
| 3 | 10 minutes |
| 4 | 1 hour |
| 5 | 6 hours (cap) |

After 5 failed attempts the delivery is marked `DEAD` and no further retries
are attempted.

## Delivery worker

The `notification-service` polls the `webhook_deliveries` table every 2 seconds
for rows with status `PENDING` or `FAILED` whose `next_attempt_at <= now()`.
On Render, Kafka is off, so user-service and match-service POST the same events
to `/internal/events/**` (idempotent with the Kafka path).
Each delivery is sent with a connect timeout of 3 seconds and a read timeout of
10 seconds.

## Admin delivery observability

Platform admins can inspect and requeue deliveries from `/admin/webhooks`
(Delivery log panel). The web BFF calls `/api/notifications/admin/deliveries`
through the same origin as other admin APIs. Locally that is notification-service
on `:18087`; in production `NOTIFICATION_SERVICE_URL` is the **gateway**, which
relays `X-Gateway-Token` and routes `/api/notifications/**`.

```
GET  /api/notifications/admin/deliveries?organizationId=&endpointId=&status=&eventType=&page=&size=
GET  /api/notifications/admin/deliveries/{id}          # includes raw payload
POST /api/notifications/admin/deliveries/{id}/retry    # FAILED/DEAD/PENDING → PENDING now
```

Auth mirrors other admin surfaces: Clerk session in the BFF, then
`X-User-Id` + `X-User-Roles: ADMIN` to notification-service.
List responses omit payloads; detail includes them.
Manual retry of `DEAD` resets the attempt budget; `FAILED` keeps the count.

## Internal API contract (user-service)

The notification-service depends on these internal endpoints exposed by
user-service:

```
GET  /internal/webhooks/subscriptions?eventType=match.completed
     -> [ { id, organizationId, targetUrl, signingSecret } ]

GET  /internal/webhooks/endpoints/{id}
     -> { id, organizationId, targetUrl, signingSecret }

POST /internal/webhooks/endpoints/{id}/mark-delivered
     -> 204 No Content (optional)
```

## Running locally

```bash
# The service binds to port 18087 by default
SERVER_PORT=18087 ./mvnw -pl services/notification-service spring-boot:run

# Or via make
make notification
```

Requires Postgres (`tennisly_notifications` database), Kafka, and Eureka to be
running. Use `make up` to start the full stack.
