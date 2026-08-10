# Spec: at-till card payment endpoint (not yet implemented)

**Status: design only.** This describes a new endpoint for `AAtish/Admin` (the real Next.js
backend at `https://admin.firehut.uk`) that does not exist yet. I don't have that codebase in
this workspace, so this was written as a self-contained contract for a session with real access
to apply — it was not applied against the live server, and nothing here has been tested against
real Prisma models or route conventions beyond what `openapi.yaml` documents.

## Why this is needed

Fire Hut's existing device API (`openapi.yaml`) assumes orders arrive already paid — from the
website checkout, before they ever reach the till. That's true for DELIVERY and COLLECTION. For
**DINE_IN** orders (the schema already has `tableLabel` for these), a customer may order at the
table and pay at the till when the food is ready, via a card terminal connected to the Android
app (Stripe Terminal / SumUp / Flatpay — see `CARD_PAYMENT_SETUP.md`). There's currently no way
to tell the backend "this order was just paid at the till."

## Proposed endpoint

```
POST /api/device/orders/{id}/charge
Authorization: Bearer <device token>   (same auth as every other /api/device/* route)
```

**Request body:**
```json
{
  "provider": "STRIPE" | "SUMUP" | "FLATPAY" | "MOCK",
  "amount": 24.50,
  "cardBrand": "Visa",
  "cardLast4": "4242",
  "terminalReference": "MOCK-A1B2C3D4"
}
```
- `provider` — which terminal integration took the payment (matches
  `com.cyebrcina.pos.payment.model.PaymentProvider` in the Android app).
- `amount` — should be validated server-side against the order's actual `total` (from the same
  Prisma order record `/orders/pending`/`/orders/history` already serialize) rather than trusted
  blindly from the client, the same way you'd validate any payment amount.
- `cardBrand` / `cardLast4` / `terminalReference` — optional, whatever the terminal SDK returned;
  for receipts/audit trail, not for charging (the actual charge already happened on the terminal
  hardware by the time this call is made — this endpoint *records* a payment, it doesn't
  *initiate* one).

**Response (200):**
```json
{ "ok": true }
```
Mirrors the existing `/orders/{id}/accept` response shape for consistency.

**Error cases**, matching the existing device route conventions in `openapi.yaml`:
- `400` — order not found, already paid, wrong order type (e.g. attempted on a DELIVERY order),
  or amount mismatch
- `401` — missing/expired bearer token (existing `Unauthorized` response shape: `{ "error": "..." }`)

## Where this likely fits in the existing codebase

Based on the route conventions already visible in `openapi.yaml` (`/api/device/orders/{id}/accept`,
`/api/device/orders/{id}/reject` presumably live in `AAtish/Admin/src/app/api/device/orders/[id]/`
route handlers, backed by helpers in `device-order.ts`):
- New route file alongside `accept`/`reject`, e.g.
  `src/app/api/device/orders/[id]/charge/route.ts`
- A new function in `device-order.ts` (matching whatever `acceptOrder(id, deviceId)` /
  `rejectOrder(id, deviceId)` currently look like) — likely something recording a payment record
  against the order (check `prisma/schema.prisma` for whether there's already a `Payment` model
  used for the website's Stripe checkout flow; if so, this should probably create a row there
  with a `source: "DEVICE_TERMINAL"` or similar discriminator rather than inventing a parallel
  concept)
- Should broadcast `order_payment_updated` over Socket.IO (already defined in the realtime
  section of `openapi.yaml`) so other devices/Admin see the payment land live — the Android
  client already listens for this event and will refresh automatically once it does

## What the Android client already does

`data/remote/FireHutDeviceApi.kt` has `chargeOrder(orderId, ChargeOrderRequest): ChargeOrderResponse`
wired to `POST /api/device/orders/{id}/charge` already — calling it today will 404, cleanly,
since the route doesn't exist. `feature/order/detail/OrderDetailViewModel.kt`'s `chargeCard()`
flow: runs the terminal charge for real via `PaymentTerminalService`, then calls this endpoint to
record it, and surfaces "Card charged, but couldn't record it on the server yet" if that record
call fails — so the cashier isn't left thinking the charge itself failed once you implement this.
