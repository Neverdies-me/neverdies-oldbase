package org.nrnr.neverdies.util.world;

import net.minecraft.util.math.Vec3d;

public class PositionTime {
    private final Vec3d position;
    private final long time;

    public PositionTime(Vec3d position) {
        this.position = position;
        this.time = System.currentTimeMillis();
    }

    public Vec3d getPosition() {
        return position;
    }

    public long getTime() {
        return time;
    }
}