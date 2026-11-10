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
 public static final String ACTION_OCR_RESULT="com.ab.analyzer.OCR_RESULT", EXTRA_OCR_TEXT="ocr_text";
 private MediaProjection projection;private VirtualDisplay display;private ImageReader reader;private HandlerThread thread;private Handler handler;private TextRecognizer recognizer;
 private boolean busy=false;private long last=0;private int w,h,density;private String lastSignature="";private long lastSave=0;
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null){stopSelf();return START_NOT_STICKY;}String ch="ab_capture";
  if(Build.VERSION.SDK_INT>=26){NotificationChannel nc=new NotificationChannel(ch,"AB Analyzer Scanner",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(nc);}
  Notification n=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,ch).setContentTitle("AB Analyzer").setContentText("Visible screen scanning active").setSmallIcon(android.R.drawable.ic_menu_view).build():new Notification.Builder(this).setContentTitle("AB Analyzer").setContentText("Visible screen scanning active").setSmallIcon(android.R.drawable.ic_menu_view).build();
  if(Build.VERSION.SDK_INT>=29)startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);else startForeground(1,n);
  Intent data=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra("data",Intent.class):intent.getParcelableExtra("data");if(data==null){stopSelf();return START_NOT_STICKY;}
  try{MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);projection=m.getMediaProjection(intent.getIntExtra("resultCode",0),data);startCapture();}catch(Exception e){stopSelf();}return START_NOT_STICKY;
 }
 private void startCapture(){
  WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE);DisplayMetrics dm=new DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(dm);w=dm.widthPixels;h=dm.heightPixels;density=dm.densityDpi;
  thread=new HandlerThread("AB-OCR");thread.start();handler=new Handler(thread.getLooper());recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);reader.setOnImageAvailableListener(this::onImage,handler);
  projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){cleanup();stopSelf();}},handler);
  display=projection.createVirtualDisplay("AB-Analyzer",w,h,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
 }
 private void onImage(ImageReader src){
  long now=System.currentTimeMillis();if(busy||now-last<1800){Image d=src.acquireLatestImage();if(d!=null)d.close();return;}
  Image im=src.acquireLatestImage();if(im==null)return;Bitmap bmp=null;
  try{Image.Plane p=im.getPlanes()[0];int stride=p.getPixelStride(),row=p.getRowStride(),pad=row-stride*w;Bitmap big=Bitmap.createBitmap(w+pad/stride,h,Bitmap.Config.ARGB_8888);big.copyPixelsFromBuffer(p.getBuffer());bmp=Bitmap.createBitmap(big,0,0,w,h);big.recycle();}catch(Exception ignored){}finally{im.close();}
  if(bmp==null)return;busy=true;last=now;final Bitmap frame=bmp;
  recognizer.process(InputImage.fromBitmap(frame,0)).addOnSuccessListener(t->{
   StringBuilder full=new StringBuilder();for(Text.TextBlock b:t.getTextBlocks()){if(full.length()>0)full.append("\\n");full.append(b.getText());}
   String cardInfo=scanVisibleCards(t.getTextBlocks(),frame.getWidth(),frame.getHeight());
   String payload=cardInfo+"\\n\\nSCREEN TEXT:\\n"+full;
   Intent out=new Intent(ACTION_OCR_RESULT);out.setPackage(getPackageName());out.putExtra(EXTRA_OCR_TEXT,payload);sendBroadcast(out);
   maybeSave(cardInfo);
  }).addOnFailureListener(e->android.util.Log.e("ABAnalyzer","OCR",e)).addOnCompleteListener(t->{frame.recycle();busy=false;});
 }
 // Calibrated initially for the supplied TriMela portrait screenshot; coordinates are normalized.
 private String scanVisibleCards(List<Text.TextBlock> blocks,int fw,int fh){
  String[] seats={"A","B","C"};float[][] zones={{0.045f,0.325f},{0.355f,0.635f},{0.665f,0.955f}};
  StringBuilder out=new StringBuilder("CARD SCAN (BETA)\\n");
  String[] hands=new String[3];boolean all=true;
  for(int i=0;i<3;i++){
   List<String> ranks=new ArrayList<>();
   float left=zones[i][0]*fw,right=zones[i][1]*fw,top=0.565f*fh,bottom=0.675f*fh;
   for(Text.TextBlock b:blocks){android.graphics.Rect r=b.getBoundingBox();if(r==null)continue;float cx=r.centerX(),cy=r.centerY();
    if(cx>=left&&cx<=right&&cy>=top&&cy<=bottom){
     Matcher m=Pattern.compile("(?i)(10|[2-9]|A|K|Q|J)").matcher(b.getText().replace("1O","10"));
     while(m.find()){String v=m.group(1).toUpperCase();if(!ranks.contains(v))ranks.add(v);}
    }
   }
   if(ranks.size()>=3){hands[i]=ranks.get(0)+" ?  "+ranks.get(1)+" ?  "+ranks.get(2)+" ?";out.append("Player ").append(seats[i]).append(": ").append(hands[i]).append("\\n");}
   else{all=false;out.append("Player ").append(seats[i]).append(": ").append(ranks.size()).append("/3 ranks read — ").append(ranks).append("\\n");}
  }
  out.append(all?"Ranks found. Suits need visual confirmation before final hand ranking.":"Waiting for clearer card text; keep the revealed cards unobstructed.");
  return out.toString();
 }
 private void maybeSave(String cardInfo){
  // Do not fabricate a winner from incomplete OCR. This build records rounds only after user confirmation/manual entry.
 }
 private void cleanup(){if(display!=null){display.release();display=null;}if(reader!=null){reader.setOnImageAvailableListener(null,null);reader.close();reader=null;}if(recognizer!=null){recognizer.close();recognizer=null;}if(thread!=null){thread.quitSafely();thread=null;handler=null;}projection=null;}
 @Override public void onDestroy(){MediaProjection p=projection;cleanup();if(p!=null)p.stop();super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
