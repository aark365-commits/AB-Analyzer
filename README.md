# AB Analyzer V7 — Android source project

## Included
- Dark dashboard UI for Android
- Manual A/B/C three-card entry and Teen Patti hand comparison
- Local confirmed-round history (up to 500), selected 15/20/30/100-round descriptive windows
- Winner counts, streak summaries, latest-round list and CSV text export/share
- Editable payout scenario calculator (multiplier and convention are configurable)
- User-approved Android MediaProjection capture, on-device ML Kit text OCR and movable overlay
- Local processing; no hidden game data or server access

## Important implementation status
The screen-capture service currently performs general text OCR. It does **not** yet reliably classify card ranks/suits, isolate A/B/C card regions, detect the official winner, or parse every bet amount. OCR output is displayed for human checking; manual confirmation is required before a round is saved. Those vision modules require representative StreamKar screenshots and device testing.

The card-ranking engine implements common Teen Patti ordering and should be checked against the exact rules used in the target game before relying on tie-break outcomes.

The history and payout modules are mathematical/record-keeping tools, not betting advice. Historical frequencies are not predictions and cannot reveal unrevealed cards. The payout calculator requires the user to enter the actual observed multiplier and payout convention.

## Build
Open this folder in Android Studio and build `app` > `assembleDebug`. Or use Gradle 8.7 and JDK 17:
`gradle assembleDebug`

Debug APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## First run
1. Install the debug APK.
2. Open AB Analyzer.
3. Tap START SCAN and approve Android's screen-capture prompt.
4. Approve overlay permission if you want the floating panel.
5. Open StreamKar TriMela. Pause/stop scanning from the app when finished.
6. Enter/verify cards and visible bets manually before saving a round.

## Privacy
Frames are processed on-device by ML Kit. The app does not upload screen images. Android screen-capture and overlay permissions are requested only for their respective features.
