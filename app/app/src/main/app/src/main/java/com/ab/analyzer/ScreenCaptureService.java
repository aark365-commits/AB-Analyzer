
package com.ab.analyzer;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.content.pm.ServiceInfo;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class ScreenCaptureService extends Service {

    private static final String TAG = "ABAnalyzer";
    private static final String CHANNEL_ID = "ab_capture";

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private HandlerThread handlerThread;
    private Handler handler;
    private TextRecognizer recognizer;

    private boolean processing = false;
    private long lastScanTime = 0;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotificationChannel();

        Notification notification = createNotification();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(1, notification);
        }

        int resultCode = intent.getIntExtra("resultCode", 0);

        Intent data;
        if (Build.VERSION.SDK_INT >= 33) {
            data = intent.getParcelableExtra("data", Intent.class);
        } else {
            data = intent.getParcelableExtra("data");
        }

        if (data == null) {
            Log.e(TAG, "Permission data missing");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            startCapture(resultCode, data);
        } catch (Exception e) {
            Log.e(TAG, "Capture failed", e);
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startCapture(int resultCode, Intent data) {

        MediaProjectionManager manager =
            (MediaProjectionManager) getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            );

        projection = manager.getMediaProjection(resultCode, data);

        if (projection == null) {
            throw new IllegalStateException("Projection unavailable");
        }

        WindowManager wm =
            (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        handlerThread = new HandlerThread("ABScanner");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        recognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        );

        imageReader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        );

        imageReader.setOnImageAvailableListener(
            this::processImage,
            handler
        );

        projection.registerCallback(
            new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.d(TAG, "Projection stopped");
                    stopCaptureResources();
                    stopSelf();
                }
            },
            handler
        );

        virtualDisplay = projection.createVirtualDisplay(
            "ABAnalyzerDisplay",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null,
            handler
        );

        Log.d(TAG, "FULL SCREEN SCANNER STARTED");
    }

    private void processImage(ImageReader reader) {

        long now = System.currentTimeMillis();

        if (processing || now - lastScanTime < 1000) {
            Image skipped = reader.acquireLatestImage();
            if (skipped != null) skipped.close();
            return;
        }

        Image image = null;
        Bitmap bitmap = null;

        try {
            image = reader.acquireLatestImage();

            if (image == null) return;

            Image.Plane plane = image.getPlanes()[0];

            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();

            int rowPadding =
                rowStride - pixelStride * screenWidth;

            int paddedWidth =
                screenWidth + rowPadding / pixelStride;

            Bitmap padded = Bitmap.createBitmap(
                paddedWidth,
                screenHeight,
                Bitmap.Config.ARGB_8888
            );

            padded.copyPixelsFromBuffer(plane.getBuffer());

            bitmap = Bitmap.createBitmap(
                padded, 0, 0,
                screenWidth, screenHeight
            );

            padded.recycle();

        } catch (Exception e) {
            Log.e(TAG, "Bitmap conversion error", e);
        } finally {
            if (image != null) image.close();
        }

        if (bitmap == null) return;

        processing = true;
        lastScanTime = now;

        final Bitmap scanBitmap = bitmap;

        try {
            InputImage input = InputImage.fromBitmap(scanBitmap, 0);

            recognizer.process(input)
                .addOnSuccessListener(this::handleOCR)
                .addOnFailureListener(
                    e -> Log.e(TAG, "OCR failed", e)
                )
                .addOnCompleteListener(task -> {
                    scanBitmap.recycle();
                    processing = false;
                });

        } catch (Exception e) {
            scanBitmap.recycle();
            processing = false;
            Log.e(TAG, "OCR start error", e);
        }
    }

    private void handleOCR(Text visionText) {

        StringBuilder output = new StringBuilder();

        for (Text.TextBlock block : visionText.getTextBlocks()) {

            android.graphics.Rect box = block.getBoundingBox();

            if (box == null) continue;

            output.append(block.getText())
                .append(" | X:")
                .append(box.left)
                .append(" Y:")
                .append(box.top)
                .append(" W:")
                .append(box.width())
                .append(" H:")
                .append(box.height())
                .append("\n");
        }

        Log.d(TAG, "SCREEN OCR:\n" + output);
    }

    private void stopCaptureResources() {

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }

        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
            handler = null;
        }

        projection = null;
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                new NotificationChannel(
                    CHANNEL_ID,
                    "AB Analyzer Scanner",
                    NotificationManager.IMPORTANCE_LOW
                );

            NotificationManager nm =
                getSystemService(NotificationManager.class);

            nm.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {

        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AB Analyzer")
                .setContentText("Full screen scanning active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
        }

        return new Notification.Builder(this)
            .setContentTitle("AB Analyzer")
            .setContentText("Screen scanning active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build();
    }

    @Override
    public void onDestroy() {

        MediaProjection oldProjection = projection;

        stopCaptureResources();

        if (oldProjection != null) {
            oldProjection.stop();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
