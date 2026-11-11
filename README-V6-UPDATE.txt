AB ANALYZER V6 SOURCE UPDATE

This is a source patch for the existing repository:
https://github.com/aark365-commits/AB-Analyzer

WHAT THIS PATCH FIXES
- Replaces the old V3 MainActivity inside the actual app source folder.
- Installs the V5-style scanner UI into the real app source folder and labels it V6.
- Adds the missing HandEngine.java and HistoryStore.java under app/src/main/java.
- Adds the draggable/minimizable floating OCR panel.
- Changes versionCode to 6 and versionName to 6.0 so Android recognizes a newer build.
- Adjusts the experimental OCR rank scan zones to the supplied portrait screenshot coordinates.

IMPORTANT LIMITATIONS
- Screen OCR currently detects text/rank candidates only. It does not reliably recognize suits.
- OCR output must be checked manually. Save only confirmed card entries.
- The included scanner is experimental and calibrated to the supplied portrait TriMela screenshot.
- A successful GitHub Actions build proves compilation, not that OCR is accurate on every device.
- Do not treat saved winner frequency as a prediction of the next random round.

HOW TO APPLY
1. Download and extract this ZIP.
2. In your GitHub repository, open the matching paths and replace the files with the files from this ZIP.
3. Keep the package name com.ab.analyzer unchanged.
4. Commit the changes to the main branch.
5. Open the Actions tab and wait for "Build AB-Analyzer APK" to finish successfully.
6. Download the AB-Analyzer-APK artifact from that successful run.
7. Install the new APK.

INSTALL NOTE
If Android says "App not installed" or "App not installed as package conflicts", it may be a signing-key mismatch between APK builds. Back up anything important first; uninstalling the old app removes its local history. Then install the new APK.

This ZIP is a source patch, not an APK. The GitHub Actions workflow builds the APK after the patch is committed.
