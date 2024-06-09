package me.eigenraven.lwjgl3ify.core;

import java.util.*;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import zone.rong.mixinbooter.IEarlyMixinLoader;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(Integer.MAX_VALUE - 2)
public class Lwjgl3ifyCoremod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public static final Logger LOGGER = LogManager.getLogger("lwjgl3ify");

    public Lwjgl3ifyCoremod() {
        try {
            LaunchClassLoader launchLoader = (LaunchClassLoader) getClass().getClassLoader();
            launchLoader.addClassLoaderExclusion("org.hotswap.agent");
            launchLoader.addClassLoaderExclusion("org.lwjglx.debug");
        } catch (ClassCastException e) {
            LOGGER.warn(
                "Unsupported launch class loader type " + getClass().getClassLoader()
                    .getClass(),
                e);
        }
        Config.loadConfig();
        if (Launch.blackboard.get("lwjgl3ify:rfb-booted") != Boolean.TRUE) {
            return;
        }
        LateInit.lateConstruct();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public List<String> getMixinConfigs() {
        List<String> mixins = new ArrayList<>();
        mixins.add("mixins.lwjgl3ify.early.json");

        // STB replacements for vanilla functions
        if (Config.MIXIN_STBI_TEXTURE_LOADING) {
            LOGGER.info("Enabling STB texture loading mixin");
            mixins.add("mixins.lwjgl3ify.early.stb_tex.json");
        } else {
            LOGGER.info("Disabling STB texture loading mixin");
        }

        if (Config.MIXIN_STBI_TEXTURE_STICHING) {
            LOGGER.info("Enabling STB texture stitching mixin");
            mixins.add("mixins.lwjgl3ify.early.stb_stitch.json");
        } else {
            LOGGER.info("Disabling STB texture stitching mixin");
        }

        return mixins;
    }
}
