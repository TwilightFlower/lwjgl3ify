package me.eigenraven.lwjgl3ify.rfb.transformers.mod;

import java.util.Iterator;
import java.util.jar.Manifest;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;
import com.gtnewhorizons.retrofuturabootstrap.api.ExtensibleClassLoader;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;

public class LagGogglesTransformer implements RfbClassTransformer {

    private static final String EVENT_BUS_TRANSFORMER = "com.github.terminatornl.laggoggles.tickcentral.EventBusTransformer";
    private static final String INITIALIZER = "com.github.terminatornl.laggoggles.tickcentral.Initializer";

    @Pattern("[a-z0-9-]+")
    @Override
    public @NotNull String id() {
        return "lag-goggles-fix";
    }

    @Override
    public boolean shouldTransformClass(@NotNull ExtensibleClassLoader classLoader, @NotNull Context context,
        @Nullable Manifest manifest, @NotNull String className, @NotNull ClassNodeHandle classNode) {
        return className.equals(EVENT_BUS_TRANSFORMER) || className.equals(INITIALIZER);
    }

    @Override
    public void transformClass(@NotNull ExtensibleClassLoader classLoader, @NotNull Context context,
        @Nullable Manifest manifest, @NotNull String className, @NotNull ClassNodeHandle classNode) {
        ClassNode node = classNode.getNode();
        if (node != null) {
            if (className.equals(EVENT_BUS_TRANSFORMER)) {
                // LagGoggles loads a class we need to transform in static init in its transformer.
                // We'll do what that static init is supposed to do at a saner time instead.

                Iterator<MethodNode> iter = node.methods.iterator();
                while (iter.hasNext()) {
                    MethodNode method = iter.next();
                    if (method.name.equals("<clinit>")) {
                        iter.remove();
                        break;
                    }
                }

                // De-final this field because we'll be setting it later.
                for (FieldNode field : node.fields) {
                    if (field.name.equals("ownerField")) {
                        field.access &= ~Opcodes.ACC_FINAL;
                        break;
                    }
                }
            } else if (className.equals(INITIALIZER)) {
                // LagGoggles doesn't properly handle isInterface.
                // This method is only called once, so I can just always set it to false.
                InsnList newInsns = new InsnList();
                newInsns.add(new VarInsnNode(Opcodes.ALOAD, 10)); // load the MethodInsnNode
                newInsns.add(new InsnNode(Opcodes.ICONST_0)); // false
                newInsns.add(new FieldInsnNode(Opcodes.PUTFIELD, "org/objectweb/asm/tree/MethodInsnNode", "itf", "Z"));

                for (MethodNode method : node.methods) {
                    if (method.name.equals("convertTargetInstruction")) {
                        for (AbstractInsnNode insn : method.instructions) {
                            if (insn instanceof MethodInsnNode methodInsn && methodInsn.name.equals("setOpcode")) {
                                method.instructions.insert(insn, newInsns);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
