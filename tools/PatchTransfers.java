import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;

/** Builds a separate executable; never edits the input executable. */
public final class PatchTransfers {
    public static void main(String[] args) throws Exception {
        Path input=Path.of(args[0]), classes=Path.of(args[1]), output=Path.of(args[2]);
        if(input.toAbsolutePath().equals(output.toAbsolutePath()))throw new IllegalArgumentException("Use uma saída separada");
        byte[] bytes=Files.readAllBytes(input);int offset=-1;
        for(int i=0;i<bytes.length-4;i++)if(bytes[i]==80&&bytes[i+1]==75&&bytes[i+2]==3&&bytes[i+3]==4){offset=i;break;}
        if(offset<0)throw new IOException("JAR ausente");
        Map<String,byte[]> entries=new LinkedHashMap<>();
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes,offset,bytes.length-offset))){
            ZipEntry e;while((e=zip.getNextEntry())!=null)entries.put(e.getName(),zip.readAllBytes());
        }
        int hooks=0;
        int storePatches=0;
        for(String name:new String[]{"a/iA","a/cz","a/jm","best/F","best/ah","mods/SponsorshipLauncher$Store"}) {
            ClassNode node=new ClassNode();new ClassReader(entries.get(name+".class")).accept(node,0);
            if(name.equals("best/F")) {
                node.fields.removeIf(f->f.name.equals("enhancedNegotiations"));
            } else if(name.equals("best/ah")) {
                node.fields.removeIf(f->f.name.equals("enhancedSponsorshipContract"));
            } else if(name.equals("mods/SponsorshipLauncher$Store")) {
                for(MethodNode m:node.methods) {
                    String target=m.name.equals("contractFor")?"contractFor":m.name.equals("save")?"save":m.name.equals("settleNewSeasons")?"settleNewSeasons":null;
                    if(target==null)continue;
                    m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
                    m.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
                    if(target.equals("save"))m.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
                    String desc=target.equals("contractFor")?"(Ljava/lang/Object;)Ljava/lang/Object;":target.equals("save")?"(Ljava/lang/Object;Ljava/lang/Object;)V":"(Ljava/lang/Object;)V";
                    m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"mods/SponsorshipState",target,desc,false));
                    if(target.equals("contractFor"))m.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST,"mods/SponsorshipLauncher$Contract"));
                    m.instructions.add(new InsnNode(target.equals("contractFor")?Opcodes.ARETURN:Opcodes.RETURN));storePatches++;
                }
            } else for(MethodNode m:node.methods) {
                // Rebuilds replace previous hooks rather than stacking another invocation.
                for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals("mods/TransferNegotiation")){
                    AbstractInsnNode previous=i.getPrevious();
                    if(previous instanceof VarInsnNode&&previous.getOpcode()==Opcodes.ALOAD&&((VarInsnNode)previous).var==0){m.instructions.remove(previous);m.instructions.remove(i);}
                }
                if(name.equals("a/iA")&&(m.name.equals("oM")||m.name.equals("sA"))&&m.desc.equals("()V")) {
                    m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
                    m.instructions.add(call("openMarket"));m.instructions.add(new InsnNode(Opcodes.RETURN));hooks++;
                } else {
                    String hook=name.equals("a/iA")&&(m.name.equals("sG")||m.name.equals("<init>"))?"refreshMarket":
                        name.equals("a/cz")&&m.name.equals("<init>")?"installOffer":
                        name.equals("a/jm")&&m.name.equals("<init>")?"installIncoming":null;
                    if(hook!=null){for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==Opcodes.RETURN)m.instructions.insertBefore(i,call(hook));hooks++;}
                }
            }
            ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);node.accept(writer);entries.put(name+".class",writer.toByteArray());
        }
        if(hooks!=6)throw new IllegalStateException("Estrutura inesperada: "+hooks+" hooks (esperado 6)");
        if(storePatches!=3)throw new IllegalStateException("Estrutura inesperada do patrocínio: "+storePatches+" métodos");
        try(var paths=Files.walk(classes.resolve("mods"))){for(Path file:paths.filter(p->p.toString().endsWith(".class")&&!p.getFileName().toString().contains("Test")).toList())
            entries.put(classes.relativize(file).toString().replace('\\','/'),Files.readAllBytes(file));}
        ByteArrayOutputStream result=new ByteArrayOutputStream();result.write(bytes,0,offset);
        try(ZipOutputStream zip=new ZipOutputStream(result)){for(var e:entries.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}
        Files.write(output,result.toByteArray(),StandardOpenOption.CREATE_NEW);
        System.out.println("Executável de teste: "+output);
    }
    private static InsnList call(String hook){InsnList ins=new InsnList();ins.add(new VarInsnNode(Opcodes.ALOAD,0));ins.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"mods/TransferNegotiation",hook,"(Ljava/lang/Object;)V",false));return ins;}
}
