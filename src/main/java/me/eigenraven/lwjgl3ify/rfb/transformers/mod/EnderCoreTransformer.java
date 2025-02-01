package me.eigenraven.lwjgl3ify.rfb.transformers.mod;

import java.util.jar.Manifest;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;
import com.gtnewhorizons.retrofuturabootstrap.api.ExtensibleClassLoader;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;

public class EnderCoreTransformer implements RfbClassTransformer {

    @Pattern("[a-z0-9-]+")
    @Override
    public @NotNull String id() {
        return "endercore-fix";
    }

    @Override
    public boolean shouldTransformClass(@NotNull ExtensibleClassLoader classLoader, @NotNull Context context,
        @Nullable Manifest manifest, @NotNull String className, @NotNull ClassNodeHandle classNode) {
        return className.equals("com.enderio.core.common.transform.EnderCoreTransformer");
    }

    @Override
    public void transformClass(@NotNull ExtensibleClassLoader classLoader, @NotNull Context context,
        @Nullable Manifest manifest, @NotNull String className, @NotNull ClassNodeHandle classNode) {
        ClassNode node = classNode.getNode();
        if (node != null) {
            // EnderCore's transformer visits a method on a ClassWriter before calling visit on it.
            // Modern ASM does not like this, so we need to patch their transformer.

            InsnList newInsns = new InsnList();
            newInsns.add(new InsnNode(Opcodes.DUP)); // dup the ClassWriter since it was just constructed
            newInsns.add(new VarInsnNode(Opcodes.ALOAD, 5)); // grab the ClassReader
            newInsns.add(new InsnNode(Opcodes.SWAP)); // swap because the method we need to call is on ClassReader
            newInsns.add(new InsnNode(Opcodes.ICONST_0)); // 0 flags
            newInsns.add(
                new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "org/objectweb/asm/ClassReader",
                    "accept",
                    "(Lorg/objectweb/asm/ClassVisitor;I)V)"));

            // insns to clean up the stack
            InsnList acceptReplacement = new InsnList();
            acceptReplacement.add(new InsnNode(Opcodes.POP));
            acceptReplacement.add(new InsnNode(Opcodes.POP));
            acceptReplacement.add(new InsnNode(Opcodes.POP));

            boolean removeAccept = false;

            for (MethodNode method : node.methods) {
                if (method.name.equals("transform")
                    && method.desc.equals("(Ljava/lang/String;Ljava/lang/String;[B)[B")) {
                    for (AbstractInsnNode insn : method.instructions) {
                        if (insn instanceof MethodInsnNode methodInsn) {
                            if (methodInsn.name.equals("<init>")
                                && methodInsn.desc.equals("(Lorg/objectweb/asm/ClassReader;I)V")) {
                                // Inject just after the ClassReader's construction
                                method.instructions.insert(methodInsn, newInsns);
                                removeAccept = true;
                            } else if (removeAccept && methodInsn.name.equals("accept")) {
                                method.instructions.insert(methodInsn, acceptReplacement);
                                method.instructions.remove(methodInsn);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
}
