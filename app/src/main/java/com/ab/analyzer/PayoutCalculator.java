package com.ab.analyzer;
public final class PayoutCalculator {
    private PayoutCalculator(){}
    public static String calculate(double a,double b,double c,double multiplier,boolean totalReturn,double fee){
        if(a<0||b<0||c<0||multiplier<0||fee<0)return "Enter valid non-negative amounts.";
        double committed=a+b+c;
        StringBuilder s=new StringBuilder("TOTAL STAKE: "+fmt(committed)+" coins\\n\\n");
        double[] stakes={a,b,c};String[] names={"A wins","B wins","C wins"};
        for(int i=0;i<3;i++){
            double payout=stakes[i]*multiplier-(totalReturn?0:stakes[i]);
            double net=payout-committed-fee;
            s.append(names[i]).append("\\n  Estimated return: ").append(fmt(totalReturn?stakes[i]*multiplier:stakes[i]*(multiplier+1))).append("\\n  Net result: ").append(net>=0?"+":"").append(fmt(net)).append(" coins\\n");
        }
        s.append("\\nWorst-case result if all selected stakes lose: -").append(fmt(committed+fee)).append(" coins");
        s.append("\\n\\nCalculation only. Verify the game's actual payout convention and deductions.");
        return s.toString();
    }
    private static String fmt(double x){return String.format(java.util.Locale.US,"%,.2f",x);}
}
