# POSNova — Kotlin POS App for Imin D4/D4-Mars

A native Android app (Kotlin + Jetpack Compose) implementing the POSNova POS Dashboard UI Kit,
targeting an **Imin D4/D4-Mars** POS terminal with its built-in thermal printer and secondary
customer-facing display.

## Requirements

- Android Studio (Ladybug or newer) or a command-line Android SDK
- JDK 17
- Android SDK: `platform-tools`, `platforms;android-35`, `build-tools;35.0.0`

## Building

```bash
./gradlew assembleDebug     # debug APK, uses the mock printer (no hardware needed)
./gradlew assembleRelease   # release APK, uses the real Imin printer — see PRINTER_SETUP.md first
```

The debug build has been verified to compile, run Hilt/KSP annotation processing, and package
successfully in this environment. It has **not** been verified on real Imin D4 hardware or with
the official Imin SDK linked — see [PRINTER_SETUP.md](PRINTER_SETUP.md) for what's left before
that's true.

## Architecture

- **UI**: Jetpack Compose + Material3, MVVM (`ViewModel` + `StateFlow` per screen)
- **DI**: Hilt (`di/RepositoryModule.kt` binds mock repositories, `di/PrinterModule.kt` picks the
  printer implementation by build type; everything else uses plain `@Inject` constructors)
- **Navigation**: Navigation-Compose, adaptive shell (`core/navigation/MainScaffold.kt`) that
  swaps a `NavigationRail` (POS-terminal/tablet width) for a `NavigationBar` (phone width) using
  `WindowSizeClass` — one screen implementation per flow rather than separate desktop/mobile
  composables
- **Data**: repository interfaces (`data/repository/*.kt`) backed today by in-memory mock
  implementations (`data/repository/mock/*.kt`) seeded with sample data. Swap in a Retrofit-backed
  implementation of the same interface when a real backend is ready — no ViewModel/UI changes
  needed
- **Printer**: `printer/PrinterService` interface; `MockPrinterService` (debug builds, logs a
  formatted receipt) and `IminPrinterService` (release builds, needs the real SDK — see
  PRINTER_SETUP.md)
- **Customer display**: `customerdisplay/CustomerDisplayManager` detects the D4's secondary
  screen via `DisplayManager` and shows a `Presentation` there, driven by the same order/cart
  state the cashier screens already own

## Design source

Screens and design tokens (colors, typography, spacing, radii) were pulled from the POSNova
Figma file via the Figma REST API (file key `7BKd90Y61sRQxvHiPnfAhr`) — see
`core/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt` for the extracted values and notes on
where the source file had gaps or errors (e.g. a malformed Warning/50 color token).

## What's implemented

Splash → Login/PIN → Register/Create PIN → Forgot/Reset Password → Dashboard → Orders (list, new
order with menu + cart, table selection, detail, payment, success with receipt printing) →
Transactions (list, detail with reprint/void) → Report (revenue, top products, 7-day chart) →
Inventory (list, add/edit, delete) → Profile (view/edit, printer diagnostics, end shift with
shift-report printing).

## Known gaps / next steps

- **Imin SDK not linked** — printer calls fail fast with a clear error until the official AAR/
  Maven dependency is added (PRINTER_SETUP.md).
- **No real backend** — all data is in-memory and resets on app restart. Repository interfaces
  are ready for a Retrofit implementation.
- **Dark mode** is a derived Material3 dark scheme from the same brand colors, not a 1:1
  extraction of the Figma file's Dark Mode section (which wasn't pulled node-by-node).
- **Not tested on real Imin hardware** or in an emulator UI walkthrough — only verified via
  `gradlew assembleDebug` succeeding.
