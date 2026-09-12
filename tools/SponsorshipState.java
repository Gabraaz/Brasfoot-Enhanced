package mods;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Properties;

/** Stores contracts by stable save name and club id, without changing serialized game classes. */
public final class SponsorshipState {
    public static synchronized Object contractFor(Object club) {
        return parse(load().getProperty(key(club)));
    }
    public static synchronized void save(Object club,Object contract) {
        Properties properties=load();
        if(contract==null)properties.remove(key(club));else properties.setProperty(key(club),(String)call(contract,"encode",new Class<?>[0]));
        write(properties);
    }
    public static synchronized void settleNewSeasons(Object club) {
        Object contract=contractFor(club);if(contract==null)return;
        int remaining=(Integer)get(contract,"remainingSeasons"),year=(Integer)get(club,"currentYear"),paid=(Integer)get(contract,"lastPaidYear");
        if(remaining<=0||year<=paid)return;int seasons=Math.min(remaining,year-paid);if(seasons<=0)return;
        call(club,"addMoney",new Class<?>[]{long.class},(Long)get(contract,"annualValue")*seasons);
        set(contract,"remainingSeasons",remaining-seasons);set(contract,"lastPaidYear",paid+seasons);save(club,contract);
    }
    static String saveKey(){
        try{Object info=call(c.a.SR,"bs",new Class<?>[0]);String name=String.valueOf(call(info,"getTc",new Class<?>[0]));
            return name==null||name.equals("null")||name.trim().isEmpty()?"carreira-padrao":name.trim();}
        catch(RuntimeException e){return "carreira-padrao";}
    }
    static String key(Object club){return "save:"+saveKey()+"#club:"+get(club,"id");}
    static File file(){return new File(new File(System.getProperty("user.dir"),"mods"),"patrocinios.properties");}
    static Properties load(){Properties p=new Properties();File f=file();if(!f.isFile())return p;try(InputStream in=new FileInputStream(f)){p.load(in);}catch(IOException ignored){}return p;}
    static void write(Properties p){
        File f=file(),parent=f.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IllegalStateException("Não foi possível criar a pasta mods");
        Path temp=new File(parent,"patrocinios.tmp").toPath();
        try(OutputStream out=Files.newOutputStream(temp)){p.store(out,"Brasfoot sponsorship contracts by save");}
        catch(IOException e){throw new IllegalStateException("Não foi possível salvar o patrocínio",e);}
        try{Files.move(temp,f.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){try{Files.move(temp,f.toPath(),StandardCopyOption.REPLACE_EXISTING);}catch(IOException x){throw new IllegalStateException("Não foi possível salvar o patrocínio",x);}}
        catch(IOException e){throw new IllegalStateException("Não foi possível salvar o patrocínio",e);}
    }
    private static Object parse(String value){if(value==null)return null;try{Class<?> t=Class.forName("mods.SponsorshipLauncher$Contract");Method m=t.getDeclaredMethod("parse",String.class);m.setAccessible(true);return m.invoke(null,value);}catch(ReflectiveOperationException e){throw new IllegalStateException("Contrato incompatível",e);}}
    private static Object get(Object o,String n){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new IllegalStateException("Campo incompatível: "+n,e);}}
    private static void set(Object o,String n,Object v){try{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}catch(ReflectiveOperationException e){throw new IllegalStateException("Campo incompatível: "+n,e);}}
    private static Object call(Object o,String n,Class<?>[]t,Object...a){try{Method m=o.getClass().getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(o,a);}catch(ReflectiveOperationException e){throw new IllegalStateException("Método incompatível: "+n,e);}}
    private SponsorshipState(){}
}
