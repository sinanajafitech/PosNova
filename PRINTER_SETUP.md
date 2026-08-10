# Imin printer integration status

The printer stack now has three real connection paths, all implemented (not stubs):

- **Bluetooth / USB** thermal printers — raw ESC/POS byte streams over a Bluetooth SPP socket or
  USB bulk transfer. No vendor SDK involved; works with any standard ESC/POS printer.
- **Imin D4 built-in printer** — the real Imin SDK, `com.github.iminsoftware:IminPrinterLibrary`
  (JitPack), wrapped in `printer/imin/IminBuiltInPrinter.kt`.

None of this has been exercised on physical hardware from this environment — it's real code
against real, verified APIs, not guesses, but "compiles against the real SDK" and "confirmed
working on a D4" are different claims. Test on-device before relying on it.

## Where the built-in printer SDK details came from

Imin's hosted docs (oss-sg.imin.sg) link out to PDF integration guides that weren't fetchable
here. Rather than guess method names from the docs page's prose, the actual source was pulled
from Imin's own public GitHub repo — **github.com/iminsoftware/IminPrinterLibrary** — specifically
`IminPrinterLibrary/src/main/java/com/imin/printer/PrinterHelper.java` and the AIDL interface
`INeoPrinterService`. That's real, verified API, not a summary of a summary.

**Two things are inferred rather than documented**, called out in `IminBuiltInPrinter.kt`'s class
doc too:
- Alignment ints (`printTextWithAli`, `printQrCodeWithAlign`, ...) are assumed to match
  `com.imin.printer.enums.Align`'s ordinal (DEFAULT=0, LEFT=1, CENTER=2, RIGHT=3) — inferred from
  the enum's name and usage, not from an explicit doc mapping.
- `getPrinterStatus()`'s return value has no status-code enum anywhere in the library source, so
  it isn't used — connection state is tracked only via `InitPrinterCallback` (bound/not bound),
  not fine-grained states like paper-out.

## What exists today

- `printer/PrinterService.kt` — the interface the rest of the app depends on
- `printer/mock/MockPrinterService.kt` — logs a formatted rendering of every print job; useful
  for testing the print *flow* (Order Success / Transaction Detail / End Shift / Profile
  diagnostics) without any hardware at all
- `printer/imin/IminPrinterService.kt` — dispatches to Bluetooth/USB (raw ESC/POS) or
  `IminBuiltInPrinter` (real Imin AIDL calls) based on which `DiscoveredPrinter` the cashier
  selected in Profile → Printer
- `printer/imin/IminBuiltInPrinter.kt` — the real Imin SDK wrapper described above
- `di/PrinterModule.kt` — currently always binds `IminPrinterService` (so Bluetooth/USB/built-in
  discovery works in every build, including debug — this was changed from the original
  debug-uses-mock split)

## Before shipping to a real D4

1. **Confirm the JitPack version.** `app/build.gradle.kts` pins
   `com.github.iminsoftware:IminPrinterLibrary:V2.0.0.19` — check
   github.com/iminsoftware/IminPrinterLibrary/tags for anything newer before release.
2. **Test the built-in printer end-to-end** on a real D4: Profile → Printer → refresh → select
   "Imin Built-in Printer" → Test Print, Open Cash Drawer, then a full order → payment → receipt
   print. Confirm text renders correctly (font size scaling, bold, alignment) and the QR code is
   legible — the size mapping in `IminBuiltInPrinter.printQrCode` is an unverified approximation.
3. **Confirm `com.imin.printerservice` is present** on your target D4's firmware — `bindService`
   will fail (and `connect()` will return a failure Result with a clear message) if that system
   service isn't installed/running.
4. If status codes matter to you (e.g. showing "out of paper" specifically instead of a generic
   error), you'll need to find Imin's actual status-code documentation (the PDF guides linked
   from oss-sg.imin.sg, not covered here) and extend `IminBuiltInPrinter` to interpret
   `getPrinterStatus()`'s return value.

## Card payment terminals

Unrelated to the printer — see `CARD_PAYMENT_SETUP.md` for Stripe Terminal / SumUp / Flatpay.

## Customer display (secondary screen)

No SDK needed — uses Android's own `DisplayManager`/`Presentation` APIs
(`customerdisplay/CustomerDisplayManager.kt`). Confirm on-device that it attaches to the D4's
second screen and the idle → cart → payment → thank-you states switch as a cashier walks through
an order.
