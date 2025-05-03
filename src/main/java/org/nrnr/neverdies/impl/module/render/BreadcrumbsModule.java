package org.nrnr.neverdies.impl.module.render;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.EventStage;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.api.render.RenderManagerWorld;
import org.nrnr.neverdies.impl.event.network.PlayerUpdateEvent;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.impl.manager.world.Render3DHelper;
import org.nrnr.neverdies.util.world.PositionTime;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class BreadcrumbsModule extends ToggleModule {
    private final List<PositionTime> positions = new CopyOnWriteArrayList<>();
    ColorConfig pcolor = new ColorConfig("Color", "The breadcrums color", new Color(157, 69, 179), true, true);
    private final Config<Integer> w = new NumberConfig<>("Width", "w", 1, 3, 10);
    private final Config<Integer> limit = new NumberConfig<>("Limit", "w", 1, 3, 100);

    public BreadcrumbsModule() {
        super("Breadcrumbs", "Draw lines on move", ModuleCategory.RENDER);
    }

    @EventListener
    public void onRender3D(RenderWorldEvent event) {
        RenderManagerWorld.post(() -> {
            drawLine(w.getValue(), false);
        });
    }

    public void drawLine(float width, boolean white) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(width);
        buffer.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        long currentTime = System.currentTimeMillis();

        for (int i = 0; i < positions.size(); i++) {
            PositionTime pt1 = i > 0 ? positions.get(i - 1) : null;
            PositionTime pt2 = positions.get(i);
            if (pt1 != null && pt2 != null) {
                Vec3d vec1 = pt1.getPosition();
                Vec3d vec2 = pt2.getPosition();

                Color c = pcolor.getValue();
                if (white) c = Color.WHITE;

                // Fade out the color over time
                long age = currentTime - pt2.getTime();
                float alpha = Math.max(0, 1 - (age / (limit.getValue() * 100)));
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * alpha));

                MatrixStack matrices = Render3DHelper.matrixFrom(vec1.getX(), vec1.getY(), vec1.getZ());
                Render3DHelper.vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (vec2.getX() - vec1.getX()), (float) (vec2.getY() - vec1.getY()), (float) (vec2.getZ() - vec1.getZ()), c);
            }
        }

        tessellator.draw();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @EventListener
    public void postSync(PlayerUpdateEvent event) {
        if (event.getStage() != EventStage.POST) return;
        positions.add(new PositionTime(new Vec3d(mc.player.getX(), mc.player.getBoundingBox().minY, mc.player.getZ())));
    }
}
