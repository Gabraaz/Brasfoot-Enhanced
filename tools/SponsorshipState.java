package mods;

import java.io.*;
import java.lang.reflect.*;
import java.util.Properties;

/** Persists sponsorship in the serialized club, independent of fixture/round labels. */
public final class SponsorshipState {
    private static final String FIELD="enhancedSponsorshipContract";
    public static Object contractFor(Object club) {
        Object raw=get(club,"raw"); Object contract=parse((String)get(raw,FIELD));
        if(contract!=null)return contract;
        contract=migrateLegacy(club);if(contract!=null)save(club,contract);return contract;
    }
    public static void save(Object club,Object contract) {
        Object raw=get(club,"raw");set(raw,FIELD,contract==null?null:(String)call(contract,"encode",new Class<?>[0]));
    }
    public static void settleNewSeasons(Object club) {
        Object contract=contractFor(club);if(contract==null)return;
        int remaining=(Integer)get(contract,"remainingSeasons"),year=(Integer)get(club,"currentYear"),paid=(Integer)get(contract,"lastPaidYear");
        if(remaining<=0||year<=paid)return;int seasons=Math.min(remaining,year-paid);if(seasons<=0)return;
        long annual=(Long)get(contract,"annualValue");call(club,"addMoney",new Class<?>[]{long.class},annual*seasons);
        set(contract,"remainingSeasons",remaining-seasons);set(contract,"lastPaidYear",paid+seasons);save(club,contract);
    }
    private static Object migrateLegacy(Object club) {
        File file=new File(new File(System.getProperty("user.dir"),"mods"),"patrocinios.properties");
        if(!file.isFile())return null;Properties properties=new Properties();
        try(InputStream input=new FileInputStream(file)){properties.load(input);}catch(IOException e){return null;}
        int id=(Integer)get(club,"id"),year=(Integer)get(club,"currentYear");Object selected=null;
        for(String key:properties.stringPropertyNames()){
            if(!key.endsWith("#"+id))continue;Object candidate=parse(properties.getProperty(key));if(candidate==null)continue;
            boolean active=(Integer)get(candidate,"remainingSeasons")>0||(Integer)get(candidate,"lastPaidYear")>=year;
            boolean selectedActive=selected!=null&&((Integer)get(selected,"remainingSeasons")>0||(Integer)get(selected,"lastPaidYear")>=year);
            if(selected==null||(active&&!selectedActive)||active==selectedActive&&(Integer)get(candidate,"lastPaidYear")>(Integer)get(selected,"lastPaidYear"))selected=candidate;
        }
        return selected;
    }
    private static Object parse(String value){
        if(value==null)return null;
        try{Class<?> type=Class.forName("mods.SponsorshipLauncher$Contract");Method m=type.getDeclaredMethod("parse",String.class);m.setAccessible(true);return m.invoke(null,value);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Contrato de patrocínio incompatível",e);}
    }
    private static Object get(Object owner,String name){try{Field f=findField(owner.getClass(),name);f.setAccessible(true);return f.get(owner);}catch(ReflectiveOperationException e){throw new IllegalStateException("Campo incompatível: "+name,e);}}
    private static void set(Object owner,String name,Object value){try{Field f=findField(owner.getClass(),name);f.setAccessible(true);f.set(owner,value);}catch(ReflectiveOperationException e){throw new IllegalStateException("Campo incompatível: "+name,e);}}
    private static Field findField(Class<?> type,String name)throws NoSuchFieldException{for(Class<?> t=type;t!=null;t=t.getSuperclass())try{return t.getDeclaredField(name);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static Object call(Object owner,String name,Class<?>[] types,Object...args){try{Method m=owner.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(owner,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Método incompatível: "+name,e);}}
    private SponsorshipState(){}
}
