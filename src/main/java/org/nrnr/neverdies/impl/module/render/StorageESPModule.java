package org.nrnr.neverdies.impl.module.render;

import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.api.render.RenderManagerWorld;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;

import java.awt.*;

import static org.nrnr.neverdies.util.world.BlockUtil.blockEntities;


public class StorageESPModule extends ToggleModule {
    Config<Boolean> tracer = new BooleanConfig("Tracers","w",false);
    Config<Boolean> chest = new BooleanConfig("Chest", "", true);
    ColorConfig chestcolor = new ColorConfig("Chest Color", "The secondary client color", new Color(255, 160, 0), false, false, () -> chest.getValue());
    Config<Boolean> trappedchest = new BooleanConfig("Trapped Chest", "", true);
    ColorConfig trapchestcolor = new ColorConfig("TrapChest Color", "The secondary client color", new Color(255, 0, 0), false, false, () -> trappedchest.getValue());
    Config<Boolean> shulker = new BooleanConfig("Shulker", "", true);
    ColorConfig shulkercolor = new ColorConfig("Shulker Color", "The secondary client color", new Color(157, 69, 179), false, false, () -> shulker.getValue());
    Config<Boolean> enderchest = new BooleanConfig("Ender Chest", "", true);
    ColorConfig endchestcolor = new ColorConfig("EnderChest Color", "The secondary client color", new Color(120, 0, 255), false, false, () -> enderchest.getValue());
    Config<Boolean> other = new BooleanConfig("Other", "", true);
    ColorConfig othercolor = new ColorConfig("Other Color", "The secondary client color", new Color(140, 140, 140), false, false, () -> other.getValue());

    public StorageESPModule() {
        super("StorageESP", "Highlights the chests", ModuleCategory.RENDER);
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.world == null) {
            return;
        }
        for (BlockEntity blockEntity : blockEntities()) {
            if (blockEntity instanceof ChestBlockEntity && chest.getValue()) {
                renderBlockEntity(blockEntity, chestcolor, event);
            } else if (blockEntity instanceof TrappedChestBlockEntity && trappedchest.getValue()) {
                renderBlockEntity(blockEntity, trapchestcolor, event);
            } else if (blockEntity instanceof ShulkerBoxBlockEntity && shulker.getValue()) {
                renderBlockEntity(blockEntity, shulkercolor, event);
            } else if (blockEntity instanceof EnderChestBlockEntity && enderchest.getValue()) {
                renderBlockEntity(blockEntity, endchestcolor, event);
            } else if ((blockEntity instanceof AbstractFurnaceBlockEntity || blockEntity instanceof DispenserBlockEntity || blockEntity instanceof HopperBlockEntity) && other.getValue()) {
                renderBlockEntity(blockEntity, othercolor, event);
            }
        }
    }

    private void renderBlockEntity(BlockEntity blockEntity, ColorConfig color, RenderWorldEvent event) {
        if (mc.player == null) {
            return;
        }
        if (tracer.getValue()) {
            boolean prevBobView = mc.options.getBobView().getValue();
            mc.options.getBobView().setValue(false);
            double x1 = mc.player.prevX + (mc.player.getX() - mc.player.prevX) * mc.getTickDelta();
            double y1 = mc.player.getEyeHeight(mc.player.getPose()) + mc.player.prevY + (mc.player.getY() - mc.player.prevY) * mc.getTickDelta();
            double z1 = mc.player.prevZ + (mc.player.getZ() - mc.player.prevZ) * mc.getTickDelta();

            Vec3d vec2 = new Vec3d(0, 0, 75)
                    .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                    .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                    .add(x1, y1, z1);
            if (blockEntity == null) {
                return;
            }
            if (color != null) {
                Vec3d entityPos = blockEntity.getPos().toCenterPos();
                RenderManagerWorld.renderLine(event.getMatrices(), vec2, entityPos.add(0.0, 0, 0.0), color.getRgb());
            }
            mc.options.getBobView().setValue(prevBobView);
        }
        BlockPos blockEntityPos = blockEntity.getPos();
        Box render = new Box(blockEntityPos.getX(), blockEntityPos.getY(), blockEntityPos.getZ(),
                blockEntityPos.getX() + 1, blockEntityPos.getY() + 1, blockEntityPos.getZ() + 1);
        RenderManagerWorld.renderBox(event.getMatrices(), render, color.getRgb(100));
        RenderManagerWorld.renderBoundingBox(event.getMatrices(), render, 2.5f, color.getRgb(145));
    }

}