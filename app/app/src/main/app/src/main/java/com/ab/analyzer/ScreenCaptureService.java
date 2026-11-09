package com.ab.analyzer;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

public class ScreenCaptureService extends Service {

    private MediaProjection projection;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotification();

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra("resultCode", 0);

        Intent data;
        if (Build.VERSION.SDK_INT >= 33) {
            data = intent.getParcelableExtra(
                "data", Intent.class);
        } else {
            data = intent.getParcelableExtra("data");
        }

        if (data == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(1, createNotification());

        MediaProjectionManager manager =
            (MediaProjectionManager) getSystemService(
                Context.MEDIA_PROJECTION_SERVICE);

        projection = manager.getMediaProjection(
            resultCode, data);

        if (projection == null) {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                new NotificationChannel(
                    "ab_capture",
                    "AB Analyzer Screen Capture",
                    NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, "ab_capture")
                .setContentTitle("AB Analyzer")
                .setContentText("Screen capture session active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
        }

        return new Notification.Builder(this)
            .setContentTitle("AB Analyzer")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build();
    }

    @Override
    public void onDestroy() {
        if (projection != null) {
            projection.stop();
            projection = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
