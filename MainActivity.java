package com.ab.analyzer;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.JSONArray;

public class MainActivity extends Activity {
    private static final int CAPTURE=1001;
    private static final int BG=0xFF08111F, PANEL=0xFF111E30, GOLD=0xFFFFC879, TEAL=0xFF24C6A5, WHITE=0xFFF1F5F9, MUTED=0xFF9AA9BC;
    private LinearLayout root;
    private EditText a,b,c,betA,betB,betC,stakeA,stakeB,stakeC,multiplier,fee;
    private TextView scanStatus,result,history,analysis,prediction,payout;
    private Spinner windowSpinner, conventionSpinner;
    private BroadcastReceiver ocrReceiver;
    private String lastOcr="";
    private boolean receiverRegistered=false;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(12),dp(16),dp(28));root.setBackgroundColor(BG);
        scroll.addView(root);setContentView(scroll);
        buildHeader();buildScanner();buildManual();buildAnalytics();buildHistory();buildCalculator();buildFooter();
        registerOcrReceiver();
    }
    private void buildHeader(){
        TextView brand=txt("AB ANALYZER",26,GOLD);brand.setGravity(Gravity.CENTER);brand.setTypeface(null,1);root.addView(brand);
        TextView sub=txt("STREAMKAR TRIMELA  •  ROUND INTELLIGENCE",11,MUTED);sub.setGravity(Gravity.CENTER);root.addView(sub);
        TextView privacy=txt("ON-DEVICE SCREEN READING  |  YOU CONTROL CAPTURE",10,TEAL);privacy.setGravity(Gravity.CENTER);root.addView(privacy);
    }
    private void buildScanner(){
        LinearLayout box=card();root.addView(box);heading(box,"LIVE SCREEN SCANNER");
        scanStatus=txt("Stopped • Tap Start, grant Android screen capture, then open StreamKar.",13,WHITE);box.addView(scanStatus);
        rowButtons(box,"START SCAN","STOP SCAN",v->requestCapture(),v->stopScanning());
        TextView note=txt("OCR currently reads visible screen text. Card ranks/suits and bet amounts must be checked before saving; unreadable values are never treated as confirmed.",11,MUTED);box.addView(note);
    }
    private void buildManual(){
        LinearLayout box=card();root.addView(box);heading(box,"CURRENT ROUND • CARD CONFIRMATION");
        TextView hint=txt("Enter rank + suit, for example: AS KH 2D   (S=Spades H=Hearts D=Diamonds C=Clubs)",11,MUTED);box.addView(hint);
        a=edit(box,"Player A • AS KH 2D");b=edit(box,"Player B • 5S 5H QD");c=edit(box,"Player C • 6C 8H JD");
        heading(box,"VISIBLE BET AMOUNTS (OPTIONAL)");
        betA=number(box,"Player A bet");betB=number(box,"Player B bet");betC=number(box,"Player C bet");
        rowButtons(box,"ANALYZE ROUND","SAVE CONFIRMED ROUND",v->analyze(false),v->analyze(true));
        result=txt("Analysis will appear here.",14,WHITE);result.setPadding(0,dp(10),0,dp(4));box.addView(result);
    }
    private void buildAnalytics(){
        LinearLayout box=card();root.addView(box);heading(box,"PATTERN LAB");
        TextView label=txt("Choose a recent-round window. These are historical descriptions, not guaranteed predictions.",11,MUTED);box.addView(label);
        windowSpinner=new Spinner(this);String[] opts={"15 rounds","20 rounds","30 rounds","100 rounds"};windowSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,opts));box.addView(windowSpinner);
        Button refresh=button("REFRESH ANALYSIS");box.addView(refresh);
        analysis=txt("Save confirmed rounds to build your local history.",13,WHITE);box.addView(analysis);
        prediction=txt("Historical estimate will appear after saving rounds.",13,TEAL);box.addView(prediction);
        refresh.setOnClickListener(v->refreshAnalysis());
    }
    private void buildHistory(){
        LinearLayout box=card();root.addView(box);heading(box,"ROUND HISTORY");
        history=txt("No rounds saved yet.",12,WHITE);box.addView(history);
        rowButtons(box,"VIEW LATEST 20","EXPORT CSV",v->{history.setText(HistoryStore.recent(this,20));},v->exportCsv());
        Button clear=button("DELETE ALL LOCAL HISTORY");box.addView(clear);
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete history?").setMessage("This permanently removes saved rounds from this device.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{HistoryStore.clear(this);refreshAnalysis();history.setText("History deleted.");}).show());
    }
    private void buildCalculator(){
        LinearLayout box=card();root.addView(box);heading(box,"PAYOUT SCENARIO CALCULATOR");
        TextView warn=txt("Enter the multiplier and payout convention verified in the game. No default payout is assumed.",11,MUTED);box.addView(warn);
        stakeA=number(box,"Stake on A");stakeB=number(box,"Stake on B");stakeC=number(box,"Stake on C");
        multiplier=number(box,"Observed payout multiplier (e.g. 2.8)");multiplier.setText("2.8");
        fee=number(box,"Fees / deductions (optional)");fee.setText("0");
        conventionSpinner=new Spinner(this);conventionSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Multiplier includes returned stake","Multiplier is net profit, stake returned separately"}));box.addView(conventionSpinner);
        Button calc=button("CALCULATE A / B / C OUTCOMES");box.addView(calc);
        payout=txt("Calculator results will appear here.",13,WHITE);box.addView(payout);
        calc.setOnClickListener(v->calculatePayout());
    }
    private void buildFooter(){TextView f=txt("AB Analyzer V7 • Local analysis only • Past results cannot reveal hidden cards or guarantee the next outcome.",10,MUTED);f.setGravity(Gravity.CENTER);f.setPadding(0,dp(16),0,0);root.addView(f);}
    private LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(1);x.setPadding(dp(14),dp(12),dp(14),dp(14));x.setBackgroundColor(PANEL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);x.setLayoutParams(p);return x;}
    private void heading(LinearLayout p,String s){TextView t=txt(s,15,GOLD);t.setTypeface(null,1);t.setPadding(0,0,0,dp(7));p.addView(t);}
    private TextView txt(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));return t;}
    private EditText edit(LinearLayout p,String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setTextSize(14);e.setPadding(dp(10),dp(6),dp(10),dp(6));e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(TEAL));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(5);p.addView(e,lp);return e;}
    private EditText number(LinearLayout p,String hint){EditText e=edit(p,hint);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTextColor(WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1D766F));return b;}
    private void rowButtons(LinearLayout p,String x,String y,View.OnClickListener l,View.OnClickListener r){LinearLayout row=new LinearLayout(this);row.setOrientation(0);Button b1=button(x),b2=button(y);row.addView(b1,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=dp(6);row.addView(b2,lp);p.addView(row);b1.setOnClickListener(l);b2.setOnClickListener(r);}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private void analyze(boolean save){
        String aa=value(a),bb=value(b),cc=value(c);String out=HandEngine.describe(aa,bb,cc);result.setText(out);
        String winner=HandEngine.winner(aa,bb,cc);if(winner.startsWith("ERROR"))return;
        if(save){HistoryStore.add(this,"Manual confirmed",winner,aa,bb,cc,valueOrZero(betA),valueOrZero(betB),valueOrZero(betC));scanStatus.setText("Confirmed round saved • Winner: "+winner);refreshAnalysis();history.setText(HistoryStore.recent(this,10));}
    }
    private String value(EditText e){return e.getText().toString().trim();}
    private String valueOrZero(EditText e){String s=value(e);return s.isEmpty()?"0":s;}
    private int selectedWindow(){int p=windowSpinner.getSelectedItemPosition();return p==0?15:p==1?20:p==2?30:100;}
    private void refreshAnalysis(){analysis.setText(HistoryStore.summary(this,selectedWindow()));prediction.setText(PredictionEngine.estimate(this,selectedWindow()));}
    private void calculatePayout(){
        try{double aa=num(stakeA),bb=num(stakeB),cc=num(stakeC),m=num(multiplier),f=num(fee);
            boolean total=conventionSpinner.getSelectedItemPosition()==0;
            payout.setText(PayoutCalculator.calculate(aa,bb,cc,m,total,f));
        }catch(Exception e){payout.setText("Please enter valid numeric values.");}
    }
    private double num(EditText e){String s=value(e);return s.isEmpty()?0:Double.parseDouble(s);}
    private void exportCsv(){
        String csv=HistoryStore.csv(this);Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/csv");i.putExtra(Intent.EXTRA_SUBJECT,"AB Analyzer round history");i.putExtra(Intent.EXTRA_TEXT,csv);startActivity(Intent.createChooser(i,"Export history"));
    }
    private void requestCapture(){
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
            scanStatus.setText("Allow Display over other apps, return, then tap START SCAN again.");return;
        }
        MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        if(m==null){scanStatus.setText("Screen capture unavailable on this device.");return;}
        startActivityForResult(m.createScreenCaptureIntent(),CAPTURE);
    }
    @Override protected void onActivityResult(int req,int code,Intent data){
        super.onActivityResult(req,code,data);if(req!=CAPTURE)return;
        if(code!=RESULT_OK||data==null){scanStatus.setText("Screen capture permission was not granted.");return;}
        Intent s=new Intent(this,ScreenCaptureService.class);s.putExtra("resultCode",code);s.putExtra("data",data);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);
        startService(new Intent(this,FloatingPanelService.class));scanStatus.setText("Capture active • OCR is reading visible text. Verify all values before saving.");
    }
    private void stopScanning(){stopService(new Intent(this,ScreenCaptureService.class));stopService(new Intent(this,FloatingPanelService.class));scanStatus.setText("Screen scanning stopped.");}
    private void registerOcrReceiver(){
        ocrReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){lastOcr=i.getStringExtra(ScreenCaptureService.EXTRA_OCR_TEXT);if(lastOcr==null)lastOcr="";scanStatus.setText("Live OCR update received • "+(lastOcr.isEmpty()?"No readable text":"Visible text detected. Check floating panel before manual confirmation."));}};
        IntentFilter f=new IntentFilter(ScreenCaptureService.ACTION_OCR_RESULT);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(ocrReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(ocrReceiver,f);
        receiverRegistered=true;
    }
    @Override protected void onDestroy(){if(receiverRegistered)try{unregisterReceiver(ocrReceiver);}catch(Exception ignored){}super.onDestroy();}
}
