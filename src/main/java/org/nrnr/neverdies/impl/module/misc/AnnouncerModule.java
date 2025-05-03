package org.nrnr.neverdies.impl.module.misc;

import com.google.common.collect.Maps;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.impl.event.network.PacketEvent;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.util.chat.ChatUtil;

import java.util.Map;
import java.util.UUID;

public class AnnouncerModule extends ToggleModule {

    private final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
    private final Map<UUID, PlayerEntity> logoutCache = Maps.newConcurrentMap();
    Config<Boolean> join = new BooleanConfig("Join", "w", true);
    Config<Boolean> leave = new BooleanConfig("Leave", "w", true);

    public AnnouncerModule() {
        super("Announcer", "Announces when a player joins/leaves the server.", ModuleCategory.MISCELLANEOUS);
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Inbound event) {
        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER) && join.getValue()) {
                for (PlayerListS2CPacket.Entry addedPlayer : packet.getPlayerAdditionEntries()) {
                    for (UUID uuid : logoutCache.keySet()) {
                        if (!uuid.equals(addedPlayer.profile().getId())) continue;
                        PlayerEntity player = logoutCache.get(uuid);
                        // TODO лист с возможными сообщениями
                        ChatUtil.clientSendIdPrefix(-11, "§7" + "Hey, " + player.getName().getString());
                        logoutCache.remove(uuid);
                    }
                }
            }
            playerCache.clear();
        } else if (event.getPacket() instanceof PlayerRemoveS2CPacket packet && leave.getValue()) {
            for (UUID uuid2 : packet.profileIds()) {
                for (UUID uuid : playerCache.keySet()) {
                    if (!uuid.equals(uuid2)) continue;
                    final PlayerEntity player = playerCache.get(uuid);
                    if (!logoutCache.containsKey(uuid)) {
                        // TODO лист с возможными сообщениями
                        ChatUtil.clientSendIdPrefix(-11, "§7" + "Catch ya later, " + player.getName().getString());
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
    }

    @EventListener
    public void onUpdate(PlayerTickEvent event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.equals(mc.player)) continue;
            playerCache.put(player.getGameProfile().getId(), player);
        }
    }

}
