package org.nrnr.neverdies.util.player;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.util.Globals;
import org.nrnr.neverdies.util.world.EntityUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chronos & xgraza
 * @since 1.0
 */
public final class PlayerUtil implements Globals {
    public static BlockPos getRoundedBlockPos(final double x, final double y, final double z) {
        final int flooredX = MathHelper.floor(x);
        final int flooredY = (int) Math.round(y);
        final int flooredZ = MathHelper.floor(z);
        return new BlockPos(flooredX, flooredY, flooredZ);
    }

    public static float getLocalPlayerHealth() {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    public static boolean isSurrounded(PlayerEntity target) {
        return !mc.world.getBlockState(target.getBlockPos().add(1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(-1, 0, 0)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, 1)).isAir() && !mc.world.getBlockState(target.getBlockPos().add(0, 0, -1)).isAir();
    }

    // from MC source
    public static int computeFallDamage(float fallDistance, float damageMultiplier) {
        if (mc.player.getType().isIn(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return 0;
        } else {
            final StatusEffectInstance statusEffectInstance = mc.player.getStatusEffect(StatusEffects.JUMP_BOOST);
            final float f = statusEffectInstance == null ? 0.0F : (float) (statusEffectInstance.getAmplifier() + 1);
            return MathHelper.ceil((fallDistance - 3.0F - f) * damageMultiplier);
        }
    }

    public static boolean isHolding(final Item item) {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (!itemStack.isEmpty() && itemStack.getItem() == item) {
            return true;
        }
        itemStack = mc.player.getOffHandStack();
        return !itemStack.isEmpty() && itemStack.getItem() == item;
    }

    public static boolean isHotbarKeysPressed() {
        for (KeyBinding binding : mc.options.hotbarKeys) {
            if (binding.isPressed()) {
                return true;
            }
        }
        return false;
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(squaredDistance(x1, y1, z1, x2, y2, z2));
    }



    public static double distanceTo(BlockPos blockPos) {
        return distanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static double distanceTo(Vec3d vec3d) {
        return distanceTo(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public static double distanceTo(double x, double y, double z) {
        return Math.sqrt(squaredDistanceTo(x, y, z));
    }

    public static double squaredDistanceTo(Entity entity) {
        return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
    }

    public static double squaredDistanceTo(BlockPos blockPos) {
        return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static double squaredDistanceTo(double x, double y, double z) {
        return squaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), x, y, z);
    }

    public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double f = x1 - x2;
        double g = y1 - y2;
        double h = z1 - z2;
        return org.joml.Math.fma(f, f, org.joml.Math.fma(g, g, h * h));
    }


    public static List<Entity> getAllTargets(float range, boolean m, boolean a, boolean n, boolean p) {
        List<Entity> allEntities = new ArrayList<>();
        mc.world.getEntities().forEach(allEntities::add);

        return allEntities.stream()
                .filter(entity -> entity.isAlive())
                .filter(entity -> entity != mc.player)
                .filter(entity -> !Managers.SOCIAL.isFriend(entity.getName()))
                .filter(entity -> {
                    boolean isPlayer = entity instanceof PlayerEntity && p;
                    boolean isMonster = EntityUtil.isMonster(entity) && m;
                    boolean isNeutral = EntityUtil.isNeutral(entity) && n;
                    boolean isPassive = EntityUtil.isPassive(entity) && a;

                    return isPlayer || isMonster || isNeutral || isPassive;
                })
                .filter(entity -> mc.player.squaredDistanceTo(entity) < range * range)
                .sorted(Comparator.comparing(e -> mc.player.squaredDistanceTo(e)))
                .collect(Collectors.toList());
    }

    public @Nullable
    static Entity getNearestTargetForAll(float range, boolean m, boolean a, boolean n, boolean p) {
        return getAllTargets(range,m,a,n,p).stream()
                .min(Comparator.comparing(entity -> mc.player.distanceTo(entity)))
                .orElse(null);
    }

    public static GameMode getGameMode(PlayerEntity player)
    {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return GameMode.SPECTATOR;
        return playerListEntry.getGameMode();
    }
}
