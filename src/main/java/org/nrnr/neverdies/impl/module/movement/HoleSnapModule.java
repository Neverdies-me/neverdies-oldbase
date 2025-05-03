package org.nrnr.neverdies.impl.module.movement;

import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.RotationModule;
import org.nrnr.neverdies.api.render.RenderManagerWorld;
import org.nrnr.neverdies.impl.event.entity.player.PlayerMoveEvent;
import org.nrnr.neverdies.impl.event.network.PacketEvent;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.impl.manager.combat.hole.Hole;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.util.chat.ChatUtil;
import org.nrnr.neverdies.util.world.BlockUtil;

import java.awt.*;

public class HoleSnapModule extends RotationModule {

    private final Config<Boolean> antiKickConfig = new BooleanConfig("AntiKick", "Prevents vanilla flight detection", false);
    private final Config<Boolean> autoTravelConfig = new BooleanConfig("AutoTravel", "Automatically moves to the nearest hole", true);
    private final Config<Boolean> autoDisableConfig = new BooleanConfig("AutoDisable", "Disables the module after use", false);
    private final Config<Boolean> renderLineConfig = new BooleanConfig("Render Line", "Renders a line to the nearest hole", false);
    private final Config<Color> lineColorConfig = new ColorConfig("Line Color", "Color of the rendered line", new Color(255, 0, 0, 60), renderLineConfig::getValue);
    private final Config<Boolean> renderLabelConfig = new BooleanConfig("Render Label", "Renders labels for holes", false);

    private float previousYaw;

    public HoleSnapModule() {
        super("HoleSnap", "Automatically moves the player to the nearest hole", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable() {
        Hole nearestHole = findNearestHole();
        if (nearestHole == null) {
            ChatUtil.clientSendMessage("No holes found nearby.");
            disable();
        }
    }

    private Hole findNearestHole() {
        Hole nearestHole = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Hole hole : Managers.HOLE.getHoles()) {
            double distance = mc.player.squaredDistanceTo(hole.getX(), hole.getY(), hole.getZ());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestHole = hole;
            }
        }

        return nearestHole;
    }

    @EventListener
    public void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null) return;

        Hole nearestHole = findNearestHole();
        if (nearestHole == null) return;

        Vec3d playerPos = mc.player.getPos();
        Vec3d holeCenter = nearestHole.getCenter();

        // Stop movement if near the center of the hole
        if (playerPos.distanceTo(holeCenter) < 0.15f) {
            event.setX(0);
            event.setZ(0);
            event.cancel();
            if (autoDisableConfig.getValue()) {
                disable();
            }
            return;
        }

        // Perform auto-travel if enabled
        if (autoTravelConfig.getValue()) {
            double diffX = holeCenter.getX() - mc.player.getX();
            double diffZ = holeCenter.getZ() - mc.player.getZ();
            double moveX = MathHelper.clamp(diffX, -0.29, 0.29);
            double moveZ = MathHelper.clamp(diffZ, -0.29, 0.29);

            event.setX(moveX);
            event.setZ(moveZ);
            event.cancel();
        }

        // Jump to avoid obstacles
        if (mc.player.horizontalCollision && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.player == null) return;

        for (Hole hole : Managers.HOLE.getHoles()) {
            Vec3d holeCenter = hole.getCenter();

            if (renderLineConfig.getValue()) {
                RenderManagerWorld.renderLine(
                        event.getMatrices(),
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        holeCenter.getX(), holeCenter.getY(), holeCenter.getZ(),
                        1.5f, lineColorConfig.getValue().getRGB()
                );
            }

            if (renderLabelConfig.getValue()) {
                RenderManagerWorld.renderSign(event.getMatrices(), "Hole", holeCenter.getX(), holeCenter.getY(), holeCenter.getZ());
            }
        }
    }

    @EventListener
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket && mc.player.isOnGround()) {
            disable();
        }
    }

    private float calculateYawTo(Vec3d targetPos) {
        Vec3d playerPos = mc.player.getPos();
        return (float) MathHelper.wrapDegrees(
                Math.toDegrees(Math.atan2(targetPos.getZ() - playerPos.getZ(), targetPos.getX() - playerPos.getX())) - 90 - mc.player.getYaw()
        );
    }

    private void handleYawAdjustment(boolean isPreEvent) {
        if (mc.player == null || !autoTravelConfig.getValue()) return;

        Hole nearestHole = findNearestHole();
        if (nearestHole == null) return;

        if (isPreEvent) {
            previousYaw = mc.player.getYaw();
            mc.player.setYaw(BlockUtil.calculateAngle(nearestHole.getCenter())[0]);
        } else {
            mc.player.setYaw(previousYaw);
        }
    }
}
