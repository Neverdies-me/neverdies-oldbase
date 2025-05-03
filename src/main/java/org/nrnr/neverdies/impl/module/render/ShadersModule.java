package org.nrnr.neverdies.impl.module.render;


import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.impl.manager.world.ShaderManager;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.util.string.EnumFormatter;

import java.awt.*;

public class ShadersModule extends ToggleModule {
    public static ShadersModule INSTANCE;
    public Config<ShaderManager.Shader> mode = new EnumConfig<>("Shader", "The rendering mode for the chams", ShaderManager.Shader.Default, ShaderManager.Shader.values());
    Config<Float> range = new NumberConfig<>("Range","",1f,256f,512f);
    // LINE
    public Config<Float> radius = new NumberConfig<>("Width", "s", 1f, 1.1f, 6f);
    public Config<Float> quality = new NumberConfig<>("Quality", "s", 0f, 0.3f, 1f);
    public Config<Float> fade = new NumberConfig<>("Fade", "s", 0f, 0f, 300f);
    public Config<Float> fadelimit = new NumberConfig<>("Multiplier", "s", 0f, 20f, 20f, () -> fade.getValue() != 0);
    // wave utils
    public Config<Integer> speed = new NumberConfig<>("Speed", "s", -30, 15, 30, () -> mode.getValue() == ShaderManager.Shader.Gradient || mode.getValue() == ShaderManager.Shader.Rainbow);
    public Config<Float> scale = new NumberConfig<>("Scale", "s", 4f, 10f, 10f, () -> mode.getValue() == ShaderManager.Shader.Gradient);
    // colors

    public Config<Color> fillColor = new ColorConfig("Fill", "The color of the chams", new Color(255, 0, 0, 60), () ->  mode.getValue() == ShaderManager.Shader.Default);
    public Config<Color> fillColor1 = new ColorConfig("Primary", "The color of the chams", new Color(255, 0, 0, 60), false, true, () ->  mode.getValue() == ShaderManager.Shader.Gradient);
    public Config<Color> fillColor2 = new ColorConfig("Secondary", "The color of the chams", new Color(238, 223, 223, 60), false, false, () ->  mode.getValue() == ShaderManager.Shader.Gradient);
    public Config<Color> outline = new ColorConfig("Outline", "The color of the chams", new Color(255, 0, 0, 60), false, true, () ->  mode.getValue() != ShaderManager.Shader.Rainbow);
    public Config<Float> alpha = new NumberConfig<>("Alpha", "s", 0f, 1f, 255f, () ->  mode.getValue() == ShaderManager.Shader.Gradient || mode.getValue() == ShaderManager.Shader.Rainbow);
    // targets
    public Config<Boolean> target = new BooleanConfig("Target", "Targets for shaders", false).setGroup();
    public Config<Boolean> self = new BooleanConfig("Self", "ww", true);
    public Config<Boolean> hands = new BooleanConfig("Hands", "ww", true);
    public Config<Boolean> players = new BooleanConfig("Players", "ww", true);
    public Config<Boolean> friends = new BooleanConfig("Friends", "ww", true);
    public Config<Boolean> monsters = new BooleanConfig("Monsters", "ww", false);
    public Config<Boolean> crystals = new BooleanConfig("Crystals", "ww", true);
    public Config<Boolean> others = new BooleanConfig("Others", "ww", false);
    public Config<Boolean> ambients = new BooleanConfig("Ambients", "ww", false);
    public Config<Boolean> items = new BooleanConfig("Items", "ww", false);

    public ShadersModule() {
        super("Shaders", "Renders entity models through walls", ModuleCategory.RENDER);
        INSTANCE = this;
    }

    @Override
    public String getModuleData() {
        return EnumFormatter.formatEnum(mode.getValue());
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null)
            return false;
        if (mc.player == null)
            return false;
        if (mc.player.distanceTo(entity) > range.getValue()) {
            return false;
        }
        if (MathHelper.sqrt((float) mc.player.squaredDistanceTo(entity.getPos())) > 256)
            return false;

        if (entity instanceof PlayerEntity) {
            if (entity == mc.player)
                return self.getValue();
            if (Managers.SOCIAL.isFriend(entity.getName()))
                return friends.getValue();
            return players.getValue();
        }

        if (entity instanceof EndCrystalEntity)
            return crystals.getValue();
        if (entity instanceof ItemEntity)
            return items.getValue();
        return switch (entity.getType().getSpawnGroup()) {
            case MONSTER -> monsters.getValue();
            case AMBIENT, WATER_AMBIENT -> ambients.getValue();
            default -> others.getValue();
        };
    }

    public void onRender3D(MatrixStack matrixStack, float partialTicks) {
        if (hands.getValue()) {
            Managers.SHADER.renderShader(() -> mc.gameRenderer.renderHand(matrixStack, mc.gameRenderer.getCamera(), mc.getTickDelta()), mode.getValue());
        }
    }

    @Override
    public void onDisable() {
        Managers.SHADER.reloadShaders();
    }
}