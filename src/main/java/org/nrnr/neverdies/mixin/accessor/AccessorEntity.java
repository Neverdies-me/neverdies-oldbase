package org.nrnr.neverdies.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor interface for modifying and accessing private or protected members of {@link Entity}.
 */
@Mixin(Entity.class)
public interface AccessorEntity {

    /**
     * Invokes the private method `unsetRemoved` on the {@link Entity} class.
     */
    @Invoker("unsetRemoved")
    void invokeUnsetRemoved();

    /**
     * Invokes the private method `setFlag` on the {@link Entity} class.
     *
     * @param index the index of the flag to set.
     * @param value the value to set for the flag.
     */
    @Invoker("setFlag")
    void invokeSetFlag(int index, boolean value);

    /**
     * Invokes the private method `getFlag` on the {@link Entity} class.
     *
     * @param index the index of the flag to retrieve.
     * @return the value of the specified flag.
     */
    @Invoker("getFlag")
    boolean invokeGetFlag(int index);

    /**
     * Sets the `pos` field of the {@link Entity}.
     *
     * @param pos the new position to set.
     */
    @Mutable
    @Accessor("pos")
    void setPosition(Vec3d pos);

    /**
     * Retrieves the `dimensions` field of the {@link Entity}.
     *
     * @return the {@link EntityDimensions} of the entity.
     */
    @Accessor("dimensions")
    EntityDimensions getDimensions();
}
