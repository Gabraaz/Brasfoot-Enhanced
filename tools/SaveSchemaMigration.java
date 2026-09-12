package mods;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import java.io.*;

/** One-time converter for saves written by the short-lived build that added mod fields. */
public final class SaveSchemaMigration {
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Uso: origem destino");
        new best.M(true);
        Object career,auxiliary;
        Kryo reader=new Kryo();reader.setRegistrationRequired(false);
        try(Input input=new Input(new FileInputStream(args[0]))){
            career=reader.readClassAndObject(input);
            auxiliary=reader.readClassAndObject(input);
        }
        Kryo writer=new Kryo();writer.setRegistrationRequired(false);
        omit(writer,best.F.class,"enhancedNegotiations");
        omit(writer,best.ah.class,"enhancedSponsorshipContract");
        try(Output output=new Output(new FileOutputStream(args[1]))){
            writer.writeClassAndObject(output,career);
            writer.writeClassAndObject(output,auxiliary);
        }
        System.out.println("Save convertido: "+args[1]);
    }
    private static void omit(Kryo kryo,Class<?> type,String field){
        Registration registration=kryo.getRegistration(type);
        FieldSerializer serializer=(FieldSerializer)registration.getSerializer();
        for(FieldSerializer.CachedField cached:serializer.getFields())if(cached.getField().getName().equals(field)){
            serializer.removeField(cached);return;
        }
    }
    private SaveSchemaMigration(){}
}
