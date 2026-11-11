package com.ab.analyzer;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.*;
import java.util.regex.*;

public class ScreenCaptureService extends Service {
    public static final String ACTION_OCR_RESULT = "com.ab.analyzer.OCR_RESULT";
    public static final String EXTRA_OCR_TEXT = "ocr_text";

    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private HandlerThread thread;
    private Handler handler;
    private TextRecognizer recognizer;
    private boolean busy = false;
    private long lastFrame = 0;
    private int width, height, density;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        String channel = "ab_capture";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel nc = new NotificationChannel(channel, "AB Analyzer Scanner",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(nc);
        }
        Notification notification = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, channel).setContentTitle("AB Analyzer")
                    .setContentText("Visible screen OCR active")
                    .setSmallIcon(android.R.drawable.ic_menu_view).build()
                : new Notification.Builder(this).setContentTitle("AB Analyzer")
                    .setContentText("Visible screen OCR active")
                    .setSmallIcon(android.R.drawable.ic_menu_view).build();

        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else startForeground(1, notification);

        Intent data;
        if (Build.VERSION.SDK_INT >= 33) data = intent.getParcelableExtra("data", Intent.class);
        else data = intent.getParcelableExtra("data");
        if (data == null) { stopSelf(); return START_NOT_STICKY; }

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(intent.getIntExtra("resultCode", 0), data);
            startCapture();
        } catch (Exception e) {
            android.util.Log.e("ABAnalyzer", "Capture start failed", e);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void startCapture() {
        WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        density = dm.densityDpi;

        thread = new HandlerThread("AB-OCR");
        thread.start();
        handler = new Handler(thread.getLooper());
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(this::onImage, handler);

        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() {
                cleanup();
                stopSelf();
            }
        }, handler);

        display = projection.createVirtualDisplay("AB-Analyzer", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, handler);
    }

    private void onImage(ImageReader source) {
        long now = System.currentTimeMillis();
        if (busy || now - lastFrame < 1800) {
            Image drop = source.acquireLatestImage();
            if (drop != null) drop.close();
            return;
        }
        Image image = source.acquireLatestImage();
        if (image == null) return;
        Bitmap bitmap = null;
        try {
            Image.Plane plane = image.getPlanes()[0];
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap padded = Bitmap.createBitmap(width + rowPadding / pixelStride,
                    height, Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(plane.getBuffer());
            bitmap = Bitmap.createBitmap(padded, 0, 0, width, height);
            padded.recycle();
        } catch (Exception e) {
            android.util.Log.e("ABAnalyzer", "Frame conversion failed", e);
        } finally {
            image.close();
        }
        if (bitmap == null) return;

        busy = true;
        lastFrame = now;
        final Bitmap frame = bitmap;
        recognizer.process(InputImage.fromBitmap(frame, 0))
            .addOnSuccessListener(result -> {
                String cardText = scanCardRanks(result.getTextBlocks(), frame.getWidth(), frame.getHeight());
                StringBuilder screenText = new StringBuilder();
                for (Text.TextBlock block : result.getTextBlocks()) {
                    if (screenText.length() > 0) screenText.append('\n');
                    screenText.append(block.getText());
                }
                Intent broadcast = new Intent(ACTION_OCR_RESULT);
                broadcast.setPackage(getPackageName());
                broadcast.putExtra(EXTRA_OCR_TEXT, cardText + "\n\nSCREEN TEXT:\n" + screenText);
                sendBroadcast(broadcast);
            })
            .addOnFailureListener(e -> android.util.Log.e("ABAnalyzer", "OCR failed", e))
            .addOnCompleteListener(task -> {
                frame.recycle();
                busy = false;
            });
    }

    // Initial calibration uses the supplied portrait TriMela screenshot (1292 x 1536).
    // This reads rank-like text only; it does NOT identify suits or certify a hand.
    private String scanCardRanks(List<Text.TextBlock> blocks, int frameWidth, int frameHeight) {
        String[] seats = {"A", "B", "C"};
        float[][] zones = {{0.045f, 0.29f}, {0.31f, 0.55f}, {0.575f, 0.82f}};
        float top = 0.285f * frameHeight;
        float bottom = 0.405f * frameHeight;
        StringBuilder out = new StringBuilder("CARD RANK OCR (EXPERIMENTAL)\n");

        for (int i = 0; i < 3; i++) {
            List<String> ranks = new ArrayList<>();
            float left = zones[i][0] * frameWidth;
            float right = zones[i][1] * frameWidth;
            for (Text.TextBlock block : blocks) {
                Rect bounds = block.getBoundingBox();
                if (bounds == null) continue;
                float x = bounds.centerX();
                float y = bounds.centerY();
                if (x < left || x > right || y < top || y > bottom) continue;
                Matcher matcher = Pattern.compile("(?i)(10|[2-9]|A|K|Q|J)")
                        .matcher(block.getText().replace("1O", "10"));
                while (matcher.find()) {
                    String rank = matcher.group(1).toUpperCase(Locale.ROOT);
                    if (!ranks.contains(rank)) ranks.add(rank);
                }
            }
            out.append("Player ").append(seats[i]).append(": ");
            if (ranks.size() >= 3)
                out.append(ranks.get(0)).append(" ?  ").append(ranks.get(1))
                   .append(" ?  ").append(ranks.get(2)).append(" ? (suits unconfirmed)");
            else out.append(ranks.size()).append("/3 ranks detected ").append(ranks);
            out.append('\n');
        }
        out.append("Confirm every card manually before saving a round.");
        return out.toString();
    }

    private void cleanup() {
        if (display != null) { display.release(); display = null; }
        if (reader != null) {
            reader.setOnImageAvailableListener(null, null);
            reader.close();
            reader = null;
        }
        if (recognizer != null) { recognizer.close(); recognizer = null; }
        if (thread != null) { thread.quitSafely(); thread = null; handler = null; }
        projection = null;
    }

    @Override public void onDestroy() {
        MediaProjection old = projection;
        cleanup();
        if (old != null) old.stop();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
