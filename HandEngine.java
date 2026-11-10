package com.ab.analyzer;
import java.util.*;
public class HandEngine {
 static int rank(String r){switch(r){case "A":return 14;case "K":return 13;case "Q":return 12;case "J":return 11;default:return Integer.parseInt(r);}}
 static class H {int type;int[] tie;H(int t,int...x){type=t;tie=x;}}
 static H hand(String s){
  String[] cs=s.toUpperCase().trim().split("[,\\s]+"); if(cs.length!=3)throw new IllegalArgumentException("Need 3 cards");
  int[] r=new int[3];String[] su=new String[3];Set<String> seen=new HashSet<>();
  for(int i=0;i<3;i++){String z=cs[i];if(z.length()<2)throw new IllegalArgumentException("Bad card "+z);
   String rr=z.substring(0,z.length()-1),ss=z.substring(z.length()-1);
   if(!Arrays.asList("A","K","Q","J","10","9","8","7","6","5","4","3","2").contains(rr)||!Arrays.asList("S","H","D","C").contains(ss))throw new IllegalArgumentException("Use AS KH 2D format");
   if(!seen.add(z))throw new IllegalArgumentException("Duplicate card");r[i]=rank(rr);su[i]=ss;
  }
  Arrays.sort(r);boolean trail=r[0]==r[2],color=su[0].equals(su[1])&&su[1].equals(su[2]);
  int seq=(r[0]==12&&r[1]==13&&r[2]==14)?15:(r[0]==2&&r[1]==3&&r[2]==14)?14:(r[0]+1==r[1]&&r[1]+1==r[2]?r[2]:0);
  if(trail)return new H(6,r[2]);if(seq>0&&color)return new H(5,seq);if(seq>0)return new H(4,seq);
  if(color)return new H(3,r[2],r[1],r[0]);if(r[0]==r[1])return new H(2,r[0],r[2]);if(r[1]==r[2])return new H(2,r[1],r[0]);return new H(1,r[2],r[1],r[0]);
 }
 static int cmp(H a,H b){if(a.type!=b.type)return a.type-b.type;for(int i=0;i<Math.min(a.tie.length,b.tie.length);i++)if(a.tie[i]!=b.tie[i])return a.tie[i]-b.tie[i];return 0;}
 public static String winner(String a,String b,String c){try{H[] h={hand(a),hand(b),hand(c)};int best=0;for(int i=1;i<3;i++)if(cmp(h[i],h[best])>0)best=i;boolean tie=cmp(h[0],h[best])==0&&cmp(h[1],h[best])==0&&cmp(h[2],h[best])==0;return tie?"TIE":"ABC".substring(best,best+1);}catch(Exception e){return "ERROR: "+e.getMessage();}}
}
