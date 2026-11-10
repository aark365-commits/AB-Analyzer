package com.ab.analyzer;
import android.app.Service;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class FloatingPanelService extends Service {
 private WindowManager wm;private LinearLayout panel,body;private TextView details,head;private BroadcastReceiver receiver;private boolean expanded=true;
 @Override public void onCreate(){
  super.onCreate();if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){stopSelf();return;}
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(12,8,12,8);panel.setBackgroundColor(0xEE101522);
  head=new TextView(this);head.setText("AB • LIVE   ▾");head.setTextColor(0xFFFFC58A);head.setTextSize(13);head.setPadding(4,4,4,4);panel.addView(head);
  body=new LinearLayout(this);body.setOrientation(1);panel.addView(body);
  details=new TextView(this);details.setText("Waiting for TriMela cards…");details.setTextColor(Color.WHITE);details.setTextSize(11);details.setMaxLines(8);body.addView(details);
  TextView hide=new TextView(this);hide.setText("MINIMIZE  ×");hide.setTextColor(0xFFFFC58A);hide.setTextSize(11);hide.setPadding(4,5,4,2);body.addView(hide);
  int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(230),WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.LEFT;p.x=dp(8);p.y=dp(70);wm.addView(panel,p);
  head.setOnClickListener(v->{expanded=!expanded;body.setVisibility(expanded?View.VISIBLE:View.GONE);head.setText(expanded?"AB • LIVE   ▴":"AB • LIVE   ▾");});
  hide.setOnClickListener(v->{expanded=false;body.setVisibility(View.GONE);head.setText("AB • LIVE   ▾");});
  // Drag the compact panel by its title bar.
  head.setOnTouchListener(new View.OnTouchListener(){int sx,sy;float tx,ty;public boolean onTouch(View v,android.view.MotionEvent e){switch(e.getAction()){case MotionEvent.ACTION_DOWN:sx=p.x;sy=p.y;tx=e.getRawX();ty=e.getRawY();return true;case MotionEvent.ACTION_MOVE:p.x=sx+(int)(e.getRawX()-tx);p.y=sy+(int)(e.getRawY()-ty);wm.updateViewLayout(panel,p);return true;case MotionEvent.ACTION_UP:if(Math.abs(e.getRawX()-tx)<8&&Math.abs(e.getRawY()-ty)<8){expanded=!expanded;body.setVisibility(expanded?View.VISIBLE:View.GONE);head.setText(expanded?"AB • LIVE   ▴":"AB • LIVE   ▾");}return true;}return false;}});
  receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){String s=i.getStringExtra(ScreenCaptureService.EXTRA_OCR_TEXT);details.setText(s==null?"No text detected":s);}};
  IntentFilter f=new IntentFilter(ScreenCaptureService.ACTION_OCR_RESULT);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);
 }
 int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 @Override public int onStartCommand(Intent i,int f,int id){return START_NOT_STICKY;}
 @Override public void onDestroy(){if(receiver!=null)try{unregisterReceiver(receiver);}catch(Exception ignored){}if(wm!=null&&panel!=null)try{wm.removeView(panel);}catch(Exception ignored){}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
