package org.nrnr.neverdies.impl.module.combat;

import org.nrnr.neverdies.api.config.Config;
import org.nrnr.neverdies.api.config.NumberDisplay;
import org.nrnr.neverdies.api.config.setting.BooleanConfig;
import org.nrnr.neverdies.api.config.setting.EnumConfig;
import org.nrnr.neverdies.api.config.setting.NumberConfig;
import org.nrnr.neverdies.api.event.listener.EventListener;
import org.nrnr.neverdies.api.module.ModuleCategory;
import org.nrnr.neverdies.api.module.RotationModule;
import org.nrnr.neverdies.api.render.RenderManager;
import org.nrnr.neverdies.api.render.RenderManagerWorld;
import org.nrnr.neverdies.impl.event.RunTickEvent;
import org.nrnr.neverdies.impl.event.network.PacketEvent;
import org.nrnr.neverdies.impl.event.network.PlayerTickEvent;
import org.nrnr.neverdies.impl.event.render.RenderWorldEvent;
import org.nrnr.neverdies.impl.event.world.AddEntityEvent;
import org.nrnr.neverdies.init.Managers;
import org.nrnr.neverdies.init.Modules;
import org.nrnr.neverdies.util.EvictingQueue;
import org.nrnr.neverdies.util.math.timer.CacheTimer;
import org.nrnr.neverdies.util.math.timer.Timer;
import org.nrnr.neverdies.util.player.InventoryUtil;
import org.nrnr.neverdies.util.player.PlayerUtil;
import org.nrnr.neverdies.util.player.RotationUtil;
import org.nrnr.neverdies.util.render.animation.Animation;
import org.nrnr.neverdies.util.world.EndCrystalUtil;
import org.nrnr.neverdies.util.world.EntityUtil;
import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.dimension.DimensionTypes;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class AutoCrystalModule extends RotationModule {
    //

    // TODO stop on eat/place/break


    // PLACE
    Config<Boolean> placeConfig = new BooleanConfig("Place", "Places crystals to damage enemies. Place settings will only function if this setting is enabled.", true).setParent();
    Config<Float> placeSpeedConfig = new NumberConfig<>("PlaceSpeed", "Speed to place crystals", 0.1f, 20.0f, 20.0f, () -> placeConfig.isOpen());
    Config<Float> placeRangeConfig = new NumberConfig<>("PlaceRange", "Range to place crystals", 0.1f, 4.0f, 6.0f, () -> placeConfig.isOpen());
    Config<Float> placeWallRangeConfig = new NumberConfig<>("PlaceWallRange", "Range to place crystals through walls", 0.1f, 4.0f, 6.0f, () -> placeConfig.isOpen());
    Config<Placements> placementsConfig = new EnumConfig<>("Placements", "Version standard for placing end crystals", Placements.NATIVE, Placements.values(), () -> placeConfig.isOpen());
    Config<Boolean> raytraceConfig = new BooleanConfig("Raytrace", "Raytrace to crystal position", false,()->placeConfig.isOpen());
    Config<Boolean> strictDirectionConfig = new BooleanConfig("StrictDirection", "Interacts with only visible directions when placing crystals", false, () -> placeConfig.isOpen());
    Config<Boolean> placeRangeEyeConfig = new BooleanConfig("PlaceRangeEye", "Calculates place ranges starting from the eye position of the player", false, () -> placeConfig.isOpen());
    Config<Boolean> placeRangeCenterConfig = new BooleanConfig("PlaceRangeCenter", "Calculates place ranges to the center of the block", true, () -> placeConfig.isOpen());
    Config<Swap> autoSwapConfig = new EnumConfig<>("Swap", "Swaps to an end crystal before placing if the player is not holding one", Swap.OFF, Swap.values(), () -> placeConfig.isOpen());
    Config<Float> swapDelayConfig = new NumberConfig<>("SwapPenalty", "Delay for attacking after swapping items which prevents NCP flags", 0.0f, 0.0f, 10.0f, () -> placeConfig.isOpen());
    Config<Boolean> antiSurroundConfig = new BooleanConfig("AntiSurround", "Places on mining blocks that when broken, can be placed on to damage enemies. Instantly destroys items spawned from breaking block and allows faster placing", false, () -> placeConfig.isOpen());

    // BREAK SETTINGS
    Config<Boolean> breaka = new BooleanConfig("Break", "", false).setParent();
    Config<Float> breakRangeConfig = new NumberConfig<>("BreakRange", "Range to break crystals", 0.1f, 4.0f, 6.0f, () -> breaka.isOpen());
    Config<Float> breakWallRangeConfig = new NumberConfig<>("BreakWallRange", "Range to break crystals through walls", 0.1f, 4.0f, 6.0f, () -> breaka.isOpen());
    Config<Float> breakSpeedConfig = new NumberConfig<>("BreakSpeed", "Speed to break crystals", 0.1f, 20.0f, 20.0f, () -> breaka.isOpen());
    Config<Float> attackDelayConfig = new NumberConfig<>("AttackDelay", "Added delays", 0.0f, 0.0f, 5.0f, () -> breaka.isOpen());
    Config<Integer> attackFactorConfig = new NumberConfig<>("AttackFactor", "Factor of attack delay", 0, 0, 3, () -> attackDelayConfig.getValue() > 0.0 && breaka.isOpen());
    Config<Integer> ticksExistedConfig = new NumberConfig<>("TickExisted", "Minimum ticks alive to consider crystals for attack", 0, 0, 10, () -> breaka.isOpen());
    Config<Boolean> inhibitConfig = new BooleanConfig("Inhibit", "Prevents excessive attacks", true, () -> breaka.isOpen());
    Config<Swap> antiWeaknessConfig = new EnumConfig<>("AntiWeak", "Swap to tools before attacking crystals", Swap.SILENT, Swap.values(), () -> breaka.isOpen());
    Config<Boolean> manualConfig = new BooleanConfig("BreakManuals", "Always breaks manually placed crystals", false, () -> breaka.isOpen());


    Config<Boolean> swingConfig = new BooleanConfig("Swing", "Swing hand when placing and attacking crystals", true);
    Config<Boolean> multitaskConfig = new BooleanConfig("Multitask", "Allows attacking while using items", true);
    Config<Boolean> whileMiningConfig = new BooleanConfig("WhileMining", "Allows attacking while mining blocks", false);
    Config<Boolean> instantConfig = new BooleanConfig("Instant", "Instantly attacks crystals when they spawn", false).setParent();
    Config<Boolean> instantCalcConfig = new BooleanConfig("Instant-Calc", "Calculates a crystal when it spawns and attacks if it meets MINIMUM requirements, this will result in non-ideal crystal attacks", false, () -> instantConfig.isOpen());
    Config<Float> instantDamageConfig = new NumberConfig<>("InstantDamage", "Minimum damage to attack crystals instantly", 1.0f, 6.0f, 10.0f, () -> instantConfig.isOpen() && instantCalcConfig.getValue());
    Config<Boolean> instantMaxConfig = new BooleanConfig("InstantMax", "Attacks crystals instantly if they exceed the previous max attack damage (Note: This is still not a perfect check because the next tick could have better damages)", true, () -> instantConfig.isOpen());

    // DAMAGE
    Config<Boolean> damag1e = new BooleanConfig("Damage", "", false).setGroup();
    public Config<Boolean> blockDestructionConfig = new BooleanConfig("BlockDestruction", "Accounts for explosion block destruction when calculating damages", false, () -> damag1e.isOpen());
    Config<Float> minDamageConfig = new NumberConfig<>("MinDamage", "Minimum damage required to consider attacking or placing an end crystal", 1.0f, 4.0f, 10.0f, () -> damag1e.isOpen());
    Config<Float> maxLocalDamageConfig = new NumberConfig<>("MaxDamage", "The maximum player damage", 4.0f, 12.0f, 20.0f, () -> damag1e.isOpen());
    Config<Boolean> safetyConfig = new BooleanConfig("Safety", "Accounts for total player safety when attacking and placing crystals", true, () -> damag1e.isOpen());
    Config<Boolean> safetyOverride = new BooleanConfig("SafetyOverride", "Overrides the safety checks if the crystal will kill an enemy", false, () -> damag1e.isOpen());
    // FACE PLACE
    Config<Boolean> facePlaceParent = new BooleanConfig("FacePlace", "", false).setParent();
    Config<Float> facePlaceHp = new NumberConfig<>("TargetHP", "", 0f, 5f, 36f, NumberDisplay.PERCENT, () -> facePlaceParent.isOpen());
    Config<Boolean> facePlaceArmor = new BooleanConfig("Armor", "", false, () -> facePlaceParent.isOpen());
    Config<Integer> facePlaceDura = new NumberConfig<>("Durability", "", 0, 3, 20, NumberDisplay.PERCENT, () -> facePlaceParent.isOpen() && facePlaceArmor.getValue());
    Config<Boolean> lethalDmgConfig = new BooleanConfig("DamageTick", "Places lethal crystals on ticks where they damage entities", false, () -> facePlaceParent.isOpen());


    // ROTATE SETTINGS
    Config<Boolean> rotateConfig = new BooleanConfig("Rotate", "Rotate before placing and breaking", false).setParent();
    //    Config<Boolean> rotateSilentConfig = new BooleanConfig("RotateSilent", "Silently updates rotations to server", false, () -> rotateConfig.isOpen());
    Config<Rotate> strictRotateConfig = new EnumConfig<>("YawStep", "Rotates yaw over multiple ticks to prevent certain rotation flags in NCP", Rotate.OFF, Rotate.values(), () -> rotateConfig.isOpen());
    Config<Integer> rotateLimitConfig = new NumberConfig<>("YawStep-Limit", "Maximum yaw rotation in degrees for one tick", 1, 180, 180, NumberDisplay.DEGREES, () -> rotateConfig.getValue() && strictRotateConfig.getValue() != Rotate.OFF && rotateConfig.isOpen());
    // SELECT

    Config<Boolean> se = new BooleanConfig("Targets", "", false).setGroup();
    Config<Float> targetRangeConfig = new NumberConfig<>("EnemyRange", "Range to search for potential enemies", 1.0f, 10.0f, 13.0f, () -> se.isOpen());
    Config<Boolean> playersConfig = new BooleanConfig("Players", "Target players", true, () -> se.isOpen());
    Config<Boolean> monstersConfig = new BooleanConfig("Monsters", "Target monsters", false, () -> se.isOpen());
    Config<Boolean> neutralsConfig = new BooleanConfig("Neutrals", "Target neutrals", false, () -> se.isOpen());
    Config<Boolean> animalsConfig = new BooleanConfig("Animals", "Target animals", false, () -> se.isOpen());
    Config<Boolean> renderConfig = new BooleanConfig("Render", "Renders the current placement", true).setParent();
    Config<Boolean> fadeConfig = new BooleanConfig("Fade", "Fades old renders out", true, () -> renderConfig.isOpen());
    Config<Integer> fadeTimeConfig = new NumberConfig<>("Fade-Time", "Timer for the fade", 0, 250, 1000, () -> renderConfig.isOpen() && fadeConfig.getValue());
    Config<Boolean> damageNametagConfig = new BooleanConfig("Render-Damage", "Renders the current expected damage of a place/attack", false, () -> renderConfig.isOpen());
    //
    private DamageData<EndCrystalEntity> attackCrystal;
    private DamageData<BlockPos> placeCrystal;
    //
    private static final Box FULL_CRYSTAL_BB = new Box(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);
    private static final Box HALF_CRYSTAL_BB = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private final Timer lastAttackTimer = new CacheTimer();
    private final Timer lastPlaceTimer = new CacheTimer();
    private final Timer lastSwapTimer = new CacheTimer();
    private final Timer autoSwapTimer = new CacheTimer();
    //
    private final Deque<Long> attackLatency = new EvictingQueue<>(20);
    private final List<BlockPos> manualCrystals = new ArrayList<>();
    private final Map<Integer, Long> attackPackets =
            Collections.synchronizedMap(new ConcurrentHashMap<>());
    private final Map<BlockPos, Long> placePackets =
            Collections.synchronizedMap(new ConcurrentHashMap<>());
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private BlockPos renderPos;
    //
    private Entity entityTarget;
    //    private int prevCrystalsAmount, crystalSpeed, invTimer;
    private Vec3d crystalRotation;
    private boolean attackRotate;
    private boolean rotated;
    private double lstt;
    private float[] silentRotations;
    public AutoCrystalModule() {
        super("AutoCrystal", "Attacks entities with end crystals",
                ModuleCategory.Combat, 750);
    }

    @Override
    public void onDisable() {
        renderPos = null;
        attackCrystal = null;
        placeCrystal = null;
        crystalRotation = null;
        silentRotations = null;
        entityTarget = null;
        attackPackets.clear();
        placePackets.clear();
        fadeList.clear();
        setStage("NONE");
    }
    @Override
    public String getModuleData() {
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(attackCrystal != null ? attackCrystal.getDamage() : 0.0) + ", " +
                InventoryUtil.getItemCount(Items.END_CRYSTAL) +
                (entityTarget != null ? ", " + entityTarget.getDisplayName().getString() : "");
    }

    @EventListener
    public void onPlayerUpdate(PlayerTickEvent event) {
        if (entityTarget != null && !entityTarget.isAlive())
            entityTarget = null;
        if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND
                || mc.options.attackKey.isPressed() || PlayerUtil.isHotbarKeysPressed()) {
            autoSwapTimer.reset();
        }
        if (mc.player.isUsingItem() && !multitaskConfig.getValue()) {
            return;
        }
        renderPos = null;
        ArrayList<Entity> entities = Lists.newArrayList(mc.world.getEntities());
        List<BlockPos> blocks = getSphere(mc.player.getPos());

        if (breaka.getValue()) attackCrystal = calculateAttackCrystal(entities);
        entityTarget = PlayerUtil.getNearestTargetForAll(targetRangeConfig.getValue(),monstersConfig.getValue(),animalsConfig.getValue(),neutralsConfig.getValue(),playersConfig.getValue());
        if (placeConfig.getValue()) placeCrystal = calculatePlaceCrystal(blocks,entityTarget);

        float breakDelay = 1000.0f - breakSpeedConfig.getValue() * 50.0f;
        attackRotate = attackCrystal != null && attackDelayConfig.getValue() <= 0.0 && lastAttackTimer.passed(breakDelay);
        if (attackCrystal != null) {
            crystalRotation = attackCrystal.damageData.getPos();
        } else if (placeCrystal != null) {
            crystalRotation = placeCrystal.damageData.toCenterPos().add(0.0, 0.5, 0.0);
        }

        if (rotateConfig.getValue() && crystalRotation != null && (placeCrystal == null || canHoldCrystal())) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), crystalRotation);
            if (strictRotateConfig.getValue() == Rotate.FULL || strictRotateConfig.getValue() == Rotate.SEMI && attackRotate) {
                float yaw;
                float serverYaw = Managers.ROTATION.getWrappedYaw();
                float diff = serverYaw - rotations[0];
                float diff1 = Math.abs(diff);
                if (diff1 > 180.0f) {
                    diff += diff > 0.0f ? -360.0f : 360.0f;
                }
                int dir = diff > 0.0f ? -1 : 1;
                float deltaYaw = dir * rotateLimitConfig.getValue();
                if (diff1 > rotateLimitConfig.getValue()) {
                    yaw = serverYaw + deltaYaw;
                    rotated = false;
                } else {
                    yaw = rotations[0];
                    rotated = true;
                    crystalRotation = null;
                }
                rotations[0] = yaw;
            } else {
                rotated = true;
                crystalRotation = null;
            }
            setRotation(rotations[0], rotations[1]);
        } else {
            silentRotations = null;
        }
        if (isRotationBlocked() || !rotated && rotateConfig.getValue()) {
            return;
        }
//        if (rotateSilentConfig.getValue() && silentRotations != null) {
//            setRotationSilent(silentRotations[0], silentRotations[1]);
//        }
        final Hand hand = getCrystalHand();
        if (attackCrystal != null && isHoldingCrystal()) {
            if (attackRotate) {
//                ChatUtil.clientSendMessage("break range:" + Math.sqrt(mc.player.getEyePos().squaredDistanceTo(attackCrystal.getDamageData().getPos())));
                attackCrystal(attackCrystal.getDamageData(), hand);
//                setStage("ATTACKING");
                lastAttackTimer.reset();
            }
        }
        if (placeCrystal != null && isHoldingCrystal()) {
            renderPos = placeCrystal.getDamageData();
            if (lastPlaceTimer.passed(1000.0f - placeSpeedConfig.getValue() * 50.0f)) {
//                 ChatUtil.clientSendMessage("place range:" + Math.sqrt(mc.player.getEyePos().squaredDistanceTo(placeCrystal.getDamageData().toCenterPos())));
                placeCrystal(placeCrystal.getDamageData(), hand);
//                setStage("PLACING");
                lastPlaceTimer.reset();
            }
        }
//        if (invTimer++ >= 20) {
//            crystalSpeed = MathHelper.clamp(prevCrystalsAmount - InventoryUtil.getItemCount(Items.END_CRYSTAL), 0, 255);
//            prevCrystalsAmount = InventoryUtil.getItemCount(Items.END_CRYSTAL);
//            invTimer = 0;
//        }
    }

    @EventListener
    public void onRunTick(RunTickEvent event) {
        if (mc.player == null || attackDelayConfig.getValue() <= 0.0) {
            return;
        }
        float attackFactor = 50.0f / Math.max(1.0f, attackFactorConfig.getValue());
        if (attackCrystal != null && lastAttackTimer.passed(attackDelayConfig.getValue() * attackFactor)) {
            attackCrystal(attackCrystal.getDamageData(), getCrystalHand());
            lastAttackTimer.reset();
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (renderConfig.getValue()) {
            if (fadeConfig.getValue()) {
                for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet()) {
                    if (set.getKey() == renderPos) {
                        continue;
                    }

                    set.getValue().setState(false);
                    int boxAlpha = (int) (80 * set.getValue().getFactor());
                    int lineAlpha = (int) (145 * set.getValue().getFactor());
                    Color boxColor = Modules.COLORS.getColor(boxAlpha);
                    Color lineColor = Modules.COLORS.getColor(lineAlpha);
                    RenderManagerWorld.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
                    RenderManagerWorld.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5f, lineColor.getRGB());
                }
            }

            if (renderPos != null && isHoldingCrystal()) {
                if (!fadeConfig.getValue()) {
                    RenderManagerWorld.renderBox(event.getMatrices(), renderPos, Modules.COLORS.getRGB(80));
                    RenderManagerWorld.renderBoundingBox(event.getMatrices(), renderPos, 1.5f,
                            Modules.COLORS.getRGB(145));

                }
                if (damageNametagConfig.getValue() && placeCrystal != null) {
                    DecimalFormat format = new DecimalFormat("0.0");
                    RenderManager.post(() -> {
                        RenderManager.renderSign(event.getMatrices(),
                                format.format(placeCrystal.getDamage()), renderPos.toCenterPos());
                    });
                }
                else {
                    Animation animation = new Animation(true, fadeTimeConfig.getValue());
                    fadeList.put(renderPos, animation);
                }
            }

            fadeList.entrySet().removeIf(e ->
                    e.getValue().getFactor() == 0.0);
        }
    }

//    @EventListener
//    public void onPacketInbound(PacketEvent.Inbound event) {
//        if (mc.player == null || mc.world == null) {
//            return;
//        }
//        if (event.getPacket() instanceof PlaySoundS2CPacket packet && packet.getCategory() == SoundCategory.BLOCKS
//                && packet.getSound().value() == SoundEvents.ENTITY_GENERIC_EXPLODE) {
//            for (Entity entity : Lists.newArrayList(mc.world.getEntities())) {
//                if (entity instanceof EndCrystalEntity && entity.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) < 144.0) {
//                    mc.executeSync(() -> {
//                        mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
//                    });
//                }
//            }
//        }
//    }

    @EventListener
    public void onAddEntity(AddEntityEvent event) {
        if (!(event.getEntity() instanceof EndCrystalEntity crystalEntity)) {
            return;
        }
        Vec3d crystalPos = crystalEntity.getPos();
        BlockPos blockPos = BlockPos.ofFloored(crystalPos.add(0.0, -1.0, 0.0));
        boolean manualPos = manualCrystals.contains(blockPos);
        if (!instantConfig.getValue() && !(manualPos && manualConfig.getValue())) {
            return;
        }
        Long time = placePackets.remove(blockPos);
        attackRotate = time != null;
        if (attackRotate || manualPos) {
            attackInternal(crystalEntity, getCrystalHand());
            setStage("ATTACKING");
            lastAttackTimer.reset();
        } else if (instantCalcConfig.getValue()) {
            if (attackRangeCheck(crystalPos)) {
                return;
            }
            double selfDamage = EndCrystalUtil.getDamageTo(mc.player,
                    crystalPos, blockDestructionConfig.getValue());
            if (playerDamageCheck(selfDamage)) {
                return;
            }
            entityTarget = PlayerUtil.getNearestTargetForAll(targetRangeConfig.getValue(),monstersConfig.getValue(),animalsConfig.getValue(),neutralsConfig.getValue(),playersConfig.getValue());
            double damage = EndCrystalUtil.getDamageTo(entityTarget,
                    crystalPos, blockDestructionConfig.getValue());
            // TODO: Test this
            DamageData<EndCrystalEntity> data = new DamageData<>(crystalEntity,
                    entityTarget, damage, selfDamage, crystalEntity.getBlockPos().down());
            attackRotate = damage > instantDamageConfig.getValue() || attackCrystal != null
                    && damage >= attackCrystal.getDamage() && instantMaxConfig.getValue()
                    || entityTarget instanceof LivingEntity entity1 && isCrystalLethalTo(data, entity1);
            if (attackRotate) {
                attackInternal(crystalEntity, getCrystalHand());
                setStage("ATTACKING");
                lastAttackTimer.reset();
            }
        }
    }

    @EventListener
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player == null) {
            return;
        }
        if (event.getPacket() instanceof UpdateSelectedSlotC2SPacket) {
            lastSwapTimer.reset();
        } else if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet && !event.isClientPacket()
                && mc.player.getStackInHand(packet.getHand()).getItem() instanceof EndCrystalItem && manualConfig.getValue()) {
            BlockHitResult result = packet.getBlockHitResult();
            manualCrystals.add(result.getBlockPos());
        } else if (event.getPacket() instanceof PlayerActionC2SPacket packet && packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
                && antiSurroundConfig.getValue() && canUseCrystalOnBlock(packet.getPos())) {
            //
            Vec3d crystalPos = crystalDamageVec(packet.getPos());
            for (Entity entity : mc.world.getEntities()) {
                if (entity == null || !entity.isAlive() || entity == mc.player
                        || !isValidTarget(entity)
                        || Managers.SOCIAL.isFriend(entity.getName())) {
                    continue;
                }
                double crystalDist = crystalPos.squaredDistanceTo(entity.getPos());
                if (crystalDist > 144.0f) {
                    continue;
                }
                double dist = mc.player.squaredDistanceTo(entity);
                if (dist > targetRangeConfig.getValue() * targetRangeConfig.getValue()) {
                    continue;
                }
                double damage = EndCrystalUtil.getDamageTo(entity,
                        crystalPos, blockDestructionConfig.getValue());
                double selfdamage = EndCrystalUtil.getDamageTo(mc.player,
                        crystalPos, blockDestructionConfig.getValue());
                if (!targetDamageCheck(new DamageData<>(packet.getPos(),entity,damage,selfdamage))) {
                    placeCrystal(packet.getPos(), getCrystalHand());
                    break;
                }
            }
        }
    }

    public boolean isAttacking() {
        return attackCrystal != null && breaka.getValue();
    }

    public boolean isPlacing() {
        return placeCrystal != null && isHoldingCrystal();
    }

    public void attackCrystal(EndCrystalEntity entity, Hand hand) {
        if (attackCheckPre(hand) || !breaka.getValue()) {
            return;
        }
        StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
        if (weakness != null && (strength == null || weakness.getAmplifier() > strength.getAmplifier())) {
            int slot = -1;
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && (stack.getItem() instanceof SwordItem
                        || stack.getItem() instanceof AxeItem
                        || stack.getItem() instanceof PickaxeItem)) {
                    slot = i;
                    break;
                }
            }
            if (slot != -1) {
                boolean canSwap = antiWeaknessConfig.getValue() != Swap.NORMAL || autoSwapTimer.passed(500);
                if (antiWeaknessConfig.getValue() != Swap.OFF && canSwap) {
                    if (antiWeaknessConfig.getValue() == Swap.SILENT_ALT) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                                slot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                    } else if (antiWeaknessConfig.getValue() == Swap.SILENT) {
                        Managers.INVENTORY.setSlot(slot);
                    } else {
                        Managers.INVENTORY.setClientSlot(slot);
                    }
                }
                attackInternal(entity, Hand.MAIN_HAND);
                if (canSwap) {
                    if (antiWeaknessConfig.getValue() == Swap.SILENT_ALT) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                                slot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                    } else if (antiWeaknessConfig.getValue() == Swap.SILENT) {
                        Managers.INVENTORY.syncToClient();
                    }
                }
            }
        } else {
            attackInternal(entity, hand);
        }
    }

    private void attackInternal(EndCrystalEntity crystalEntity, Hand hand) {
        if (!breaka.getValue()) return;
        hand = hand != null ? hand : Hand.MAIN_HAND;
        // ((AccessorPlayerInteractEntityC2SPacket) packet).hookSetEntityId(id);
        Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
        attackPackets.put(crystalEntity.getId(), System.currentTimeMillis());
        if (swingConfig.getValue()) {
            mc.player.swingHand(hand);
        } else {
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    public void placeCrystal(BlockPos blockPos, Hand hand) {
        if (checkMultitask()) {
            return;
        }

        Direction sidePlace = getPlaceDirection(blockPos);
        BlockHitResult result = new BlockHitResult(blockPos.toCenterPos(), sidePlace, blockPos, false);

      //  if (autoSwapConfig.getValue() == Swap.NORMAL){
      //      InventoryUtil.swap(getCrystalSlot(), false);
    //    }

        if (autoSwapConfig.getValue() != Swap.OFF && hand != Hand.OFF_HAND && getCrystalHand() == null) {
            if (isSilentSwap(autoSwapConfig.getValue()) && Managers.INVENTORY.count(Items.END_CRYSTAL) == 0) {
                return;
            }
            int crystalSlot = getCrystalSlot();
            if (crystalSlot != -1) {
                boolean canSwap = autoSwapConfig.getValue() != Swap.NORMAL || autoSwapTimer.passed(500);
                if (canSwap) {
                    if (autoSwapConfig.getValue() == Swap.SILENT_ALT) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                                crystalSlot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                    } else if (autoSwapConfig.getValue() == Swap.SILENT) {
                        Managers.INVENTORY.setSlot(crystalSlot);
                    }
                    else {
                        Managers.INVENTORY.setClientSlot(crystalSlot);
                    }
                }

                placeInternal(result, Hand.MAIN_HAND);
                placePackets.put(blockPos, System.currentTimeMillis());
                if (canSwap) {
                    if (autoSwapConfig.getValue() == Swap.SILENT_ALT) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                                crystalSlot + 36, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                    } else if (autoSwapConfig.getValue() == Swap.SILENT) {
                        Managers.INVENTORY.syncToClient();
                    }
                }
            }
        } else if (isHoldingCrystal()) {
            placeInternal(result, hand);
            placePackets.put(blockPos, System.currentTimeMillis());
        }
    }

    private void placeInternal(BlockHitResult result, Hand hand) {
        if (hand == null) {
            return;
        }
        Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, result, id));
        if (swingConfig.getValue()) {
            mc.player.swingHand(hand);
        } else {
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(hand));
        }
    }

    private boolean isSilentSwap(Swap swap) {
        return swap == Swap.SILENT || swap == Swap.SILENT_ALT;
    }

    private int getCrystalSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof EndCrystalItem) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private Direction getPlaceDirection(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        if (strictDirectionConfig.getValue()) {
            if (mc.player.getY() >= blockPos.getY()) {
                return Direction.UP;
            }
            BlockHitResult result = mc.world.raycast(new RaycastContext(
                    mc.player.getEyePos(), new Vec3d(x + 0.5, y + 0.5, z + 0.5),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, mc.player));
            if (result != null && result.getType() == HitResult.Type.BLOCK) {
                Direction direction = result.getSide();
                // TODO
//                if (!exposedDirectionConfig.getValue() || mc.world.isAir(blockPos.offset(direction))) {
//                    return direction;
//                }
                if (mc.world.isAir(blockPos.offset(direction))) {
                    return direction;
                }
            }
        } else {
            if (mc.world.isInBuildLimit(blockPos)) {
                return Direction.DOWN;
            }
            BlockHitResult result = mc.world.raycast(new RaycastContext(
                    mc.player.getEyePos(), new Vec3d(x + 0.5, y + 0.5, z + 0.5),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, mc.player));
            if (result != null && result.getType() == HitResult.Type.BLOCK) {
                return result.getSide();
            }
        }
        return Direction.UP;
    }
    // TODO переделать
    private DamageData<EndCrystalEntity> calculateAttackCrystal(List<Entity> entities) {
        if (entities.isEmpty()) {
            return null;
        }
        DamageData<EndCrystalEntity> data = null;
        for (Entity crystal : entities) {
            if (!(crystal instanceof EndCrystalEntity crystal1) || !crystal.isAlive()) {
                continue;
            }
            Long time = attackPackets.get(crystal.getId());
            boolean attacked = time != null && time < getBreakMs();
            if ((crystal.age < ticksExistedConfig.getValue() || attacked) && inhibitConfig.getValue()) {
                continue;
            }
            if (attackRangeCheck(crystal1)) {
                continue;
            }
            double selfDamage = EndCrystalUtil.getDamageTo(mc.player,
                    crystal.getPos(), blockDestructionConfig.getValue());
            boolean unsafeToPlayer = playerDamageCheck(selfDamage);
            if (unsafeToPlayer && !safetyOverride.getValue()) {
                continue;
            }
            for (Entity entity : entities) {
                if (Modules.BLINK.isEnabled()) break;
                if (entity == null || !entity.isAlive()) {
                    continue;
                }
                if (entity == mc.player && (!Modules.SUICIDE.isEnabled() || Modules.SUICIDE.modeConfig.getValue() != SuicideModule.Mode.AutoCrystal)) {
                    continue;
                }
                if (!isValidTarget(entity)) {
                    continue;
                }
                if (entity.getDisplayName() != null && Managers.SOCIAL.isFriend(entity.getDisplayName())) {
                    continue;
                }
                double crystalDist = crystal.squaredDistanceTo(entity);
                if (crystalDist > 144.0f) {
                    continue;
                }
                double dist = mc.player.squaredDistanceTo(entity);
                if (dist > targetRangeConfig.getValue() * targetRangeConfig.getValue()) {
                    continue;
                }
                double damage = EndCrystalUtil.getDamageTo(entity,
                        crystal.getPos(), blockDestructionConfig.getValue());
                if (checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                    continue;
                }
                if (data == null || damage > data.getDamage()) {
                    data = new DamageData<>(crystal1, entity,
                            damage, selfDamage, crystal1.getBlockPos().down());
                }
            }
        }
        if (data == null || targetDamageCheck(data)) {
            return null;
        }
        return data;
    }

    private boolean attackRangeCheck(EndCrystalEntity entity) {
        return attackRangeCheck(entity.getPos());
    }

    /**
     * @param entityPos
     * @return
     */
    // TODO переделать
    private boolean attackRangeCheck(Vec3d entityPos) {
        Vec3d playerPos = mc.player.getEyePos();
        double dist = playerPos.squaredDistanceTo(entityPos);
        if (dist > breakRangeConfig.getValue() * breakRangeConfig.getValue()) {
            return true;
        }
        double yOff = Math.abs(entityPos.getY() - mc.player.getY());
        if (yOff > breakRangeConfig.getValue()) {
            return true;
        }
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                playerPos, entityPos, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() != HitResult.Type.MISS
                && dist > breakWallRangeConfig.getValue() * breakWallRangeConfig.getValue();
    }
    // TODO переделать
    private DamageData<BlockPos> calculatePlaceCrystal(List<BlockPos> placeBlocks,Entity entity) {
        if (placeBlocks.isEmpty() || entity == null) {
            return null;
        }
        DamageData<BlockPos> data = null;
        for (BlockPos pos : placeBlocks) {
            if (!canUseCrystalOnBlock(pos) || placeRangeCheck(pos)) {
                continue;
            }
            if (mc.world.getBlockState(pos.up()).getBlock() instanceof FireBlock && mc.world.getDimensionKey() != DimensionTypes.THE_END) {
                mc.interactionManager.attackBlock(pos.up(),getPlaceDirection(pos.up()));
            }
            double selfDamage = EndCrystalUtil.getDamageTo(mc.player,
                    crystalDamageVec(pos), blockDestructionConfig.getValue());
            boolean unsafeToPlayer = playerDamageCheck(selfDamage);
            if (unsafeToPlayer && !safetyOverride.getValue()) {
                continue;
            }
            double damage = EndCrystalUtil.getDamageTo(entity,
                    crystalDamageVec(pos), blockDestructionConfig.getValue());
            if (checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                continue;
            }
            if (data == null || damage > data.getDamage()) {
                data = new DamageData<>(pos, entity, damage, selfDamage);
            }
        }
        if (data == null || targetDamageCheck(data)) {
            return null;
        }
        return data;
    }

    /**
     * @param pos
     * @return
     */
    // TODO переделать
    private boolean placeRangeCheck(BlockPos pos) {
        Vec3d player = placeRangeEyeConfig.getValue() ? mc.player.getEyePos() : mc.player.getPos();
        double dist = placeRangeCenterConfig.getValue() ?
                player.squaredDistanceTo(pos.toCenterPos()) : pos.getSquaredDistance(player.x, player.y, player.z);
        if (dist > placeRangeConfig.getValue() * placeRangeConfig.getValue()) {
            return true;
        }
        Vec3d raytrace = Vec3d.of(pos).add(0.0, 1.0, 0.0);
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                mc.player.getEyePos(), raytrace,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, mc.player));
        if (raytraceConfig.getValue()) {
            if (result != null && result.getType() != HitResult.Type.MISS) {
                return true;
            }
        }

        float maxDist = breakRangeConfig.getValue() * breakRangeConfig.getValue();
        if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos() != pos) {
            maxDist = breakWallRangeConfig.getValue() * breakWallRangeConfig.getValue();
            if (dist > placeWallRangeConfig.getValue() * placeWallRangeConfig.getValue()) {
                return true;
            }
        }

        return dist > maxDist;
    }

    private boolean checkOverrideSafety(boolean unsafeToPlayer, double damage, Entity entity) {
        return safetyOverride.getValue() && unsafeToPlayer && damage < EntityUtil.getHealth(entity) + 0.5;
    }

    private boolean targetDamageCheck(DamageData<?> crystal) {
        double minDmg = minDamageConfig.getValue();
        if (crystal.getAttackTarget() instanceof LivingEntity entity && isCrystalLethalTo(crystal, entity)) {
            minDmg = 2.0f;
        }
        return crystal.getDamage() < minDmg;
    }

    private boolean playerDamageCheck(double playerDamage) {
        if (!mc.player.isCreative()) {
            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (safetyConfig.getValue() && !Modules.SUICIDE.isEnabled() && playerDamage >= health + 0.5f) {
                return true;
            }
            if (!Modules.SUICIDE.isEnabled()) {
                return playerDamage > maxLocalDamageConfig.getValue();
            }
        }
        return false;
    }

    private boolean isFeetSurrounded(LivingEntity entity) {
        BlockPos pos1 = entity.getBlockPos();
        if (!mc.world.getBlockState(pos1).isReplaceable()) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (!direction.getAxis().isHorizontal()) {
                continue;
            }
            BlockPos pos2 = pos1.offset(direction);
            if (mc.world.getBlockState(pos2).isReplaceable()) {
                return false;
            }
        }
        return true;
    }

    private boolean isCrystalLethalTo(DamageData<?> crystal, LivingEntity entity) {
        if (!isFeetSurrounded(entity)) {
            return false;
        }

        float health = entity.getHealth() + entity.getAbsorptionAmount();
//        if (crystal.getDamage() * (1.0f + lethalMultiplier.getValue()) >= health + 0.5f) {
//            return true;
//        }

        if (facePlaceParent.getValue()) {

            if (facePlaceHp.getValue() >= health && inTick()) {
                return true;
            }

            if (facePlaceArmor.getValue())
                for (ItemStack armorStack : entity.getArmorItems()) {
                    int n = armorStack.getDamage();
                    int n1 = armorStack.getMaxDamage();
                    float durability = ((n1 - n) / (float) n1) * 100.0f;
                    if (durability <= facePlaceDura.getValue() && inTick()) {
                        return true;
                    }
                }
        }
        return false;
    }

    private boolean attackCheckPre(Hand hand) {
        if (!lastSwapTimer.passed(swapDelayConfig.getValue() * 25.0f)) {
            return true;
        }
        if (hand == Hand.MAIN_HAND) {
            return checkMultitask();
        }
        return false;
    }

    private boolean checkMultitask() {
        return !multitaskConfig.getValue() && mc.player.isUsingItem()
                || !whileMiningConfig.getValue() && mc.interactionManager.isBreakingBlock();
    }

    private boolean isHoldingCrystal() {
        if (!checkMultitask() && (autoSwapConfig.getValue() == Swap.SILENT || autoSwapConfig.getValue() == Swap.SILENT_ALT)) {
            return true;
        }
        return getCrystalHand() != null;
    }

    private Vec3d crystalDamageVec(BlockPos pos) {
        return Vec3d.of(pos).add(0.5, 1.0, 0.5);
    }

    /**
     * Returns <tt>true</tt> if the {@link Entity} is a valid enemy to attack.
     *
     * @param e The potential enemy entity
     * @return <tt>true</tt> if the entity is an enemy
     */
    private boolean isValidTarget(Entity e) {
        return e instanceof PlayerEntity && playersConfig.getValue()
                || EntityUtil.isMonster(e) && monstersConfig.getValue()
                || EntityUtil.isNeutral(e) && neutralsConfig.getValue()
                || EntityUtil.isPassive(e) && animalsConfig.getValue();
    }

    /**
     * Returns <tt>true</tt> if an {@link EndCrystalItem} can be used on the
     * param {@link BlockPos}.
     *
     * @param p The block pos
     * @return Returns <tt>true</tt> if the crystal item can be placed on the
     * block
     */
    public boolean canUseCrystalOnBlock(BlockPos p) {
        BlockState state = mc.world.getBlockState(p);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) {
            return false;
        }
        BlockPos p2 = p.up();
        BlockState state2 = mc.world.getBlockState(p2);
        // ver 1.12.2 and below
        if (placementsConfig.getValue() == Placements.PROTOCOL && !mc.world.isAir(p2.up())) {
            return false;
        }
        if (!mc.world.isAir(p2) && !state2.isOf(Blocks.FIRE)) {
            return false;
        } else {
            final Box bb = Managers.NETWORK.isCrystalPvpCC() ? HALF_CRYSTAL_BB : FULL_CRYSTAL_BB;
            double d = p2.getX();
            double e = p2.getY();
            double f = p2.getZ();
            List<Entity> list = getEntitiesBlockingCrystal(new Box(d, e, f,
                    d + bb.maxX, e + bb.maxY, f + bb.maxZ));
            return list.isEmpty();
        }
    }

    private List<Entity> getEntitiesBlockingCrystal(Box box) {
        List<Entity> entities = new CopyOnWriteArrayList<>(
                mc.world.getOtherEntities(null, box));
        //
        for (Entity entity : entities) {
            if (entity == null || !entity.isAlive()
                    || entity instanceof ExperienceOrbEntity) {
                entities.remove(entity);
            } else if (entity instanceof EndCrystalEntity entity1
                    // && !intersectingCrystalCheck(entity1) // TODO: More advanced check for intersecting crystals
                    && entity1.getBoundingBox().intersects(box) || attackPackets.containsKey(entity.getId()) && entity.age < ticksExistedConfig.getValue()) {
                entities.remove(entity);
            }
        }
        return entities;
    }

    private boolean intersectingCrystalCheck(EndCrystalEntity entity) {
        // if (entity.age < ticksExistedConfig.getValue())
        // {
        //    return false;
        // }
        return attackRangeCheck(entity);
    }

    private List<BlockPos> getSphere(Vec3d origin) {
        List<BlockPos> sphere = new ArrayList<>();
        double rad = Math.ceil(placeRangeConfig.getValue());
        for (double x = -rad; x <= rad; ++x) {
            for (double y = -rad; y <= rad; ++y) {
                for (double z = -rad; z <= rad; ++z) {
                    Vec3i pos = new Vec3i((int) (origin.getX() + x),
                            (int) (origin.getY() + y), (int) (origin.getZ() + z));
                    final BlockPos p = new BlockPos(pos);
                    sphere.add(p);
                }
            }
        }
        return sphere;
    }

    private boolean canHoldCrystal() {
        return isHoldingCrystal() || autoSwapConfig.getValue() != Swap.OFF && getCrystalSlot() != -1;
    }

    private Hand getCrystalHand() {
        final ItemStack offhand = mc.player.getOffHandStack();
        final ItemStack mainhand = mc.player.getMainHandStack();
        if (offhand.getItem() instanceof EndCrystalItem) {
            return Hand.OFF_HAND;
        } else if (mainhand.getItem() instanceof EndCrystalItem) {
            return Hand.MAIN_HAND;
        }
        return null;
    }

    // Debug info
    public void setStage(String crystalStage) {
        // this.crystalStage = crystalStage;
    }

    public int getBreakMs() {
        float avg = 0.0f;
        // fix ConcurrentModificationException
        ArrayList<Long> latencyCopy = Lists.newArrayList(attackLatency);
        if (!latencyCopy.isEmpty()) {
            for (float t : latencyCopy) {
                avg += t;
            }
            avg /= latencyCopy.size();
        }
        return (int) avg;
    }

    public boolean inTick() {
        if (lethalDmgConfig.getValue()) {
            return lastAttackTimer.passed(500);
            // хуета чот
        } else return !lethalDmgConfig.getValue();
    }
    public enum Swap {
        NORMAL,
        SILENT,
        SILENT_ALT,
        OFF
    }

    public enum Sequential {
        NORMAL,
        STRICT,
        NONE
    }

    public enum Rotate {
        FULL,
        SEMI,
        OFF
    }
    public enum Placements {
        NATIVE,
        PROTOCOL
    }

    private static class DamageData<T> {
        //
        private final List<String> tags = new ArrayList<>();
        private T damageData;
        private Entity attackTarget;
        private BlockPos blockPos;
        //
        private double damage, selfDamage;

        //
        public DamageData() {

        }

        public DamageData(BlockPos damageData, Entity attackTarget, double damage, double selfDamage) {
            this.damageData = (T) damageData;
            this.attackTarget = attackTarget;
            this.damage = damage;
            this.selfDamage = selfDamage;
            this.blockPos = damageData;
        }

        public DamageData(T damageData, Entity attackTarget, double damage, double selfDamage, BlockPos blockPos) {
            this.damageData = damageData;
            this.attackTarget = attackTarget;
            this.damage = damage;
            this.selfDamage = selfDamage;
            this.blockPos = blockPos;
        }

        //

        public T getDamageData() {
            return damageData;
        }

        public Entity getAttackTarget() {
            return attackTarget;
        }

        public double getDamage() {
            return damage;
        }

        public double getSelfDamage() {
            return selfDamage;
        }

        public BlockPos getBlockPos() {
            return blockPos;
        }
    }
}