package me.eigenraven.lwjgl3ify.rfb.transformers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.gtnewhorizons.retrofuturabootstrap.api.ClassNodeHandle;
import com.gtnewhorizons.retrofuturabootstrap.api.ExtensibleClassLoader;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;

import me.eigenraven.lwjgl3ify.IExtensibleEnum;
import me.eigenraven.lwjgl3ify.api.MakeEnumExtensible;
import me.eigenraven.lwjgl3ify.rfb.EarlyConfig;

public class ExtensibleEnumTransformer implements RfbClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("lwjgl3ify");
    private static final Type MARKER_IFACE = Type.getType("Lme/eigenraven/lwjgl3ify/IExtensibleEnum;");
    private static final Type MARKER_ANNOTATION = Type.getType("Lme/eigenraven/lwjgl3ify.api/MakeEnumExtensible;");
    private static final Type VALUES_MARKER_ANNOTATION = Type.getType("Lme/eigenraven/lwjgl3ify/EnumValuesField;");
    private static final Type LOOKUP = Type.getType(MethodHandles.Lookup.class);
    private static final Type METHOD_HANDLE = Type.getType(MethodHandle.class);

    @Pattern("[a-z0-9-]+")
    @Override
    public @NotNull String id() {
        return "extensible-enum";
    }

    @Override
    public boolean shouldTransformClass(@NotNull ExtensibleClassLoader extensibleClassLoader,
        @NotNull RfbClassTransformer.Context context, @Nullable Manifest manifest, @NotNull String className,
        @NotNull ClassNodeHandle classNodeHandle) {
        return classNodeHandle.getFastAccessor() != null && classNodeHandle.getFastAccessor()
            .isEnum();
    }

    @Override
    public void transformClass(@NotNull ExtensibleClassLoader extensibleClassLoader,
        @NotNull RfbClassTransformer.Context context, @Nullable Manifest manifest, @NotNull String className,
        @NotNull ClassNodeHandle classNodeHandle) {
        final Type classType = Type.getObjectType(className.replace('.', '/'));
        final ClassNode classNode = classNodeHandle.getNode();
        if (classNode == null) {
            return;
        }

        boolean process = false;

        if(classNode.interfaces.contains(MARKER_IFACE.getInternalName())) {
            process = true;
        } else if (EarlyConfig.EXTENSIBLE_ENUMS.contains(className)) {
            addInterface(classNode);
            process = true;
        } else if (classNode.visibleAnnotations != null && !classNode.visibleAnnotations.isEmpty()) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) {
                if (annotation.desc.equals(MARKER_ANNOTATION.getDescriptor())) {
                    process = true;
                    addInterface(classNode);
                    break;
                }
            }
        }

        if (!process) {
            return;
        }

        Type array = Type.getType("[" + classType.getDescriptor());
        final int flags = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;

        final FieldNode values = classNode.fields.stream()
            .filter(f -> f.desc.contentEquals(array.getDescriptor()) && ((f.access & flags) == flags))
            .findFirst()
            .orElse(
                classNode.fields.stream()
                    .filter(f -> f.desc.contentEquals(array.getDescriptor()) && (f.name.equals("$VALUES") || f.name.equals("ENUM$VALUES"))) // ecj and javac do different things
                    .findFirst()
                    .orElse(null));

        // Make values public and non-final, mark it with an annotation
        if (values != null) {
            values.access &= ~Opcodes.ACC_FINAL & ~Opcodes.ACC_PRIVATE;
            values.access |= Opcodes.ACC_PUBLIC;

            values.visitAnnotation(VALUES_MARKER_ANNOTATION.getDescriptor(), true).visitEnd();
        } else {
            if (LOGGER.isErrorEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Enum marked as extensible but we could not find $VALUES. Found:\n");
                classNode.fields.stream()
                    .filter(f -> (f.access & Opcodes.ACC_STATIC) != 0)
                    .forEach(
                        m -> sb.append("  ")
                            .append(m.name)
                            .append(" ")
                            .append(m.desc)
                            .append("\n"));
                LOGGER.error(sb.toString());
            }
            throw new IllegalStateException("Enum marked as extensible but we could not find $VALUES");
        }

        // Make all constructors public
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("<init>")) {
                methodNode.access &= ~Opcodes.ACC_PRIVATE;
                methodNode.access |= Opcodes.ACC_PUBLIC;
            }
        }
    }

    private void addInterface(@NotNull ClassNode classNode) {
        if (classNode.interfaces == null) {
            classNode.interfaces = new ArrayList<>(1);
        }
        classNode.interfaces.add(MARKER_IFACE.getInternalName());
    }
}
