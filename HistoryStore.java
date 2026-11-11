package com.ab.analyzer;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public final class HistoryStore {
    private static final String PREF="ab_history",KEY="rounds"; private static final int MAX=500;
    private HistoryStore(){}
    public static synchronized void add(Context c,String source,String winner,String a,String b,String d){
        add(c,source,winner,a,b,d,"0","0","0");
    }
    public static synchronized void add(Context c,String source,String winner,String a,String b,String d,String ba,String bb,String bc){
        try{
            JSONArray old=rounds(c),next=new JSONArray();JSONObject r=new JSONObject();
            r.put("id",UUID.randomUUID().toString());
            r.put("time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(new Date()));
            r.put("source",source);r.put("winner",winner);r.put("a",a);r.put("b",b);r.put("c",d);
            r.put("betA",ba);r.put("betB",bb);r.put("betC",bc);next.put(r);
            for(int i=0;i<old.length()&&i<MAX-1;i++)next.put(old.getJSONObject(i));
            c.getSharedPreferences(PREF,0).edit().putString(KEY,next.toString()).apply();
        }catch(Exception ignored){}
    }
    public static JSONArray rounds(Context c){
        try{return new JSONArray(c.getSharedPreferences(PREF,0).getString(KEY,"[]"));}
        catch(Exception e){return new JSONArray();}
    }
    public static void clear(Context c){c.getSharedPreferences(PREF,0).edit().remove(KEY).apply();}
    public static String summary(Context c,int window){
        JSONArray all=rounds(c);int n=Math.min(all.length(),window);if(n==0)return "No saved rounds yet.";
        int[] wins=new int[3],run=new int[3],longest=new int[3];int ties=0;StringBuilder seq=new StringBuilder();
        for(int i=n-1;i>=0;i--){
            JSONObject r=all.optJSONObject(i);if(r==null)continue;String w=r.optString("winner","");
            if(w.equals("A")||w.equals("B")||w.equals("C")){
                int ix=w.charAt(0)-'A';wins[ix]++;run[ix]++;for(int j=0;j<3;j++)if(j!=ix)run[j]=0;
                longest[ix]=Math.max(longest[ix],run[ix]);
            }else if(w.equals("TIE"))ties++;
            if(seq.length()>0)seq.append(" → ");seq.append(w.isEmpty()?"?":w);
        }
        int total=wins[0]+wins[1]+wins[2],max=Math.max(wins[0],Math.max(wins[1],wins[2]));
        String lead=max==0?"No wins":(wins[0]==max?"A ":"")+(wins[1]==max?"B ":"")+(wins[2]==max?"C":"");
        return "LAST "+n+" VALID ROUNDS\n\nA  "+wins[0]+" wins  ("+pct(wins[0],total)+"%)\nB  "+wins[1]+" wins  ("+pct(wins[1],total)+"%)\nC  "+wins[2]+" wins  ("+pct(wins[2],total)+"%)\nTies  "+ties+
        "\n\nLongest streak — A "+longest[0]+" | B "+longest[1]+" | C "+longest[2]+"\nHistorical leader: "+lead.trim()+"\n\nOldest → newest\n"+seq+
        "\n\nHistorical frequencies describe past rounds; they do not establish the next winner's probability.";
    }
    public static String recent(Context c,int limit){
        JSONArray a=rounds(c);if(a.length()==0)return "No rounds saved.";StringBuilder s=new StringBuilder();
        for(int i=0;i<Math.min(limit,a.length());i++){JSONObject r=a.optJSONObject(i);if(r==null)continue;
            s.append(i+1).append(". ").append(r.optString("time")).append("  •  Winner ").append(r.optString("winner"))
            .append("\nA ").append(r.optString("a")).append(" [").append(r.optString("betA","0")).append("]")
            .append(" | B ").append(r.optString("b")).append(" [").append(r.optString("betB","0")).append("]")
            .append(" | C ").append(r.optString("c")).append(" [").append(r.optString("betC","0")).append("]\n\n");
        }return s.toString();
    }
    public static String csv(Context c){
        JSONArray a=rounds(c);StringBuilder s=new StringBuilder("time,source,winner,A_cards,B_cards,C_cards,A_bet,B_bet,C_bet\n");
        for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(r==null)continue;String[] k={"time","source","winner","a","b","c","betA","betB","betC"};
            for(int j=0;j<k.length;j++){if(j>0)s.append(',');s.append('"').append(r.optString(k[j],"").replace("\"","'")).append('"');}s.append('\n');
        }return s.toString();
    }
    private static int pct(int x,int n){return n==0?0:Math.round(x*100f/n);}
}
