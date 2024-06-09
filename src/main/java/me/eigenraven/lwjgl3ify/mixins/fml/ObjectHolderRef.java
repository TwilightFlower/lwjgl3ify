package me.eigenraven.lwjgl3ify.mixins.fml;

import java.lang.reflect.Field;

import net.minecraft.init.Blocks;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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
