package org.nrnr.neverdies.impl.module;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.impl.event.MouseClickEvent;
import org.nrnr.neverdies.impl.event.network.PacketEvent;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.impl.module.combat.AuraModule;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.chat.ChatUtil;
import org.nrnr.neverdies.util.math.timer.TickTimer;
import org.nrnr.neverdies.util.math.timer.Timer;
import org.nrnr.neverdies.util.player.InventoryUtil;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author xgraza
 * @since 1.0
 */
public final class CrystalMacroModule extends ToggleModule {
    Config<Integer> delayConfig = new NumberConfig<>("Delay", "The delay before throttling packets again", 0, 5, 20);
    public CrystalMacroModule() {
        super("CrystalMacro", "throttle packets", ModuleCategory.Combat);
        register(delayConfig);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
    }

    private int nextAction = -1;
    boolean breakCrystal = false;
    private int delay = 0;
    private int first = -1;
    BlockHitResult hitResult = null;
    @EventListener
    public void onPlayerTick(final PlayerTickEvent ignored) {
        // mouse event fix
        if(mc.mouse.wasRightButtonClicked()) {
            if(mc.crosshairTarget.getType() == HitResult.Type.BLOCK && mc.player.getMainHandStack().getItem() instanceof SwordItem) {
                first = mc.player.getInventory().selectedSlot;
                nextAction = 2;
                hitResult = (BlockHitResult) mc.crosshairTarget;
            }
        }


        if(hitResult == null || nextAction == -1) return;
        if(delay > 0) {
            delay--;
            return;
        }

        if(breakCrystal) {
            for (Entity entity : mc.world.getEntities()) {
                if(entity.distanceTo(mc.player) <= 3 && entity instanceof EndCrystalEntity) { // legit macro, vanilla reach
                    mc.interactionManager.attackEntity(mc.player, entity);
                    breakCrystal = false;
                    break;
                }
            }
        }

        switch (nextAction) {
            case 0 -> {
                if(!breakCrystal) nextAction = 1;
                return;
            }
            case 1 -> {
                if(first != -1) {
                    mc.player.getInventory().selectedSlot = first;
                    first = -1;
                    nextAction = -1;
                }
            }
            case 2 -> {
                // place obby
                int slot = InventoryUtil.findInHotbar(Items.OBSIDIAN).slot();
                if(slot == -1) {
                    nextAction = -1;
                    return;
                }
                mc.player.getInventory().selectedSlot = slot;
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                nextAction = 3;
            }
            case 3 -> {
                // place crystal
                int slot = InventoryUtil.findInHotbar(Items.END_CRYSTAL).slot();
                if(slot == -1 || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
                    nextAction = -1;
                    return;
                }
                mc.player.getInventory().selectedSlot = slot;
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, ((BlockHitResult) mc.crosshairTarget));
                breakCrystal = true;
                nextAction = 0;
            }
        }

        delay += delayConfig.getValue();
    }
}
