package mods;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import java.io.FileInputStream;

/** Read-only probe for real Brasfoot saves. It never initializes the game UI or writes the save. */
public final class SaveCompatibilityTest {
    public static void main(String[] args) throws Exception {
        if(args.length==0)throw new IllegalArgumentException("Informe ao menos um arquivo .s22 ou .sbck");
        new best.M(true);
        for(String path:args){
            Kryo kryo=new Kryo();kryo.setRegistrationRequired(false);
            try(Input input=new Input(new FileInputStream(path))){
                Object career=kryo.readClassAndObject(input);
                Object auxiliary=kryo.readClassAndObject(input);
                if(!(career instanceof best.f)||!(auxiliary instanceof best.ay))throw new AssertionError("Tipos inesperados em "+path);
            }
            System.out.println("Save compatível: "+path);
        }
    }
    private SaveCompatibilityTest(){}
}
