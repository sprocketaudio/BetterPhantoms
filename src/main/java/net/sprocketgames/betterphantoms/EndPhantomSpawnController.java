package net.sprocketgames.betterphantoms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

public final class EndPhantomSpawnController {
    private static final int SPAWN_WEIGHT_SCALE = 200;
    private static final int BASE_SPAWN_BONUS_WEIGHT = 4;
    private static final int END_CITY_SEARCH_RADIUS_CHUNKS = 12;
    private static final int END_CITY_BONUS_RADIUS_CHUNKS = 4;
    private static final int END_CITY_BONUS_RADIUS_BLOCKS = END_CITY_BONUS_RADIUS_CHUNKS * 16;
    private static final int GENERAL_LOCAL_CAP = 2;
    private static final int END_CITY_LOCAL_CAP = 10;
    private static final int END_CITY_ESCORT_MIN = 3;
    private static final int END_CITY_ESCORT_MAX = 4;
    private static final double GENERAL_PAIR_CHANCE = 0.35D;
    private static final double END_CITY_LOW_LAYER_SHARE = 0.34D;
    private static final int ESCORT_HORIZONTAL_OFFSET = 9;
    private static final int ESCORT_VERTICAL_MIN = 3;
    private static final int ESCORT_VERTICAL_RANGE = 6;
    private static final int NATURAL_SPAWN_MIN_RAISE = 18;
    private static final int NATURAL_SPAWN_HEIGHT_VARIANCE = 6;
    private static final long END_CITY_CACHE_TTL_TICKS = 200L;
    private static final int END_CITY_CACHE_MAX_ENTRIES = 4096;
    private static final String HEALTH_TUNED_TAG = BetterPhantomsMod.MOD_ID + ":health_tuned";
    static final String CITY_HIGH_LAYER_TAG = BetterPhantomsMod.MOD_ID + ":city_high_layer";
    static final String CITY_LOW_LAYER_TAG = BetterPhantomsMod.MOD_ID + ":city_low_layer";
    private static final int END_CITY_LOW_LAYER_MIN_OFFSET = -8;
    private static final int END_CITY_LOW_LAYER_MAX_OFFSET = 6;
    private static final int END_CITY_HIGH_LAYER_MIN_OFFSET = 34;
    private static final int END_CITY_HIGH_LAYER_MAX_OFFSET = 50;
    private static final Map<ResourceKey<Level>, Map<Long, EndCityCacheEntry>> END_CITY_CACHE = new HashMap<>();
    private static final TagKey<Structure> END_CITY_BONUS_ZONE = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(BetterPhantomsMod.MOD_ID, "end_city_bonus_zone")
    );

    private EndPhantomSpawnController() {}

    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getEntityType() != EntityType.PHANTOM) {
            return;
        }

        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        ServerLevel level = event.getLevel().getLevel();
        if (!level.dimension().equals(Level.END)) {
            return;
        }

        if (!Config.enableNaturalEndSpawningAfterDragonDeath || !isNaturalEndSpawningWindowOpen(level)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }

        boolean nearEndCity = isNearEndCity(level, event.getPos());
        int localCap = nearEndCity ? END_CITY_LOCAL_CAP : GENERAL_LOCAL_CAP;
        int nearbyPhantoms = countNearbyPhantoms(level, event.getPos(), null);
        if (nearbyPhantoms >= localCap) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }

        int effectiveWeight = getEffectiveSpawnWeight(nearEndCity);
        if (effectiveWeight <= 0) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }

        int roll = event.getRandom().nextInt(SPAWN_WEIGHT_SCALE + 1);
        if (roll > effectiveWeight) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!(event.getEntity() instanceof Phantom leader)) {
            return;
        }

        if (!(event.getLevel().getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().equals(Level.END)) {
            return;
        }

        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        if (!Config.enableNaturalEndSpawningAfterDragonDeath || !isNaturalEndSpawningWindowOpen(level)) {
            return;
        }

        boolean nearEndCity = isNearEndCity(level, leader.blockPosition());
        int localCap = nearEndCity ? END_CITY_LOCAL_CAP : GENERAL_LOCAL_CAP;
        int nearbyPhantoms = countNearbyPhantoms(level, leader.blockPosition(), leader);
        if (nearbyPhantoms >= localCap) {
            event.setSpawnCancelled(true);
            return;
        }

        int wingmates = getWingmateCount(level, nearEndCity);
        nearbyPhantoms = countNearbyPhantoms(level, leader.blockPosition(), leader);
        int availableSlots = Math.max(0, localCap - nearbyPhantoms);
        int spawnCount = Math.min(Math.max(0, wingmates), availableSlots);
        int totalGroup = spawnCount + 1;

        int lowLayerTargetCount = 0;
        int highLayerTargetCount = 0;
        if (nearEndCity) {
            LayerCount nearbyLayers = countNearbyCityLayerPhantoms(level, leader.blockPosition(), leader);
            LayerCount groupTargets = computeCityLayerTargets(totalGroup, nearbyLayers.low, nearbyLayers.high);
            lowLayerTargetCount = groupTargets.low;
            highLayerTargetCount = groupTargets.high;

            boolean leaderHighLayer = chooseLeaderHighLayer(level, lowLayerTargetCount, highLayerTargetCount);
            assignCityLayer(leader, leaderHighLayer);
            if (leaderHighLayer) {
                highLayerTargetCount = Math.max(0, highLayerTargetCount - 1);
            } else {
                lowLayerTargetCount = Math.max(0, lowLayerTargetCount - 1);
            }
        }

        liftToPatrolAltitude(level, leader);
        applyPhantomHealthTuning(leader);

        for (int i = 0; i < spawnCount; i++) {
            boolean highLayer = false;
            if (nearEndCity) {
                if (highLayerTargetCount > 0 && lowLayerTargetCount > 0) {
                    int totalRemaining = highLayerTargetCount + lowLayerTargetCount;
                    highLayer = level.random.nextInt(totalRemaining) < highLayerTargetCount;
                } else if (highLayerTargetCount > 0) {
                    highLayer = true;
                }

                if (highLayer) {
                    highLayerTargetCount--;
                } else if (lowLayerTargetCount > 0) {
                    lowLayerTargetCount--;
                }
            }
            spawnWingmate(level, leader, event.getDifficulty(), nearEndCity ? Boolean.valueOf(highLayer) : null);
        }

        if (spawnCount <= 0) {
            return;
        }
    }

    private static boolean isNaturalEndSpawningWindowOpen(ServerLevel level) {
        EndDragonFight dragonFight = level.getDragonFight();
        if (dragonFight == null || !dragonFight.hasPreviouslyKilledDragon()) {
            return false;
        }
        return !isDragonFightOrRespawnActive(level, dragonFight);
    }

    private static boolean isDragonFightOrRespawnActive(ServerLevel level, EndDragonFight dragonFight) {
        if (dragonFight.getCrystalsAlive() > 0) {
            return true;
        }

        if (dragonFight.getDragonUUID() == null) {
            return false;
        }

        Entity dragonEntity = level.getEntity(dragonFight.getDragonUUID());
        return dragonEntity instanceof EnderDragon dragon && dragon.isAlive();
    }

    private static int getWingmateCount(ServerLevel level, boolean nearEndCity) {
        if (nearEndCity) {
            return Mth.nextInt(level.random, END_CITY_ESCORT_MIN, END_CITY_ESCORT_MAX);
        }

        return level.random.nextDouble() < GENERAL_PAIR_CHANCE ? 1 : 0;
    }

    private static int countNearbyPhantoms(ServerLevel level, BlockPos center, @Nullable Phantom exclude) {
        int count = 0;
        for (Phantom phantom : level.getEntitiesOfClass(Phantom.class, new AABB(center).inflate(64.0D))) {
            if (!phantom.isAlive()) {
                continue;
            }
            if (exclude != null && phantom.getUUID().equals(exclude.getUUID())) {
                continue;
            }
            count++;
        }
        return count;
    }

    private static void liftToPatrolAltitude(ServerLevel level, Phantom phantom) {
        double oldX = phantom.getX();
        double oldY = phantom.getY();
        double oldZ = phantom.getZ();
        float oldYRot = phantom.getYRot();
        float oldXRot = phantom.getXRot();

        int minY = level.getMinBuildHeight() + 16;
        int maxY = level.getMaxBuildHeight() - 10;
        int baseAltitude = Mth.clamp(
                Math.max(Config.highPatrolAltitude, phantom.getBlockY() + NATURAL_SPAWN_MIN_RAISE),
                minY,
                maxY
        );
        int targetY;
        if (phantom.getTags().contains(CITY_HIGH_LAYER_TAG)) {
            targetY = Mth.clamp(
                    baseAltitude + Mth.nextInt(level.random, END_CITY_HIGH_LAYER_MIN_OFFSET, END_CITY_HIGH_LAYER_MAX_OFFSET),
                    minY,
                    maxY
            );
        } else if (phantom.getTags().contains(CITY_LOW_LAYER_TAG)) {
            targetY = Mth.clamp(
                    baseAltitude + Mth.nextInt(level.random, END_CITY_LOW_LAYER_MIN_OFFSET, END_CITY_LOW_LAYER_MAX_OFFSET),
                    minY,
                    maxY
            );
        } else {
            targetY = Mth.clamp(
                    baseAltitude + Mth.nextInt(level.random, -NATURAL_SPAWN_HEIGHT_VARIANCE, NATURAL_SPAWN_HEIGHT_VARIANCE),
                    minY,
                    maxY
            );
        }

        phantom.moveTo(oldX, targetY + 0.5D, oldZ, oldYRot, oldXRot);
        if (!level.noCollision(phantom)) {
            phantom.moveTo(oldX, oldY, oldZ, oldYRot, oldXRot);
        }
    }

    private static void spawnWingmate(ServerLevel level, Phantom leader, DifficultyInstance difficulty, @Nullable Boolean highLayer) {
        Phantom wingmate = EntityType.PHANTOM.create(level);
        if (wingmate == null) {
            return;
        }

        BlockPos leaderPos = leader.blockPosition();
        int offsetX = Mth.nextInt(level.random, -ESCORT_HORIZONTAL_OFFSET, ESCORT_HORIZONTAL_OFFSET);
        int offsetZ = Mth.nextInt(level.random, -ESCORT_HORIZONTAL_OFFSET, ESCORT_HORIZONTAL_OFFSET);
        int offsetY = ESCORT_VERTICAL_MIN + level.random.nextInt(ESCORT_VERTICAL_RANGE);
        BlockPos spawnPos = leaderPos.offset(offsetX, offsetY, offsetZ);

        wingmate.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY() + 0.5D,
                spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );
        if (highLayer != null) {
            assignCityLayer(wingmate, highLayer.booleanValue());
        }
        wingmate.finalizeSpawn(level, difficulty, MobSpawnType.EVENT, null);
        liftToPatrolAltitude(level, wingmate);
        applyPhantomHealthTuning(wingmate);
        if (!level.noCollision(wingmate)) {
            return;
        }

        level.addFreshEntity(wingmate);
    }

    static void assignCityLayer(Phantom phantom, boolean highLayer) {
        phantom.removeTag(CITY_HIGH_LAYER_TAG);
        phantom.removeTag(CITY_LOW_LAYER_TAG);
        phantom.addTag(highLayer ? CITY_HIGH_LAYER_TAG : CITY_LOW_LAYER_TAG);
    }

    private static boolean chooseLeaderHighLayer(ServerLevel level, int lowRemaining, int highRemaining) {
        if (highRemaining <= 0 && lowRemaining > 0) {
            return false;
        }
        if (lowRemaining <= 0 && highRemaining > 0) {
            return true;
        }
        if (highRemaining == lowRemaining) {
            return level.random.nextBoolean();
        }
        return highRemaining > lowRemaining;
    }

    private static LayerCount countNearbyCityLayerPhantoms(ServerLevel level, BlockPos center, @Nullable Phantom exclude) {
        int low = 0;
        int high = 0;
        for (Phantom phantom : level.getEntitiesOfClass(Phantom.class, new AABB(center).inflate(64.0D))) {
            if (!phantom.isAlive()) {
                continue;
            }
            if (exclude != null && phantom.getUUID().equals(exclude.getUUID())) {
                continue;
            }
            if (phantom.getTags().contains(CITY_HIGH_LAYER_TAG)) {
                high++;
            } else if (phantom.getTags().contains(CITY_LOW_LAYER_TAG)) {
                low++;
            }
        }
        return new LayerCount(low, high);
    }

    private static LayerCount computeCityLayerTargets(int groupSize, int nearbyLow, int nearbyHigh) {
        int existingTagged = nearbyLow + nearbyHigh;
        int totalAfterSpawn = existingTagged + groupSize;
        int targetLowAfterSpawn = Mth.clamp((int) Math.round(totalAfterSpawn * END_CITY_LOW_LAYER_SHARE), 0, totalAfterSpawn);
        int lowNeeded = Mth.clamp(targetLowAfterSpawn - nearbyLow, 0, groupSize);
        int highNeeded = groupSize - lowNeeded;

        if (groupSize >= 3 && nearbyLow == 0 && lowNeeded == 0) {
            lowNeeded = 1;
            highNeeded = groupSize - 1;
        }
        if (groupSize >= 3 && nearbyHigh == 0 && highNeeded == 0) {
            highNeeded = 1;
            lowNeeded = groupSize - 1;
        }

        return new LayerCount(lowNeeded, highNeeded);
    }

    public static void applyPhantomHealthTuning(Phantom phantom) {
        applyPhantomHealthTuning(phantom, Config.phantomHealthMultiplier);
    }

    public static void applyPhantomHealthTuning(Phantom phantom, double configuredMultiplier) {
        if (phantom.getTags().contains(HEALTH_TUNED_TAG)) {
            return;
        }

        AttributeInstance maxHealth = phantom.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double multiplier = Mth.clamp(configuredMultiplier, 0.25D, 1.0D);
        if (Math.abs(multiplier - 1.0D) < 0.0001D) {
            phantom.addTag(HEALTH_TUNED_TAG);
            return;
        }

        double tunedMax = Math.max(1.0D, maxHealth.getBaseValue() * multiplier);
        maxHealth.setBaseValue(tunedMax);
        phantom.setHealth((float) tunedMax);
        phantom.addTag(HEALTH_TUNED_TAG);
    }

    private static int getEffectiveSpawnWeight(boolean nearEndCity) {
        int baseWeight = Mth.clamp(Config.generalEndSpawnWeight, 0, SPAWN_WEIGHT_SCALE);
        if (baseWeight == 0) {
            return 0;
        }

        int tunedBaseWeight = Mth.clamp(baseWeight + BASE_SPAWN_BONUS_WEIGHT, 0, SPAWN_WEIGHT_SCALE);

        if (!nearEndCity) {
            return tunedBaseWeight;
        }

        int boosted = (int) Math.round(tunedBaseWeight * Math.max(0.0D, Config.endCitySpawnDensityMultiplier));
        return Mth.clamp(boosted, 0, SPAWN_WEIGHT_SCALE);
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        END_CITY_CACHE.clear();
    }

    static boolean isNearEndCity(ServerLevel level, BlockPos pos) {
        long gameTime = level.getGameTime();
        Map<Long, EndCityCacheEntry> cacheByChunk = END_CITY_CACHE.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        long chunkKey = ChunkPos.asLong(pos);
        EndCityCacheEntry cached = cacheByChunk.get(chunkKey);
        if (cached != null && cached.expiresAtTick >= gameTime) {
            return cached.nearEndCity;
        }

        BlockPos nearest = level.findNearestMapStructure(END_CITY_BONUS_ZONE, pos, END_CITY_SEARCH_RADIUS_CHUNKS, false);
        boolean nearEndCity = nearest != null
                && horizontalDistSqr(nearest, pos) <= (long) END_CITY_BONUS_RADIUS_BLOCKS * END_CITY_BONUS_RADIUS_BLOCKS;
        cacheByChunk.put(chunkKey, new EndCityCacheEntry(nearEndCity, gameTime + END_CITY_CACHE_TTL_TICKS));
        if (cacheByChunk.size() > END_CITY_CACHE_MAX_ENTRIES) {
            trimCache(cacheByChunk, gameTime);
        }
        return nearEndCity;
    }

    private static void trimCache(Map<Long, EndCityCacheEntry> cacheByChunk, long gameTime) {
        Iterator<Map.Entry<Long, EndCityCacheEntry>> iterator = cacheByChunk.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, EndCityCacheEntry> entry = iterator.next();
            if (entry.getValue().expiresAtTick < gameTime) {
                iterator.remove();
            }
        }

        if (cacheByChunk.size() <= END_CITY_CACHE_MAX_ENTRIES) {
            return;
        }

        Iterator<Long> keyIterator = cacheByChunk.keySet().iterator();
        while (cacheByChunk.size() > END_CITY_CACHE_MAX_ENTRIES && keyIterator.hasNext()) {
            keyIterator.next();
            keyIterator.remove();
        }
    }

    private static long horizontalDistSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private record LayerCount(int low, int high) {}

    private record EndCityCacheEntry(boolean nearEndCity, long expiresAtTick) {}
}
