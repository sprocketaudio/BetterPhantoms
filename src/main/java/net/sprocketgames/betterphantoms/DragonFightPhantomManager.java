package net.sprocketgames.betterphantoms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class DragonFightPhantomManager {
    private static final int WAVE_DELAY_TICKS = 20;
    private static final int DRAGON_ARENA_RADIUS = 196;
    private static final int PHANTOM_TARGET_RADIUS_SQR = 160 * 160;
    private static final int PHANTOM_SPAWN_OFFSET = 10;
    private static final String FIGHT_PHANTOM_TAG = BetterPhantomsMod.MOD_ID + ":dragon_fight_wave";
    public static final String FRENZY_PHANTOM_TAG = BetterPhantomsMod.MOD_ID + ":dragon_fight_frenzy";
    private static final Map<net.minecraft.resources.ResourceKey<Level>, FightState> STATES = new HashMap<>();

    private DragonFightPhantomManager() {}

    public static void onCrystalDestroyed(EndDragonFight fight, EndCrystal crystal) {
        if (!Config.enableDragonFightPhantomWaves) {
            return;
        }

        if (!(crystal.level() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().equals(Level.END) || !isArenaCrystal(crystal.blockPosition())) {
            return;
        }

        if (!isDragonFightActive(level, fight)) {
            return;
        }

        int waveSize = getWaveSize(level.random, fight.getCrystalsAlive());
        if (waveSize <= 0) {
            return;
        }

        FightState state = STATES.computeIfAbsent(level.dimension(), key -> new FightState());
        int spawnTick = level.getServer().getTickCount() + WAVE_DELAY_TICKS;
        state.pendingWaves.add(new PendingWave(crystal.blockPosition(), spawnTick, waveSize));

        if (fight.getCrystalsAlive() == 0) {
            applyFrenzy(level, state, crystal.blockPosition());
        }
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (!Config.enableDragonFightPhantomWaves) {
            clearAllTrackedState(event.getServer());
            return;
        }

        int serverTick = event.getServer().getTickCount();
        Iterator<Map.Entry<net.minecraft.resources.ResourceKey<Level>, FightState>> stateIterator = STATES.entrySet().iterator();

        while (stateIterator.hasNext()) {
            Map.Entry<net.minecraft.resources.ResourceKey<Level>, FightState> entry = stateIterator.next();
            ServerLevel level = event.getServer().getLevel(entry.getKey());
            if (level == null) {
                continue;
            }

            FightState state = entry.getValue();
            EndDragonFight fight = level.getDragonFight();
            if (!isDragonFightActive(level, fight)) {
                state.pendingWaves.clear();
                clearFightTags(level, state);
                clearFrenzyTags(level, state);
                state.trackedPhantoms.clear();
                stateIterator.remove();
                continue;
            }

            pruneTrackedPhantoms(level, state);
            expireFrenzy(level, state, serverTick);

            Iterator<PendingWave> waveIterator = state.pendingWaves.iterator();
            while (waveIterator.hasNext()) {
                PendingWave wave = waveIterator.next();
                if (wave.spawnTick > serverTick) {
                    continue;
                }

                spawnWave(level, state, wave);
                waveIterator.remove();
            }

            if (state.pendingWaves.isEmpty() && state.trackedPhantoms.isEmpty()) {
                stateIterator.remove();
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        clearAllTrackedState(event.getServer());
    }

    private static boolean isDragonFightActive(ServerLevel level, EndDragonFight fight) {
        if (fight == null) {
            return false;
        }

        UUID dragonUuid = fight.getDragonUUID();
        if (dragonUuid == null) {
            return false;
        }

        Entity dragonEntity = level.getEntity(dragonUuid);
        return dragonEntity instanceof EnderDragon dragon && dragon.isAlive();
    }

    private static boolean isArenaCrystal(BlockPos crystalPos) {
        return crystalPos.distSqr(BlockPos.ZERO) <= (long) DRAGON_ARENA_RADIUS * DRAGON_ARENA_RADIUS;
    }

    private static int getWaveSize(RandomSource random, int crystalsRemaining) {
        int min = Math.max(0, Config.phantomsPerCrystalWaveMin);
        int max = Math.max(min, Config.phantomsPerCrystalWaveMax);
        int waveSize = Mth.nextInt(random, min, max);

        if (crystalsRemaining <= 4 && random.nextFloat() < 0.5F) {
            waveSize = Math.min(max, waveSize + 1);
        }

        return waveSize;
    }

    private static void pruneTrackedPhantoms(ServerLevel level, FightState state) {
        state.trackedPhantoms.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return !(entity instanceof Phantom phantom) || !phantom.isAlive() || !phantom.getTags().contains(FIGHT_PHANTOM_TAG);
        });
        state.frenzyExpiryByPhantom.keySet().removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            return !(entity instanceof Phantom phantom) || !phantom.isAlive();
        });
    }

    private static void expireFrenzy(ServerLevel level, FightState state, int serverTick) {
        Iterator<Map.Entry<UUID, Integer>> iterator = state.frenzyExpiryByPhantom.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            if (entry.getValue() > serverTick) {
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof Phantom phantom && phantom.isAlive()) {
                phantom.removeTag(FRENZY_PHANTOM_TAG);
            }
            iterator.remove();
        }
    }

    private static void clearAllTrackedState(net.minecraft.server.MinecraftServer server) {
        for (Map.Entry<net.minecraft.resources.ResourceKey<Level>, FightState> entry : STATES.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            clearFightTags(level, entry.getValue());
            clearFrenzyTags(level, entry.getValue());
        }
        STATES.clear();
    }

    private static void clearFightTags(ServerLevel level, FightState state) {
        for (UUID uuid : state.trackedPhantoms) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Phantom phantom && phantom.isAlive()) {
                phantom.removeTag(FIGHT_PHANTOM_TAG);
            }
        }
    }

    private static void clearFrenzyTags(ServerLevel level, FightState state) {
        for (UUID uuid : state.frenzyExpiryByPhantom.keySet()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Phantom phantom && phantom.isAlive()) {
                phantom.removeTag(FRENZY_PHANTOM_TAG);
            }
        }
        state.frenzyExpiryByPhantom.clear();
    }

    private static void spawnWave(ServerLevel level, FightState state, PendingWave wave) {
        int currentActive = state.trackedPhantoms.size();
        int slotsRemaining = Math.max(0, Config.activeFightPhantomCap - currentActive);
        int phantomsToSpawn = Math.min(wave.size, slotsRemaining);

        if (phantomsToSpawn <= 0) {
            return;
        }

        for (int i = 0; i < phantomsToSpawn; i++) {
            spawnFightPhantom(level, state, wave.origin);
        }
    }

    private static void spawnFightPhantom(ServerLevel level, FightState state, BlockPos origin) {
        Phantom phantom = EntityType.PHANTOM.create(level);
        if (phantom == null) {
            return;
        }

        BlockPos spawnPos = origin.offset(
                Mth.nextInt(level.random, -PHANTOM_SPAWN_OFFSET, PHANTOM_SPAWN_OFFSET),
                18 + level.random.nextInt(10),
                Mth.nextInt(level.random, -PHANTOM_SPAWN_OFFSET, PHANTOM_SPAWN_OFFSET)
        );

        phantom.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY() + 0.5D,
                spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );
        phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
        phantom.addTag(FIGHT_PHANTOM_TAG);

        ServerPlayer target = findNearestTarget(level, phantom);
        if (target != null) {
            phantom.setTarget(target);
        }

        if (level.addFreshEntity(phantom)) {
            state.trackedPhantoms.add(phantom.getUUID());
        }
    }

    private static ServerPlayer findNearestTarget(ServerLevel level, Phantom phantom) {
        ServerPlayer closest = null;
        double closestDist = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }

            double dist = player.distanceToSqr(phantom);
            if (dist > PHANTOM_TARGET_RADIUS_SQR || dist >= closestDist) {
                continue;
            }

            closestDist = dist;
            closest = player;
        }

        return closest;
    }

    private static void applyFrenzy(ServerLevel level, FightState state, BlockPos crystalPos) {
        int frenzyDurationTicks = Math.max(1, Config.frenzyDurationSeconds) * 20;
        int frenzyExpiresAt = level.getServer().getTickCount() + frenzyDurationTicks;
        pruneTrackedPhantoms(level, state);

        for (UUID uuid : state.trackedPhantoms) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Phantom phantom && phantom.isAlive()) {
                phantom.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, frenzyDurationTicks, 1, false, false, true));
                phantom.addTag(FRENZY_PHANTOM_TAG);
                state.frenzyExpiryByPhantom.put(uuid, frenzyExpiresAt);
            }
        }

        level.playSound(
                null,
                crystalPos,
                SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE,
                0.9F,
                1.1F
        );
    }

    private static final class FightState {
        private final Set<UUID> trackedPhantoms = new HashSet<>();
        private final List<PendingWave> pendingWaves = new ArrayList<>();
        private final Map<UUID, Integer> frenzyExpiryByPhantom = new HashMap<>();
    }

    private record PendingWave(BlockPos origin, int spawnTick, int size) {}
}
