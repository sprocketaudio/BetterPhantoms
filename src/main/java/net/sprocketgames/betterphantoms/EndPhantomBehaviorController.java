package net.sprocketgames.betterphantoms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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
    private static final int HORIZONTAL_DETECTION_RANGE = 56;
    private static final int HORIZONTAL_DETECTION_RANGE_SQR = HORIZONTAL_DETECTION_RANGE * HORIZONTAL_DETECTION_RANGE;
    private static final int FIGHT_HORIZONTAL_DETECTION_RANGE = 160;
    private static final int FIGHT_HORIZONTAL_DETECTION_RANGE_SQR = FIGHT_HORIZONTAL_DETECTION_RANGE * FIGHT_HORIZONTAL_DETECTION_RANGE;
    private static final int EXPOSURE_TICKS_REQUIRED = 30;
    private static final int FIGHT_EXPOSURE_TICKS_REQUIRED = 8;
    private static final int LOCK_ON_TICKS = 20;
    private static final int LOCK_ON_TICKS_END_CITY = 12;
    private static final int LOCK_ON_TICKS_FIGHT = 8;
    private static final int COMMIT_TICKS = 220;
    private static final int COMMIT_TICKS_END_CITY = 260;
    private static final int COMMIT_TICKS_FIGHT = 280;
    private static final int FRENZY_LOCK_ON_TICKS = 8;
    private static final int FRENZY_COMMIT_BONUS_TICKS = 60;
    private static final double FRENZY_REENGAGE_MULTIPLIER = 0.6D;
    private static final int COVER_LINGER_TICKS = 60;
    private static final int LOITER_TICKS = 120;
    private static final int REENGAGE_TICKS = 55;
    private static final int REENGAGE_TICKS_END_CITY = 38;
    private static final int REENGAGE_TICKS_FIGHT = 30;
    private static final int ENVIRONMENT_RECHECK_TICKS = 80;
    private static final int PATROL_ANCHOR_RECHECK_TICKS = 60;
    private static final int LOITER_ANCHOR_RECHECK_TICKS = 20;
    private static final int LOCKON_ANCHOR_HEIGHT = 26;
    private static final int COMBAT_ANCHOR_MIN = 14;
    private static final int COMBAT_ANCHOR_RANGE = 6;
    private static final int LOITER_ALTITUDE = 18;

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
            state.nextEnvironmentRecheckTick = tick + ENVIRONMENT_RECHECK_TICKS;
        }

        if (state.committedTarget != null) {
            ServerPlayer committed = level.getServer().getPlayerList().getPlayer(state.committedTarget);
            if (!isValidTarget(phantom, committed, getCommitRangeSqr(fightPhantom))) {
                disengageToLoiter(phantom, accessor, state, phantom.blockPosition());
                return;
            }

            if (!isPlayerExposed(level, committed)) {
                state.coverTicks++;
                if (tick >= state.nextAnchorUpdateTick) {
                    state.loiterCenter = committed.blockPosition();
                    setLoiterAnchor(accessor, phantom, state.loiterCenter);
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
            if (!isValidDetectionTarget(level, phantom, candidate, fightPhantom)) {
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
            if (isValidDetectionTarget(level, phantom, vanillaTarget, fightPhantom)) {
                progressExposure(state, vanillaTarget.getUUID());
            } else {
                resetDetectionState(state);
            }
        } else {
            ServerPlayer candidate = findDetectionTarget(level, phantom, fightPhantom);
            if (candidate != null) {
                progressExposure(state, candidate.getUUID());
                if (state.exposureTicks >= getExposureTicksRequired(fightPhantom)) {
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
                setLoiterAnchor(accessor, phantom, state.loiterCenter);
                state.nextAnchorUpdateTick = tick + LOITER_ANCHOR_RECHECK_TICKS;
            }
            return;
        }

        if (tick >= state.nextAnchorUpdateTick) {
            setHighPatrolAnchor(level, accessor, phantom);
            state.nextAnchorUpdateTick = tick + PATROL_ANCHOR_RECHECK_TICKS;
        }
    }

    private static boolean isValidTarget(Phantom phantom, @Nullable ServerPlayer player, double maxHorizontalRangeSqr) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.isCreative()) {
            return false;
        }
        return horizontalDistanceSqr(phantom, player) <= maxHorizontalRangeSqr;
    }

    private static boolean isValidDetectionTarget(ServerLevel level, Phantom phantom, @Nullable ServerPlayer player, boolean fightPhantom) {
        return isValidTarget(phantom, player, getDetectionRangeSqr(fightPhantom))
                && isPlayerExposed(level, player);
    }

    private static boolean isPlayerExposed(ServerLevel level, ServerPlayer player) {
        return level.canSeeSky(player.blockPosition().above());
    }

    private static ServerPlayer findDetectionTarget(ServerLevel level, Phantom phantom, boolean fightPhantom) {
        List<ServerPlayer> players = level.players();
        ServerPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            if (!isValidDetectionTarget(level, phantom, player, fightPhantom)) {
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

    private static void setHighPatrolAnchor(ServerLevel level, PhantomAccessor accessor, Phantom phantom) {
        int minY = level.getMinBuildHeight() + 16;
        int maxY = level.getMaxBuildHeight() - 10;
        int baseAltitude = Mth.clamp(Math.max(Config.highPatrolAltitude, level.getSeaLevel() + 20), minY, maxY);
        int y = Mth.clamp(baseAltitude + Mth.nextInt(phantom.getRandom(), -8, 10), minY, maxY);
        int offsetX = Mth.nextInt(phantom.getRandom(), -20, 20);
        int offsetZ = Mth.nextInt(phantom.getRandom(), -20, 20);
        accessor.betterphantoms$setAnchorPoint(new BlockPos(phantom.getBlockX() + offsetX, y, phantom.getBlockZ() + offsetZ));
    }

    private static void setLockOnAnchor(PhantomAccessor accessor, Phantom phantom, ServerPlayer target) {
        int y = Math.max(Config.highPatrolAltitude, target.getBlockY() + LOCKON_ANCHOR_HEIGHT);
        accessor.betterphantoms$setAnchorPoint(new BlockPos(target.getBlockX(), y, target.getBlockZ()));
    }

    private static void setCombatAnchor(PhantomAccessor accessor, Phantom phantom, ServerPlayer target) {
        int y = target.getBlockY() + COMBAT_ANCHOR_MIN + phantom.getRandom().nextInt(COMBAT_ANCHOR_RANGE);
        accessor.betterphantoms$setAnchorPoint(new BlockPos(target.getBlockX(), y, target.getBlockZ()));
    }

    private static void setLoiterAnchor(PhantomAccessor accessor, Phantom phantom, BlockPos center) {
        int hash = phantom.getUUID().hashCode();
        double angle = ((hash & 0xFFFF) / 65535.0D) * Math.PI * 2.0D + phantom.tickCount * 0.03D;
        int radius = 14 + ((hash >>> 16) & 15);
        int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
        int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
        int y = Math.max(Config.highPatrolAltitude - 8, center.getY() + LOITER_ALTITUDE);
        accessor.betterphantoms$setAnchorPoint(new BlockPos(x, y, z));
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

    private static int getExposureTicksRequired(boolean fightPhantom) {
        return fightPhantom ? FIGHT_EXPOSURE_TICKS_REQUIRED : EXPOSURE_TICKS_REQUIRED;
    }

    private static double getDetectionRangeSqr(boolean fightPhantom) {
        return fightPhantom ? FIGHT_HORIZONTAL_DETECTION_RANGE_SQR : HORIZONTAL_DETECTION_RANGE_SQR;
    }

    private static double getCommitRangeSqr(boolean fightPhantom) {
        return getDetectionRangeSqr(fightPhantom) * 4.0D;
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
        private int nextAnchorUpdateTick;
        private int nextEnvironmentRecheckTick;
        private boolean nearEndCity;
    }
}
