# PAX SDK goes here

Copy `NeptuneLiteApi_V4.05.00_20240305.jar` (from PAX's NeptuneLiteApiDemo,
`app/libs/`) into this folder before building.

- With the jar here, the app is built with the PAX built-in printer driver.
- Without it, the app still builds and runs, but receipts are only shown on
  screen (Settings shows "PAX SDK not bundled").

The jar is git-ignored on purpose: it is PAX proprietary software and this
repository is public.
