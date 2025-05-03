package org.nrnr.neverdies.impl.manager.world;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL30C;
import org.nrnr.neverdies.impl.imixin.IShaderEffect;
import org.nrnr.neverdies.impl.module.render.ShadersModule;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.Globals;

import java.util.ArrayList;
import java.util.List;

public class ShaderManager implements Globals {
    private final static List<RenderTask> tasks = new ArrayList<>();
    public static ManagedShaderEffect DEFAULT_OUTLINE;
    public static ManagedShaderEffect RAINBOW_OUTLINE;
    public static ManagedShaderEffect GRADIENTT_OUTLINE;
    public static ManagedShaderEffect GRADIENTT;
    public static ManagedShaderEffect DEFAULT;
    public static ManagedShaderEffect RAINBOW;
    public float time = 0;
    private NeverdiesBuffer shaderBuffer;

    public void renderShader(Runnable runnable, Shader mode) {
        tasks.add(new RenderTask(runnable, mode));
    }

    public void renderShaders() {
        if (DEFAULT == null) {
            shaderBuffer = new NeverdiesBuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            reloadShaders();
        }

        if (shaderBuffer == null)
            return;

        tasks.forEach(t -> applyShader(t.task(), t.shader()));
        tasks.clear();
    }

    public void applyShader(Runnable runnable, Shader mode) {
        Framebuffer MCBuffer = MinecraftClient.getInstance().getFramebuffer();
        RenderSystem.assertOnRenderThreadOrInit();
        if (shaderBuffer.textureWidth != MCBuffer.textureWidth || shaderBuffer.textureHeight != MCBuffer.textureHeight)
            shaderBuffer.resize(MCBuffer.textureWidth, MCBuffer.textureHeight, false);
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, shaderBuffer.fbo);
        shaderBuffer.beginWrite(true);
        runnable.run();
        shaderBuffer.endWrite();
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, MCBuffer.fbo);
        MCBuffer.beginWrite(false);
        ManagedShaderEffect shader = getShader(mode);
        Framebuffer mainBuffer = MinecraftClient.getInstance().getFramebuffer();
        PostEffectProcessor effect = shader.getShaderEffect();

        if (effect != null)
            ((IShaderEffect) effect).client_nextgen_master$addFakeTargetHook("bufIn", shaderBuffer);

        Framebuffer outBuffer = shader.getShaderEffect().getSecondaryTarget("bufOut");
        setupShader(mode, shader);
        shaderBuffer.clear(false);
        mainBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.backupProjectionMatrix();
        outBuffer.draw(outBuffer.textureWidth, outBuffer.textureHeight, false);
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public ManagedShaderEffect getShader(@NotNull Shader mode) {
        return switch (mode) {
            case Gradient -> GRADIENTT;
            case Rainbow -> RAINBOW;
//            case Image -> IMAGE;
            default -> DEFAULT;
        };
    }

    public ManagedShaderEffect getShaderOutline(@NotNull Shader mode) {
        return switch (mode) {
            case Gradient -> GRADIENTT_OUTLINE;
            case Rainbow -> RAINBOW_OUTLINE;
//            case Image -> IMAGE_OUTLINE;
            default -> DEFAULT_OUTLINE;
        };
    }

    public void setupShader(Shader shader, ManagedShaderEffect effect) {
        ShadersModule shaderChams = ShadersModule.INSTANCE;
        if (shader == Shader.Rainbow) {
            effect.setUniformValue("alpha2", (shaderChams.alpha.getValue() / 255f));
            effect.setUniformValue("radius", shaderChams.radius.getValue());
            effect.setUniformValue("quality", shaderChams.quality.getValue());
            effect.setUniformValue("fade", shaderChams.fade.getValue());
            effect.setUniformValue("fadelimit", shaderChams.fadelimit.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("time", time);
            effect.render(mc.getTickDelta());
            time += (float) shaderChams.speed.getValue() * 0.002f;
        } else if (shader == Shader.Default) {
            effect.setUniformValue("line", Modules.SHADERS.outline.getValue().getRed() / 255f, Modules.SHADERS.outline.getValue().getGreen() / 255f, Modules.SHADERS.outline.getValue().getBlue() / 255f);
            effect.setUniformValue("alpha", (shaderChams.fillColor.getValue().getAlpha() / 255f));
            effect.setUniformValue("color", shaderChams.fillColor.getValue().getRed() / 255f, shaderChams.fillColor.getValue().getGreen() / 255f, shaderChams.fillColor.getValue().getBlue() / 255f);
            effect.setUniformValue("radius", shaderChams.radius.getValue());
            effect.setUniformValue("quality", shaderChams.quality.getValue());
            effect.setUniformValue("fade", shaderChams.fade.getValue());
            effect.setUniformValue("fadelimit", shaderChams.fadelimit.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.render(mc.getTickDelta());
        } else if (shader == Shader.Gradient) {
            effect.setUniformValue("color", Modules.SHADERS.outline.getValue().getRed() / 255f, Modules.SHADERS.outline.getValue().getGreen() / 255f, Modules.SHADERS.outline.getValue().getBlue() / 255f);
            effect.setUniformValue("alpha", (shaderChams.alpha.getValue() / 255f));
            effect.setUniformValue("radius", shaderChams.radius.getValue());
            effect.setUniformValue("quality", shaderChams.quality.getValue());
            effect.setUniformValue("fade", shaderChams.fade.getValue());
            effect.setUniformValue("fadelimit", shaderChams.fadelimit.getValue());
            effect.setUniformValue("resolution", (float) mc.getWindow().getScaledWidth(), (float) mc.getWindow().getScaledHeight());
            effect.setUniformValue("speed", Modules.SHADERS.speed.getValue() * 30f);
            effect.setUniformValue("colorDistance", Modules.SHADERS.scale.getValue() * 10f);
            effect.setUniformValue("primaryColor", Modules.SHADERS.fillColor1.getValue().getRed() / 255f, Modules.SHADERS.fillColor1.getValue().getGreen() / 255f, Modules.SHADERS.fillColor1.getValue().getBlue() / 255f, Modules.SHADERS.fillColor1.getValue().getAlpha() / 255f);
            effect.setUniformValue("secondaryColor", Modules.SHADERS.fillColor2.getValue().getRed() / 255f, Modules.SHADERS.fillColor2.getValue().getGreen() / 255f, Modules.SHADERS.fillColor2.getValue().getBlue() / 255f, Modules.SHADERS.fillColor1.getValue().getAlpha() / 255f);
            effect.setUniformValue("time", (System.currentTimeMillis() % 100000) / 1000f);
            effect.render(mc.getTickDelta());
        }
    }

    public void reloadShaders() {
        RAINBOW = ShaderEffectManager.getInstance().manage(new Identifier("shaders/post/rainbow.json"));

        RAINBOW_OUTLINE = ShaderEffectManager.getInstance().manage(new Identifier("shaders/post/rainbow.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).client_nextgen_master$addFakeTargetHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).client_nextgen_master$addFakeTargetHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        DEFAULT = ShaderEffectManager.getInstance().manage(new Identifier("shaders/post/outline.json"));

        DEFAULT_OUTLINE = ShaderEffectManager.getInstance().manage(new Identifier("shaders/post/outline.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).client_nextgen_master$addFakeTargetHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).client_nextgen_master$addFakeTargetHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });
        GRADIENTT = ShaderEffectManager.getInstance().manage(new Identifier("shaders/post/gradient.json"));

        GRADIENTT_OUTLINE = ShaderEffectManager.getInstance().manage(new Identifier("shaders/post/gradient.json"), managedShaderEffect -> {
            PostEffectProcessor effect = managedShaderEffect.getShaderEffect();
            if (effect == null) return;

            ((IShaderEffect) effect).client_nextgen_master$addFakeTargetHook("bufIn", mc.worldRenderer.getEntityOutlinesFramebuffer());
            ((IShaderEffect) effect).client_nextgen_master$addFakeTargetHook("bufOut", mc.worldRenderer.getEntityOutlinesFramebuffer());
        });

    }

    public boolean fullNullCheck() {
        if (GRADIENTT == null || RAINBOW == null || DEFAULT == null) {
            shaderBuffer = new NeverdiesBuffer(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
            reloadShaders();
            return true;
        }

        return false;
    }

    public enum Shader {
        Default,
        Gradient,
        Rainbow
    }

    public static class NeverdiesBuffer extends Framebuffer {
        public NeverdiesBuffer(int width, int height) {
            super(false);
            RenderSystem.assertOnRenderThreadOrInit();
            resize(width, height, true);
            setClearColor(0f, 0f, 0f, 0f);
        }
    }

    public record RenderTask(Runnable task, Shader shader) {
    }
}
