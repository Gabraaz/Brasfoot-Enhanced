package mods;

import best.ah;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Properties;

/** Verifies that sponsorship follows the serialized career club across rounds. */
public final class SponsorshipStateTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Object wrap(ah raw)throws Exception{Class<?> t=Class.forName("mods.SponsorshipLauncher$Club");Constructor<?> c=t.getDeclaredConstructor(Object.class);c.setAccessible(true);return c.newInstance(raw);}
    static Object contract(String brand,long annual,int original,int remaining,int paid)throws Exception{Class<?> t=Class.forName("mods.SponsorshipLauncher$Contract");Constructor<?> c=t.getDeclaredConstructor(String.class,long.class,int.class,int.class,int.class);c.setAccessible(true);return c.newInstance(brand,annual,original,remaining,paid);}
    static Object store(String method,Class<?>[] types,Object...args)throws Exception{Class<?> t=Class.forName("mods.SponsorshipLauncher$Store");Method m=t.getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(null,args);}
    static Object get(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static ah copy(ah club)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();try(ObjectOutputStream o=new ObjectOutputStream(b)){o.writeObject(club);}try(ObjectInputStream i=new ObjectInputStream(new ByteArrayInputStream(b.toByteArray()))){return(ah)i.readObject();}}
    public static void main(String[]args)throws Exception{
        c.a.SR=new best.f();c.a.SR.k(1);ah raw=new ah();raw.bX(139);raw.e(10000);raw.setReputacao(3);raw.setNivel(3);TransferNegotiation.field(raw,"mV","São Paulo");
        String originalDirectory=System.getProperty("user.dir");Path legacyRoot=Files.createTempDirectory("brasfoot-sponsor-test-");Files.createDirectories(legacyRoot.resolve("mods"));
        Properties legacy=new Properties();legacy.setProperty("São Paulo-C - Flamengo - 1ª rodada#777","Emirates|2000|2|1|1");
        try(OutputStream output=Files.newOutputStream(legacyRoot.resolve("mods/patrocinios.properties"))){legacy.store(output,"legacy fixture-dependent key");}
        System.setProperty("user.dir",legacyRoot.toString());
        ah legacyRaw=new ah();legacyRaw.bX(777);legacyRaw.e(10000);legacyRaw.setReputacao(3);legacyRaw.setNivel(3);TransferNegotiation.field(legacyRaw,"mV","São Paulo");
        Object migrated=store("contractFor",new Class<?>[]{wrap(legacyRaw).getClass()},wrap(legacyRaw));
        check(migrated!=null&&get(migrated,"brand").equals("Emirates")&&TransferNegotiation.field(legacyRaw,"enhancedSponsorshipContract")!=null,"Legacy round key migrates into club");
        System.setProperty("user.dir",originalDirectory);
        Object club=wrap(raw),signed=contract("Spotify",1000,2,1,1);Class<?> clubType=club.getClass(),contractType=signed.getClass();
        store("save",new Class<?>[]{clubType,contractType},club,signed);check(TransferNegotiation.field(raw,"enhancedSponsorshipContract")!=null,"Stored in club");
        ah loaded=copy(raw);Object roundTwo=wrap(loaded);Object current=store("contractFor",new Class<?>[]{clubType},roundTwo);
        check(current!=null&&get(current,"brand").equals("Spotify"),"Survives fixture and save reload");
        long before=loaded.kb();store("settleNewSeasons",new Class<?>[]{clubType},roundTwo);check(loaded.kb()==before,"No same-season payment");
        c.a.SR.k(2);store("settleNewSeasons",new Class<?>[]{clubType},wrap(loaded));Object renewed=store("contractFor",new Class<?>[]{clubType},wrap(loaded));
        check(loaded.kb()==before+1000&&(Integer)get(renewed,"remainingSeasons")==0&&(Integer)get(renewed,"lastPaidYear")==2,"One annual payment");
        System.out.println("Sponsorship: persistent contract, same-round guard and annual settlement passed.");
    }
}
