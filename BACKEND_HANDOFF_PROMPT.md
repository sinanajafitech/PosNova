# Prompt for Claude: three open gaps in the FIRE HUT device/EPOS API

Paste everything below the `---` into a Claude Code session that has real access to `AAtish/Admin`
(the Next.js backend at `https://admin.firehut.uk`) — this repo (the Android till app) only has
`openapi.yaml` and the specs referenced below, not the backend source, so none of this has been
applied or tested against real Prisma models or route conventions.

---

I have a working Next.js backend (`AAtish/Admin`) for a pizza restaurant's ordering platform. It
exposes a device/EPOS API at `https://admin.firehut.uk/api/device/*`, used today by a live Android
till app. `openapi.yaml` in the Android repo documents what's implemented today — three gaps in it
were just found during a client-side audit of that app, in priority order:

## 1. `POST /api/device/orders` has no idempotency key (HIGH — silent duplicate-order risk, live today)

This endpoint is real and live — the till uses it for every order, right now, in production. It
has no way to detect a retried request. If the server successfully creates an order but its
response never reaches the client (timeout, dropped connection after processing), the Android app
sees that as a failure and retries the *exact same* request — either immediately (an `IOException`
during `createOrder()`) or later from its offline queue (`flushPendingOrders()`, every 15s or on
reconnect). With no dedup key, that retry creates a **second, real order** for the same items and
payment.

The Android client has already been updated to send a new field on every request:

```json
{ "...": "...(existing fields, see openapi.yaml)...", "idempotencyKey": "a stable UUID, generated once per order attempt and unchanged across every retry of that same order" }
```

Please add server-side handling: treat a `POST /api/device/orders` whose `idempotencyKey` matches
one already recorded (scope the uniqueness however fits the schema — e.g. per device or per store,
within a reasonable window like 24h) as **returning the existing order**, `200`, same response
shape as a fresh create — not creating a second one. A new unique-indexed column on whatever model
backs `DeviceOrder`/the order table is the obvious shape; check `prisma/schema.prisma` for the
existing conventions before adding it.

## 2. `POST /api/device/orders/{id}/charge` doesn't exist yet (card payments taken at the till aren't recorded)

Full contract already written up: **`BACKEND_CARD_PAYMENT_SPEC.md`** in the Android repo. Short
version: when a DINE_IN order is paid at the till via a card terminal (Stripe Terminal / SumUp /
Flatpay), the charge happens for real on the physical terminal, but there's currently no way to
tell the backend it happened — `chargeOrder()` in `FireHutDeviceApi.kt` calls this route today and
gets a clean 404 every time. Read that spec file in full before implementing; it covers the
request/response shape, validation (amount must be checked against the order's real total, not
trusted from the client), where it likely fits alongside the existing `accept`/`reject` routes, and
how it should share a payment-recording path with item 1 above if that makes sense against the
real schema.

## 3. QR-pay orders have no `paymentStatus` (till sits on the QR screen forever once this is added)

Full contract already written up: the **"QR payment for till-created orders"** section of
**`BACKEND_ORDER_CREATE_SPEC.md`** in the Android repo (the rest of that doc describes item 1's
endpoint, which is already live — only this section is still open). Short version: a QR-paid order
is created unpaid, and the client needs some field to poll for confirmation once the customer
actually pays on their phone. The client already watches `DeviceOrder.paymentStatus` for `"PAID"`
(see `NewOrderViewModel.kt`, search `paymentStatus`) and reacts correctly — it's just never sent
today, so this is genuinely dead code on the client until the field exists and gets set from the
Stripe webhook, broadcast over the existing `order_payment_updated` Socket.IO event.

---

Before writing code: read the relevant route file(s) and their Prisma models
(`AAtish/Admin/prisma/schema.prisma`) in full, and confirm your understanding of the existing
auth/broadcast/polling flow back to me before implementing — this is a live production system
serving a real restaurant. Do item 1 first; it's the only one of the three where the current
behavior (not just missing behavior) is a real bug against a real, already-used endpoint.
