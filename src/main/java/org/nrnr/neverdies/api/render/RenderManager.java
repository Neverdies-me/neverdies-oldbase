package org.nrnr.neverdies.api.render;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.nrnr.neverdies.impl.font.FontRenderers;
import org.nrnr.neverdies.init.Fonts;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.mixin.accessor.AccessorWorldRenderer;
import org.nrnr.neverdies.util.Globals;

import static org.nrnr.neverdies.api.render.RenderBuffers.LINES;
import static org.nrnr.neverdies.api.render.RenderBuffers.QUADS;


public class RenderManager implements Globals {
    //
    public static final Tessellator TESSELLATOR = RenderSystem.renderThreadTesselator();
    public static final BufferBuilder BUFFER = TESSELLATOR.getBuffer();

    public static void post(Runnable callback) {
        RenderBuffers.post(callback);
    }

    public static void renderBox(MatrixStack matrices, BlockPos p, int color) {
        renderBox(matrices, new Box(p), color);
    }

    public static void renderBox(MatrixStack matrices, Box box, int color) {
        if (!isFrustumVisible(box)) {
            return;
        }
        matrices.push();
        drawBox(matrices, box, color);
        matrices.pop();
    }
    public static void drawBox(MatrixStack matrices, Box box, int color) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        drawBox(matrices, minX, minY, minZ, maxX, maxY, maxZ, color);
    }

    public static void drawBox(MatrixStack matrices, double x1, double y1,
                               double z1, double x2, double y2, double z2, int color) {
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        QUADS.begin(matrix4f);
        QUADS.color(color);

        QUADS.vertex(x1, y1, z1).vertex(x2, y1, z1).vertex(x2, y1, z2).vertex(x1, y1, z2);
        QUADS.vertex(x1, y2, z1).vertex(x1, y2, z2).vertex(x2, y2, z2).vertex(x2, y2, z1);
        QUADS.vertex(x1, y1, z1).vertex(x1, y2, z1).vertex(x2, y2, z1).vertex(x2, y1, z1);
        QUADS.vertex(x2, y1, z1).vertex(x2, y2, z1).vertex(x2, y2, z2).vertex(x2, y1, z2);
        QUADS.vertex(x1, y1, z2).vertex(x2, y1, z2).vertex(x2, y2, z2).vertex(x1, y2, z2);
        QUADS.vertex(x1, y1, z1).vertex(x1, y1, z2).vertex(x1, y2, z2).vertex(x1, y2, z1);

        QUADS.end();
    }

    public static void renderBoundingBox(MatrixStack matrices, BlockPos p,
                                         float width, int color) {
        renderBoundingBox(matrices, new Box(p), width, color);
    }

    public static void renderBoundingBox(MatrixStack matrices, Box box,
                                         float width, int color) {
        if (!isFrustumVisible(box)) {
            return;
        }
        matrices.push();
        RenderSystem.lineWidth(width);
        drawBoundingBox(matrices, box, color);
        matrices.pop();
    }

    public static void drawBoundingBox(MatrixStack matrices, Box box, int color) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        drawBoundingBox(matrices, minX, minY, minZ, maxX, maxY, maxZ, color);
    }

    public static void drawBoundingBox(MatrixStack matrices, double x1, double y1,
                                       double z1, double x2, double y2, double z2, int color) {
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        LINES.begin(matrix4f);
        LINES.color(color);

        LINES.vertex(x1, y1, z1).vertex(x2, y1, z1);
        LINES.vertex(x2, y1, z1).vertex(x2, y1, z2);
        LINES.vertex(x2, y1, z2).vertex(x1, y1, z2);
        LINES.vertex(x1, y1, z2).vertex(x1, y1, z1);

        LINES.vertex(x1, y1, z1).vertex(x1, y2, z1);
        LINES.vertex(x2, y1, z1).vertex(x2, y2, z1);
        LINES.vertex(x2, y1, z2).vertex(x2, y2, z2);
        LINES.vertex(x1, y1, z2).vertex(x1, y2, z2);

        LINES.vertex(x1, y2, z1).vertex(x2, y2, z1);
        LINES.vertex(x2, y2, z1).vertex(x2, y2, z2);
        LINES.vertex(x2, y2, z2).vertex(x1, y2, z2);
        LINES.vertex(x1, y2, z2).vertex(x1, y2, z1);

        LINES.end();
    }
    // line
    public static void renderLine(MatrixStack matrices, Vec3d s,
                                  Vec3d d, int color) {
        drawLine(matrices, s.x, s.y, s.z, d.x, d.y, d.z, color);
    }

    public static void drawLine(MatrixStack matrices, double x1, double y1,
                                double z1, double x2, double y2, double z2, int color) {
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        LINES.begin(matrix4f);
        LINES.color(color);
        LINES.vertex(x1 - mc.getEntityRenderDispatcher().camera.getPos().getX(), y1 - mc.getEntityRenderDispatcher().camera.getPos().getY(), z1 - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        LINES.vertex(x2 - mc.getEntityRenderDispatcher().camera.getPos().getX(), y2 - mc.getEntityRenderDispatcher().camera.getPos().getY(), z2 - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        LINES.end();
    }
    // text 2d
    // drawcontext
    public static void renderText(DrawContext context, String text, float x, float y, int color) {
        if (!Modules.CUSTOM_FONT.isEnabled()) {
            context.drawText(mc.textRenderer, text, (int) x, (int) y, color, true);
        } else {
            FontRenderers.Verdana.drawString(context.getMatrices(), text, x, y, color);
        }
    }
    public static void renderText(DrawContext context, String text, float x, float y, int color,float scale) {
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        if (!Modules.CUSTOM_FONT.isEnabled()) {
            context.drawText(mc.textRenderer, text, (int) (x / scale), (int) (y / scale), color, true);
        } else {
            FontRenderers.Verdana.drawString(context.getMatrices(), text, (x / scale), (y / scale), color);
        }
        context.getMatrices().pop();
    }
    // matrixstack
    public static void renderText(MatrixStack matrixStack, String text, float x, float y, int color) {
        if (!Modules.CUSTOM_FONT.isEnabled()) {
            Fonts.VANILLA.drawWithShadow(matrixStack, text, (int) x, (int) y, color);
        } else {
            FontRenderers.Verdana.drawString(matrixStack, text, x, y, color);
        }
    }

    public static int textWidth(String text) {
        if (!Modules.CUSTOM_FONT.isEnabled()) {
            return mc.textRenderer.getWidth(text);
        } else {
            return FontRenderers.Verdana.getWidth(text);
        }
    }
    public static int textHeight(String text) {
        if (!Modules.CUSTOM_FONT.isEnabled()) {
            return mc.textRenderer.fontHeight;
        } else {
            //хз
            return (int) FontRenderers.Verdana.getFontHeight(text);
        }
    }
    // text 3d

    /**
     * @param pos
     */
    public static void renderSign(MatrixStack matrices, String text, Vec3d pos, int color) {
        renderSign(matrices, text, pos.getX(), pos.getY(), pos.getZ(), color);
    }

    public static void renderSign(MatrixStack matrices, String text, Vec3d pos) {
        renderSign(matrices, text, pos.getX(), pos.getY(), pos.getZ(), -1);
    }

    public static void renderSign(MatrixStack matrices, String text,
                                  double x1, double x2, double x3, int color) {
        double dist = Math.sqrt(mc.player.squaredDistanceTo(x1, x2, x3));
        float scaling = 0.0018f + Modules.NAMETAGS.getScaling() * (float) dist;
        if (dist <= 8.0) {
            scaling = 0.0245f;
        }
        Camera camera = mc.gameRenderer.getCamera();
        final Vec3d pos = camera.getPos();
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrixStack.translate(x1 - pos.getX(), x2 - pos.getY(), x3 - pos.getZ());
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        matrixStack.scale(-scaling, -scaling, -1.0f);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        VertexConsumerProvider.Immediate vertexConsumers =
                VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        float hwidth = mc.textRenderer.getWidth(text) / 2.0f;
        renderText(matrixStack, text, -hwidth, 0.0f, color);
        vertexConsumers.draw();
        RenderSystem.disableBlend();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        matrixStack.pop();
    }
    public static void Text3d(MatrixStack matrices, String text,
                              double x1, double x2, double x3, int color) {
        double dist = Math.sqrt(mc.player.squaredDistanceTo(x1, x2, x3));
        float scaling = 0.0010f + Modules.NAMETAGS.getScaling() * (float) dist;
        if (dist <= 10.0) {
            scaling = 0.0245f;
        }
        Camera camera = mc.gameRenderer.getCamera();
        final Vec3d pos = camera.getPos();
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrixStack.translate(x1 - pos.getX(), x2 - pos.getY(), x3 - pos.getZ());
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        matrixStack.scale(-scaling, -scaling, -1.0f);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        VertexConsumerProvider.Immediate vertexConsumers =
                VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        float hwidth = mc.textRenderer.getWidth(text) / 2.0f;
        renderText(matrixStack, text, -hwidth, 0.0f, color);
        vertexConsumers.draw();
        RenderSystem.disableBlend();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        matrixStack.pop();
    }
    public static void renderSign(String text, Entity entity) {
        RenderManager.post(() -> {
            Vec3d interpolate = Interpolation.getRenderPosition(mc.getCameraEntity(), mc.getTickDelta());
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d pos = camera.getPos();
            Vec3d pinterpolate = Interpolation.getRenderPosition(entity, mc.getTickDelta());
            double rx = entity.getX() - pinterpolate.getX();
            double ry;
            ry = entity.getY() - pinterpolate.getY();
            double rz = entity.getZ() - pinterpolate.getZ();
            float width = RenderManager.textWidth(text);
            float hwidth = width / 2.0f;
            double dx = (pos.getX() - interpolate.getX()) - rx;
            double dy = (pos.getY() - interpolate.getY()) - ry;
            double dz = (pos.getZ() - interpolate.getZ()) - rz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 4096.0) {
                return;
            }
            float scaling = 0.0018f + Modules.NAMETAGS.getScaling() * (float) dist;
            if (dist <= 8.0) {
                scaling = 0.0245f;
            }
            renderSign(text, hwidth, entity, rx, ry, rz, camera, scaling, -1);
        });
    }

    private static void renderSign(String info, float width, Entity entity,
                                   double x, double y, double z, Camera camera, float scaling, int color) {
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
        RenderManager.post(() -> {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            GL11.glDepthFunc(GL11.GL_ALWAYS);

            RenderManager.renderText(matrices, info, -width, 0.0f, color);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            RenderSystem.disableBlend();
        });
    }

    public static boolean isFrustumVisible(Box box) {
        return ((AccessorWorldRenderer) mc.worldRenderer).getFrustum().isVisible(box);
    }


}