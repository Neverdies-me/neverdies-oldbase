package org.nrnr.neverdies.impl.module.world;

import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.ColorConfig;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.RotationModule;
import org.nrnr.neverdies.api.render.RenderManagerWorld;
import org.nrnr.neverdies.impl.event.network.AttackBlockEvent;
import org.nrnr.neverdies.impl.event.network.PacketEvent;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.math.timer.CacheTimer;
import org.nrnr.neverdies.util.math.timer.Timer;
import org.nrnr.neverdies.util.player.RotationUtil;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;


public class SpeedmineModule extends RotationModule {
    public static BlockPos minePosition;
    public static float progress, prevProgress;
    private static SpeedmineModule instance;
    private final Timer attackTimer = new CacheTimer();
    Config<Boolean> multitask = new BooleanConfig("MultiTask", "Allows mining while using items", false);
    Config<Float> speed = new NumberConfig<>("Speed", "The speed to mine blocks", 0.7f, 1f, 1.0f);
    Config<Float> range = new NumberConfig<>("Range", "Range for mine", 1.0f, 5f, 6.0f);
    Config<Boolean> rotate = new BooleanConfig("Rotate", "Rotates for mining pos", true);
    Config<Boolean> remine = new BooleanConfig("Remine", "Remine mines blocks", false).setParent();
    Config<Boolean> autoRemineConfig = new BooleanConfig("AutoRemine", "Automatically remines mined blocks", true);
    Config<Boolean> fast = new BooleanConfig("Fast", "Mining air blocks", false);
    Config<Boolean> instantConfig = new BooleanConfig("Instant", "Instant remines mined blocks", true);
    Config<swap> switchmode = new EnumConfig<>("Swap", "Swaps fzz", swap.Silent, swap.values());
    Config<Boolean> sync = new BooleanConfig("Sync", "Inventory sync for alternative swap", false, () -> switchmode.getValue() == swap.Silent);
    Config<Boolean> clickreset = new BooleanConfig("Click Reset", "Reset mine", true);
    Config<Boolean> strict = new BooleanConfig("Strict", "reset mining block", false);
    Config<Boolean> grim = new BooleanConfig("Grim", "Using grim packets to break", true);
    Config<m> c = new EnumConfig<>("Color", "Color Config", m.Damage, m.values());
    Config<Color> colorConfig = new ColorConfig("Fill Color", "The primary color", new Color(255, 255, 255, 120), true, true, () -> c.getValue().equals(m.Custom));
    Config<Color> color1Config = new ColorConfig("Outline Color", "The primary color", new Color(255, 255, 255, 120), true, true, () -> c.getValue().equals(m.Custom));
    private Direction mineFacing;
    private int mineBreaks;
    private boolean swing;

    public SpeedmineModule() {
        super("Speedmine", "Mines faster", ModuleCategory.WORLD, 900);
        instance = this;
    }

    public enum m {
        Damage,
        Custom
    }

    @Override
    public String getModuleData() {
        if (minePosition != null) {
            return String.format("%.1f", Math.min(progress, speed.getValue()));
        }
        return super.getModuleData();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onEnable() {
        reset();
    }

    @EventListener
    public void ontick(PlayerTickEvent event) {
        if (mc.player == null
                || mc.world == null
                || mc.interactionManager == null
                || mc.player.getAbilities().creativeMode) return;
        if (minePosition != null) {
            if (mineBreaks >= (remine.getValue() ? Integer.MAX_VALUE : 1) || mc.player.squaredDistanceTo(minePosition.toCenterPos()) > ((NumberConfig<Float>) range).getValueSq()) {
                reset();
                return;
            }
            if (mc.world.isAir(minePosition) && fast.getValue() && remine.getValue()) {
                if (progress < speed.getValue()) {
                    progress += getBlockStrength(mc.world.getBlockState(minePosition), minePosition);
                    if (progress >= speed.getValue()) {
                        progress = speed.getValue();
                    }
                }
                prevProgress = progress;
                return;
            }
            if (progress == 0 && !mc.world.isAir(minePosition) && swing) {
                mc.interactionManager.attackBlock(minePosition, mineFacing);
                mc.player.swingHand(Hand.MAIN_HAND);
                swing = false;
//                sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

        }
        if (minePosition != null && !mc.world.isAir(minePosition)) {
            int invPickSlot = getTool(minePosition);
            if (progress >= speed.getValue()) {
                if (!multitask.getValue() && mc.player.isUsingItem()) {
                    return;
                }
                if (switchmode.getValue() == swap.Silent && strict.getValue() && invPickSlot != -1) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, invPickSlot < 9 ? invPickSlot + 36 : invPickSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                    if (sync.getValue()) closeScreen();
                } else if (switchmode.getValue() == swap.Silent) {
                    Managers.INVENTORY.setSlot(Modules.AUTO_TOOL.getBestToolNoFallback(mc.world.getBlockState(minePosition)));
                } else if (switchmode.getValue() == swap.Normal) {
                    Managers.INVENTORY.setClientSlot(Modules.AUTO_TOOL.getBestToolNoFallback(mc.world.getBlockState(minePosition)));
                }
                if (rotate.getValue()) {
                    Rotate(minePosition, grim.getValue());
                }
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, minePosition, mineFacing));
                if (!grim.getValue())
                    Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, minePosition, mineFacing));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, minePosition, mineFacing));
                if (!grim.getValue())
                    Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, minePosition, mineFacing));

                if (switchmode.getValue() == swap.Silent && strict.getValue() && invPickSlot != -1) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, invPickSlot < 9 ? invPickSlot + 36 : invPickSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                    if (sync.getValue()) closeScreen();
                } else if (switchmode.getValue() == swap.Silent) {
                    Managers.INVENTORY.syncToClient();
                }
                progress = 0;
                mineBreaks++;
            }
            prevProgress = progress;
            progress += getBlockStrength(mc.world.getBlockState(minePosition), minePosition);
        } else {
            progress = 0;
            prevProgress = 0;
        }
    }

    // Start manual mining
    @EventListener
    @SuppressWarnings("unused")
    public void onAttackBlock(@NotNull AttackBlockEvent event) {
        if (event.getState().getBlock().getHardness() == -1f || mc.player.isCreative() || mc.player == null || mc.world == null) {
            return;
        }
        if (canBreak(event.getPos()) && !mc.player.getAbilities().creativeMode && !event.getPos().equals(minePosition)) {
            addBlockToMine(event.getPos(), event.getDirection(), true);
        }
        if (minePosition.equals(event.getPos()) && clickreset.getValue()) {
            progress = 0;
            prevProgress = 0;
        }
    }

    // Packet Send
    @EventListener
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket && strict.getValue()) {
            if (mc.player == null || mc.world == null || minePosition == null) return;
            addBlockToMine(minePosition, mineFacing, true);
            swing = true;
        }
    }

    public void addBlockToMine(BlockPos pos, @Nullable Direction facing, boolean allowReMine) {
        if (!allowReMine && (minePosition != null || progress != 0))
            return;
        if (mc.player == null)
            return;
        progress = 0;
        mineBreaks = 0;
        minePosition = pos;
        mineFacing = facing == null ? mc.player.getHorizontalFacing() : facing;
        if (pos != null && mineFacing != null) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, mineFacing));
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, minePosition, mineFacing));
        }
    }

    @EventListener
    public void onRenderWorld(final RenderWorldEvent event) {
        renderMiningData(event.getMatrices());
    }

    private void renderMiningData(MatrixStack matrixStack) {
        if (minePosition != null && !mc.player.isCreative() && !mc.world.getBlockState(minePosition).isAir()) {
            BlockPos mining = minePosition;
            VoxelShape outlineShape = VoxelShapes.fullCube();
            Box render1 = outlineShape.getBoundingBox();
            Box render = new Box(mining.getX() + render1.minX, mining.getY() + render1.minY,
                    mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                    mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            Vec3d center = render.getCenter();
            float scale = MathHelper.clamp(progress / speed.getValue(), 0, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);
            if (c.getValue().equals(m.Damage)) {
                RenderManagerWorld.renderBox(matrixStack, scaled,
                        progress > (0.95f * speed.getValue()) ? 0x6000ff00 : 0x60ff0000);
                RenderManagerWorld.renderBoundingBox(matrixStack, scaled,
                        2.5f, progress > (0.95f * speed.getValue()) ? 0x6000ff00 : 0x60ff0000);
            } else if (c.getValue().equals(m.Custom)) {
                RenderManagerWorld.renderBox(matrixStack, scaled, ((ColorConfig) colorConfig).getRgb());
                RenderManagerWorld.renderBoundingBox(matrixStack, scaled, 2.5f, ((ColorConfig) color1Config).getRgb());
            }
        }
    }

    private void closeScreen() {
        if (mc.player == null) return;
        Managers.NETWORK.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
    }

    private void reset() {
        minePosition = null;
        mineFacing = null;
        progress = 0;
        mineBreaks = 0;
        prevProgress = 0;
    }

    public void Rotate(BlockPos pos, boolean silent) {
        float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), pos.toCenterPos());
        setRotationSilent(rotations[0], rotations[1], silent);
    }

    public enum Rotate {
        End,
        Start,
        StartEnd,
        None
    }

    public enum swap {
        Silent,
        Normal
    }

    public static float getBlockStrength(@NotNull BlockState state, BlockPos position) {
        if (state == Blocks.AIR.getDefaultState())
            return 0.02f;

        float hardness = state.getHardness(mc.world, position);

        if (hardness < 0)
            return 0;

        return getDigSpeed(state, position) / hardness / (canBreak(position) ? 30f : 100f);
    }

    public static float getDestroySpeed(BlockPos position, BlockState state) {
        float destroySpeed = 1;
        int slot = getTool(position);

        if (mc.player == null)
            return 0;
        if (slot != -1 && mc.player.getInventory().getStack(slot) != null && !mc.player.getInventory().getStack(slot).isEmpty()) {
            destroySpeed *= mc.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
        }

        return destroySpeed;
    }

    public static float getDigSpeed(BlockState state, BlockPos position) {
        if (mc.player == null) return 0;
        float digSpeed = getDestroySpeed(position, state);

        if (digSpeed > 1) {
            int slot = getTool(position);
            if (slot != -1) {
                ItemStack itemstack = mc.player.getInventory().getStack(slot);
                int efficiencyModifier = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemstack);
                if (efficiencyModifier > 0 && !itemstack.isEmpty()) {
                    digSpeed += (float) (StrictMath.pow(efficiencyModifier, 2) + 1);
                }
            }
        }

        if (mc.player.hasStatusEffect(StatusEffects.HASTE))
            digSpeed *= 1 + (Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.HASTE)).getAmplifier() + 1) * 0.2F;


        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE))
            digSpeed *= (float) Math.pow(0.3f, Objects.requireNonNull(mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE)).getAmplifier() + 1);

//
//        if (mc.player.isSubmergedInWater())
//            digSpeed *= (float) mc.player.getAttributeInstance(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED).getValue();

        if (!mc.player.isOnGround())
            digSpeed /= 5;

        return digSpeed < 0 ? 0 : digSpeed * 1;
    }

    public static int getTool(final BlockPos pos) {
        int index = -1;
        float currentFastest = 1.f;

        if (mc.world == null
                || mc.player == null
                || mc.world.getBlockState(pos).getBlock() instanceof AirBlock)
            return -1;

        for (int i = 9; i < 45; ++i) {
            final ItemStack stack = mc.player.getInventory().getStack(i >= 36 ? i - 36 : i);

            if (stack != ItemStack.EMPTY) {
                if (!(stack.getMaxDamage() - stack.getDamage() > 10))
                    continue;

                final float digSpeed = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
                final float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));

                if (digSpeed + destroySpeed > currentFastest) {
                    currentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
        }

        return index >= 36 ? index - 36 : index;
    }

    public static boolean canBreak(BlockPos pos) {
        if (mc.world == null || mc.player == null)
            return false;

        final BlockState blockState = mc.world.getBlockState(pos);
        final Block block = blockState.getBlock();
        return block.getHardness() != -1;
    }
}