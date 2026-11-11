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
    private WindowManager wm;private LinearLayout panel,body;private TextView details,header;private BroadcastReceiver receiver;private boolean expanded=true;
    @Override public void onCreate(){
        super.onCreate();if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){stopSelf();return;}
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(10),dp(7),dp(10),dp(7));panel.setBackgroundColor(0xEE101522);
        header=new TextView(this);header.setText("AB ANALYZER • LIVE  ▾");header.setTextColor(0xFFFFC879);header.setTextSize(13);header.setPadding(dp(4),dp(5),dp(4),dp(5));panel.addView(header);
        body=new LinearLayout(this);body.setOrientation(1);panel.addView(body);
        details=new TextView(this);details.setText("Waiting for visible screen OCR...");details.setTextColor(Color.WHITE);details.setTextSize(11);details.setMaxLines(10);body.addView(details);
        TextView min=new TextView(this);min.setText("TAP HEADER TO MINIMIZE / EXPAND");min.setTextColor(0xFF24C6A5);min.setPadding(dp(4),dp(7),dp(4),dp(3));body.addView(min);
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(dp(250),-2,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.x=dp(8);p.y=dp(70);wm.addView(panel,p);
        header.setOnTouchListener(new View.OnTouchListener(){int sx,sy;float tx,ty;boolean moved;
            public boolean onTouch(View v,MotionEvent e){switch(e.getAction()){
                case MotionEvent.ACTION_DOWN:sx=p.x;sy=p.y;tx=e.getRawX();ty=e.getRawY();moved=false;return true;
                case MotionEvent.ACTION_MOVE:int dx=(int)(e.getRawX()-tx),dy=(int)(e.getRawY()-ty);if(Math.abs(dx)>4||Math.abs(dy)>4)moved=true;p.x=sx+dx;p.y=sy+dy;wm.updateViewLayout(panel,p);return true;
                case MotionEvent.ACTION_UP:if(!moved)toggle();return true;}return true;}});
        receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){String t=i.getStringExtra(ScreenCaptureService.EXTRA_OCR_TEXT);details.setText(t==null||t.trim().isEmpty()?"No readable text detected.":t);}};
        IntentFilter f=new IntentFilter(ScreenCaptureService.ACTION_OCR_RESULT);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);
    }
    private void toggle(){expanded=!expanded;body.setVisibility(expanded?View.VISIBLE:View.GONE);header.setText(expanded?"AB ANALYZER • LIVE  ▴":"AB ANALYZER • LIVE  ▾");}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override public int onStartCommand(Intent i,int f,int id){return START_NOT_STICKY;}
    @Override public void onDestroy(){if(receiver!=null)try{unregisterReceiver(receiver);}catch(Exception ignored){}if(wm!=null&&panel!=null)try{wm.removeView(panel);}catch(Exception ignored){}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
