
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

import java.nio.ByteBuffer;

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
            Log.e(TAG, "Screen capture permission data missing");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            startCapture(resultCode, data);
        } catch (Exception e) {
            Log.e(TAG, "Capture start failed", e);
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
            throw new IllegalStateException("MediaProjection unavailable");
        }

        WindowManager windowManager =
            (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        handlerThread = new HandlerThread("ABScreenCapture");
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
            reader -> processLatestImage(reader),
            handler
        );

        projection.registerCallback(
            new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.d(TAG, "Screen capture stopped");
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

        Log.d(TAG, "Screen capture started successfully");
    }

    private void processLatestImage(ImageReader reader) {

        if (processing) return;

        Image image = null;
        Bitmap bitmap = null;

        try {
            image = reader.acquireLatestImage();

            if (image == null) return;

            Image.Plane[] planes = image.getPlanes();

            ByteBuffer buffer = planes[0].getBuffer();

            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();

            int rowPadding =
                rowStride - pixelStride * screenWidth;

            int paddedWidth =
                screenWidth + rowPadding / pixelStride;

            Bitmap paddedBitmap = Bitmap.createBitmap(
                paddedWidth,
                screenHeight,
                Bitmap.Config.ARGB_8888
            );

            paddedBitmap.copyPixelsFromBuffer(buffer);

            bitmap = Bitmap.createBitmap(
                paddedBitmap,
                0,
                0,
                screenWidth,
                screenHeight
            );

            paddedBitmap.recycle();

        } catch (Exception e) {
            Log.e(TAG, "Image conversion failed", e);
        } finally {
            if (image != null) {
                image.close();
            }
        }

        if (bitmap == null) return;

        processing = true;

        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);

        Bitmap finalBitmap = bitmap;

        recognizer.process(inputImage)
            .addOnSuccessListener(visionText -> {
                Log.d(TAG, "OCR RESULT:\n" + visionText.getText());

                for (Text.TextBlock block : visionText.getTextBlocks()) {
                    Log.d(TAG, "Detected: " + block.getText());
                }
            })
            .addOnFailureListener(e ->
                Log.e(TAG, "OCR failed", e)
            )
            .addOnCompleteListener(task -> {
                finalBitmap.recycle();
                processing = false;
            });
    }

    private void stopCaptureResources() {

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
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

        if (projection != null) {
            projection = null;
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                new NotificationChannel(
                    CHANNEL_ID,
                    "AB Analyzer Screen Capture",
                    NotificationManager.IMPORTANCE_LOW
                );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {

        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("AB Analyzer")
                .setContentText("Screen capture and OCR active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
        }

        return new Notification.Builder(this)
            .setContentTitle("AB Analyzer")
            .setContentText("Screen capture active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build();
    }

    @Override
    public void onDestroy() {

        stopCaptureResources();

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
