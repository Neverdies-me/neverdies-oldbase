package org.nrnr.neverdies.impl.module.render;


import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.util.math.timer.CacheTimer;
import org.nrnr.neverdies.util.math.timer.Timer;

import java.awt.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class CircleModule extends ToggleModule {
    private final ConcurrentHashMap<Entity, Circle> cryList = new ConcurrentHashMap<Entity, Circle>();
    private final Timer timer = new CacheTimer();
    Config<Integer> rangeValue = new NumberConfig<>("Range", "range check for render", 0, 12, 256);
    Config<Color> color = new ColorConfig("Color", "w", new Color(255, 255, 255, 150));
    Config<Float> speed = new NumberConfig<>("Speed", "w", 0f, 6f, 15f);
    Config<Integer> time = new NumberConfig<>("Time", "w", 0, 6, 15);
    Config<Integer> points = new NumberConfig<>("Points", "w", 1, 3, 10);
    //    Config<Type> type = new EnumConfig<>("Type","d",Type.Scale,Type.values(),()->spawns.isOpen());
    Config<Boolean> spawns = new BooleanConfig("Spawn", "targets", true).setParent();
    Config<Boolean> jump = new BooleanConfig("Jump", "targets", false).setParent();
    Config<Boolean> self = new BooleanConfig("Only self", "", true, () -> jump.isOpen());
    public CircleModule() {
        super("Circle", "Draw circle", ModuleCategory.RENDER);
    }


    @EventListener
    public void onRender3D(RenderWorldEvent event) {
        if (spawns.getValue()) {
            for (Entity e : new Iterable<Entity>() {
                @Override
                public Iterator<Entity> iterator() {
                    return mc.world.getEntities().iterator();
                }
            }) {
                if (e instanceof EndCrystalEntity) {
                    if (mc.player.distanceTo(e) > rangeValue.getValue()) continue;
                    if (!cryList.containsKey(e)) {
                        cryList.put(e, new Circle(e, System.currentTimeMillis()));
                    }
                    // JUMP
                } else if (e instanceof PlayerEntity pl && jump.getValue()) {
                    if (mc.player.distanceTo(e) > rangeValue.getValue()) continue;
                    if (pl != null && !pl.isOnGround()) {
                        if (self.getValue()) {
                            if (pl == mc.player) {
                                cryList.put(e, new Circle(e, System.currentTimeMillis()));
                            }
                        } else {
                            cryList.put(e, new Circle(e, System.currentTimeMillis()));
                        }
                    }
                }
            }
            var time = 0;
            for (int i = 0; i < points.getValue(); i++) {
                if (timer.passed(500)) {
                    int finalTime = time;
                    cryList.forEach((e, renderInfo) ->
                            render(event.getMatrices(), renderInfo.entity, renderInfo.time - finalTime, renderInfo.time - finalTime, event)
                    );
                }
                time += 1;
            }
            cryList.forEach((e, renderInfo) -> {
                if (((System.currentTimeMillis() - renderInfo.time) > (this.time.getValue() * 100)) && !e.isAlive()) {
                    cryList.remove(e);
                }
                if (((System.currentTimeMillis() - renderInfo.time) > (this.time.getValue() * 100)) && mc.player.distanceTo(e) > rangeValue.getValue()) {
                    cryList.remove(e);
                }
            });
        }
    }

    private void render(MatrixStack matrixStack, Entity entity, long radTime, long heightTime, RenderWorldEvent event) {
        var rad = System.currentTimeMillis() - radTime;
        var height = System.currentTimeMillis() - heightTime;
        if (rad <= (time.getValue() * 100)) {

            float radius = rad / (speed.getValue() * 100f);
            float heightOffset = height / 1000f;
            cir(matrixStack, entity, radius, heightOffset, color.getValue(), event, false);

        }
    }
    public static void cir(MatrixStack stack, Entity ent, float radius, float height, Color color, RenderWorldEvent event, boolean yoffset) {
        event.getMatrices().push();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        double x = ent.prevX + (ent.getX() - ent.prevX) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = ent.prevY + (ent.getY() - ent.prevY) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = ent.prevZ + (ent.getZ() - ent.prevZ) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        stack.push();
        stack.translate(x, y, z);
        Matrix4f matrix = stack.peek().getPositionMatrix();
        for (int i = 0; i <= 180; i++) {
            bufferBuilder.vertex(matrix, (float) (radius * Math.cos(i * 6.28 / 45)), 0f, (float) (radius * Math.sin(i * 6.28 / 45))).color(color.getRGB()).next();
        }
        tessellator.draw();
        stack.translate(-x, -y + height, -z);
        event.getMatrices().pop();
        stack.pop();
    }
    @Override
    public void onDisable() {
        cryList.clear();
    }

    public enum Mode {
        Alternative,
        Normal
    }


    public enum Type {
        Yoffset,
        Scale
    }

    record Circle(Entity entity, long time) {
    }

}
