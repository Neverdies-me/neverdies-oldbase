package org.nrnr.neverdies.impl.manager.world;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.nrnr.neverdies.util.Globals.mc;


public class Render3DHelper {
    /** better renderer **/
    public static void box(MatrixStack matrixStack,Box box, int sideColor, int lineColor, boolean side,boolean lines, float width) {
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.getCamera().getYaw() + 180.0f));
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        mcBox(matrixStack, box.offset(-camPos.x, -camPos.y, -camPos.z), sideColor, lineColor, side,lines, width);
        matrixStack.pop();
    }

    public static void boxc(MatrixStack matrixStack,Box box, int sideColor, int lineColor, boolean side,boolean lines, float width,boolean shine,boolean walls) {
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.getCamera().getYaw() + 180.0f));
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        mcBoxc(matrixStack, box.offset(-camPos.x, -camPos.y, -camPos.z), sideColor, lineColor, side,lines, width,shine,walls);
        matrixStack.pop();
    }
    public static void mcBoxc(MatrixStack stack, Box box, int sideColor, int lineColor, boolean lines,boolean side, float width,boolean shine,boolean walls) {
        start(walls,shine);
        if (side) {
            renderSides(stack, box, sideColor);
        }
        if (lines) {
            renderOutlines(stack, box, lineColor, width);
        }
        end(walls);
    }
    public static void mcBox(MatrixStack stack, Box box, int sideColor, int lineColor, boolean lines,boolean side, float width) {
        start(true,false);
        if (side) {
            renderSides(stack, box, sideColor);
        }
        if (lines) {
            renderOutlines(stack, box, lineColor, width);
        }
        end(true);
    }

    public static void renderOutlines(MatrixStack stack, Box box, int color, float width) {
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        drawOutlines(stack, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ, ColorHelper.Argb.getRed(color) / 255.0f, ColorHelper.Argb.getGreen(color) / 255.0f, ColorHelper.Argb.getBlue(color) / 255.0f, ColorHelper.Argb.getAlpha(color) / 255.0f);
        tessellator.draw();
    }

    public static void drawOutlines(MatrixStack stack, VertexConsumer vertexConsumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        Matrix3f matrix3f = stack.peek().getNormalMatrix();
        line(matrix4f, matrix3f, vertexConsumer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);

        line(matrix4f, matrix3f, vertexConsumer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);

        line(matrix4f, matrix3f, vertexConsumer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(matrix4f, matrix3f, vertexConsumer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
    }

    private static void line(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexConsumer, float sx, float sy, float sz, float ex, float ey, float ez, float r, float g, float b, float a) {
        float dx = ex - sx;
        float dy = ey - sy;
        float dz = ez - sz;
        float length = (float) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
        float nx = dx / length;
        float ny = dy / length;
        float nz = dz / length;
        vertexConsumer.vertex(matrix4f, sx, sy, sz).color(r, g, b, a).normal(matrix3f, nx, ny, nz).next();
        vertexConsumer.vertex(matrix4f, ex, ey, ez).color(r, g, b, a).normal(matrix3f, nx, ny, nz).next();
    }

    public static void renderSides(MatrixStack stack, Box box, int color) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        drawSides(stack, bufferBuilder, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ, ColorHelper.Argb.getRed(color) / 255.0f, ColorHelper.Argb.getGreen(color) / 255.0f, ColorHelper.Argb.getBlue(color) / 255.0f, ColorHelper.Argb.getAlpha(color) / 255.0f);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawSides(MatrixStack stack, VertexConsumer vertexConsumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, minY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, minZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, minY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, minX, maxY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, maxY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
        vertexConsumer.vertex(matrix4f, maxX, minY, maxZ).color(r, g, b, a).normal(0.0f, 0.0f, 0.0f).next();
    }


    public static void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();
        Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);
        buffer.vertex(model, x1, y1, z1).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(normal, normalVec.x(), normalVec.y(), normalVec.z()).next();
        buffer.vertex(model, x2, y2, z2).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(normal, normalVec.x(), normalVec.y(), normalVec.z()).next();
    }

    public static @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);

        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static @NotNull MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);

        return matrices;
    }

    public static void Line(MatrixStack matrixStack, Vec3d start, Vec3d end, Color color, float lineWidth) {
        GL11.glLineWidth(lineWidth);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        RenderSystem.setShader(GameRenderer::getPositionProgram);

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).next();
        bufferBuilder.vertex(matrix, (float) end.x, (float) end.y, (float) end.z).next();

        tessellator.draw();

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
    public static void end(boolean walls) {
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
    public static void start(boolean walls,boolean altblend) {
        RenderSystem.enableBlend();
        if (altblend) {RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);}
        else {RenderSystem.defaultBlendFunc();}
        RenderSystem.disableCull();
        if (walls) RenderSystem.disableDepthTest();
        else RenderSystem.enableDepthTest();
    }

}