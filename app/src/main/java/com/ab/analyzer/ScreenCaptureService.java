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

public class ScreenCaptureService extends Service {
    public static final String ACTION_OCR_RESULT="com.ab.analyzer.OCR_RESULT",EXTRA_OCR_TEXT="ocr_text";
    private MediaProjection projection;private VirtualDisplay display;private ImageReader reader;
    private HandlerThread thread;private Handler handler;private TextRecognizer recognizer;
    private boolean busy=false;private long last=0;private int w,h,density;
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        String channel="ab_capture";
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(channel,"AB Analyzer Scanner",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(ch);}
        Notification n=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel).setContentTitle("AB Analyzer").setContentText("Screen scanning active").setSmallIcon(android.R.drawable.ic_menu_view).build():new Notification.Builder(this).setContentTitle("AB Analyzer").setContentText("Screen scanning active").setSmallIcon(android.R.drawable.ic_menu_view).build();
        if(Build.VERSION.SDK_INT>=29)startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);else startForeground(1,n);
        Intent data;if(Build.VERSION.SDK_INT>=33)data=intent.getParcelableExtra("data",Intent.class);else data=intent.getParcelableExtra("data");
        if(data==null){stopSelf();return START_NOT_STICKY;}
        try{MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);projection=m.getMediaProjection(intent.getIntExtra("resultCode",0),data);startCapture();}
        catch(Exception e){android.util.Log.e("ABAnalyzer","Capture start failed",e);stopSelf();}
        return START_NOT_STICKY;
    }
    private void startCapture(){
        WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE);DisplayMetrics dm=new DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(dm);w=dm.widthPixels;h=dm.heightPixels;density=dm.densityDpi;
        thread=new HandlerThread("AB-OCR");thread.start();handler=new Handler(thread.getLooper());recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);reader.setOnImageAvailableListener(this::onImage,handler);
        projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){cleanup();stopSelf();}},handler);
        display=projection.createVirtualDisplay("AB-Analyzer",w,h,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
    }
    private void onImage(ImageReader src){
        long now=System.currentTimeMillis();if(busy||now-last<1500){Image d=src.acquireLatestImage();if(d!=null)d.close();return;}
        Image im=src.acquireLatestImage();if(im==null)return;Bitmap bmp=null;
        try{Image.Plane p=im.getPlanes()[0];int stride=p.getPixelStride(),row=p.getRowStride(),pad=row-stride*w;Bitmap big=Bitmap.createBitmap(w+pad/stride,h,Bitmap.Config.ARGB_8888);big.copyPixelsFromBuffer(p.getBuffer());bmp=Bitmap.createBitmap(big,0,0,w,h);big.recycle();}
        catch(Exception e){android.util.Log.e("ABAnalyzer","Frame error",e);}finally{im.close();}
        if(bmp==null)return;busy=true;last=now;final Bitmap frame=bmp;
        recognizer.process(InputImage.fromBitmap(frame,0)).addOnSuccessListener(t->{StringBuilder s=new StringBuilder();for(Text.TextBlock b:t.getTextBlocks()){if(s.length()>0)s.append("\n");s.append(b.getText());}
            Intent out=new Intent(ACTION_OCR_RESULT);out.setPackage(getPackageName());out.putExtra(EXTRA_OCR_TEXT,s.toString());sendBroadcast(out);
        }).addOnFailureListener(e->android.util.Log.e("ABAnalyzer","OCR error",e)).addOnCompleteListener(t->{frame.recycle();busy=false;});
    }
    private void cleanup(){if(display!=null){display.release();display=null;}if(reader!=null){reader.setOnImageAvailableListener(null,null);reader.close();reader=null;}if(recognizer!=null){recognizer.close();recognizer=null;}if(thread!=null){thread.quitSafely();thread=null;handler=null;}projection=null;}
    @Override public void onDestroy(){MediaProjection p=projection;cleanup();if(p!=null)p.stop();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
