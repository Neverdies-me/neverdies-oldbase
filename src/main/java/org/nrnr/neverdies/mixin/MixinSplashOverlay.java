package org.nrnr.neverdies.mixin;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.util.Globals;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay implements Globals {
    private static final Identifier background = new Identifier("neverdies", "icon/icon.png");
    //public final Identifier ALLAH_SOUND = new Identifier("neverdies", "sounds/allah.wav");
    //public final Identifier NL_SOUND = new Identifier("neverdies", "sounds/nl.wav");
   // public SoundEvent ALLAH_SOUNDEVENT = SoundEvent.of(ALLAH_SOUND);
   // public SoundEvent NL_SOUNDEVENT = SoundEvent.of(NL_SOUND);
    boolean played = false;
    @Final
    @Shadow
    private boolean reloading;
    @Shadow
    private float progress;
    @Shadow
    private long reloadCompleteTime = -1L;
    @Shadow
    private long reloadStartTime = -1L;
    @Final
    @Shadow
    private ResourceReload reload;
    @Final
    @Shadow
    private Consumer<Optional<Throwable>> exceptionHandler;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
            ci.cancel();
            renderCustom(context, mouseX, mouseY, delta);
    }

    public void renderCustom(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = mc.getWindow().getScaledWidth();
        int j = mc.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        if (reloading && reloadStartTime == -1L) {
            reloadStartTime = l;
        }

        float f = reloadCompleteTime > -1L ? (float) (l - reloadCompleteTime) / 1000.0F : -1.0F;
        float g = reloadStartTime > -1L ? (float) (l - reloadStartTime) / 500.0F : -1.0F;
        float h;
        int k;
        double d = Math.min((double) context.getScaledWindowWidth() * 0.75, context.getScaledWindowHeight()) * 0.25;
        double e = d * 4.0;
        int r = (int) (e * 0.5);

        if (f >= 1.0F) {
            if (mc.currentScreen != null)
                mc.currentScreen.render(context, 0, 0, delta);

        } else if (reloading) {
            if (mc.currentScreen != null && g < 1.0F)
                mc.currentScreen.render(context, mouseX, mouseY, delta);

            k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            context.drawTexture(background, 0, 0, 0, 0, i, j, i, j);
            h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else {
            context.drawTexture(background, 0, 0, 0, 0, i, j, i, j);
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        int s = (int) ((double) context.getScaledWindowHeight() * 0.8325);

        float t = reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + t * 0.050000012F, 0.0F, 1.0F);
        if (f < 1.0F) {
            this.renderProgressBar(context, i / 2 - r, s - 5, i / 2 + r, s + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
        }
        if (f >= 2.0F) {
            mc.setOverlay(null);
        }

        if (reloadCompleteTime == -1L && reload.isComplete() && (!reloading || g >= 2.0F)) {
            try {
                reload.throwException();
                exceptionHandler.accept(Optional.empty());
            } catch (Throwable var23) {
                exceptionHandler.accept(Optional.of(var23));
            }

            reloadCompleteTime = Util.getMeasuringTimeMs();
            if (mc.currentScreen != null) {
                mc.currentScreen.init(mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());

            }
        }
    }

    private void renderProgressBar(DrawContext drawContext, int minX, int minY, int maxX, int maxY, float opacity) {
        int i = MathHelper.ceil((float) (maxX - minX - 2) * this.progress);
        int j = Math.round(opacity * 255.0F);
        int k = ColorHelper.Argb.getArgb(j, 255, 255, 255);
        int s = ColorHelper.Argb.getArgb(j, 248, 131, 121);
        drawContext.fill(minX + 2, minY + 2, minX + i, maxY - 2, s);
        drawContext.fill(minX + 1, minY, maxX - 1, minY + 1, k);
        drawContext.fill(minX + 1, maxY, maxX - 1, maxY - 1, k);
        drawContext.fill(minX, minY, minX + 1, maxY, k);
        drawContext.fill(maxX, minY, maxX - 1, maxY, k);
    }
}