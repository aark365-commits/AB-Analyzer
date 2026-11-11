package com.ab.analyzer;

import android.app.Service;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class FloatingPanelService extends Service {
    private WindowManager windowManager;
    private LinearLayout panel, body;
    private TextView details, header;
    private BroadcastReceiver receiver;
    private boolean expanded = true;

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(7), dp(10), dp(7));
        panel.setBackgroundColor(0xEE101522);

        header = new TextView(this);
        header.setText("AB ANALYZER • LIVE  ▾");
        header.setTextColor(0xFFFFC58A);
        header.setTextSize(13);
        header.setPadding(dp(4), dp(5), dp(4), dp(5));
        panel.addView(header);

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        panel.addView(body);

        details = new TextView(this);
        details.setText("Waiting for TriMela screen OCR...");
        details.setTextColor(Color.WHITE);
        details.setTextSize(11);
        details.setMaxLines(10);
        body.addView(details);

        TextView minimize = new TextView(this);
        minimize.setText("MINIMIZE");
        minimize.setTextColor(0xFFFFC58A);
        minimize.setPadding(dp(4), dp(7), dp(4), dp(3));
        body.addView(minimize);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(245), WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = dp(8);
        params.y = dp(70);
        windowManager.addView(panel, params);

        header.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float touchX, touchY;
            boolean moved;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(event.getRawX() - touchX);
                        int dy = (int)(event.getRawY() - touchY);
                        if (Math.abs(dx) > 4 || Math.abs(dy) > 4) moved = true;
                        params.x = startX + dx;
                        params.y = startY + dy;
                        windowManager.updateViewLayout(panel, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggle();
                        return true;
                }
                return true;
            }
        });
        minimize.setOnClickListener(v -> toggle());

        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String text = intent.getStringExtra(ScreenCaptureService.EXTRA_OCR_TEXT);
                details.setText(text == null ? "No readable text detected." : text);
            }
        };
        IntentFilter filter = new IntentFilter(ScreenCaptureService.ACTION_OCR_RESULT);
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, filter);
    }

    private void toggle() {
        expanded = !expanded;
        body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        header.setText(expanded ? "AB ANALYZER • LIVE  ▴" : "AB ANALYZER • LIVE  ▾");
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        if (receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        }
        if (windowManager != null && panel != null) {
            try { windowManager.removeView(panel); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
