package net.sprocketgames.betterphantoms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.sprocketgames.betterphantoms.mixin.PhantomAccessor;

public final class EndPhantomBehaviorController {
    private static final int HORIZONTAL_DETECTION_RANGE = 17;
    private static final int HORIZONTAL_DETECTION_RANGE_SQR = HORIZONTAL_DETECTION_RANGE * HORIZONTAL_DETECTION_RANGE;
    private static final int END_CITY_HORIZONTAL_DETECTION_RANGE = 28;
    private static final int END_CITY_HORIZONTAL_DETECTION_RANGE_SQR =
            END_CITY_HORIZONTAL_DETECTION_RANGE * END_CITY_HORIZONTAL_DETECTION_RANGE;
    private static final int FIGHT_HORIZONTAL_DETECTION_RANGE = 30;
    private static final int FIGHT_HORIZONTAL_DETECTION_RANGE_SQR = FIGHT_HORIZONTAL_DETECTION_RANGE * FIGHT_HORIZONTAL_DETECTION_RANGE;
    private static final int VERTICAL_DETECTION_RANGE = 180;
    private static final int FIGHT_VERTICAL_DETECTION_RANGE = VERTICAL_DETECTION_RANGE;
    private static final int COMMIT_HORIZONTAL_RANGE = 25;
    private static final int COMMIT_HORIZONTAL_RANGE_SQR = COMMIT_HORIZONTAL_RANGE * COMMIT_HORIZONTAL_RANGE;
    private static final int END_CITY_COMMIT_HORIZONTAL_RANGE = 38;
    private static final int END_CITY_COMMIT_HORIZONTAL_RANGE_SQR =
            END_CITY_COMMIT_HORIZONTAL_RANGE * END_CITY_COMMIT_HORIZONTAL_RANGE;
    private static final int FIGHT_COMMIT_HORIZONTAL_RANGE = 40;
    private static final int FIGHT_COMMIT_HORIZONTAL_RANGE_SQR = FIGHT_COMMIT_HORIZONTAL_RANGE * FIGHT_COMMIT_HORIZONTAL_RANGE;
    private static final int COMMIT_VERTICAL_RANGE = 240;
    private static final int FIGHT_COMMIT_VERTICAL_RANGE = COMMIT_VERTICAL_RANGE;
    private static final int EXPOSURE_TICKS_REQUIRED = 30;
    private static final int EXPOSURE_TICKS_REQUIRED_END_CITY = 16;
    private static final int FIGHT_EXPOSURE_TICKS_REQUIRED = 8;
    private static final int LOCK_ON_TICKS = 20;
    private static final int LOCK_ON_TICKS_END_CITY = 14;
    private static final int LOCK_ON_TICKS_FIGHT = 8;
    private static final int COMMIT_TICKS = 220;
    private static final int COMMIT_TICKS_END_CITY = 250;
    private static final int COMMIT_TICKS_FIGHT = 280;
    private static final int FRENZY_LOCK_ON_TICKS = 8;
    private static final int FRENZY_COMMIT_BONUS_TICKS = 60;
    private static final double FRENZY_REENGAGE_MULTIPLIER = 0.6D;
    private static final int COVER_LINGER_TICKS = 60;
    private static final int LOITER_TICKS = 120;
    private static final int REENGAGE_TICKS = 55;
    private static final int REENGAGE_TICKS_END_CITY = 40;
    private static final int REENGAGE_TICKS_FIGHT = 30;
    private static final int ENVIRONMENT_RECHECK_TICKS = 80;
    private static final int PATROL_ANCHOR_RECHECK_TICKS = 60;
    private static final int LOITER_ANCHOR_RECHECK_TICKS = 20;
    private static final int LOCKON_ANCHOR_HEIGHT = 26;
    private static final int COMBAT_ANCHOR_MIN = 14;
    private static final int COMBAT_ANCHOR_RANGE = 6;
    private static final int LOITER_ALTITUDE = 18;
    private static final int COVER_CHECK_HEIGHT = 20;
    private static final int PATROL_OFFSET_DEFAULT = 20;
    private static final int PATROL_OFFSET_END_CITY = 8;
    private static final int END_CITY_PATROL_LEASH = 40;
    private static final int END_CITY_LOW_LAYER_MIN_OFFSET = -8;
    private static final int END_CITY_LOW_LAYER_MAX_OFFSET = 6;
    private static final int END_CITY_HIGH_LAYER_MIN_OFFSET = 34;
    private static final int END_CITY_HIGH_LAYER_MAX_OFFSET = 50;
    private static final int END_CITY_HIGH_LAYER_FLOOR_OFFSET = 30;
    private static final int END_CITY_HIGH_LAYER_ENGAGE_VERTICAL_WINDOW = 34;
    private static final double END_CITY_HIGH_LAYER_RATIO = 0.5D;

    private static final Map<UUID, PhantomState> STATES = new HashMap<>();

    private EndPhantomBehaviorController() {}

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Phantom phantom)) {
            return;
        }

        if (!(phantom.level() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().equals(net.minecraft.world.level.Level.END) || !phantom.isAlive()) {
            STATES.remove(phantom.getUUID());
            return;
        }

        PhantomState state = STATES.computeIfAbsent(phantom.getUUID(), key -> new PhantomState());
        tickPhantom(level, phantom, state);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Phantom phantom) {
            STATES.remove(phantom.getUUID());
        }
    }

    private static void tickPhantom(ServerLevel level, Phantom phantom, PhantomState state) {
        PhantomAccessor accessor = (PhantomAccessor) phantom;
        int tick = phantom.tickCount;
        boolean fightPhantom = isFightPhantom(phantom);
        boolean frenzied = isFrenzied(phantom);

        if (tick >= state.nextEnvironmentRecheckTick) {
            state.nearEndCity = EndPhantomSpawnController.isNearEndCity(level, phantom.blockPosition());
            if (state.nearEndCity && state.cityPatrolCenter == null) {
                state.cityPatrolCenter = phantom.blockPosition();
            }
            if (state.nearEndCity && state.cityHighPatrolLayer == null) {
                state.cityHighPatrolLayer = resolveCityLayer(phantom, level);
            }
            state.nextEnvironmentRecheckTick = tick + ENVIRONMENT_RECHECK_TICKS;
        }

        // Keep high-layer End City phantoms from drifting down into low airspace while idle.
        if (Boolean.TRUE.equals(state.cityHighPatrolLayer)
                && state.cityPatrolCenter != null
                && state.committedTarget == null
                && state.lockOnTarget == null
                && state.loiterTicksRemaining <= 0
                && phantom.getY() < getEndCityHighLayerFloorY(level)) {
            setHighPatrolAnchor(level, accessor, phantom, state);
            state.nextAnchorUpdateTick = tick + LOITER_ANCHOR_RECHECK_TICKS;
            return;
        }

        if (state.committedTarget != null) {
            ServerPlayer committed = level.getServer().getPlayerList().getPlayer(state.committedTarget);
            if (!isValidTarget(
                    phantom,
                    committed,
                    getCommitRangeSqr(state.nearEndCity, fightPhantom),
                    getCommitVerticalRange(fightPhantom)
            ) || !allowsCityLayerAggro(phantom, committed, state, fightPhantom)) {
                disengageToLoiter(phantom, accessor, state, phantom.blockPosition());
                return;
            }

            if (!isPlayerExposed(level, committed)) {
                state.coverTicks++;
                if (tick >= state.nextAnchorUpdateTick) {
                    state.loiterCenter = committed.blockPosition();
                    setLoiterAnchor(level, accessor, phantom, state, state.loiterCenter);
                    state.nextAnchorUpdateTick = tick + LOITER_ANCHOR_RECHECK_TICKS;
                }

                if (state.coverTicks > COVER_LINGER_TICKS) {
                    disengageToLoiter(phantom, accessor, state, committed.blockPosition());
                }
                return;
            }

            state.coverTicks = 0;
            state.commitTicksRemaining--;
            phantom.setTarget(committed);

            if (tick >= state.nextAnchorUpdateTick) {
                setCombatAnchor(accessor, phantom, committed);
                state.nextAnchorUpdateTick = tick + LOITER_ANCHOR_RECHECK_TICKS;
            }

            if (state.nearEndCity && Config.endCityDiveSpeedBonus > 0.0D) {
                applyEndCityDivePressure(phantom);
            }

            if (state.commitTicksRemaining <= 0) {
                disengageToLoiter(phantom, accessor, state, committed.blockPosition());
                return;
            }

            if (tick >= state.nextReengageTick) {
                phantom.setTarget(null);
                phantom.setTarget(committed);
                state.nextReengageTick = tick + getReengageTicks(state.nearEndCity, frenzied, fightPhantom);
            }
            return;
        }

        if (state.lockOnTarget != null) {
            ServerPlayer candidate = level.getServer().getPlayerList().getPlayer(state.lockOnTarget);
            if (!isValidDetectionTarget(level, phantom, candidate, state, fightPhantom)) {
                resetDetectionState(state);
            } else {
                phantom.setTarget(null);
                if (tick >= state.nextAnchorUpdateTick) {
                    setLockOnAnchor(accessor, phantom, candidate);
                    state.nextAnchorUpdateTick = tick + LOITER_ANCHOR_RECHECK_TICKS;
                }

                state.lockOnTicksRemaining--;
                if (state.lockOnTicksRemaining <= 0) {
                    beginCommit(phantom, accessor, state, candidate, frenzied, fightPhantom);
                }
            }
            return;
        }

        if (phantom.getTarget() instanceof ServerPlayer vanillaTarget) {
            phantom.setTarget(null);
            if (isValidDetectionTarget(level, phantom, vanillaTarget, state, fightPhantom)) {
                progressExposure(state, vanillaTarget.getUUID());
            } else {
                resetDetectionState(state);
            }
        } else {
            ServerPlayer candidate = findDetectionTarget(level, phantom, state, fightPhantom);
            if (candidate != null) {
                progressExposure(state, candidate.getUUID());
                if (state.exposureTicks >= getExposureTicksRequired(state.nearEndCity, fightPhantom)) {
                    state.lockOnTarget = candidate.getUUID();
                    state.lockOnTicksRemaining = getLockOnTicks(state.nearEndCity, frenzied, fightPhantom);
                    state.exposureTicks = 0;
                    state.nextAnchorUpdateTick = tick;
                }
            } else {
                resetDetectionState(state);
            }
        }

        phantom.setTarget(null);

        if (state.loiterTicksRemaining > 0) {
            state.loiterTicksRemaining--;
            if (tick >= state.nextAnchorUpdateTick && state.loiterCenter != null) {
                setLoiterAnchor(level, accessor, phantom, state, state.loiterCenter);
                state.nextAnchorUpdateTick = tick + LOITER_ANCHOR_RECHECK_TICKS;
            }
            return;
        }

        if (tick >= state.nextAnchorUpdateTick) {
            setHighPatrolAnchor(level, accessor, phantom, state);
            state.nextAnchorUpdateTick = tick + PATROL_ANCHOR_RECHECK_TICKS;
        }
    }

    private static boolean isValidTarget(
            Phantom phantom,
            @Nullable ServerPlayer player,
            double maxHorizontalRangeSqr,
            double maxVerticalRange
    ) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.isCreative()) {
            return false;
        }
        return horizontalDistanceSqr(phantom, player) <= maxHorizontalRangeSqr
                && Math.abs(player.getY() - phantom.getY()) <= maxVerticalRange;
    }

    private static boolean isValidDetectionTarget(
            ServerLevel level,
            Phantom phantom,
            @Nullable ServerPlayer player,
            PhantomState state,
            boolean fightPhantom
    ) {
        return isValidTarget(
                phantom,
                player,
                getDetectionRangeSqr(state.nearEndCity, fightPhantom),
                getDetectionVerticalRange(fightPhantom)
        )
                && allowsCityLayerAggro(phantom, player, state, fightPhantom)
                && isPlayerExposed(level, player);
    }

    private static boolean isPlayerExposed(ServerLevel level, ServerPlayer player) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int startY = Mth.clamp(player.getBlockY() + 1, minY, maxY);
        int endY = Mth.clamp(player.getBlockY() + COVER_CHECK_HEIGHT, minY, maxY);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(player.getBlockX(), startY, player.getBlockZ());
        for (int y = startY; y <= endY; y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.getCollisionShape(level, cursor).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static ServerPlayer findDetectionTarget(ServerLevel level, Phantom phantom, PhantomState state, boolean fightPhantom) {
        List<ServerPlayer> players = level.players();
        ServerPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            if (!isValidDetectionTarget(level, phantom, player, state, fightPhantom)) {
                continue;
            }

            double dist = horizontalDistanceSqr(phantom, player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }

        return closest;
    }

    private static void progressExposure(PhantomState state, UUID candidate) {
        if (candidate.equals(state.exposureTarget)) {
            state.exposureTicks++;
        } else {
            state.exposureTarget = candidate;
            state.exposureTicks = 1;
        }
    }

    private static void resetDetectionState(PhantomState state) {
        state.exposureTarget = null;
        state.exposureTicks = 0;
        state.lockOnTarget = null;
        state.lockOnTicksRemaining = 0;
    }

    private static void beginCommit(
            Phantom phantom,
            PhantomAccessor accessor,
            PhantomState state,
            ServerPlayer target,
            boolean frenzied,
            boolean fightPhantom
    ) {
        state.lockOnTarget = null;
        state.lockOnTicksRemaining = 0;
        state.exposureTicks = 0;
        state.exposureTarget = null;
        state.committedTarget = target.getUUID();
        state.commitTicksRemaining = getCommitTicks(state.nearEndCity, frenzied, fightPhantom);
        state.coverTicks = 0;
        state.nextReengageTick = phantom.tickCount + getReengageTicks(state.nearEndCity, frenzied, fightPhantom);
        state.nextAnchorUpdateTick = phantom.tickCount;

        phantom.setTarget(target);
        setCombatAnchor(accessor, phantom, target);
    }

    private static void disengageToLoiter(Phantom phantom, PhantomAccessor accessor, PhantomState state, BlockPos center) {
        phantom.setTarget(null);
        state.committedTarget = null;
        state.commitTicksRemaining = 0;
        state.coverTicks = 0;
        state.loiterTicksRemaining = LOITER_TICKS;
        state.loiterCenter = center;
        state.nextAnchorUpdateTick = phantom.tickCount;
        resetDetectionState(state);
    }

    private static void setHighPatrolAnchor(ServerLevel level, PhantomAccessor accessor, Phantom phantom, PhantomState state) {
        int minY = level.getMinBuildHeight() + 16;
        int maxY = level.getMaxBuildHeight() - 10;
        int baseAltitude = Mth.clamp(Math.max(Config.highPatrolAltitude, level.getSeaLevel() + 20), minY, maxY);
        int y;
        BlockPos anchorCenter = phantom.blockPosition();
        int patrolOffset = PATROL_OFFSET_DEFAULT;

        if (state.nearEndCity || state.cityPatrolCenter != null) {
            if (state.cityPatrolCenter == null) {
                state.cityPatrolCenter = phantom.blockPosition();
            }
            if (state.cityHighPatrolLayer == null) {
                state.cityHighPatrolLayer = resolveCityLayer(phantom, level);
            }

            anchorCenter = state.cityPatrolCenter;
            patrolOffset = PATROL_OFFSET_END_CITY;
            if (Boolean.TRUE.equals(state.cityHighPatrolLayer)) {
                y = Mth.clamp(
                        baseAltitude + Mth.nextInt(phantom.getRandom(), END_CITY_HIGH_LAYER_MIN_OFFSET, END_CITY_HIGH_LAYER_MAX_OFFSET),
                        minY,
                        maxY
                );
            } else {
                y = Mth.clamp(
                        baseAltitude + Mth.nextInt(phantom.getRandom(), END_CITY_LOW_LAYER_MIN_OFFSET, END_CITY_LOW_LAYER_MAX_OFFSET),
                        minY,
                        maxY
                );
            }
            if (!state.nearEndCity) {
                double distToCityCenter = phantom.blockPosition().distSqr(state.cityPatrolCenter);
                double leashSqr = (double) END_CITY_PATROL_LEASH * END_CITY_PATROL_LEASH;
                if (distToCityCenter > leashSqr) {
                    anchorCenter = state.cityPatrolCenter;
                }
            }
        } else {
            y = Mth.clamp(baseAltitude + Mth.nextInt(phantom.getRandom(), -8, 10), minY, maxY);
        }

        int offsetX = Mth.nextInt(phantom.getRandom(), -patrolOffset, patrolOffset);
        int offsetZ = Mth.nextInt(phantom.getRandom(), -patrolOffset, patrolOffset);
        accessor.betterphantoms$setAnchorPoint(new BlockPos(anchorCenter.getX() + offsetX, y, anchorCenter.getZ() + offsetZ));
    }

    private static void setLockOnAnchor(PhantomAccessor accessor, Phantom phantom, ServerPlayer target) {
        int y = Math.max(Config.highPatrolAltitude, target.getBlockY() + LOCKON_ANCHOR_HEIGHT);
        accessor.betterphantoms$setAnchorPoint(new BlockPos(target.getBlockX(), y, target.getBlockZ()));
    }

    private static void setCombatAnchor(PhantomAccessor accessor, Phantom phantom, ServerPlayer target) {
        int y = target.getBlockY() + COMBAT_ANCHOR_MIN + phantom.getRandom().nextInt(COMBAT_ANCHOR_RANGE);
        accessor.betterphantoms$setAnchorPoint(new BlockPos(target.getBlockX(), y, target.getBlockZ()));
    }

    private static void setLoiterAnchor(
            ServerLevel level,
            PhantomAccessor accessor,
            Phantom phantom,
            PhantomState state,
            BlockPos center
    ) {
        int hash = phantom.getUUID().hashCode();
        double angle = ((hash & 0xFFFF) / 65535.0D) * Math.PI * 2.0D + phantom.tickCount * 0.03D;
        int radius = 14 + ((hash >>> 16) & 15);
        int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
        int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
        int y;
        if (Boolean.TRUE.equals(state.cityHighPatrolLayer) && state.cityPatrolCenter != null) {
            int minY = level.getMinBuildHeight() + 16;
            int maxY = level.getMaxBuildHeight() - 10;
            int baseAltitude = Mth.clamp(Math.max(Config.highPatrolAltitude, level.getSeaLevel() + 20), minY, maxY);
            y = Mth.clamp(
                    baseAltitude + Mth.nextInt(phantom.getRandom(), END_CITY_HIGH_LAYER_MIN_OFFSET, END_CITY_HIGH_LAYER_MAX_OFFSET),
                    minY,
                    maxY
            );
        } else {
            y = Math.max(Config.highPatrolAltitude - 8, center.getY() + LOITER_ALTITUDE);
        }
        accessor.betterphantoms$setAnchorPoint(new BlockPos(x, y, z));
    }

    private static int getEndCityHighLayerFloorY(ServerLevel level) {
        int minY = level.getMinBuildHeight() + 16;
        int maxY = level.getMaxBuildHeight() - 10;
        int baseAltitude = Mth.clamp(Math.max(Config.highPatrolAltitude, level.getSeaLevel() + 20), minY, maxY);
        return Mth.clamp(baseAltitude + END_CITY_HIGH_LAYER_FLOOR_OFFSET, minY, maxY);
    }

    private static int getLockOnTicks(boolean nearEndCity, boolean frenzied, boolean fightPhantom) {
        if (frenzied) {
            return FRENZY_LOCK_ON_TICKS;
        }
        if (fightPhantom) {
            return LOCK_ON_TICKS_FIGHT;
        }
        return nearEndCity ? LOCK_ON_TICKS_END_CITY : LOCK_ON_TICKS;
    }

    private static int getCommitTicks(boolean nearEndCity, boolean frenzied, boolean fightPhantom) {
        int base = fightPhantom ? COMMIT_TICKS_FIGHT : (nearEndCity ? COMMIT_TICKS_END_CITY : COMMIT_TICKS);
        if (frenzied) {
            return base + FRENZY_COMMIT_BONUS_TICKS;
        }
        return base;
    }

    private static int getReengageTicks(boolean nearEndCity, boolean frenzied, boolean fightPhantom) {
        int base = fightPhantom ? REENGAGE_TICKS_FIGHT : (nearEndCity ? REENGAGE_TICKS_END_CITY : REENGAGE_TICKS);
        if (!frenzied) {
            return base;
        }
        return Math.max(10, (int) Math.round(base * FRENZY_REENGAGE_MULTIPLIER));
    }

    private static int getExposureTicksRequired(boolean nearEndCity, boolean fightPhantom) {
        if (fightPhantom) {
            return FIGHT_EXPOSURE_TICKS_REQUIRED;
        }
        return nearEndCity ? EXPOSURE_TICKS_REQUIRED_END_CITY : EXPOSURE_TICKS_REQUIRED;
    }

    private static double getDetectionRangeSqr(boolean nearEndCity, boolean fightPhantom) {
        if (fightPhantom) {
            return FIGHT_HORIZONTAL_DETECTION_RANGE_SQR;
        }
        return nearEndCity ? END_CITY_HORIZONTAL_DETECTION_RANGE_SQR : HORIZONTAL_DETECTION_RANGE_SQR;
    }

    private static double getDetectionVerticalRange(boolean fightPhantom) {
        return fightPhantom ? FIGHT_VERTICAL_DETECTION_RANGE : VERTICAL_DETECTION_RANGE;
    }

    private static double getCommitRangeSqr(boolean nearEndCity, boolean fightPhantom) {
        if (fightPhantom) {
            return FIGHT_COMMIT_HORIZONTAL_RANGE_SQR;
        }
        return nearEndCity ? END_CITY_COMMIT_HORIZONTAL_RANGE_SQR : COMMIT_HORIZONTAL_RANGE_SQR;
    }

    private static double getCommitVerticalRange(boolean fightPhantom) {
        return fightPhantom ? FIGHT_COMMIT_VERTICAL_RANGE : COMMIT_VERTICAL_RANGE;
    }

    private static boolean isFightPhantom(Phantom phantom) {
        return phantom.getTags().contains(DragonFightPhantomManager.FIGHT_PHANTOM_TAG);
    }

    private static boolean isFrenzied(Phantom phantom) {
        if (!phantom.getTags().contains(DragonFightPhantomManager.FRENZY_PHANTOM_TAG)) {
            return false;
        }

        if (!(phantom.level() instanceof ServerLevel level)) {
            return false;
        }

        EndDragonFight fight = level.getDragonFight();
        if (fight == null || fight.getDragonUUID() == null) {
            return false;
        }

        Entity dragon = level.getEntity(fight.getDragonUUID());
        return dragon instanceof EnderDragon enderDragon && enderDragon.isAlive();
    }

    private static boolean allowsCityLayerAggro(
            Phantom phantom,
            @Nullable ServerPlayer player,
            PhantomState state,
            boolean fightPhantom
    ) {
        if (fightPhantom
                || player == null
                || state.cityPatrolCenter == null
                || !Boolean.TRUE.equals(state.cityHighPatrolLayer)) {
            return true;
        }

        return player.getY() >= phantom.getY() - END_CITY_HIGH_LAYER_ENGAGE_VERTICAL_WINDOW;
    }

    private static void applyEndCityDivePressure(Phantom phantom) {
        double bonus = Math.max(0.0D, Config.endCityDiveSpeedBonus);
        if (bonus <= 0.0D) {
            return;
        }

        int amplifier = Mth.clamp((int) Math.floor(Math.max(0.0D, bonus - 0.1D) / 0.25D), 0, 4);
        int duration = Mth.clamp((int) Math.round(30 + (bonus * 40.0D)), 20, 100);
        MobEffectInstance current = phantom.getEffect(MobEffects.MOVEMENT_SPEED);
        if (current != null && current.getAmplifier() >= amplifier && current.getDuration() > 10) {
            return;
        }

        phantom.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier, false, false, true));
    }

    private static double horizontalDistanceSqr(Phantom phantom, Player player) {
        double dx = player.getX() - phantom.getX();
        double dz = player.getZ() - phantom.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean resolveCityLayer(Phantom phantom, ServerLevel level) {
        if (phantom.getTags().contains(EndPhantomSpawnController.CITY_HIGH_LAYER_TAG)) {
            return true;
        }
        if (phantom.getTags().contains(EndPhantomSpawnController.CITY_LOW_LAYER_TAG)) {
            return false;
        }
        return level.random.nextDouble() < END_CITY_HIGH_LAYER_RATIO;
    }

    private static final class PhantomState {
        @Nullable
        private UUID exposureTarget;
        private int exposureTicks;
        @Nullable
        private UUID lockOnTarget;
        private int lockOnTicksRemaining;
        @Nullable
        private UUID committedTarget;
        private int commitTicksRemaining;
        private int coverTicks;
        private int nextReengageTick;
        private int loiterTicksRemaining;
        @Nullable
        private BlockPos loiterCenter;
        @Nullable
        private BlockPos cityPatrolCenter;
        @Nullable
        private Boolean cityHighPatrolLayer;
        private int nextAnchorUpdateTick;
        private int nextEnvironmentRecheckTick;
        private boolean nearEndCity;
    }
}
