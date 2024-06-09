package me.eigenraven.lwjgl3ify.mixins.game;

import java.awt.image.BufferedImage;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite {

    @Inject(
        method = "loadSpriteFrames(Lnet/minecraft/client/resources/IResource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/awt/image/BufferedImage;getRGB(IIII[III)[I",
            remap = false,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    void cleanupNativeBackedImage(IResource resource, int mipmapLevels, CallbackInfo info, BufferedImage image) {
        if (image instanceof AutoCloseable) {
            try {
                ((AutoCloseable) image).close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
