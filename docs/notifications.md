# Notifications

Tennisly sends transactional email from `notification-service`. Preferences live
in `user-service` (`user_preferences`); delivery logs live in
`tennisly_notifications.email_deliveries`.

## Providers

| `NOTIFICATION_EMAIL_PROVIDER` | Behavior |
|-------------------------------|----------|
| `logging` (default) | Logs `to` + `subject` only. Safe for local/dev. |
| `resend` | Calls Resend HTTP API. Falls back to logging if `RESEND_API_KEY` is blank. |

```bash
NOTIFICATION_EMAIL_ENABLED=true
NOTIFICATION_EMAIL_PROVIDER=logging   # or resend
NOTIFICATION_EMAIL_FROM='Tennisly <onboarding@resend.dev>'
RESEND_API_KEY=re_xxx
```

## Categories

Stored under `extra_settings.emailCategories`:

| Key | Trigger |
|-----|---------|
| `welcome` | `USER_CREATED` Kafka event |
| `apiKeyRevoked` | Webhook domain event when an org API key is revoked |
| `webhookFailed` | Webhook delivery marked `DEAD` after retries |

Master gates: `notificationsEnabled` **and** `emailNotifications` must be true.

## Preference API

- User: `GET/PUT /api/users/me/preferences` (via web BFF `/api/preferences`)
- Internal: `GET /internal/users/by-clerk/{clerkId}/email-preference?category=welcome`
- Internal: `GET /internal/organizations/{orgId}/email-recipients?category=apiKeyRevoked`

## Idempotency

Each send is keyed by `category:eventId:email`. Replays skip duplicates. Statuses:
`SENT`, `SKIPPED`, `FAILED`.

## UI

Signed-in users manage toggles at `/settings/notifications`.
