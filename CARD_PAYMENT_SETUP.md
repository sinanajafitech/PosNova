# Wiring in a real card payment terminal

The Payment screen's "Card" option now drives a real present-card → processing →
approved/declined flow (`PaymentViewModel.chargeCard()`), but it's backed by
`MockPaymentTerminalService` until a real provider is configured — same honest-scaffold approach
as the Imin printer. This doc covers connecting a real one.

## What exists today

- `payment/PaymentTerminalService.kt` — the interface the Payment screen depends on
- `payment/mock/MockPaymentTerminalService.kt` — default; simulates ~2.5s present→process→
  approve (and randomly declines ~1 in 12 charges so the decline/retry path is testable)
- `payment/stripe/StripeTerminalPaymentService.kt` — Stripe Terminal skeleton. Stripe's SDK is
  public (no developer-portal gate like Imin's), so this is the most concrete of the three —
  read the class doc for the real call shape
- `payment/sumup/SumUpPaymentService.kt` + `SumUpResultBridge.kt` — SumUp skeleton. SumUp's
  checkout SDK launches its own Activity and returns a result, so `SumUpResultBridge` bridges
  that into a suspend function (`MainActivity` forwards `onActivityResult` into it). **Lower
  confidence** — SumUp has changed their public API across SDK versions; verify against their
  current docs before trusting exact method names.
- `payment/flatpay/FlatpayPaymentService.kt` — Flatpay skeleton. **I don't have verified
  knowledge of Flatpay's Android SDK** (small Nordic/EU provider); this only implements the
  contract with a clear "not configured" failure. Confirm with Flatpay whether they even offer a
  native Android SDK or whether integration is REST-API-based before writing this one for real.
- `di/PaymentModule.kt` — binds whichever provider is active; `MockPaymentTerminalService` today

## Steps to connect a real provider

1. **Pick one provider** to bind — `PaymentModule.bindPaymentTerminalService` binds exactly one
   implementation at a time. (If you genuinely need to support multiple providers side by side —
   e.g. different terminals for different store locations — that's a bigger change: inject a
   `Map<PaymentProvider, PaymentTerminalService>` via Hilt's multibinding instead of a single
   `@Binds`, and pick the active one at runtime from a settings value.)
2. **Get the SDK + credentials** from that provider's developer portal (Stripe: a Stripe account
   + secret/publishable keys; SumUp: an App ID + affiliate key; Flatpay: whatever they require —
   confirm with them directly).
3. **Add the dependency** to `app/build.gradle.kts` — Stripe's is on Maven Central
   (`com.stripe:stripeterminal-core`, confirm current version); SumUp publishes to their own
   Maven repo (add it to `settings.gradle.kts`, same commented-placeholder pattern already there
   for Imin); Flatpay — confirm distribution method with them.
4. **Stripe only — stand up a backend endpoint.** Stripe Terminal needs your backend to (a)
   create connection tokens and (b) create PaymentIntents server-side, both of which require
   your Stripe *secret* key and must never ship inside this app. This is exactly the kind of
   thing `data/repository`'s "swappable interface, real backend later" design was built for —
   add a `PaymentApi` alongside the existing repository interfaces once you have that backend.
5. **Implement the chosen provider class.** Replace its `TODO`-marked bodies following the shape
   documented in its class doc comment.
6. **Test on-device**: Payment screen → select Card → confirm the present/processing/approved
   states match your reader's real behavior, and that a decline shows the retry path
   (`state.errorMessage` + the method selector re-enables).

## Data captured on a successful card charge

`Transaction.cardBrand` / `cardLast4` / `terminalReference` are populated from the provider's
`CardChargeResult` and now show up on `TransactionDetailScreen` ("Visa •••• 4242" instead of
"CARD") and on printed receipts ("Paid via Visa •••• 4242"). If a real provider's SDK doesn't
give you a card brand/last4 (some do, some redact it), leave those fields `null` — the UI/receipt
fall back to the generic payment-method label.
