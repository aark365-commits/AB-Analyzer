
package com.ab.analyzer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.provider.Settings;
import android.net.Uri;

public class MainActivity extends Activity {

    private static final int CAPTURE_REQUEST = 1001;

    private MediaProjectionManager projectionManager;
    private LinearLayout root;
    private EditText playerA, playerB, playerC;
    private TextView result, status, historyText;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(10, 17, 28));

        scroll.addView(root);

        TextView title = label("AB ANALYZER V6", 28, 0xFFFFC58A);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        root.addView(label(
                "TRIMELA • SCREEN OCR • TEEN PATTI HAND ANALYSIS",
                12, Color.LTGRAY));

        Button start = button("START SCREEN SCAN");
        Button stop = button("STOP SCREEN SCAN");

        root.addView(start);
        root.addView(stop);

        status = label(
                "Scanner stopped. Start scan, then open StreamKar TriMela.",
                14, Color.WHITE);

        root.addView(status);

        root.addView(label(
                "MANUAL CARD ENTRY / CONFIRMATION",
                18, 0xFFFFC58A));

        root.addView(label(
                "Format: AS KH 2D | S=Spades H=Hearts D=Diamonds C=Clubs",
                12, Color.LTGRAY));

        playerA = entry("Player A cards e.g. AS KH 2D");
        playerB = entry("Player B cards e.g. 5S 5H QD");
        playerC = entry("Player C cards e.g. 6C 8H JD");

        Button analyze = button("ANALYZE ALL 3 PLAYERS");
        Button save = button("SAVE CONFIRMED ROUND");

        root.addView(analyze);
        root.addView(save);

        result = label(
                "Analysis result will appear here.",
                15, Color.WHITE);

        root.addView(result);

        root.addView(label(
                "ROUND HISTORY & WIN STATS (LAST 100)",
                18, 0xFFFFC58A));

        Button history = button(
                "VIEW 100-ROUND HISTORY & STATS");

        root.addView(history);

        historyText = label("", 13, Color.LTGRAY);
        root.addView(historyText);

        start.setOnClickListener(v -> requestCapture());
        stop.setOnClickListener(v -> stopScanning());

        analyze.setOnClickListener(v -> analyzePlayers(false));
        save.setOnClickListener(v -> analyzePlayers(true));

        history.setOnClickListener(v ->
                historyText.setText(HistoryStore.summary(this)));

        setContentView(scroll);
    }

    private int dp(int n) {
        return (int) (
                n * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);

        view.setPadding(dp(4), dp(9), dp(4), dp(9));

        return view;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private EditText entry(String hint) {
        EditText e = new EditText(this);

        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(0xFF888888);
        e.setTextSize(16);

        root.addView(e);

        return e;
    }

    private void analyzePlayers(boolean saveRound) {

        String a = playerA.getText().toString().trim();
        String b = playerB.getText().toString().trim();
        String c = playerC.getText().toString().trim();

        String output = HandEngine.describe(a, b, c);

        result.setText(output);

        if (saveRound) {

            String winner = HandEngine.winner(a, b, c);

            if (winner.startsWith("ERROR")) {
                status.setText(winner);
                return;
            }

            HistoryStore.add(
                    this,
                    "Manual/Confirmed",
                    winner,
                    a,
                    b,
                    c
            );

            status.setText(
                    "Confirmed round saved. Winner: " + winner
            );

            historyText.setText(
                    HistoryStore.summary(this)
            );
        }
    }

    private void requestCapture() {

        if (Build.VERSION.SDK_INT >= 23
                && !Settings.canDrawOverlays(this)) {

            Intent settings = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );

            startActivity(settings);

            status.setText(
                    "Allow Display over other apps, return, then tap START SCREEN SCAN again."
            );

            return;
        }

        projectionManager = (MediaProjectionManager)
                getSystemService(MEDIA_PROJECTION_SERVICE);

        if (projectionManager == null) {
            status.setText(
                    "Screen capture is not available on this device."
            );
            return;
        }

        startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                CAPTURE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != CAPTURE_REQUEST) return;

        if (resultCode != RESULT_OK || data == null) {
            status.setText(
                    "Screen capture permission was not granted."
            );
            return;
        }

        Intent service = new Intent(
                this,
                ScreenCaptureService.class
        );

        service.putExtra("resultCode", resultCode);
        service.putExtra("data", data);

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }

        startService(
                new Intent(this, FloatingPanelService.class)
        );

        status.setText(
                "Screen capture active. Open StreamKar TriMela and keep the visible cards unobstructed."
        );
    }

    private void stopScanning() {

        stopService(
                new Intent(this, ScreenCaptureService.class)
        );

        stopService(
                new Intent(this, FloatingPanelService.class)
        );

        status.setText("Screen scanning stopped.");
    }
}
