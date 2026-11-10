package com.ab.analyzer;

import android.app.*;
import android.os.*;
import android.content.*;
import android.media.projection.MediaProjectionManager;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.provider.Settings;
import android.net.Uri;
import org.json.*;

public class MainActivity extends Activity {
    static final int CAPTURE_REQUEST=1001;
    MediaProjectionManager projectionManager;
    TextView status, historyText;
    LinearLayout root;
    EditText a,b,c;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(1);
        root.setPadding(22,24,22,24); root.setBackgroundColor(Color.rgb(10,17,28));
        scroll.addView(root);
        TextView title=label("AB ANALYZER V5",28,0xFFFFC58A);
        title.setGravity(Gravity.CENTER); root.addView(title);
        root.addView(label("LIVE CARD SCANNER • ROUND HISTORY • TREND",13,Color.LTGRAY));
        Button start=button("START SCREEN SCAN"); root.addView(start);
        Button stop=button("STOP SCREEN SCAN"); root.addView(stop);
        status=label("Scanner stopped. Start scanning and open StreamKar TriMela.",14,Color.WHITE);
        root.addView(status);
        Button history=button("VIEW SAVED HISTORY"); root.addView(history);
        historyText=label("",14,Color.WHITE); root.addView(historyText);
        root.addView(label("MANUAL CARD ENTRY (BACKUP)",18,0xFFFFC58A));
        a=entry("Player A: AS KH 2D"); b=entry("Player B: 5S 5H QD"); c=entry("Player C: 6C 8H JD");
        Button manual=button("SAVE MANUAL ROUND"); root.addView(manual);
        manual.setOnClickListener(v -> {
            String winner=HandEngine.winner(a.getText().toString(),b.getText().toString(),c.getText().toString());
            if(winner.startsWith("ERROR")) { status.setText(winner); return; }
            HistoryStore.add(this,"Manual",winner,a.getText().toString(),b.getText().toString(),c.getText().toString());
            status.setText("Saved: "+winner); showHistory();
        });
        start.setOnClickListener(v -> requestCapture());
        stop.setOnClickListener(v -> { stopService(new Intent(this,ScreenCaptureService.class)); stopService(new Intent(this,FloatingPanelService.class)); status.setText("Scanner stopped."); });
        history.setOnClickListener(v -> showHistory());
        setContentView(scroll);
    }
    TextView label(String s,int size,int color){ TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(4,12,4,12);return t; }
    Button button(String s){ Button b=new Button(this);b.setText(s);return b; }
    EditText entry(String hint){ EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xFF888888);root.addView(e);return e; }
    void requestCapture(){
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));
            status.setText("Allow Display over other apps, return and tap Start again."); return;
        }
        projectionManager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(),CAPTURE_REQUEST);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==CAPTURE_REQUEST && res==RESULT_OK && data!=null){
            Intent s=new Intent(this,ScreenCaptureService.class);s.putExtra("resultCode",res);s.putExtra("data",data);
            if(Build.VERSION.SDK_INT>=26) startForegroundService(s); else startService(s);
            startService(new Intent(this,FloatingPanelService.class));
            status.setText("Capture active. Open StreamKar TriMela; keep cards visible.");
        } else if(req==CAPTURE_REQUEST) status.setText("Screen capture permission was not granted.");
    }
    void showHistory(){ historyText.setText(HistoryStore.summary(this)); }
}
