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
        int floor=NegotiationRules.sellerFloor(c,normal);
        check(floor<normal,"Seller has room to concede");
        check(NegotiationRules.nextSellerAsk((int)(normal*.85),0,normal,floor,1,0)==normal,"First counter keeps target");
        int lower=NegotiationRules.nextSellerAsk((int)(normal*.90),(int)(normal*.85),normal,floor,2,0);
        check(lower<normal&&lower>=floor,"Second counter concedes without crossing floor");
        check(NegotiationRules.sellerRound((int)(normal*.70),0,normal,floor,1,0)==NegotiationRules.Decision.REJECT,"Low offer rejected");
        check(NegotiationRules.sellerRound(floor-1,(int)(normal*.85),lower,floor,4,0)==NegotiationRules.Decision.REJECT,"Final floor enforced");
        check(NegotiationRules.sellerRound(floor,(int)(normal*.85),lower,floor,4,0)==NegotiationRules.Decision.ACCEPT,"Final acceptable offer");
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
        check(NegotiationRules.buyerRound(200,0,100,150,1,0)==NegotiationRules.Decision.REJECT,"Buyer refuses excessive demand");
        check(NegotiationRules.nextBuyerOffer(140,150,100,150,2,0)>100,"Buyer improves counteroffer");
        System.out.println("18 verificações de negociação passaram.");
    }
}
