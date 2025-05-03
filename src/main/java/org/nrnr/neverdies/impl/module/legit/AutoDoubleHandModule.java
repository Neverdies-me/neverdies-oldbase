package org.nrnr.neverdies.impl.module.legit;

import net.minecraft.block.Blocks;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.RotationModule;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.util.player.InventoryUtil;
import org.nrnr.neverdies.util.player.RotationUtil;
import org.nrnr.neverdies.util.world.BlockUtil;
import org.nrnr.neverdies.util.world.EndCrystalUtil;
import org.nrnr.neverdies.util.world.ExplosionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AutoDoubleHandModule extends RotationModule {
    Config<Boolean> totemHover = new BooleanConfig("TotemHover", "WIP", true);
    private PlayerEntity playerToTrack;
    private boolean totemUsed;

    public AutoDoubleHandModule() {

        super("AutoDoubleHand", "AutoDoubleHand Module for Ghost CPVP", ModuleCategory.LEGIT);
    }

    Config<Boolean> checkPlayersAround = new BooleanConfig("CheckPlayersAround", "if on, AutoDoubleHand will only activate when players are around", false);
    Config<Float> distance = new NumberConfig<>("Distance", "the distance for your enemy to activate", 2F, 6F, 10F, () -> checkPlayersAround.getValue());
    Config<Boolean> predictCrystals = new BooleanConfig("PredictCrystals", "whether or not to predict crystal placements", false);
    Config<Boolean> checkEnemiesAim = new BooleanConfig("CheckEnemyAim", "when enabled, crystal prediction will only activate when someone is pointing at an obsidian", false, () -> predictCrystals.getValue());
    Config<Boolean> checkHoldingItems = new BooleanConfig("CheckHoldingItems", "when enabled, crystal prediction will only activate when someone is pointing at an obsidian with crystals out", false, () -> predictCrystals.getValue() && checkEnemiesAim.getValue());
    Config<Float> activatesAbove = new NumberConfig<>("ActivatesAbove", "AutoDoubleHand will only activate when you are above this height, set to 0 to disable", 0F, 0.5F, 4F, () -> checkPlayersAround.getValue());


    private List<EndCrystalEntity> getNearByCrystals()
    {
        Vec3d pos = mc.player.getPos();
        return mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos.add(-6, -6, -6), pos.add(6, 6, 6)), a -> true);
    }

    @EventListener
    public void onPlayerTick(PlayerTickEvent event)
    {
        double distanceSq = distance.getValue() * distance.getValue();
        if (checkPlayersAround.getValue() && mc.world.getPlayers().parallelStream()
                .filter(e -> e != mc.player)
                .noneMatch(player -> mc.player.squaredDistanceTo(player) <= distanceSq))
            return;

        double activatesAboveV = activatesAbove.getValue();
        int f = (int) Math.floor(activatesAboveV);
        for (int i = 1; i <= f; i++)
            if (BlockUtil.hasBlock(mc.player.getBlockPos().add(0, -i, 0)))
                return;
    //    if (BlockUtil.hasBlock(new BlockPos(mc.player.getPos().add(0, -activatesAboveV, 0))))
     //       return;

        List<EndCrystalEntity> crystals = getNearByCrystals();
        ArrayList<Vec3d> crystalsPos = new ArrayList<>();
        crystals.forEach(e -> crystalsPos.add(e.getPos()));

        if (predictCrystals.getValue())
        {
            Stream<BlockPos> stream =
                    BlockUtil.getAllInBoxStream(mc.player.getBlockPos().add(-6, -8, -6), mc.player.getBlockPos().add(6, 2, 6))
                            .filter(e -> BlockUtil.isBlock(Blocks.OBSIDIAN, e) || BlockUtil.isBlock(Blocks.BEDROCK, e))
                            .filter(EndCrystalUtil::canPlaceCrystalClient);
            if (checkEnemiesAim.getValue())
            {
                if (checkHoldingItems.getValue())
                    stream = stream.filter(this::arePeopleAimingAtBlockAndHoldingCrystals);
                else
                    stream = stream.filter(this::arePeopleAimingAtBlock);
            }
            stream.forEachOrdered(e -> crystalsPos.add(Vec3d.ofBottomCenter(e).add(0, 1, 0)));
        }

        for (Vec3d pos : crystalsPos)
        {
            double damage =
                    ExplosionUtil.getDamageTo(mc.player, pos, true);
            if (damage >= mc.player.getHealth() + mc.player.getAbsorptionAmount())
            {
                InventoryUtil.selectItemFromHotbar(Items.TOTEM_OF_UNDYING);
                break;
            }
        }


    }


    private boolean arePeopleAimingAtBlock(BlockPos block)
    {
        return mc.world.getPlayers().parallelStream()
                .filter(e -> e != mc.player)
                .anyMatch(e ->
                {
                    Vec3d eyesPos = RotationUtil.getEyesPos(e);
                    BlockHitResult hitResult = mc.world.raycast(new RaycastContext(eyesPos, eyesPos.add(RotationUtil.getPlayerLookVec(e).multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
                    return hitResult != null && hitResult.getBlockPos().equals(block);
                });
    }

    private boolean arePeopleAimingAtBlockAndHoldingCrystals(BlockPos block)
    {
        return mc.world.getPlayers().parallelStream()
                .filter(e -> e != mc.player)
                .filter(e -> e.isHolding(Items.END_CRYSTAL))
                .anyMatch(e ->
                {
                    Vec3d eyesPos = RotationUtil.getEyesPos(e);
                    BlockHitResult hitResult = mc.world.raycast(new RaycastContext(eyesPos, eyesPos.add(RotationUtil.getPlayerLookVec(e).multiply(4.5)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
                    return hitResult != null && hitResult.getBlockPos().equals(block);
                });
    }


}
