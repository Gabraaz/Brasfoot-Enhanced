import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;

public final class PatchSponsorshipContract {
    private static final String DIALOG = "mods/SponsorshipLauncher$SponsorshipDialog";
    private static final String CONTRACT = "mods/SponsorshipLauncher$Contract";
    private static final String CLUB = "mods/SponsorshipLauncher$Club";
    private static final String DIALOG_ENTRY = DIALOG + ".class";

    private PatchSponsorshipContract() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Uso: PatchSponsorshipContract <arquivo.exe>");
        }
        Path executable = Path.of(args[0]).toAbsolutePath();
        byte[] executableBytes = Files.readAllBytes(executable);
        int zipOffset = findZipOffset(executableBytes);
        if (zipOffset < 0) {
            throw new IllegalStateException("O executável não contém um JAR embutido.");
        }
        byte[] patchedJar = patchJar(executableBytes, zipOffset);
        Path temporary = executable.resolveSibling(executable.getFileName() + ".tmp");
        ByteArrayOutputStream output = new ByteArrayOutputStream(zipOffset + patchedJar.length);
        output.write(executableBytes, 0, zipOffset);
        output.write(patchedJar);
        Files.write(temporary, output.toByteArray());
        Files.move(temporary, executable, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Contrato de patrocínio corrigido em: " + executable);
    }

    private static byte[] patchJar(byte[] executableBytes, int zipOffset) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean patched = false;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(
                executableBytes, zipOffset, executableBytes.length - zipOffset));
             ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                zipOutput.putNextEntry(new ZipEntry(entry.getName()));
                byte[] contents = readAll(input);
                if (DIALOG_ENTRY.equals(entry.getName())) {
                    contents = patchDialog(contents);
                    patched = true;
                }
                zipOutput.write(contents);
                zipOutput.closeEntry();
            }
        }
        if (!patched) {
            throw new IllegalStateException("Classe de patrocínio não encontrada no executável.");
        }
        return output.toByteArray();
    }

    private static byte[] patchDialog(byte[] classBytes) {
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, 0);
        boolean acceptPatched = false;
        boolean refreshPatched = false;
        for (MethodNode method : node.methods) {
            if ("acceptSelected".equals(method.name) && "()V".equals(method.desc)) {
                acceptPatched = patchAcceptanceGuard(method);
            } else if ("refresh".equals(method.name) && "()V".equals(method.desc)) {
                refreshPatched = patchCurrentContractLabel(method);
            }
        }
        if (!acceptPatched || !refreshPatched) {
            throw new IllegalStateException("Estrutura da UI de patrocínio incompatível com este patch.");
        }
        ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean patchAcceptanceGuard(MethodNode method) {
        for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (!(node instanceof JumpInsnNode) || node.getOpcode() != Opcodes.IFLE) {
                continue;
            }
            JumpInsnNode futurePaymentsEmpty = (JumpInsnNode) node;
            if (!isField(previousReal(node), CONTRACT, "remainingSeasons")) {
                continue;
            }
            AbstractInsnNode activeBlockStart = nextReal(node);
            LabelNode activeAgreement = new LabelNode();
            method.instructions.insertBefore(activeBlockStart, activeAgreement);
            LabelNode currentSeasonCheck = new LabelNode();
            InsnList check = new InsnList();
            check.add(currentSeasonCheck);
            check.add(new VarInsnNode(Opcodes.ALOAD, 2));
            check.add(new FieldInsnNode(Opcodes.GETFIELD, CONTRACT, "lastPaidYear", "I"));
            check.add(new VarInsnNode(Opcodes.ALOAD, 0));
            check.add(new FieldInsnNode(Opcodes.GETFIELD, DIALOG, "club", "L" + CLUB + ";"));
            check.add(new FieldInsnNode(Opcodes.GETFIELD, CLUB, "currentYear", "I"));
            check.add(new JumpInsnNode(Opcodes.IF_ICMPLT, futurePaymentsEmpty.label));
            check.add(new JumpInsnNode(Opcodes.GOTO, activeAgreement));
            method.instructions.insert(node, check);
            futurePaymentsEmpty.label = currentSeasonCheck;
            return true;
        }
        return false;
    }

    private static boolean patchCurrentContractLabel(MethodNode method) {
        JumpInsnNode noContract = null;
        JumpInsnNode activeContract = null;
        for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node instanceof JumpInsnNode && node.getOpcode() == Opcodes.IFNULL) {
                noContract = (JumpInsnNode) node;
            } else if (node instanceof JumpInsnNode && node.getOpcode() == Opcodes.IFGT
                    && isField(previousReal(node), CONTRACT, "remainingSeasons")) {
                activeContract = (JumpInsnNode) node;
                break;
            }
        }
        if (noContract == null || activeContract == null) {
            return false;
        }
        LabelNode checkCurrentSeason = new LabelNode();
        InsnList check = new InsnList();
        check.add(checkCurrentSeason);
        check.add(new VarInsnNode(Opcodes.ALOAD, 1));
        check.add(new FieldInsnNode(Opcodes.GETFIELD, CONTRACT, "lastPaidYear", "I"));
        check.add(new VarInsnNode(Opcodes.ALOAD, 0));
        check.add(new FieldInsnNode(Opcodes.GETFIELD, DIALOG, "club", "L" + CLUB + ";"));
        check.add(new FieldInsnNode(Opcodes.GETFIELD, CLUB, "currentYear", "I"));
        check.add(new JumpInsnNode(Opcodes.IF_ICMPLT, noContract.label));
        check.add(new JumpInsnNode(Opcodes.GOTO, activeContract.label));
        method.instructions.insert(activeContract, check);
        return true;
    }

    private static boolean isField(AbstractInsnNode node, String owner, String name) {
        return node instanceof FieldInsnNode
            && ((FieldInsnNode) node).getOpcode() == Opcodes.GETFIELD
            && owner.equals(((FieldInsnNode) node).owner)
            && name.equals(((FieldInsnNode) node).name);
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode node) {
        AbstractInsnNode current = node.getPrevious();
        while (current != null && current.getType() == AbstractInsnNode.LABEL) current = current.getPrevious();
        return current;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode node) {
        AbstractInsnNode current = node.getNext();
        while (current != null && current.getType() == AbstractInsnNode.LABEL) current = current.getNext();
        return current;
    }

    private static int findZipOffset(byte[] bytes) {
        for (int index = 0; index <= bytes.length - 4; index++) {
            if (bytes[index] == 0x50 && bytes[index + 1] == 0x4b
                    && bytes[index + 2] == 0x03 && bytes[index + 3] == 0x04) return index;
        }
        return -1;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static final class SafeClassWriter extends ClassWriter {
        SafeClassWriter(int flags) { super(flags); }
        @Override protected String getCommonSuperClass(String first, String second) {
            return "java/lang/Object";
        }
    }
}


