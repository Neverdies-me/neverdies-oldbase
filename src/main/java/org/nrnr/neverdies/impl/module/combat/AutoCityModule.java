package org.nrnr.neverdies.impl.module.combat;

import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.RotationModule;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.impl.module.world.SpeedmineModule;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.math.position.PositionUtil;
import org.nrnr.neverdies.util.math.timer.CacheTimer;
import org.nrnr.neverdies.util.math.timer.Timer;
import org.nrnr.neverdies.util.player.RotationUtil;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author sk33t
 */
public class AutoCityModule extends RotationModule {

    private static final List<Block> SHULKER_BLOCKS = new ArrayList<>() {{
        add(Blocks.SHULKER_BOX);
        add(Blocks.BLACK_SHULKER_BOX);
        add(Blocks.BLUE_SHULKER_BOX);
        add(Blocks.BROWN_SHULKER_BOX);
        add(Blocks.CYAN_SHULKER_BOX);
        add(Blocks.GRAY_SHULKER_BOX);
        add(Blocks.GREEN_SHULKER_BOX);
        add(Blocks.LIGHT_BLUE_SHULKER_BOX);
        add(Blocks.LIGHT_GRAY_SHULKER_BOX);
        add(Blocks.LIME_SHULKER_BOX);
        add(Blocks.MAGENTA_SHULKER_BOX);
        add(Blocks.ORANGE_SHULKER_BOX);
        add(Blocks.PINK_SHULKER_BOX);
        add(Blocks.PURPLE_SHULKER_BOX);
        add(Blocks.RED_SHULKER_BOX);
        add(Blocks.WHITE_SHULKER_BOX);
        add(Blocks.YELLOW_SHULKER_BOX);
    }};
    private final Timer pause = new CacheTimer();
    Config<Boolean> autoMineConfig = new BooleanConfig("AutoMine", "Autoselect for AutoMine", true).setParent();
    Config<Boolean> doubleMine = new BooleanConfig("DoubleMine", "Doublemine for AutoMine", true,() -> autoMineConfig.isOpen());
    Config<Boolean> rotate = new BooleanConfig("Rotate", "Swing hand when start mining", true);
    Config<prio> priority = new EnumConfig<>("Priority", "Priority players for automine", prio.Distance, prio.values());
    Config<Float> range = new NumberConfig<>("Range", "Range for mining", 0f, 5f, 6f);
    Config<Boolean> dir = new BooleanConfig("StrictDirection", "Only mines on visible faces", false);
    // Targets
    Config<Boolean> surround = new BooleanConfig("Surround", "Automatically mines nearby players feet", true);
    Config<Boolean> burrow = new BooleanConfig("Burrow", "Automatically mines nearby players burrow", true);
    //    Config<Boolean> face = new BooleanConfig("Trap", "Automatically mines nearby players trap blocks", true);
//    Config<Boolean> head = new BooleanConfig("Cev", "Automatically mines nearby players up-head block", true);//.setParent();
    //    Config<Boolean> fish1 = new BooleanConfig("Fish", "Automatically mines nearby swimming players up-head block", true,()->head.isOpen());
    Config<Boolean> fish = new BooleanConfig("AntiTrap", "Automatically mines self up or down block (if u in swimmingpos)", true); // antifish
    Config<Boolean> autofish = new BooleanConfig("AutoFish", "Automatically mines down blocks", true); // antifish
    Config<Boolean> antiShulker = new BooleanConfig("AntiRegear", "Automatically mines self up block (if u in swimmingpos)", true);
    private BlockPos blockPos;

    public AutoCityModule() {
        super("AutoCity", "Automatically mines blocks", ModuleCategory.Combat, 1000);
    }

    @EventListener
    public void onTick(PlayerTickEvent event) {
        if (mc.player == null
                || mc.world == null
                || mc.interactionManager == null
                || mc.player.getAbilities().creativeMode) return;
        final PlayerEntity target = getTargetPlayer();

        if (target == null) return;
        if (!pause.passed(600))
            return;

        if (blockPos != null) {
            if (mc.world.isAir(blockPos) || mc.player.squaredDistanceTo(blockPos.toCenterPos()) > ((NumberConfig<Float>) range).getValueSq()) {
                blockPos = null;
                return;
            }

            if (Modules.PACKETMINE.isEnabled()) {
                if (SpeedmineModule.minePosition == blockPos || (SpeedmineModule.minePosition != null && !mc.world.isAir(SpeedmineModule.minePosition)))
                    return;
                float[] angle = RotationUtil.getRotationsTo(blockPos.toCenterPos());
                if (rotate.getValue()) Managers.ROTATION.setRotationSilent(angle[0],angle[1],false); ;
                mc.interactionManager.attackBlock(blockPos, getdir(blockPos, true, false));
            }
            else if (autoMineConfig.getValue()){
                float[] angle = RotationUtil.getRotationsTo(blockPos.toCenterPos());
                if (rotate.getValue()) Managers.ROTATION.setRotationSilent(angle[0],angle[1],false); ;
                mc.interactionManager.attackBlock(blockPos, getdir(blockPos, true, false));
            }

            else Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }


        // AntiFish
        if (fish.getValue()) {
            if (mc.player.isInSwimmingPose()) {
                BlockPos posAbove = mc.player.getBlockPos().up();
                BlockPos posBelow = mc.player.getBlockPos().down();
                BlockState blockAbove = mc.world.getBlockState(posAbove);
                if (blockAbove.isOf(Blocks.BEDROCK)) {
                    blockPos = posBelow;
                    return;
                } else {
                    blockPos = posAbove;
                    return;
                }
            }
        }
        // Burrow
        if (burrow.getValue()) {
            BlockPos burrow = BlockPos.ofFloored(target.getPos());
            BlockState burrowState = mc.world.getBlockState(burrow);
            if (burrowState.getBlock() == Blocks.OBSIDIAN || burrowState.getBlock() == Blocks.ENDER_CHEST) {
                blockPos = burrow;
                return;
            }
        }

        // surround
        if (surround.getValue()) {
            List<BlockPos> surroundBlocks = Modules.SURROUND.getEntitySurroundNoSupport(target);
            for (BlockPos pos : surroundBlocks) {
                if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
                    blockPos = pos;
                    return;
                }
            }
        }
        // trap
//        if (face.get()) {
//            for (Direction dir : Direction.Type.HORIZONTAL) {
//                BlockPos trap = getPos(target.getPos()).offset(dir).up(1);
//                if (mc.world.getBlockState(trap).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(trap).getBlock() == Blocks.ENDER_CHEST) {
//                    blockPos = trap;
//                    return;
//                }
//            }
//        }
        // CEV
//        if (head.get()) {
//            BlockPos head = new BlockPos(target.getBlockX(), (int) Math.floor(target.getBoundingBox().maxY) + 1, target.getBlockZ());
//            if (mc.world.getBlockState(head).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(head).getBlock() == Blocks.ENDER_CHEST) {
//                blockPos = head;
//                return;
//            }
//        }hgv v
        // AutoFish
        if (autofish.getValue()) {
            BlockPos enemyPos = target.getBlockPos();
            BlockPos block = enemyPos.down();
            BlockPos blockTwo = block.down();
            if (!mc.world.getBlockState(block).isAir() &&
                    !mc.world.getBlockState(blockTwo).isAir() &&
                    isEntityBurrow(target) && autofishing(target)) {
                blockPos = block;
                return;
            }
        }
        if (antiShulker.getValue()) {
            BlockPos shulker = findShulker(range.getValue());
            blockPos = shulker;
        }
//        // Fish-Cev
//        for (Direction dir : Direction.Type.HORIZONTAL) {
//            BlockPos fishhead = new BlockPos(target.getBlockX(), (int) Math.floor(target.getBoundingBox().maxY), target.getBlockZ());
//            if (mc.world.getBlockState(fishhead).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(fishhead).getBlock() == Blocks.ENDER_CHEST) {
//                blockPos = fishhead;
//                return;
//            }
//        }
    }

    public Direction getdir(BlockPos pos, boolean strict, boolean grim) {
        Managers.INTERACT.getInteractDirection(pos, grim, strict);
        return Direction.UP;
    }


    public void pause() {
        pause.reset();
    }

    private BlockPos getPos(Vec3d vec) {
        return new BlockPos((int) Math.floor(vec.x), (int) Math.round(vec.y), (int) Math.floor(vec.z));
    }

    private PlayerEntity getTargetPlayer() {
        final List<Entity> entities = Lists.newArrayList(mc.world.getEntities());
        return (PlayerEntity) entities.stream()
                .filter((entity) -> entity instanceof PlayerEntity && entity.isAlive() && !mc.player.equals(entity) && !Managers.SOCIAL.isFriend(entity.getName()))
                .filter((entity) -> mc.player.squaredDistanceTo(entity) <= ((NumberConfig<Float>) range).getValueSq())
                .min(Comparator.comparingDouble((entity) -> {
//                    if (priority.get() == prio.Armor) {
//                        //
//                        return ((PlayerEntity) entity).getArmor();
                    if (priority.getValue() == prio.Health) {
                        return ((PlayerEntity) entity).getHealth();
                    } else if (priority.getValue() == prio.Distance) {
                        return mc.player.squaredDistanceTo(entity);
                    } else {
                        return mc.player.squaredDistanceTo(entity);
                    }
                }))
                .orElse(null);
    }

    private BlockPos findShulker(double range) {
        BlockPos playerPos = mc.player.getBlockPos();
        for (int x = (int) -range; x <= (int) range; x++) {
            for (int z = (int) -range; z <= (int) range; z++) {
                for (int y = (int) -range; y <= (int) range; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (SHULKER_BLOCKS.contains(block)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private boolean isEntityBurrow(PlayerEntity entity) {
        BlockPos entityPos = entity.getBlockPos();
        return !mc.world.isAir(entityPos);
    }

    public boolean autofishing(PlayerEntity entity) {
        return PositionUtil.getAllInBox(entity.getBoundingBox(), entity.getBlockPos()).stream().anyMatch(pos -> !mc.world.getBlockState(pos).isReplaceable());
    }

    public enum prio {
        //        Armor,
        Health,
        Distance
    }

    private record BreakData(BlockPos blockPos, float damage) {
    }
}