# AB Analyzer — Android project

AB Analyzer is an Android utility project for manually comparing three Teen Patti hands and requesting user-authorized screen capture for OCR experimentation.

## Current implemented features
- Manual three-player card entry and Teen Patti hand ranking.
- Duplicate-card validation across players.
- Android MediaProjection screen capture after the system permission prompt.
- On-device Google ML Kit text recognition on captured frames.
- Optional floating OCR text panel (requires Android “Display over other apps” permission).

## Important limitation
The current OCR pipeline extracts **screen text only**. It does not yet reliably identify the nine playing-card ranks and suits from StreamKar. Card detection needs calibrated card-region crops and rank/suit visual classification tested against real screenshots from the target device. Do not treat OCR text as a verified game result or use the app to make betting decisions.

## Build
- Android Studio with JDK 17
- Android Gradle Plugin 8.6.1
- Compile SDK 35; target SDK 34; min SDK 23

Open this folder in Android Studio and run the `app` configuration. A debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Screen capture
1. Install and open AB Analyzer.
2. Allow “Display over other apps” when prompted.
3. Tap Start Screen Capture and approve Android's screen-sharing prompt.
4. Stop scanning from the app when finished.

Screen capture is user initiated and can be stopped at any time. The app does not bypass StreamKar protections or access its private APIs.
