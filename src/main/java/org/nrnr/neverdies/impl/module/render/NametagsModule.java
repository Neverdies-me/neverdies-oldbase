package org.nrnr.neverdies.impl.module.render;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnchantedGoldenAppleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.api.render.Interpolation;
import org.nrnr.neverdies.api.render.RenderLayersClient;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.api.render.RenderManagerWorld;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.impl.manager.world.Render2DHelper;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.mixin.accessor.AccessorItemRenderer;
import org.nrnr.neverdies.util.chat.ChatUtil;
import org.nrnr.neverdies.util.render.ColorUtil;
import org.nrnr.neverdies.util.world.FakePlayerEntity;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.nrnr.neverdies.impl.manager.world.Render3DHelper.Line;


public class NametagsModule extends ToggleModule {
    public boolean penis;
    public Config<Boolean> armorConfig = new BooleanConfig("Armor", "Displays the player's armor", true);
    Config<Boolean> durabilityConfig = new BooleanConfig("Durability", "Displays item durability", true, () -> armorConfig.getValue());
    Config<Boolean> itemzConfig = new BooleanConfig("Items", "Displays the player's itemz", true);
    Config<Boolean> itemCountConfig = new BooleanConfig("ItemCount", "Displays the item count", true);
    Config<Boolean> enchantmentsConfig = new BooleanConfig("Enchantments", "Displays a list of the item's enchantments", true);
    Config<Boolean> itemNameConfig = new BooleanConfig("ItemName", "Displays the player's current held item name", false);
    Config<Boolean> entityIdConfig = new BooleanConfig("Id", "Displays the player's entity id", false);
    Config<Boolean> gamemodeConfig = new BooleanConfig("Gamemode", "Displays the player's gamemode", false);
    Config<Boolean> pingConfig = new BooleanConfig("Ping", "Displays the player's server connection ping", true);
    Config<Boolean> healthConfig = new BooleanConfig("Health", "Displays the player's current health", true);
    Config<Boolean> totemsConfig = new BooleanConfig("Totems", "Displays the player's popped totem count", false);
    Config<Float> scalingConfig = new NumberConfig<>("Scale", "The nametag label scale", 0.001f, 0.003f, 0.01f);
    Config<Boolean> invisiblesConfig = new BooleanConfig("Invisibles", "Renders nametags on invisible players", true);
    Config<Boolean> borderedConfig = new BooleanConfig("TextBorder", "Renders a border behind the nametag", true);
    Config<Boolean> outlineConfig = new BooleanConfig("Outline", "Renders a outline behind the nametag", false);
    Config<Color> friendColor = new ColorConfig("FriendColor", "Friend Color Config", new Color(0, 0, 255), false, true);
    Config<Color> enemyColor = new ColorConfig("EnemyColor", "Enemy Color Config", new Color(255, 255, 255), false, true);
    Config<Boolean> Self = new BooleanConfig("Self", "Renders nametag on you", false);

    public NametagsModule() {
        super("Nametags", "Renders info on player nametags", ModuleCategory.RENDER);
    }

    public static VertexConsumer getItemGlintConsumer(VertexConsumerProvider vertexConsumers,
                                                      RenderLayer layer, boolean glint) {
        if (glint) {
            return VertexConsumers.union(vertexConsumers.getBuffer(RenderLayersClient.GLINT), vertexConsumers.getBuffer(layer));
        }
        return vertexConsumers.getBuffer(layer);
    }

    @Override
    public void onEnable() {
        //ChatUtil.clientSendMessage(mc.player.getUuid().toString());
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.gameRenderer == null || mc.getCameraEntity() == null) {
            return;
        }

        Vec3d interpolate = Interpolation.getRenderPosition(mc.getCameraEntity(), mc.getTickDelta());
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d pos = camera.getPos();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player) {
                if (!Self.getValue())
                    penis = player == mc.player;
                if (!player.isAlive() || penis || !invisiblesConfig.getValue() && player.isInvisible()) {
                    continue;
                }
                String info = getNametagInfo(player);
                Vec3d pinterpolate = Interpolation.getRenderPosition(player, mc.getTickDelta());
                double rx = player.getX() - pinterpolate.getX();
                double ry;
                    ry = player.getY() - pinterpolate.getY();
                double rz = player.getZ() - pinterpolate.getZ();
                float width = RenderManagerWorld.textWidth(info);
                float hwidth = width / 2.0f;
                double dx = (pos.getX() - interpolate.getX()) - rx;
                double dy = (pos.getY() - interpolate.getY()) - ry;
                double dz = (pos.getZ() - interpolate.getZ()) - rz;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 4096.0) {
                    continue;
                }
                float scaling = 0.0018f + scalingConfig.getValue() * (float) dist;
                if (dist <= 8.0) {
                    scaling = 0.0245f;
                }
                renderInfo(info, hwidth, player, rx, ry, rz, camera, scaling);
            }
        }
        RenderSystem.enableBlend();
    }

    private void renderInfo(String info, float width, PlayerEntity entity,
                            double x, double y, double z, Camera camera, float scaling) {
        final Vec3d pos = camera.getPos();
        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(x - pos.getX(),
                y + (double) entity.getHeight() + (entity.isSneaking() ? 0.4f : 0.43f) - pos.getY(),
                z - pos.getZ());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale(-scaling, -scaling, -1.0f);
        if (borderedConfig.getValue()) {
            Render2DHelper.rect(matrices, -width - 1.0f, -1.0f, width * 2.0f + 2.0f,
                    mc.textRenderer.fontHeight + 1.0f, 0x55000400);
        }
        if (outlineConfig.getValue()) {
            outlineRect(matrices, -width - 1.0f, -1.0f, width * 2.0f + 2.0f,
                    mc.textRenderer.fontHeight + 1.0f, Modules.COLORS.getColor(), 20f);
        }
        int color = getNametagColor(entity);
        RenderManager.post(() -> {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            GL11.glDepthFunc(GL11.GL_ALWAYS);

            RenderManager.renderText(matrices, info, -width, 0.0f, color);
            renderItems(matrices, entity);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            RenderSystem.disableBlend();
        });
    }

    public void outlineRect(MatrixStack matrices, float x, float y, float width, float height, Color color, float w) {
        Line(matrices, new Vec3d(x, y, 0), new Vec3d(x + width, y, 0), color, w);
        Line(matrices, new Vec3d(x, y + height, 0), new Vec3d(x + width, y + height, 0), color, w);
        Line(matrices, new Vec3d(x, y, 0), new Vec3d(x, y + height, 0), color, w);
        Line(matrices, new Vec3d(x + width, y, 0), new Vec3d(x + width, y + height, 0), color, w);
    }
    private void renderItems(MatrixStack matrixStack, PlayerEntity player) {
        List<ItemStack> displayItems = new CopyOnWriteArrayList<>();
        if (!player.getOffHandStack().isEmpty() && itemzConfig.getValue()) {
            displayItems.add(player.getOffHandStack());
        }
        player.getInventory().armor.forEach(armorStack ->
        {
            if (!armorStack.isEmpty() && armorConfig.getValue()) {
                displayItems.add(armorStack);
            }
        });
        if (!player.getMainHandStack().isEmpty() && itemzConfig.getValue()) {
            displayItems.add(player.getMainHandStack());
        }
        Collections.reverse(displayItems);
        float n10 = 0;
        int n11 = 0;
        for (ItemStack stack : displayItems) {
            n10 -= 8;
            if (stack.getEnchantments().size() > n11) {
                n11 = stack.getEnchantments().size();
            }
        }
        float m2 = enchantOffset(n11);
        for (ItemStack stack : displayItems) {
            // mc.getBufferBuilders().getEntityVertexConsumers().draw();
            matrixStack.push();
            matrixStack.translate(n10, m2, 0.0f);
            matrixStack.translate(8.0f, 8.0f, 0.0f);
            matrixStack.scale(16.0f, 16.0f, 0.0f);
            matrixStack.multiplyPositionMatrix(new Matrix4f().scaling(1.0f, -1.0f, 0.0f));
            DiffuseLighting.disableGuiDepthLighting();
            renderItem(stack, ModelTransformationMode.GUI, 0xff0000, OverlayTexture.DEFAULT_UV,
                    matrixStack, mc.getBufferBuilders().getEntityVertexConsumers(), mc.world, 0);
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            DiffuseLighting.enableGuiDepthLighting();
            matrixStack.pop();
            renderItemOverlay(matrixStack, stack, (int) n10, (int) m2);
            // int n4 = (n11 > 4) ? ((n11 - 4) * 8 / 2) : 0;
            // mc.getItemRenderer().renderInGui(matrixStack, mc.textRenderer, stack, n10, m2);
            matrixStack.scale(0.5f, 0.5f, 0.5f);
            if (durabilityConfig.getValue()) {
                renderDurability(matrixStack, stack, n10 + 2.0f, m2 - 4.5f);
            }
            if (enchantmentsConfig.getValue()) {
                renderEnchants(matrixStack, stack, n10 + 2.0f, m2);
            }
            matrixStack.scale(2.0f, 2.0f, 2.0f);
            n10 += 16;
        }
        //
        ItemStack heldItem = player.getMainHandStack();
        if (heldItem.isEmpty()) {
            return;
        }
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        if (itemNameConfig.getValue()) {
            renderItemName(matrixStack, heldItem, 0, durabilityConfig.getValue() ? m2 - 9.0f : m2 - 4.5f);
        }
        matrixStack.scale(2.0f, 2.0f, 2.0f);
    }

    private void renderItem(ItemStack stack, ModelTransformationMode renderMode, int light, int overlay, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers, World world, int seed) {
        BakedModel bakedModel = mc.getItemRenderer().getModel(stack, world, null, seed);
        if (stack.isEmpty()) {
            return;
        }
        boolean bl = renderMode == ModelTransformationMode.GUI || renderMode == ModelTransformationMode.GROUND || renderMode == ModelTransformationMode.FIXED;
        if (bl) {
            if (stack.isOf(Items.TRIDENT)) {
                bakedModel = mc.getItemRenderer().getModels().getModelManager().getModel(ModelIdentifier.ofVanilla("trident", "inventory"));
            } else if (stack.isOf(Items.SPYGLASS)) {
                bakedModel = mc.getItemRenderer().getModels().getModelManager().getModel(ModelIdentifier.ofVanilla("spyglass", "inventory"));
            }
        }
        bakedModel.getTransformation().getTransformation(renderMode).apply(false, matrices);
        matrices.translate(-0.5f, -0.5f, -0.5f);
        if (bakedModel.isBuiltin() || stack.isOf(Items.TRIDENT) && !bl) {
            ((AccessorItemRenderer) mc.getItemRenderer()).hookGetBuiltinModelItemRenderer().render(stack, renderMode,
                    matrices, vertexConsumers, light, overlay);
        } else {
            ((AccessorItemRenderer) mc.getItemRenderer()).hookRenderBakedItemModel(bakedModel, stack, light,
                    overlay, matrices, getItemGlintConsumer(vertexConsumers, RenderLayers.getItemLayer(stack, false), stack.hasGlint()));
        }
    }

    private void renderItemOverlay(MatrixStack matrixStack, ItemStack stack, int x, int y) {
        matrixStack.push();
        if (itemCountConfig.getValue() && stack.getCount() != 1) {
            String string = String.valueOf(stack.getCount());
            // this.matrices.translate(0.0f, 0.0f, 200.0f);
            RenderManager.renderText(matrixStack, string, x + 17 - mc.textRenderer.getWidth(string), y + 9.0f, -1);
        }
        if (stack.isItemBarVisible()) {
            int i = stack.getItemBarStep();
            int j = stack.getItemBarColor();
            int k = x + 2;
            int l = y + 13;
            Render2DHelper.rect(matrixStack, k, l, 13, 1, Colors.BLACK);
            Render2DHelper.rect(matrixStack, k, l, i, 1, j | Colors.BLACK);
        }
        matrixStack.pop();
    }

    private void renderDurability(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
        if (!itemStack.isDamageable()) {
            return;
        }
        int n = itemStack.getMaxDamage();
        int n2 = itemStack.getDamage();
        int durability = (int) ((n - n2) / ((float) n) * 100.0f);
        RenderManager.renderText(matrixStack, durability + "%", x * 2, y * 2,
                ColorUtil.hslToColor((float) (n - n2) / (float) n * 120.0f, 100.0f, 50.0f, 1.0f).getRGB());
    }

    private void renderEnchants(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
        if (itemStack.getItem() instanceof EnchantedGoldenAppleItem) {
            RenderManager.renderText(matrixStack, "God", x * 2, y * 2, 0xffc34e41);
            return;
        }
        if (!itemStack.hasEnchantments()) {
            return;
        }
        Map<Enchantment, Integer> enchants = EnchantmentHelper.get(itemStack);

        float n2 = 0;
        for (Enchantment enchantment : enchants.keySet()) {
            int lvl = enchants.get(enchantment);
            StringBuilder enchantString = new StringBuilder();
            String translatedName = enchantment.getName(lvl).getString();
            if (translatedName.contains("Vanish")) {
                enchantString.append("Van");
            } else if (translatedName.contains("Bind")) {
                enchantString.append("Bind");
            } else {
                int maxLen = lvl > 1 ? 2 : 3;
                if (translatedName.length() > maxLen) {
                    translatedName = translatedName.substring(0, maxLen);
                }
                enchantString.append(translatedName);
                enchantString.append(lvl);
            }
            RenderManager.renderText(matrixStack, enchantString.toString(), x * 2, (y + n2) * 2, -1);
            n2 += 4.5f;
        }
    }

    private float enchantOffset(final int n) {
        if (!enchantmentsConfig.getValue() || n <= 3) {
            return -18.0f;
        }
        float n2 = -14.0f;
        n2 -= (n - 3) * 4.5f;
        return n2;
    }

    private void renderItemName(MatrixStack matrixStack, ItemStack itemStack, float x, float y) {
        String itemName = itemStack.getName().getString();
        float width = mc.textRenderer.getWidth(itemName) / 4.0f;
        RenderManager.renderText(matrixStack, itemName, (x - width) * 2, y * 2, -1);
    }

    private String getNametagInfo(PlayerEntity player) {
        //final StringBuilder info = new StringBuilder(Managers.SOCIAL.isDev(player.getUuid().toString()) ? Formatting.GOLD + "</> " : "", player.getName().getString());
        final StringBuilder info = new StringBuilder(player.getName().getString());
       //check if dev maybe
        info.append(" ");
        if (entityIdConfig.getValue()) {
            info.append("ID: ");
            info.append(" ");
        }
        if (gamemodeConfig.getValue()) {
            if (player.isCreative()) {
                info.append("[C] ");
            } else if (player.isSpectator()) {
                info.append("[I] ");
            } else {
                info.append("[S] ");
            }
        }
        if (pingConfig.getValue() && mc.getNetworkHandler() != null) {
            PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(player.getGameProfile().getId());
            if (playerEntry != null) {
                info.append(playerEntry.getLatency());
                info.append("ms ");
            }
        }
        if (healthConfig.getValue()) {
            double health = player.getHealth() + player.getAbsorptionAmount();

            Formatting hcolor;
            if (health > 18) {
                hcolor = Formatting.GREEN;
            } else if (health > 16) {
                hcolor = Formatting.DARK_GREEN;
            } else if (health > 12) {
                hcolor = Formatting.YELLOW;
            } else if (health > 8) {
                hcolor = Formatting.GOLD;
            } else if (health > 4) {
                hcolor = Formatting.RED;
            } else {
                hcolor = Formatting.DARK_RED;
            }
            String phealth = String.format(Locale.US, "%.1f", health);
            info.append(hcolor);
            info.append(phealth);
            info.append(" ");
        }
        if (totemsConfig.getValue()) {
            int totems = Managers.TOTEM.getTotems(player);
            if (totems > 0) {

                Formatting pcolor = Formatting.GREEN;

                if (totems > 1) {
                    pcolor = Formatting.DARK_GREEN;
                }
                if (totems > 2) {
                    pcolor = Formatting.YELLOW;
                }
                if (totems > 3) {
                    pcolor = Formatting.GOLD;
                }
                if (totems > 4) {
                    pcolor = Formatting.RED;
                }
                if (totems > 5) {
                    pcolor = Formatting.DARK_RED;
                }
                info.append(pcolor);
                info.append(-totems);
                info.append(" ");
            }
        }
        return info.toString().trim();
    }

    private int getNametagColor(PlayerEntity player) {
        if (player.getDisplayName() != null && Managers.SOCIAL.isFriend(player.getDisplayName())) {
            //friend color
            return friendColor.getValue().getRGB();
        }
        if (player.getDisplayName() != null && !Managers.SOCIAL.isFriend(player.getDisplayName())){
            return enemyColor.getValue().getRGB();
        }

        if (player.isInvisible()) {
            return 0xffff2500;
        }
        // fakeplayer
        if (player instanceof FakePlayerEntity) {
            return 0xffef0147;
        }
        if (player.isSneaking()) {
            return 0xffff9900;
        }
        return 0xffffffff;
    }

    private int getNametagColor1(ItemEntity itemEntity) {
        return 0xffffffff;
    }

    public float getScaling() {
        return scalingConfig.getValue();
    }

    public enum outlinemode {
        IN,
        OUT
    }
}