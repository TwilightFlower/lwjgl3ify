package me.eigenraven.lwjgl3ify.mixins.early.fml;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import scala.tools.asm.Opcodes;

@Mixin(value = net.minecraftforge.fml.common.eventhandler.ASMEventHandler.class, remap = false)
public class ASMEventHandler {

    @ModifyConstant(
        method = "createWrapper(Ljava/lang/reflect/Method;)Ljava/lang/Class;",
        constant = @Constant(intValue = Opcodes.V1_6),
        remap = false)
    private int useClassVersion52(int originalValue) {
        return Opcodes.V1_8;
    }

    @ModifyConstant(
        method = "createWrapper(Ljava/lang/reflect/Method;)Ljava/lang/Class;",
        slice = @Slice(
            from = @At(
                value = "INVOKE:LAST",
                target = "org/objectweb/asm/MethodVisitor.visitTypeInsn(ILjava/lang/String;)V")),
        constant = @Constant(intValue = 0),
        remap = false)
    private int fixMethodRefType(int originalValue, Method callback) {
        return callback.getDeclaringClass()
            .isInterface() ? 1 : 0;
    }

    // LagGoggles fix
    @Inject(
        method = "<clinit>",
        at = @At("TAIL"),
        remap = false)
    private static void loadLagGogglesField(CallbackInfo info) {
        Field ownerFieldField;
        try {
            Class<?> transformerClass = Class.forName("com.github.terminatornl.laggoggles.tickcentral.EventBusTransformer");
            ownerFieldField = transformerClass.getDeclaredField("ownerField");
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            // LagGoggles isn't installed, or updated to remove this field. We don't care.
            return;
        }

        try {
            Field ownerField = net.minecraftforge.fml.common.eventhandler.ASMEventHandler.class.getDeclaredField("owner");
            ownerField.setAccessible(true);
            ownerFieldField.set(null, ownerField);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
