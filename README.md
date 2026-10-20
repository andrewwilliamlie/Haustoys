# Haustoys POS

A simple point-of-sale app for **PAX Android EDC terminals** (A920, A910, A930, ...),
printing receipts on the terminal's built-in thermal printer.

## Features

- **Sell**: tap products to add them to the cart, adjust quantities, then charge.
- **Checkout**: Cash (with quick amounts and change), Card (EDC) or QRIS.
  Card/QRIS payments are taken in the bank's EDC app as usual; this app records the sale.
- **Receipts**: printed automatically after each sale (can be turned off), reprint any time.
- **Products**: add, edit and delete products with prices in Rupiah.
- **History**: browse sales by day, see the daily summary per payment method, print a daily report.
- **Settings**: store name, address and receipt footer; printer status and test print.

All data is stored on the terminal (SQLite). No internet connection is needed.

## Building

Requirements: Android Studio (Ladybug or newer) or JDK 17.

1. **Add the PAX SDK.** Copy `NeptuneLiteApi_V4.05.00_20240305.jar` from PAX's
   NeptuneLiteApiDemo (`app/libs/`) into this project's `app/libs/`.
   It is git-ignored because it is PAX proprietary software.
   Without it the app still builds, but cannot print.
2. Build: open the project in Android Studio and Run, or
   `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
3. Install on the terminal: `adb install -r app-release.apk`.

> **Note:** PAX terminals in *production* mode (most bank-issued EDCs) only install
> apps signed by PAX / distributed through PAXSTORE. Use a terminal in debug/dev mode,
> or ask PAX or your acquiring bank to sign and deploy the APK.

GitHub Actions builds a debug APK and runs the unit tests on every push
(artifact `haustoys-pos-debug-no-printer`). That APK has no printer driver
because the PAX jar is not in the repository.

## Project layout

```
app/src/main/java/com/haustoys/pos/
  core/   Money, Cart, ReceiptFormatter, models (plain Kotlin, unit-tested)
  data/   SQLite database and store settings
  print/  ReceiptPrinter interface
  ui/     Jetpack Compose screens
app/src/pax/    PAX NeptuneLite printer (used when the jar is in app/libs)
app/src/nopax/  No-printer fallback (used when it is not)
```

Receipts are formatted for 58mm paper (32 characters per line).
