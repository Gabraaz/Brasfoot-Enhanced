package mods;

import best.ah;
import java.lang.reflect.*;

/** Verifies round persistence and isolation between two saves controlling the same club. */
public final class SponsorshipStateTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Object wrap(ah raw)throws Exception{Class<?> t=Class.forName("mods.SponsorshipLauncher$Club");Constructor<?> c=t.getDeclaredConstructor(Object.class);c.setAccessible(true);return c.newInstance(raw);}
    static Object contract(String brand,long annual,int original,int remaining,int paid)throws Exception{Class<?> t=Class.forName("mods.SponsorshipLauncher$Contract");Constructor<?> c=t.getDeclaredConstructor(String.class,long.class,int.class,int.class,int.class);c.setAccessible(true);return c.newInstance(brand,annual,original,remaining,paid);}
    static Object store(String method,Class<?>[] types,Object...args)throws Exception{Class<?> t=Class.forName("mods.SponsorshipLauncher$Store");Method m=t.getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(null,args);}
    static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static ah club(){ah raw=new ah();raw.bX(139);raw.e(10000);raw.setReputacao(3);raw.setNivel(3);TransferNegotiation.field(raw,"mV","São Paulo");return raw;}
    public static void main(String[]args)throws Exception{
        System.setProperty("user.dir",java.nio.file.Files.createTempDirectory("brasfoot-sponsor-test-").toString());
        c.a.SR=new best.f();c.a.SR.k(1);c.a.SR.bs().setTc("carreira-antiga");ah oldRaw=club();Object oldClub=wrap(oldRaw),signed=contract("Spotify",1000,2,1,1);Class<?> clubType=oldClub.getClass(),contractType=signed.getClass();
        store("save",new Class<?>[]{clubType,contractType},oldClub,signed);
        Object nextRound=store("contractFor",new Class<?>[]{clubType},wrap(oldRaw));check(nextRound!=null&&get(nextRound,"brand").equals("Spotify"),"Same save keeps contract across rounds");
        c.a.SR.bs().setI("Outra partida - 2ª rodada");check(store("contractFor",new Class<?>[]{clubType},wrap(oldRaw))!=null,"Fixture text is ignored");
        c.a.SR.bs().setTc("carreira-nova");ah newRaw=club();check(store("contractFor",new Class<?>[]{clubType},wrap(newRaw))==null,"New save starts without sponsor");
        c.a.SR.bs().setTc("carreira-antiga");long before=oldRaw.kb();store("settleNewSeasons",new Class<?>[]{clubType},wrap(oldRaw));check(oldRaw.kb()==before,"No duplicate same-season payment");
        c.a.SR.k(2);store("settleNewSeasons",new Class<?>[]{clubType},wrap(oldRaw));Object renewed=store("contractFor",new Class<?>[]{clubType},wrap(oldRaw));
        check(oldRaw.kb()==before+1000&&(Integer)get(renewed,"remainingSeasons")==0,"One payment in next season");
        System.out.println("Sponsorship: round persistence, new-save isolation and annual settlement passed.");
    }
}
