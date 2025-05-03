package org.nrnr.neverdies.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.nrnr.neverdies.util.Globals.mc;

/**
 * @author heedi
 * @since 1.0
 */

public class HoleUtil {

    private static final Vec3i[] VECTOR_PATTERN = {
            new Vec3i(0, 0, 1),
            new Vec3i(0, 0, -1),
            new Vec3i(1, 0, 0),
            new Vec3i(-1, 0, 0)
    };

    public static boolean isHole(BlockPos pos, int depth) {
        BlockPos adjustedPos = pos.down(depth);
        return isSingleHole(adjustedPos)
                || isDoubleBedrock(adjustedPos)
                || isDoubleResistant(adjustedPos)
                || isValidQuadIndestructible(adjustedPos)
                || isValidQuadBedrock(adjustedPos)
                || isValidBedrock(adjustedPos)
                || isSingleResistance(adjustedPos);
    }

    public static boolean isSingleHole(BlockPos pos) {
        return isSingleResistance(pos) || isSingleBedrock(pos);
    }

    public static boolean isSingleResistance(@NotNull BlockPos pos) {
        return !isSingleBedrock(pos) && areSurroundingsIndestructibleOrBedrock(pos) && isVerticalSpaceReplaceable(pos);
    }

    public static boolean isSingleBedrock(@NotNull BlockPos pos) {
        return areSurroundingsBedrock(pos) && isVerticalSpaceReplaceable(pos);
    }

    public static boolean isValidBedrock(@NotNull BlockPos pos) {
        return areSurroundingsBedrock(pos) && isVerticalSpaceReplaceable(pos);
    }

    public static boolean isDoubleBedrock(@NotNull BlockPos pos) {
        Vec3i direction = findTwoBlockDirection(pos);
        if (direction == null || !isReplaceable(pos)) return false;

        return checkSurroundings(pos, pos.add(direction), true, false);
    }

    public static boolean isDoubleResistant(@NotNull BlockPos pos) {
        Vec3i direction = findTwoBlockDirection(pos);
        if (direction == null || !isReplaceable(pos)) return false;

        return checkSurroundings(pos, pos.add(direction), true, true);
    }

    public static boolean isValidQuadIndestructible(@NotNull BlockPos pos) {
        List<BlockPos> quadPositions = findQuadPositions(pos);
        if (quadPositions == null) return false;

        return checkQuadSurroundings(quadPositions, true);
    }

    public static boolean isValidQuadBedrock(@NotNull BlockPos pos) {
        List<BlockPos> quadPositions = findQuadPositions(pos);
        if (quadPositions == null) return false;

        return checkQuadSurroundings(quadPositions, false);
    }

    private static boolean areSurroundingsIndestructibleOrBedrock(BlockPos pos) {
        for (Vec3i vec : VECTOR_PATTERN) {
            BlockPos adjacent = pos.add(vec);
            if (!isIndestructible(adjacent) && !isBedrock(adjacent)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areSurroundingsBedrock(BlockPos pos) {
        for (Vec3i vec : VECTOR_PATTERN) {
            BlockPos adjacent = pos.add(vec);
            if (!isBedrock(adjacent)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isVerticalSpaceReplaceable(BlockPos pos) {
        return isReplaceable(pos) && isReplaceable(pos.up()) && isReplaceable(pos.up(2));
    }

    private static boolean checkSurroundings(BlockPos pos1, BlockPos pos2, boolean checkIndestructible, boolean allowIndestructible) {
        BlockPos[] positions = {pos1, pos2};
        boolean foundIndestructible = false;

        for (BlockPos pos : positions) {
            if (!isValidDownBlock(pos, allowIndestructible)) return false;

            for (Vec3i vec : VECTOR_PATTERN) {
                BlockPos adjacent = pos.add(vec);
                if (isIndestructible(adjacent)) {
                    foundIndestructible = true;
                } else if (!isBedrock(adjacent) && !adjacent.equals(pos1) && !adjacent.equals(pos2)) {
                    return false;
                }
            }
        }

        return !checkIndestructible || foundIndestructible;
    }

    private static boolean checkQuadSurroundings(List<BlockPos> quadPositions, boolean allowIndestructible) {
        boolean foundIndestructible = false;

        for (BlockPos pos : quadPositions) {
            if (!isValidDownBlock(pos, allowIndestructible)) return false;

            for (Vec3i vec : VECTOR_PATTERN) {
                BlockPos adjacent = pos.add(vec);
                if (isIndestructible(adjacent)) {
                    foundIndestructible = true;
                } else if (!isBedrock(adjacent) && !quadPositions.contains(adjacent)) {
                    return false;
                }
            }
        }

        return !allowIndestructible || foundIndestructible;
    }

    private static boolean isValidDownBlock(BlockPos pos, boolean allowIndestructible) {
        BlockPos down = pos.down();
        return allowIndestructible ? isIndestructible(down) || isBedrock(down) : isBedrock(down);
    }

    private static @Nullable Vec3i findTwoBlockDirection(BlockPos pos) {
        for (Vec3i vec : VECTOR_PATTERN) {
            if (isReplaceable(pos.add(vec))) return vec;
        }
        return null;
    }

    private static @Nullable List<BlockPos> findQuadPositions(@NotNull BlockPos pos) {
        List<BlockPos> quadPositions = new ArrayList<>();
        quadPositions.add(pos);

        if (!isReplaceable(pos)) return null;

        Vec3i[][] patterns = {
                {new Vec3i(1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(1, 0, 1)},
                {new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(-1, 0, -1)},
                {new Vec3i(1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(1, 0, -1)},
                {new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1)}
        };

        for (Vec3i[] pattern : patterns) {
            if (isReplaceable(pos.add(pattern[0])) && isReplaceable(pos.add(pattern[1])) && isReplaceable(pos.add(pattern[2]))) {
                for (Vec3i vec : pattern) {
                    quadPositions.add(pos.add(vec));
                }
                return quadPositions;
            }
        }

        return null;
    }

    private static boolean isIndestructible(BlockPos pos) {
        Block block = getBlock(pos);
        return block == Blocks.OBSIDIAN || block == Blocks.NETHERITE_BLOCK || block == Blocks.CRYING_OBSIDIAN || block == Blocks.RESPAWN_ANCHOR;
    }

    private static boolean isBedrock(BlockPos pos) {
        return getBlock(pos) == Blocks.BEDROCK;
    }

    private static boolean isReplaceable(BlockPos pos) {
        return mc.world != null && mc.world.getBlockState(pos).isReplaceable();
    }

    private static Block getBlock(BlockPos pos) {
        return mc.world != null ? mc.world.getBlockState(pos).getBlock() : Blocks.AIR;
    }

    public static BlockPos addToPlayer(@NotNull BlockPos playerPos, double x, double y, double z) {
        return playerPos.add(new BlockPos(
                adjustForNegative(playerPos.getX(), x),
                adjustForNegative(playerPos.getY(), y),
                adjustForNegative(playerPos.getZ(), z)
        ));
    }

    private static int adjustForNegative(int coordinate, double offset) {
        return coordinate < 0 ? (int) -offset : (int) offset;
    }
}
