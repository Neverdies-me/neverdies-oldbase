package org.nrnr.neverdies.impl.module.movement;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.ToggleModule;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.util.HoleUtil;

/**
 * @author heedi
 * @since 1.0
 */

public class AnchorModule extends ToggleModule {

    // Configuration for pitch threshold
    private final Config<Integer> pitchThreshold = new NumberConfig<>("Pitch", "Pitch angle to activate the module", 0, 50, 90);

    // Constructor initializing the module
    public AnchorModule() {
        super("Anchor", "Helps you move into holes quickly and easily.", ModuleCategory.MOVEMENT);
    }

    // Event listener for player tick updates
    @EventListener
    public void onPlayerTickEvent(PlayerTickEvent event) {
        // Check if the player's pitch is greater than the threshold
        if (mc.player.getPitch() > pitchThreshold.getValue()) {
            BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());

            // Check for nearby holes at varying depths
            if (isHoleNearby(playerPos)) {
                centerPlayerToHole();
            }
        }
    }

    // Helper method to check for nearby holes
    private boolean isHoleNearby(BlockPos playerPos) {
        for (int depth = 1; depth <= 3; depth++) {
            if (HoleUtil.isHole(playerPos, depth)) {
                return true;
            }
        }
        return false;
    }

    // Helper method to center the player within the hole
    private void centerPlayerToHole() {
        Vec3d playerPosition = mc.player.getPos();
        Vec3d holeCenter = new Vec3d(
                Math.floor(playerPosition.getX()) + 0.5,
                Math.floor(playerPosition.getY()),
                Math.floor(playerPosition.getZ()) + 0.5
        );

        // Adjust player velocity to center them within the hole
        if (Math.abs(holeCenter.x - playerPosition.getX()) > 0.1 || Math.abs(holeCenter.z - playerPosition.getZ()) > 0.1) {
            double xVelocity = Math.min((holeCenter.x - playerPosition.getX()) / 2.0, 0.2);
            double zVelocity = Math.min((holeCenter.z - playerPosition.getZ()) / 2.0, 0.2);
            mc.player.setVelocity(xVelocity, mc.player.getVelocity().getY(), zVelocity);
        }
    }
}
