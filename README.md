# AB Analyzer — Android project

Android companion app for recording and comparing revealed Teen Patti cards for Players A, B and C.

## Included

- Java Android app (`com.ab.analyzer`)
- Manual three-player Teen Patti hand comparison (Trail, Pure Sequence, Sequence, Color, Pair, High Card)
- Duplicate-card validation and tie handling
- Local rolling history of the latest 30 manually analysed rounds
- Explicit Android MediaProjection permission flow
- Foreground screen-capture service and on-device ML Kit Latin text-recognition preview
- GitHub Actions workflow that builds a debug APK and uploads it as an Actions artifact

## Important current limitation

The screen scanner currently captures the screen and displays OCR text that ML Kit can read. It does **not yet reliably recognize card artwork, identify the A/B/C player regions, or automatically convert OCR output into card entries**. Those features require a separate card/seat detection implementation and testing against real screenshots. The app cannot see unrevealed cards, access StreamKar private servers, or guarantee/predict future winners. Recent-round patterns are descriptive only and may not predict an independent random game.

## Build the APK with GitHub Actions

1. Extract this ZIP on your phone or computer.
2. Open your GitHub repository.
3. Choose **Add file → Upload files**.
4. Upload the *contents* of the extracted `AB-Analyzer-Android-Project` folder, including the hidden `.github` folder. Do not upload only the ZIP file.
5. Commit the upload to the `main` branch.
6. Open the repository's **Actions** tab and open **Build AB Analyzer APK**.
7. When the workflow completes successfully, open its run and download the `AB-Analyzer-APK` artifact. It contains `app-debug.apk`.

The workflow can also be started manually from Actions → Build AB Analyzer APK → Run workflow.

## Local Android Studio build

Open the extracted project folder in Android Studio. Use JDK 17 and Android SDK Platform 35. Allow Gradle to sync, then run **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## App identifiers

- Package / application ID: `com.ab.analyzer`
- Android Gradle Plugin: `8.6.1`
- Gradle: `8.7` (provided by the GitHub Actions workflow)
- Compile / target SDK: `35`
- Minimum SDK: `23` (Android 6.0)
- Java: `17`
- ML Kit Text Recognition: `16.0.1`

## Screen-capture privacy

Screen capture begins only after the Android system permission prompt is accepted. Android shows a foreground-service notification while scanning. Stop scanning from the app when finished. OCR is processed on-device using ML Kit; no screen images are uploaded by this project.
