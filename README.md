# AB Analyzer V5 (Beta)

Android companion app for visible TriMela screen OCR, manual Teen Patti hand comparison, and a local rolling history.

## Included
- Compact 230dp draggable floating OCR panel with minimize/expand control.
- User-authorized MediaProjection screen capture.
- On-device Google ML Kit OCR.
- Experimental normalized A/B/C card-region rank OCR based on the supplied portrait screenshot.
- Manual A/B/C hand comparison and local history (up to 30 rounds).
- Recent A/B/C winner frequency summary from saved rounds.

## Important limitations
- Automatic card reading is experimental and calibrated to the supplied TriMela screenshot layout. Different device resolution, orientation, UI scaling, animations, banners, or overlays can shift the card regions.
- This version attempts to read ranks from OCR text. It does not yet reliably identify all four suits, so it does not automatically confirm complete card hands or save unverified OCR outcomes.
- History is saved from manually entered and confirmed rounds. The trend is descriptive frequency only; it cannot reliably predict the next independent random result. It never guarantees whether A, B, or C will win.
- It only analyzes information visible on screen. It cannot see hidden cards or access StreamKar private systems.

## Build
Open the project in Android Studio with JDK 17 and Android SDK 35. Gradle plugin 8.6.1. The GitHub Actions workflow in `.github/workflows/build.yml` builds a debug APK.

## Permissions
Screen capture starts only after the Android system permission prompt is accepted. Overlay permission is used only for the small floating panel. OCR runs on-device.
