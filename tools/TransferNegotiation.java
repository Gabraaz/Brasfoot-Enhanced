package mods;

import best.F;
import best.ah;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Random;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static mods.NegotiationRules.*;

/** Adapter for verified career fields; state is stored with the player in the save. */
public final class TransferNegotiation {
    private TransferNegotiation() {}

    static Object field(Object owner, String name) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Estrutura incompatível: " + name, e);
        }
    }

    static void field(Object owner, String name, Object value) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(owner, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Estrutura incompatível: " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    static HashMap<String, long[]> state(F player) {
        HashMap<String, long[]> result = (HashMap<String, long[]>) field(player, "enhancedNegotiations");
        if (result == null) {
            result = new HashMap<>();
            field(player, "enhancedNegotiations", result);
        }
        return result;
    }

    static long day() {
        return c.a.SR.bb().getTimeInMillis() / 86400000L;
    }

    static double averageQuality(ah club) {
        double sum = 0;
        for (Object item : club.kc()) sum += ((F) item).fi();
        return sum / Math.max(1, club.kc().size());
    }

    static int appearances(F player) {
        int count = 0;
        if (player.gr() != null) for (Object item : player.gr()) {
            best.k record = (best.k) item;
            if (record.H() == c.a.SR.H()) count += record.w();
        }
        return count;
    }

    static Context context(F p, ah buyer) {
        ah seller = p.fg(); Context x = new Context();
        x.market = Math.max(1,p.fk()); x.asking = Math.max(1,p.fl());
        x.listed = Boolean.TRUE.equals(p.ft()); x.age = p.getIdade(); x.quality = p.fi();
        x.salary = Math.max(1,p.fj()); x.contractDays = p.fR();
        x.sellerCash = seller.kb(); x.buyerCash = buyer.kb();
        x.sellerReputation = clamp(seller.getReputacao()*20,0,100);
        x.buyerReputation = clamp(buyer.getReputacao()*20,0,100);
        x.sellerQuality = averageQuality(seller); x.buyerQuality = averageQuality(buyer);
        int mostGames=0, better=0; double wages=0;
        for(Object item:seller.kc()) {
            F peer=(F)item; mostGames=Math.max(mostGames,appearances(peer)); wages+=peer.fj();
            if(peer.getPosicao()==p.getPosicao()) {x.depth++; if(peer.fi()>p.fi()) better++;}
        }
        x.usage=mostGames==0?.5:clamp((double)appearances(p)/mostGames,0,1);
        x.importance=clamp(.8-.22*better+.2*(x.usage-.5),0,1);
        double avgWage=wages/Math.max(1,seller.kc().size());
        x.happiness=clamp(.55+.35*(x.usage-.5)+.12*clamp(x.salary/Math.max(1,avgWage)-1,-1,1)
            -(x.contractDays<=90?.12:0)-(x.listed?.10:0),0,1);
        x.potential=clamp((28-x.age)/12.0,0,1)*clamp(x.quality/60.0,0,1);
        x.abroad=seller.getPais()!=buyer.getPais();
        x.competition=clamp((seller.getDivisao()-buyer.getDivisao())*.3,-1,1);
        x.career=x.age>=32?.25:x.age<=23?clamp((x.quality-x.buyerQuality)/30,-1,1):0;
        return x;
    }

    public static void refreshMarket(Object market) {
        F p=(F)field(market,"yK"); ah buyer=(ah)field(market,"ul");
        JButton button=(JButton)field(market,"FN");
        button.setText(p!=null && Boolean.TRUE.equals(p.ft())?"Comprar":"Fazer Proposta");
        button.setEnabled(p!=null && p.fg()!=null && p.fg()!=buyer);
        ((JButton)field(market,"MK")).setVisible(false);
    }

    public static void openMarket(Object market) {
        F p=(F)field(market,"yK"); ah buyer=(ah)field(market,"ul");
        if(p==null || p.fg()==buyer) return;
        if(!Boolean.TRUE.equals(p.ft()) && !c.a.vL()) {
            JOptionPane.showMessageDialog((Component)market,"Ofertas fora da lista de venda exigem a versão registrada."); return;
        }
        best.l.l(false);
        JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor((Component)market),"Negociação",Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(new Panel(dialog,p,buyer,false,0,null));
        size(dialog); dialog.setVisible(true);
        try {java.lang.reflect.Method m=market.getClass().getDeclaredMethod("sH");m.setAccessible(true);m.invoke(market);}
        catch(ReflectiveOperationException e) {throw new IllegalStateException(e);}
        refreshMarket(market);
    }

    public static void installOffer(Object host) {
        if(Boolean.TRUE.equals(field(host,"CZ"))) return;
        install(host,(F)field(host,"CY"),(ah)field(host,"ul"),false,0);
    }
    public static void installIncoming(Object host) {
        if(Boolean.TRUE.equals(field(host,"Nx"))) return;
        install(host,(F)field(host,"uz"),(ah)field(host,"Nu"),true,(Integer)field(host,"Nw"));
    }
    static void install(Object host,F p,ah buyer,boolean incoming,int amount) {
        JDialog dialog=(JDialog)field(host,"ub"); JPanel container=(JPanel)host;
        container.removeAll();container.setLayout(new BorderLayout());
        container.add(new Panel(dialog,p,buyer,incoming,amount,incoming?host:null));
        SwingUtilities.invokeLater(()->size(dialog));
    }
    static void size(JDialog d) {
        Rectangle r=d.getGraphicsConfiguration().getBounds(); Insets in=Toolkit.getDefaultToolkit().getScreenInsets(d.getGraphicsConfiguration());
        d.setSize(Math.min(690,r.width-in.left-in.right),Math.min(510,r.height-in.top-in.bottom));
        d.setLocationRelativeTo(d.getOwner());d.setResizable(true);
    }
    static String cash(int n) {return best.C.c(n);}

    static final class Panel extends JPanel {
        final JDialog dialog; final F player; final ah buyer,seller; final boolean incoming; final Object host;
        final Context ctx; final long[] record; final double variation; final int minimum,maximum;
        final JTextArea message=new JTextArea(6,40); final JLabel interest=new JLabel();
        final JSpinner offer,wage; final JComboBox<String> term=new JComboBox<>(new String[]{"6 meses","1 ano","2 anos","3 anos"});
        final JButton send=new JButton("Enviar proposta"), accept=new JButton("Aceitar"), close=new JButton("Encerrar negociação");
        int rounds,salaryRounds,agreed,counter; boolean started,finished,contract;
        Panel(JDialog d,F p,ah b,boolean received,int initial,Object h) {
            dialog=d;player=p;buyer=b;seller=p.fg();incoming=received;host=h;ctx=context(p,b);
            record=state(p).computeIfAbsent(seller.lk()+":"+buyer.lk(),k->new long[]{0,new Random().nextLong()});
            variation=new Random(record[1]).nextDouble()*.06-.03;
            minimum=sellerMinimum(ctx,variation);
            maximum=Math.max(buyerMaximum(ctx,variation),(int)Math.min(Math.max(0,b.kb()),initial));
            offer=new JSpinner(new SpinnerNumberModel(Math.max(1,received?initial:ctx.listed?ctx.asking:ctx.market),1,Integer.MAX_VALUE-1000,1000));
            wage=new JSpinner(new SpinnerNumberModel(Math.min(100000000,ctx.salary),1,100000000,1000));
            term.setSelectedIndex(1);wage.setEnabled(false);term.setEnabled(false);
            setLayout(new BorderLayout(10,10));setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
            JPanel header=new JPanel(new GridLayout(0,1,4,4));
            JLabel title=new JLabel((incoming?"Proposta de ":"Negociação com ")+(incoming?buyer:seller).getNome());
            title.setFont(title.getFont().deriveFont(Font.BOLD,18));header.add(title);
            header.add(new JLabel(player.getNome()+" | Mercado: "+cash(ctx.market)));
            header.add(new JLabel("Reputação: "+(int)ctx.sellerReputation+" → "+(int)ctx.buyerReputation+" | Satisfação estimada: "+(int)(ctx.happiness*100)+"/100"));header.add(interest);
            JPanel fields=new JPanel(new GridLayout(0,2,8,8));
            fields.add(new JLabel(incoming?"Sua contraproposta:":"Valor da proposta:"));fields.add(offer);
            fields.add(new JLabel("Salário:"));fields.add(wage);fields.add(new JLabel("Duração:"));fields.add(term);
            JPanel center=new JPanel(new BorderLayout(8,8));center.add(fields,BorderLayout.NORTH);
            message.setEditable(false);message.setLineWrap(true);message.setWrapStyleWord(true);center.add(new JScrollPane(message));
            JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT));buttons.add(send);buttons.add(accept);buttons.add(close);
            add(header,BorderLayout.NORTH);add(center);add(buttons,BorderLayout.SOUTH);
            counter=incoming?initial:ctx.listed?ctx.asking:0;accept.setVisible(counter>0);
            send.setText(incoming?"Enviar contraproposta":"Enviar proposta");
            if(ctx.listed && !incoming){offer.setEnabled(false);send.setVisible(false);accept.setText("Comprar por "+cash(counter));}
            send.addActionListener(e->safely(this::submit));accept.addActionListener(e->safely(this::accept));
            close.addActionListener(e->{if(incoming&&!finished)begin();finish();dialog.dispose();});wage.addChangeListener(e->interest());
            dialog.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){finish();}public void windowClosed(WindowEvent e){finish();}});
            interest();
            if(record[0]>day()){message.setText("Aguarde "+(record[0]-day())+" dias do jogo para negociar novamente.");disable();}
            else message.setText(incoming?buyer.getNome()+" oferece "+cash(initial)+". Aceite, rejeite ou faça uma contraproposta.":"O acordo exige aprovação do clube e do jogador. Limite: três rodadas por etapa.");
        }
        void safely(Runnable action){try{action.run();}catch(Exception e){message.setText("Falha na negociação: "+e.getMessage());disable();e.printStackTrace();}}
        int value(JSpinner input){try{input.commitEdit();}catch(java.text.ParseException e){throw new IllegalArgumentException("Valor inválido.");}return ((Number)input.getValue()).intValue();}
        void interest(){interest.setText("Interesse do jogador: "+interestLabel(NegotiationRules.interest(ctx,((Number)wage.getValue()).intValue(),variation*100)));}
        @Override public void disable(){send.setEnabled(false);accept.setEnabled(false);}
        void begin(){if(!started){started=true;record[0]=day()+COOLDOWN_DAYS;}}
        void finish(){if(started&&!finished){record[0]=day()+COOLDOWN_DAYS;finished=true;}}
        boolean valid(){
            if(finished||(!started&&record[0]>day()))return false;
            if(player.fg()!=seller||!seller.kc().contains(player)||buyer.kc().contains(player)){reject("O jogador mudou de clube.");return false;}
            if(buyer.kc().size()>=30){reject("Limite de 30 jogadores no comprador.");return false;}return true;
        }
        void reject(String text){begin();finished=true;disable();message.setText(text+" Nova tentativa em 14 dias do jogo.");}
        void submit(){
            if(!valid())return;if(contract){salary();return;}
            int amount=value(offer);if(!incoming&&amount>buyer.kb()){message.setText("Dinheiro insuficiente.");return;}
            begin();rounds++;
            Decision decision=incoming?buyerDecision(amount,maximum,rounds):sellerDecision(amount,minimum,rounds);
            if(decision==Decision.REJECT){reject("O clube encerrou a negociação sem acordo.");return;}
            if(decision==Decision.ACCEPT){agreement(amount);return;}
            counter=incoming?maximum:minimum;accept.setVisible(true);accept.setText("Aceitar "+cash(counter));
            message.setText((incoming?buyer:seller).getNome()+" propõe "+cash(counter)+". Rodada "+rounds+"/3.");
        }
        void accept(){if(!valid()||counter<=0)return;begin();if(contract){wage.setValue(Math.min(100000000,counter));salary();}else agreement(counter);}
        void agreement(int price){
            if(price>buyer.kb()){reject("O comprador não tem saldo suficiente.");return;}
            if(Boolean.TRUE.equals(seller.jZ())&&!incoming&&JOptionPane.showConfirmDialog(dialog,seller.getNome()+": aceitar "+cash(price)+"?","Acordo entre clubes",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION){reject("O vendedor recusou.");return;}
            agreed=price;double level=NegotiationRules.interest(ctx,ctx.salary,variation*100);
            if(level<15){reject("O jogador não deseja atuar no clube interessado.");return;}
            if(incoming){int salary=salaryMinimum(ctx,level);if(NegotiationRules.interest(ctx,salary,variation*100)<35){reject("O jogador recusou o contrato oferecido pelo comprador.");return;}complete(salary,365);return;}
            contract=true;counter=0;accept.setVisible(false);send.setVisible(true);send.setText("Propor contrato");offer.setEnabled(false);wage.setEnabled(true);term.setEnabled(true);
            message.setText("Clubes de acordo por "+cash(price)+". Negocie o contrato com o jogador.");
        }
        void salary(){
            int amount=value(wage);salaryRounds++;int days=new int[]{180,365,730,1095}[term.getSelectedIndex()];
            int wanted=salaryMinimum(ctx,NegotiationRules.interest(ctx,ctx.salary,variation*100));
            if(ctx.age>=34&&days>365)wanted=money(wanted*1.15);
            if(amount>=wanted&&NegotiationRules.interest(ctx,amount,variation*100)>=35){complete(amount,days);return;}
            if(salaryRounds>=MAX_ROUNDS){reject("O jogador encerrou a negociação salarial.");return;}
            counter=money(Math.max(wanted,amount*1.1));if(NegotiationRules.interest(ctx,counter,variation*100)<35)counter=money(Math.max(counter,ctx.salary*1.6));
            message.setText("O jogador pede "+cash(counter)+" pelo prazo selecionado. Rodada salarial "+salaryRounds+"/3.");accept.setText("Aceitar salário");accept.setVisible(true);
        }
        void complete(int salary,int days){
            if(!valid())return;if(agreed<=0||agreed>buyer.kb()){reject("Saldo insuficiente na conclusão.");return;}
            player.a(buyer,agreed,false,false,false);
            // Native transfer finances apply only to human clubs. Account for AI clubs once.
            if(!Boolean.TRUE.equals(buyer.jZ()))buyer.w(agreed,1);
            if(!Boolean.TRUE.equals(seller.jZ()))seller.v(agreed,1);
            player.ae(salary);player.a((long)days,true);best.l.l(true);
            if(host!=null)field(host,"Nv",true);
            finished=true;disable();close.setText("Fechar");message.setText("Transferência concluída por "+cash(agreed)+". Salário: "+cash(salary)+". Contrato: "+days+" dias.");
        }
    }
}
