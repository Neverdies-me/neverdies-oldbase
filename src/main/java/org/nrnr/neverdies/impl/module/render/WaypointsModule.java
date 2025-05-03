package org.nrnr.neverdies.impl.module.render;

import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.impl.event.network.PacketEvent;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.mixin.accessor.AccessorEntity;
import org.nrnr.neverdies.util.chat.ChatUtil;

import java.awt.*;
import java.util.*;


public class WaypointsModule extends ToggleModule {

    private final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
    private final Map<UUID, PlayerEntity> logoutCache = Maps.newConcurrentMap();
    private DeathCoordinates dpos;
    Config<Boolean> logoutsConfig = new BooleanConfig("Logouts", "Marks the position of player logouts", true);
    Config<Boolean> deathsConfig = new BooleanConfig("Deaths", "Marks the position of player deaths", false);
    Config<Boolean> message = new BooleanConfig("Notification", "terpusha femboy", false);
    Config<Boolean> render = new BooleanConfig("Render", "render", true).setParent();
    Config<type> typee = new EnumConfig<>("Mode","",type.Box,type.values(), () -> render.isOpen());
    Config<Color> colorConfig = new ColorConfig("Box", "Esp color", new Color(255, 255, 255, 60), true, true, () -> render.isOpen() && typee.getValue().equals(type.Box));
    Config<Color> color1Config = new ColorConfig("Outline", "Esp color", new Color(255, 255, 255, 145), true, true, () -> render.isOpen() && typee.getValue().equals(type.Box));
    Config<Boolean> text = new BooleanConfig("Nametag", "Draw text on position of player log", true, () -> render.isOpen());
    Config<Boolean> coords1 = new BooleanConfig("Coordinate", "Draw coords on position of player log", true, () -> render.isOpen() && text.getValue());
    Config<Color> textcolor = new ColorConfig("Text", "w", new Color(1, 1, 1, 255), true, true, () -> render.isOpen() && text.getValue());
    public WaypointsModule() {
        super("Waypoints", "Renders a waypoint at marked locations",
                ModuleCategory.RENDER);
    }
    public enum type {
        Box,
        Ghost,
        None
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Inbound event) {
        if (event.getPacket() instanceof PlayerListS2CPacket packet && logoutsConfig.getValue()) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry addedPlayer : packet.getPlayerAdditionEntries()) {
                    for (UUID uuid : logoutCache.keySet()) {
                        if (!uuid.equals(addedPlayer.profile().getId())) continue;
                        PlayerEntity player = logoutCache.get(uuid);
                        if (message.getValue()) {
                            ChatUtil.clientSendIdPrefix(Objects.hash(player) + 1432, "§s" + player.getName().getString() + " §7logged back at: §s" + (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ());
                        }
                        logoutCache.remove(uuid);
                    }
                }
            }
            playerCache.clear();
        } else if (event.getPacket() instanceof PlayerRemoveS2CPacket packet && logoutsConfig.getValue()) {
            for (UUID uuid2 : packet.profileIds()) {
                for (UUID uuid : playerCache.keySet()) {
                    if (!uuid.equals(uuid2)) continue;
                    final PlayerEntity player = playerCache.get(uuid);
                    if (!logoutCache.containsKey(uuid)) {
                        if (message.getValue()) {
                            ChatUtil.clientSendIdPrefix(Objects.hash(player) + 1432, "§s" + player.getName().getString() + " §7logged out at: §s" + (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ());
                        }
                        logoutCache.put(uuid, player);
                    }
                }
            }
            playerCache.clear();
        }
    }

    @Override
    public void onEnable() {
        playerCache.clear();
        logoutCache.clear();
        dpos = null;
    }

    @EventListener
    public void onUpdate(PlayerTickEvent event) {
        if (mc.player != null && mc.player.isDead() && deathsConfig.getValue()) {
            dpos = new DeathCoordinates(mc.player.getX(),mc.player.getY(),mc.player.getZ());
        }
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.equals(mc.player)) continue;
            playerCache.put(player.getGameProfile().getId(), player);
        }
    }

    @EventListener
    public void onRender3D(RenderWorldEvent event) {
        if (!render.getValue()) return;
        if (dpos != null) {
            // DEATH
            RenderManager.drawBox(event.getMatrices(), dpos.getBox(), Modules.COLORS.getRGB(60));
            RenderManager.drawBoundingBox(event.getMatrices(), dpos.getBox(), Modules.COLORS.getRGB(140));
            if (text.getValue()) {
                String formattedCoords = String.format("%.1f %.1f %.1f", dpos.getX(), dpos.getY(), dpos.getZ());
                RenderManager.post(() -> {
                    RenderManager.renderSign(event.getMatrices(), "Death " + (coords1.getValue() ? formattedCoords : ""), new Vec3d(dpos.getX(), dpos.getY() + 2.4, dpos.getZ()), textcolor.getValue().getRGB());
                });
            }
        }
        // LOGS
        for (UUID uuid : logoutCache.keySet()) {
            final PlayerEntity data = logoutCache.get(uuid);
            if (data == null) continue;
            if (typee.getValue().equals(type.Box)) {
                RenderManager.drawBox(event.getMatrices(), data.getBoundingBox(), colorConfig.getValue().getRGB());
                RenderManager.drawBoundingBox(event.getMatrices(), data.getBoundingBox(), color1Config.getValue().getRGB());
            } else if (typee.getValue().equals(type.Ghost)){
                PlayerEntityModel<PlayerEntity> modelPlayer = new PlayerEntityModel<>(new EntityRendererFactory.Context(
                        mc.getEntityRenderDispatcher(), mc.getItemRenderer(),
                        mc.getBlockRenderManager(), mc.getEntityRenderDispatcher().getHeldItemRenderer(),
                        mc.getResourceManager(), mc.getEntityModelLoader(), mc.textRenderer).getPart(EntityModelLayers.PLAYER), false);
                modelPlayer.getHead().scale(new Vector3f(-0.3f, -0.3f, -0.3f));

                renderEntity(event.getMatrices(), data, modelPlayer, ((OtherClientPlayerEntity)data).getSkinTextures().texture(), 150);
            }
            if (text.getValue()) {
                String formattedCoords = String.format("%.1f %.1f %.1f", data.getX(), data.getY(), data.getZ());
                RenderManager.post(() -> {
                    String text = coords1.getValue() ? " logged out at " :  " logout ";
                    RenderManager.Text3d(event.getMatrices(), data.getName().copy().formatted(Formatting.RESET).getString() + text + (coords1.getValue() ? formattedCoords : ""), data.getX(), ((AccessorEntity) data).getDimensions().getBoxAt(data.getPos()).maxY + 0.5, data.getZ(), textcolor.getValue().getRGB());
                });
            }
        }
    }

    // hueta
    private static class DeathCoordinates {
        private final double x;
        private final double y;
        private final double z;

        public DeathCoordinates(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        // hueta
        public Box getBox() {
            return new Box(x - 0.3, y, z - 0.3, x + 0.3, y + 2, z + 0.3);
        }
    }
    private void renderEntity(@NotNull MatrixStack matrices, @NotNull LivingEntity entity, @NotNull PlayerEntityModel<PlayerEntity> modelBase, Identifier texture, int alpha) {
        modelBase.leftPants.visible = true;
        modelBase.rightPants.visible = true;
        modelBase.leftSleeve.visible = true;
        modelBase.rightSleeve.visible = true;
        modelBase.jacket.visible = true;
        modelBase.hat.visible = true;

        double x = entity.getX() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = entity.getY() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = entity.getZ() - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        ((AccessorEntity) entity).setPosition(entity.getPos());
        matrices.push();
        matrices.translate((float) x, (float) y, (float) z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rad(180 - entity.bodyYaw)));
        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.scale(1.6f, 1.8f, 1.6f);
        matrices.translate(0.0F, -1.501F, 0.0F);
        modelBase.animateModel((PlayerEntity) entity, entity.limbAnimator.getPos(), entity.limbAnimator.getSpeed(), mc.getTickDelta());
        float limbSpeed = Math.min(entity.limbAnimator.getSpeed(), 1f);
        modelBase.setAngles((PlayerEntity) entity, entity.limbAnimator.getPos(), limbSpeed, entity.age, entity.headYaw - entity.bodyYaw, entity.getPitch());
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha / 255f);
        modelBase.render(matrices,builder, 10, 0,1f,1f,1f,0.5f);
        BufferBuilder.BuiltBuffer builtBuffer = builder.endNullable();
        if (builtBuffer != null)
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        matrices.pop();
    }
    public static float rad(float angle) {
        return (float) (angle * Math.PI / 180);
    }
}

