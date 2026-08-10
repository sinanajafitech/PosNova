# Spec: till-created order endpoint (not yet implemented)

**Status: design only.** Same situation as `BACKEND_CARD_PAYMENT_SPEC.md` — this describes a new
endpoint for `AAtish/Admin` that doesn't exist yet. I don't have that codebase in this workspace,
so this is a self-contained contract for a session with real access to apply, not something
tested against real Prisma models or route conventions beyond what `openapi.yaml` documents.

## Why this is needed

`GET /api/device/menu` exists specifically "for a till that builds its own orders" (per its own
description in `openapi.yaml`), but there is currently **no endpoint to submit that order** —
every other device order route (`accept`, `reject`, `receipt`, `ticket`) operates on an order
that already exists. Fire Hut orders today only originate from the website's checkout flow. This
endpoint is what lets the Android till ring up walk-in/phone orders directly, using the menu it
can already fetch.

## Proposed endpoint

```
POST /api/device/orders
Authorization: Bearer <device token>
```

**Request body:**
```json
{
  "type": "DINE_IN",
  "tableLabel": "5",
  "customerName": "Walk-in",
  "customerPhone": null,
  "notes": "No onions on item 2",
  "items": [
    {
      "productId": "clx...",
      "sizeId": "clx...",
      "quantity": 2,
      "addOnIds": ["clx...", "clx..."],
      "notes": "extra spicy"
    }
  ],
  "payment": {
    "method": "CASH",
    "amount": 24.50,
    "cashTendered": 30.00,
    "provider": null,
    "cardBrand": null,
    "cardLast4": null,
    "terminalReference": null
  }
}
```

- `type` — reuses the existing `OrderType` enum (`DELIVERY`/`COLLECTION`/`DINE_IN`). Till-created
  orders are expected to use `DINE_IN` (with `tableLabel`) or `COLLECTION` (walk-in/phone, no
  table) — `DELIVERY` doesn't make sense for an order rung up in person and is presumably
  rejected or ignored server-side.
- `items[].productId` / `sizeId` / `addOnIds` reference the IDs `GET /api/device/menu` already
  returns — the server should resolve names/prices from those IDs itself (same principle as
  every other device order: the device never dictates pricing, the server does), not trust
  prices sent from the client.
- `payment` — `method` is `CASH`, `CARD`, or `QR` (see "QR payment" section below — added after
  the original CASH/CARD-only version of this spec, once a real `payment-link` endpoint went
  live for *existing* orders and the till needed the same capability for orders it creates
  itself). For `CASH`/`CARD`, the order is paid **at the point of creation** — payment has
  already happened by the time this call is made. For `CARD`, `provider` matches
  `com.cyebrcina.pos.payment.model.PaymentProvider` (`STRIPE`/`SUMUP`/`FLATPAY`/`MOCK`) and the
  charge has **already happened on the physical terminal** — same principle as
  `BACKEND_CARD_PAYMENT_SPEC.md`'s `/charge` endpoint, this call *records* a payment, it doesn't
  *initiate* one. For `CASH`, `cashTendered` lets the server's Z-report compute cash-drawer
  reconciliation correctly. `QR` is different: the order is created **unpaid**, see below.

**Response (200):**
```json
{ "order": { /* full DeviceOrder, same shape as /orders/pending and /orders/history */ } }
```
Returning the full resolved `DeviceOrder` lets the till immediately call the *existing*
`GET /api/device/orders/{id}/receipt` and `GET /api/device/orders/{id}/ticket` endpoints to print
— no new printing logic needed, this endpoint only needs to create the order and hand back its ID
(and ideally the full order for convenience, avoiding an extra round-trip).

**Error cases**, matching existing device route conventions:
- `400` — empty `items`, unknown `productId`/`sizeId`/`addOnId`, missing `tableLabel` for
  `DINE_IN`, payment amount mismatch, or `type: DELIVERY`
- `401` — missing/expired bearer token (existing `Unauthorized` shape: `{ "error": "..." }`)

## Where this likely fits in the existing codebase

Based on the route conventions visible in `openapi.yaml` (`src/app/api/device/orders/...` route
handlers backed by `device-order.ts`):
- New route file: `src/app/api/device/orders/route.ts` (POST handler alongside wherever
  `orders/pending` and `orders/history` GET handlers live)
- A new function in `device-order.ts`, e.g. `createDeviceOrder(deviceId, payload)` — should
  resolve product/size/add-on prices from the database (never trust client-sent prices), create
  the order + a payment record, and return it shaped like the existing `DeviceOrder` serializer
  already used by `pending`/`history` (reuse that mapping function rather than duplicating it)
- Should broadcast `new_order` and/or `order_status_updated` over Socket.IO so other
  devices/Admin see it appear live, same as website-originated orders
- Payment recording should probably share a code path with the `/orders/{id}/charge` endpoint
  from `BACKEND_CARD_PAYMENT_SPEC.md` if that's implemented first — both are "record a payment
  taken at the till" operations, just at different points in an order's lifecycle

## QR payment for till-created orders (added after `/payment-link` went live)

`POST /api/device/orders/{id}/payment-link` already exists and works for orders that already
have an id — it's wired into Order Detail today for DINE_IN orders paying at the table. The till
order-creation flow needs the same capability for a walk-in/phone order that hasn't been rung up
yet, but that's a chicken-and-egg problem: `payment-link` needs an order id, and an order doesn't
have one until `POST /api/device/orders` creates it. Unlike CASH/CARD, a QR order **can't** be
"already paid" at creation time — the customer scans and pays on their own phone, possibly
minutes later, after the till has moved on to the next customer.

**Request:** send `payment: { "method": "QR", "amount": 24.50 }` — no `cashTendered`, no
`provider`/`cardBrand`/`cardLast4`/`terminalReference` (nothing has been charged yet).

**What should happen server-side** (this part needs real judgment against the actual Prisma
schema — treat it as a starting proposal, not a spec to implement blindly):
1. Create the order as normal, but mark it unpaid. This needs *some* field the device can poll
   for — proposed addition to the `DeviceOrder` schema:
   ```
   paymentStatus: "PENDING" | "PAID" | "FAILED"   (nullable — absent/null on order types that
                                                     don't track this, e.g. already-paid CASH/CARD
                                                     orders, or website orders)
   ```
2. The order should still appear in `GET /api/device/orders/pending` (or wherever it already
   fits in the existing accept/reject queue) with `paymentStatus: "PENDING"` — reusing the
   existing polling/Socket.IO infrastructure rather than inventing a new list endpoint. Whether
   "Accept" should be blocked/hidden until `paymentStatus` is `PAID` is a business-logic call —
   flagging it, not deciding it here.
3. When Stripe confirms payment (the same webhook that already updates the order for the
   "Payment QR" button in Admin), flip `paymentStatus` to `PAID` and broadcast the *already
   existing* `order_payment_updated` Socket.IO event. No new realtime event needed — the Android
   client already refreshes `pending` on every `order_payment_updated` it receives
   ([FireHutRealtimeManager.kt](app/src/main/java/com/cyebrcina/pos/data/remote/realtime/FireHutRealtimeManager.kt)),
   it just currently has nothing to *check* once it refreshes. Adding `paymentStatus` is what
   closes that loop.
4. If the customer never pays, this is a real edge case with no obvious right answer from here —
   an expiring Stripe Checkout Session, a manual "cancel" via the existing `/reject` endpoint, or
   a timeout job are all reasonable; whoever implements this should decide against the real
   order lifecycle, not this doc.

**Response (200):** same `{ "order": { ...DeviceOrder with paymentStatus: "PENDING"... } }` shape
as CASH/CARD. The device then calls the already-existing `POST /orders/{id}/payment-link` with
that id to get the QR, exactly like it does for an existing DINE_IN order today.

## What the Android client already does

`feature/order/create/` builds a cart from the real `GET /api/device/menu` catalog (category
browsing, per-product size/add-on/notes selection, running totals), then `CheckoutScreen` collects
payment via three tabs — Cash (tendered/change), Card (real terminal charge via the existing
`PaymentTerminalService`), or QR — and calls `POST /api/device/orders` with everything above.
Until that route exists, submission fails with a clear "couldn't reach the server" / 404-derived
error rather than silently pretending to succeed.

For Cash/Card, on success the app immediately fetches and prints the customer receipt + kitchen
ticket via the existing (already-live) `/orders/{id}/receipt` and `/orders/{id}/ticket` endpoints
— no client changes needed for that part.

For QR, the client instead: creates the order with `payment.method: "QR"`, calls the
already-live `POST /orders/{id}/payment-link` with the returned order id, and displays
`qrCodeDataUrl` full-screen. It then watches `OrderRepository.pendingOrders` (kept fresh by the
existing 15s poll + `order_payment_updated` socket trigger — no new client-side realtime
handling needed) for that specific order id's `paymentStatus` to flip to `"PAID"`, at which point
it prints the receipt/ticket and proceeds to Order Successful, same as Cash/Card. If
`paymentStatus` never appears on the returned order (i.e. this proposed field doesn't exist yet
server-side), the client has no way to detect payment completion and will sit on the QR screen
indefinitely — that's expected until the server side of this section is implemented, not a client
bug to chase.
