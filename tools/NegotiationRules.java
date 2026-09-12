package mods;

/** Pure negotiation policy. Monetary calculations use long before conversion to game ints. */
public final class NegotiationRules {
    public static final int MAX_ROUNDS = 3;
    public static final int COOLDOWN_DAYS = 14;
    public enum Decision { ACCEPT, COUNTER, REJECT }
    public static final class Context {
        public int market, asking, age, quality, contractDays, salary;
        public double sellerReputation, buyerReputation, importance, happiness, depth, potential;
        public double buyerQuality, sellerQuality, usage, rivalry, competition, career;
        public long sellerCash, buyerCash;
        public boolean listed, abroad;
    }
    public static double clamp(double n, double low, double high) { return Math.max(low, Math.min(high,n)); }
    public static int money(double n) {
        if (!Double.isFinite(n) || n < 0) throw new IllegalArgumentException("Valor inválido");
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE-1000L, Math.round(n)));
    }
    public static int sellerMinimum(Context c, double variation) {
        if(c.listed) return Math.max(1,c.asking);
        double multiplier = 0.88 + .38*c.importance + .13*c.potential
            + .18*(c.happiness-.5) - .10*clamp(c.depth-2,0,3)
            + .10*(c.usage-.5) + .06*clamp((c.quality-50)/50.0,-1,1);
        multiplier += c.contractDays <= 90 ? -.24 : c.contractDays <= 180 ? -.13 : c.contractDays > 730 ? .10 : 0;
        multiplier += c.age >= 33 ? -.10 : c.age <= 23 ? .10 : 0;
        multiplier += c.sellerCash < 0 ? -.16 : c.sellerCash < c.market/4L ? -.07 : .03;
        multiplier += .04*clamp((c.buyerReputation-c.sellerReputation)/50,-1,1);
        return money(c.market*clamp(multiplier+clamp(variation,-.035,.035),.50,1.95));
    }
    public static double interest(Context c, int offeredSalary, double variation) {
        double salaryGain=clamp((double)offeredSalary/Math.max(1,c.salary)-1,-.6,1.2);
        return clamp(49 + .40*(c.buyerReputation-c.sellerReputation)
            + 30*(.5-c.happiness) + 16*(.5-c.usage)
            + 19*salaryGain + .12*(c.buyerQuality-c.sellerQuality)
            - 14*c.rivalry - (c.abroad && c.age<23 ? 6:0)
            + 8*c.competition + 7*c.career + clamp(variation,-3,3),0,100);
    }
    public static int salaryMinimum(Context c, double interest) {
        return money(Math.max(1,c.salary)*clamp(1.08+(55-interest)/100.0,.80,1.70));
    }
    public static int buyerMaximum(Context c, double variation) {
        return Math.min(money(c.market*clamp(1.10+.15*c.potential
            + .20*clamp((c.quality-c.buyerQuality)/30,-1,1)+variation,.65,1.65)),
            (int)Math.max(0,Math.min(Integer.MAX_VALUE-1000L,c.buyerCash)));
    }
    public static Decision sellerDecision(int offer,int minimum,int round) {
        if(offer<=0 || round<1 || round>MAX_ROUNDS) return Decision.REJECT;
        if(offer>=minimum) return Decision.ACCEPT;
        return offer>=minimum*.72 && round<MAX_ROUNDS ? Decision.COUNTER:Decision.REJECT;
    }
    public static Decision buyerDecision(int demand,int maximum,int round) {
        if(demand<=0 || round<1 || round>MAX_ROUNDS) return Decision.REJECT;
        if(demand<=maximum) return Decision.ACCEPT;
        return demand<=maximum*1.35 && round<MAX_ROUNDS ? Decision.COUNTER:Decision.REJECT;
    }
    public static String interestLabel(double value) {
        return value>=80?"Grande interesse":value>=60?"Interesse":value>=40?"Indeciso":value>=20?"Pouco interesse":"Sem interesse";
    }
    private NegotiationRules() {}
}
