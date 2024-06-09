package me.eigenraven.lwjgl3ify.mixins.fml;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.google.common.base.Throwables;

@Mixin(targets = { "net.minecraftforge.registries.ObjectHolderRef$FinalFieldHelper" }, remap = false)
public class ObjectHolderRef {

    /**
     * @author eigenraven
     * @reason Simple helper function
     */
    @Overwrite(remap = false)
    static Field makeWritable(Field f) {
        try {
            f.setAccessible(true);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return f;
    }

    /**
     * @author TwilightFlower
     * @reason avoid reflection on modifiers field
     */
    @Overwrite(remap = false)
    static void setField(Field field, Object instance, Object thing) throws ReflectiveOperationException {
        field.set(instance, thing);
    }
}
