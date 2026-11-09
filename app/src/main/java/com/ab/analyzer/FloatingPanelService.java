package com.ab.analyzer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingPanelService extends Service {

    private WindowManager windowManager;
    private LinearLayout panel;
    private TextView status;
    private TextView content;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 23 &&
                !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(18, 14, 18, 14);
        panel.setBackgroundColor(0xEE101522);

        TextView title = new TextView(this);
        title.setText("AB ANALYZER • LIVE");
        title.setTextColor(0xFFFFC58A);
        title.setTextSize(16);
        panel.addView(title);

        status = new TextView(this);
        status.setText("Screen scanning active");
        status.setTextColor(Color.WHITE);
        status.setTextSize(13);
        panel.addView(status);

        content = new TextView(this);
        content.setText(
            "Waiting for screen OCR...\n" +
            "Player A: Not detected\n" +
            "Player B: Not detected\n" +
            "Player C: Not detected\n\n" +
            "Card recognition is not enabled yet."
        );
        content.setTextColor(Color.WHITE);
        content.setTextSize(13);
        content.setMaxLines(10);
        panel.addView(content);

        TextView hide = new TextView(this);
        hide.setText("HIDE PANEL  X");
        hide.setTextColor(0xFFFFC58A);
        hide.setPadding(0, 12, 0, 0);
        hide.setOnClickListener(v -> stopSelf());
        panel.addView(hide);

        int type;

        if (Build.VERSION.SDK_INT >= 26) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP;
        params.y = 80;

        windowManager.addView(panel, params);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String text = intent.getStringExtra(
                    ScreenCaptureService.EXTRA_OCR_TEXT
                );

                if (text == null || text.trim().isEmpty()) {
                    content.setText("No readable text detected.");
                } else {
                    content.setText(text);
                }

                status.setText("LIVE OCR • Updated");
            }
        };

        IntentFilter filter = new IntentFilter(
            ScreenCaptureService.ACTION_OCR_RESULT
        );

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(receiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
        }

        if (windowManager != null && panel != null) {
            try {
                windowManager.removeView(panel);
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
          }
