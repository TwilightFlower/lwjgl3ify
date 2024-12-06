package me.eigenraven.lwjgl3ify.mixins.early.fml;

import java.util.zip.ZipEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = { net.minecraftforge.fml.common.discovery.JarDiscoverer.class }, remap = false)
public class JarDiscoverer {

    @Redirect(
        method = {
            "Lnet/minecraftforge/fml/common/discovery/JarDiscoverer;findClassesASM(Lnet/minecraftforge/fml/common/discovery/ModCandidate;Lnet/minecraftforge/fml/common/discovery/ASMDataTable;Ljava/util/jar/JarFile;Ljava/util/List;Lnet/minecraftforge/fml/common/MetadataCollection;)V",
            "Lnet/minecraftforge/fml/common/discovery/JarDiscoverer;findClassesJSON(Lnet/minecraftforge/fml/common/discovery/ModCandidate;Lnet/minecraftforge/fml/common/discovery/ASMDataTable;Ljava/util/jar/JarFile;Ljava/util/List;Lnet/minecraftforge/fml/common/MetadataCollection;)V" },
        // target based on the string constant to ensure we're hitting the right one in both methods
        at = @At(ordinal = 0, value = "CONSTANT", args = { "stringValue=__MACOSX" }, shift = At.Shift.BEFORE),
        remap = false,
        require = 1)
    public String getZipEntryName(ZipEntry ze) {
        String name = ze.getName();
        if (name == null) {
            return null;
        }
        if (name.contains("module-info.class") || name.startsWith("META-INF/versions/")
            || name.contains("org/openjdk/nashorn")
            || name.contains("jakarta/servlet/")) {
            // Triggers the continue in the loop
            return "__MACOSX_ignoreme";
        }
        return name;
    }
}
