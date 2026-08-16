# Notifications

Tennisly sends transactional **email** and **push** from `notification-service`.
Preferences live in `user-service` (`user_preferences`); delivery logs and device
tokens live in `tennisly_notifications`. On Render, Kafka is off and email/push
use the `logging` provider until Resend/FCM keys exist.

## Email providers

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

## Push providers

| `NOTIFICATION_PUSH_PROVIDER` | Behavior |
|------------------------------|----------|
| `logging` (default) | Logs token suffix + title. Safe for local/dev. |
| `fcm` | FCM HTTP v1. Needs `FCM_PROJECT_ID` + short-lived `FCM_ACCESS_TOKEN`. |

```bash
NOTIFICATION_PUSH_ENABLED=true
NOTIFICATION_PUSH_PROVIDER=logging   # or fcm
FCM_PROJECT_ID=your-gcp-project
FCM_ACCESS_TOKEN=ya29....             # mint via service account / workload identity
```

Device registration (authenticated):

```
GET    /api/notifications/me/device-tokens
POST   /api/notifications/me/device-tokens   { "token": "...", "platform": "WEB"|"IOS"|"ANDROID" }
DELETE /api/notifications/me/device-tokens/{id}
```

Web BFF: `/api/device-tokens`.

## Categories

Shared keys under `extra_settings.emailCategories` and `extra_settings.pushCategories`:

| Key | Trigger |
|-----|---------|
| `welcome` | `USER_CREATED` Kafka event |
| `apiKeyRevoked` | Org API key revoked |
| `webhookFailed` | Webhook delivery marked `DEAD` |

Master gates:

- Email: `notificationsEnabled` **and** `emailNotifications`
- Push: `notificationsEnabled` **and** `pushNotifications`

## Preference API

- User: `GET/PUT /api/users/me/preferences` (via web BFF `/api/preferences`)
- Internal email: `/internal/users/by-clerk/{clerkId}/email-preference?category=`
- Internal push: `/internal/users/by-clerk/{clerkId}/push-preference?category=`
- Org recipients: `/internal/organizations/{orgId}/email-recipients|push-recipients?category=`

## Idempotency

- Email: `category:eventId:email`
- Push: `category:eventId:deviceTokenId` (or `:clerkId:skipped`)

Statuses: `SENT`, `SKIPPED`, `FAILED`.

## UI

Signed-in users manage toggles and register a demo web device token at
`/settings/notifications`.
