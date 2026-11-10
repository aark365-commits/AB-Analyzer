package com.ab.analyzer;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class HistoryStore {
 static final String PREF="ab_history",KEY="rounds";
 public static synchronized void add(Context c,String source,String winner,String a,String b,String d){
  try{SharedPreferences p=c.getSharedPreferences(PREF,0);JSONArray old=new JSONArray(p.getString(KEY,"[]"));JSONArray n=new JSONArray();
   JSONObject o=new JSONObject();o.put("time",new SimpleDateFormat("dd MMM HH:mm",Locale.getDefault()).format(new Date()));o.put("source",source);o.put("winner",winner);o.put("a",a);o.put("b",b);o.put("c",d);n.put(o);
   for(int i=0;i<old.length()&&i<29;i++)n.put(old.getJSONObject(i));p.edit().putString(KEY,n.toString()).apply();
  }catch(Exception ignored){}
 }
 public static String summary(Context c){
  try{JSONArray a=new JSONArray(c.getSharedPreferences(PREF,0).getString(KEY,"[]"));if(a.length()==0)return "No saved rounds yet. A round is saved only when all three hands are readable/confirmed.";
   int[] count=new int[3];StringBuilder s=new StringBuilder("LATEST ROUNDS (max 30)\\n");
   for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String w=o.optString("winner");if(w.equals("A"))count[0]++;if(w.equals("B"))count[1]++;if(w.equals("C"))count[2]++;
    s.append(i+1).append(". ").append(o.optString("time")).append("  Winner: ").append(w).append("\\n   A ").append(o.optString("a")).append(" | B ").append(o.optString("b")).append(" | C ").append(o.optString("c")).append("\\n");
   }
   s.append("\\nRECENT FREQUENCY: A ").append(count[0]).append(" • B ").append(count[1]).append(" • C ").append(count[2]);
   int max=Math.max(count[0],Math.max(count[1],count[2]));int ties=(count[0]==max?1:0)+(count[1]==max?1:0)+(count[2]==max?1:0);
   s.append("\\nTrend: ").append(a.length()<10?"Not enough rounds":ties>1?"No clear leader":(count[0]==max?"A":count[1]==max?"B":"C")+" appeared most in this small history");
   s.append("\\nNote: frequency is descriptive, not a reliable prediction of the next independent round.");return s.toString();
  }catch(Exception e){return "History unavailable.";}
 }
}
