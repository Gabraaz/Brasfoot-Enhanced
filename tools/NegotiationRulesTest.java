package mods;

public final class NegotiationRulesTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        NegotiationRules.Context c=new NegotiationRules.Context();
        c.market=50000000;c.asking=45000000;c.salary=100000;c.age=26;c.quality=60;
        c.contractDays=365;c.importance=.6;c.happiness=.5;c.depth=3;c.usage=.5;
        c.sellerCash=100000000;c.buyerCash=100000000;c.sellerReputation=60;c.buyerReputation=60;
        int normal=NegotiationRules.sellerMinimum(c,0);
        check(NegotiationRules.sellerDecision(normal,normal,1)==NegotiationRules.Decision.ACCEPT,"Accept price");
        check(NegotiationRules.sellerDecision((int)(normal*.8),normal,1)==NegotiationRules.Decision.COUNTER,"Counter close price");
        check(NegotiationRules.sellerDecision((int)(normal*.8),normal,3)==NegotiationRules.Decision.REJECT,"Round limit");
        check(NegotiationRules.sellerDecision(normal,normal,4)==NegotiationRules.Decision.REJECT,"No fourth round");
        c.contractDays=60;check(NegotiationRules.sellerMinimum(c,0)<normal,"Expiring contract discount");
        c.listed=true;check(NegotiationRules.sellerMinimum(c,.03)==c.asking,"Listed price fixed");c.listed=false;
        double neutral=NegotiationRules.interest(c,c.salary,0);c.buyerReputation=90;
        check(NegotiationRules.interest(c,c.salary,0)>neutral,"Reputation increases interest");
        c.happiness=1;double happy=NegotiationRules.interest(c,c.salary,0);c.happiness=0;
        check(NegotiationRules.interest(c,c.salary,0)>happy,"Dissatisfaction increases interest");
        check(NegotiationRules.interest(c,c.salary*2,0)>NegotiationRules.interest(c,c.salary,0),"Wage effect");
        c.buyerCash=1234;check(NegotiationRules.buyerMaximum(c,0)<=1234,"Budget capped");
        check(NegotiationRules.buyerDecision(200,100,1)==NegotiationRules.Decision.REJECT,"Buyer refuses unreasonable demand");
        check(NegotiationRules.money(Double.MAX_VALUE)>0,"No monetary overflow");
        System.out.println("12 verificações de negociação passaram.");
    }
}
