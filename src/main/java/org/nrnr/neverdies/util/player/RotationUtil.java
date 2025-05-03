package org.nrnr.neverdies.util.player;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.nrnr.neverdies.impl.manager.player.rotation.Rotation;
import org.nrnr.neverdies.util.Globals;
import org.nrnr.neverdies.util.world.RenderUtil;

/**
 * @author chronos
 * @since 1.0
 */
public class RotationUtil implements Globals {


    /**
     * @param src
     * @param dest
     * @return
     */
    public static float[] getRotationsTo(Vec3d src, Vec3d dest) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dest.subtract(src).z,
                dest.subtract(src).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(dest.subtract(src).y,
                Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
        return new float[]
                {
                        MathHelper.wrapDegrees(yaw),
                        MathHelper.wrapDegrees(pitch)
                };
    }

    public static double getYaw(Entity entity) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX())) - 90f - mc.player.getYaw());
    }

    public static float getYaw(Vec3d pos) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pos.getZ() - mc.player.getZ(), pos.getX() - mc.player.getX())) - 90f - mc.player.getYaw());
    }



    public static float getPitch(BlockPos pos) {
        double diffX = pos.getX() + 0.5 - mc.player.getX();
        double diffY = pos.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.getZ() + 0.5 - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public static float getPitch(Vec3d pos) {
        double diffX = pos.getX() - mc.player.getX();
        double diffY = pos.getY() - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.getZ() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public static float[] getRotationsTo(Vec3d dest) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dest.subtract(mc.player.getEyePos()).z,
                dest.subtract(mc.player.getEyePos()).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(dest.subtract(mc.player.getEyePos()).y,
                Math.hypot(dest.subtract(mc.player.getEyePos()).x, dest.subtract(mc.player.getEyePos()).z)));
        return new float[]
                {
                        MathHelper.wrapDegrees(yaw),
                        MathHelper.wrapDegrees(pitch)
                };
    }

    /**
     * @param pitch
     * @param yaw
     * @return
     */
    public static Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180.0f);
        float g = -yaw * ((float) Math.PI / 180.0f);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public static Vec3d getEyesPos()
    {
        return getEyesPos(mc.player);
    }

    public static Vec3d getEyesPos(PlayerEntity player)
    {
//		return new Vec3d(player.getX(),
//				player.getY() + player.getEyeHeight(player.getPose()),
//				player.getZ());
        return mc.getBlockEntityRenderDispatcher().camera.getPos();
    }


    public static Vec3d getPlayerLookVec(PlayerEntity player)
    {
        float f = 0.017453292F;
        float pi = (float)Math.PI;

        float f1 = MathHelper.cos(-player.getYaw() * f - pi);
        float f2 = MathHelper.sin(-player.getYaw() * f - pi);
        float f3 = -MathHelper.cos(-player.getPitch() * f);
        float f4 = MathHelper.sin(-player.getPitch() * f);

        return new Vec3d(f2 * f3, f4, f1 * f3).normalize();
    }
}
