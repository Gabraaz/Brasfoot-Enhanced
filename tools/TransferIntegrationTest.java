package mods;

import best.F;
import best.ah;
import java.io.*;
import javax.swing.*;

/** Exercises patched classes with synthetic career data; never opens or writes user saves. */
public final class TransferIntegrationTest {
    static int playerSequence;
    static void layout(java.awt.Container c){c.doLayout();for(java.awt.Component child:c.getComponents())if(child instanceof java.awt.Container)layout((java.awt.Container)child);}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static ah club(int id,boolean human){
        ah c=new ah();c.bX(id);c.k(human);c.e(100000000L);c.setReputacao(3);c.setDivisao(1);
        TransferNegotiation.field(c,"mV","Clube "+id);return c;
    }
    static F player(ah seller){
        F p=new F();p.setNome("Jogador de teste");p.setIdade(26);p.setPosicao(2);p.ad(60);p.ae(100000);p.af(5000000);p.ag(5000000);
        TransferNegotiation.field(p,"ei",++playerSequence);TransferNegotiation.field(p,"ej",0);
        p.n(seller);p.a(365L,true);seller.kc().add(p);return p;
    }
    static void scenario() throws Exception {
        System.setProperty("user.dir",java.nio.file.Files.createTempDirectory("brasfoot-transfer-test-").toString());
        c.a.SR=new best.f();c.a.SR.l(0);c.a.SR.R().add(new best.a());
        // Loading verifies every modified class, even where UI fixtures are not needed.
        for(String name:new String[]{"a.iA","a.cz","a.jm","best.F","best.ah"})Class.forName(name);
        check(java.util.Arrays.stream(F.class.getDeclaredFields()).noneMatch(f->f.getName().equals("enhancedNegotiations")),"Player class layout must remain save-compatible");
        check(java.util.Arrays.stream(ah.class.getDeclaredFields()).noneMatch(f->f.getName().equals("enhancedSponsorshipContract")),"Club class layout must remain save-compatible");
        ah seller=club(1,false),buyer=club(2,true);F p=player(seller);
        JDialog d=new JDialog();
        try {
            TransferNegotiation.Panel panel=new TransferNegotiation.Panel(d,p,buyer,false,0,null);
            panel.setSize(690,510);layout(panel);
            java.awt.image.BufferedImage preview=new java.awt.image.BufferedImage(690,510,java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics=preview.createGraphics();panel.paint(graphics);graphics.dispose();
            javax.imageio.ImageIO.write(preview,"png",new File("tools/transfer-analysis/negotiation-preview.png"));
            panel.offer.setValue(panel.minimum);panel.submit();
            check(panel.contract,"Club agreement should open salary stage");
            check(p.fg()==seller && buyer.kb()==100000000,"No transfer before contract agreement");
            panel.wage.setValue(300000);panel.term.setSelectedIndex(2);panel.submit();
            check(p.fg()==buyer && !seller.kc().contains(p) && buyer.kc().contains(p),"Ownership and roster");
            check(buyer.kb()==100000000L-panel.minimum && seller.kb()==100000000L+panel.minimum,"Exactly one debit and credit");
            check(p.fj()==300000 && p.fR()>=729 && p.fR()<=730,"Salary and duration");
            long balance=buyer.kb();panel.submit();panel.accept();check(balance==buyer.kb(),"No double purchase");
            check(c.a.SR.bo().size()==1,"One native transfer record");
        } finally {d.dispose();}
        F blocked=player(seller);JDialog second=new JDialog();
        try {
            TransferNegotiation.Panel panel=new TransferNegotiation.Panel(second,blocked,buyer,false,0,null);
            panel.offer.setValue(1);
            for(int attempt=0;attempt<4&&!panel.finished;attempt++)panel.submit();
            check(panel.finished,"Persistently low offers eventually reject");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(blocked);}
            F restored;try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){restored=(F)in.readObject();}
            check(TransferNegotiation.state(restored,seller,buyer)[0]>TransferNegotiation.day(),"Cooldown survives save reload");
            TransferNegotiation.Panel reopen=new TransferNegotiation.Panel(second,blocked,buyer,false,0,null);
            check(!reopen.send.isEnabled(),"Closing dialog does not reset cooldown");
            c.a.SR.bb().add(java.util.Calendar.DAY_OF_MONTH,14);
            TransferNegotiation.Panel expired=new TransferNegotiation.Panel(second,blocked,buyer,false,0,null);
            check(expired.send.isEnabled(),"Cooldown expires in game days");
        }finally{second.dispose();}
        ah ai=club(3,false);F sale=player(buyer);JDialog third=new JDialog();
        try {
            TransferNegotiation.Panel incoming=new TransferNegotiation.Panel(third,sale,ai,true,5000000,null);
            incoming.offer.setValue(incoming.maximum);incoming.submit();
            if(sale.fg()!=ai)incoming.accept();
            check(sale.fg()==ai,"Incoming counterproposal transfers to AI");
        }finally{third.dispose();}
        System.out.println("Integration: outgoing, incoming, finances, contracts, duplicate prevention and serialized cooldown passed.");
    }
    public static void main(String[] args)throws Exception{
        final Throwable[] error={null};SwingUtilities.invokeAndWait(()->{try{scenario();}catch(Throwable t){error[0]=t;}});
        if(error[0]!=null){error[0].printStackTrace();System.exit(1);}System.exit(0);
    }
}
