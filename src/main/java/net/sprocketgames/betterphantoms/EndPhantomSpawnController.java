package net.sprocketgames.betterphantoms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

public final class EndPhantomSpawnController {
    private static final int SPAWN_WEIGHT_SCALE = 200;
    private static final int END_CITY_RADIUS_CHUNKS = 12;
    private static final int END_CITY_RADIUS_BLOCKS = END_CITY_RADIUS_CHUNKS * 16;
    private static final int GENERAL_LOCAL_CAP = 4;
    private static final int END_CITY_LOCAL_CAP = 8;
    private static final int END_CITY_ESCORT_MIN = 2;
    private static final int END_CITY_ESCORT_MAX = 4;
    private static final double GENERAL_PAIR_CHANCE = 0.35D;
    private static final int ESCORT_HORIZONTAL_OFFSET = 9;
    private static final int ESCORT_VERTICAL_MIN = 3;
    private static final int ESCORT_VERTICAL_RANGE = 6;
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

        if (!Config.enableNaturalEndSpawningAfterDragonDeath || !isDragonDefeated(level)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }

        int effectiveWeight = getEffectiveSpawnWeight(level, event.getPos());
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

        if (!Config.enableNaturalEndSpawningAfterDragonDeath || !isDragonDefeated(level)) {
            return;
        }

        boolean nearEndCity = isNearEndCity(level, leader.blockPosition());
        int wingmates = getWingmateCount(level, nearEndCity);
        if (wingmates <= 0) {
            return;
        }

        int localCap = nearEndCity ? END_CITY_LOCAL_CAP : GENERAL_LOCAL_CAP;
        int nearbyPhantoms = level.getEntitiesOfClass(Phantom.class, leader.getBoundingBox().inflate(48.0D)).size();
        int availableSlots = Math.max(0, localCap - nearbyPhantoms);
        int spawnCount = Math.min(wingmates, availableSlots);
        if (spawnCount <= 0) {
            return;
        }

        for (int i = 0; i < spawnCount; i++) {
            spawnWingmate(level, leader, event.getDifficulty());
        }
    }

    private static boolean isDragonDefeated(ServerLevel level) {
        EndDragonFight dragonFight = level.getDragonFight();
        return dragonFight != null && dragonFight.hasPreviouslyKilledDragon();
    }

    private static int getWingmateCount(ServerLevel level, boolean nearEndCity) {
        if (nearEndCity) {
            return Mth.nextInt(level.random, END_CITY_ESCORT_MIN, END_CITY_ESCORT_MAX);
        }

        return level.random.nextDouble() < GENERAL_PAIR_CHANCE ? 1 : 0;
    }

    private static void spawnWingmate(ServerLevel level, Phantom leader, DifficultyInstance difficulty) {
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
        wingmate.finalizeSpawn(level, difficulty, MobSpawnType.EVENT, null);
        if (!level.noCollision(wingmate)) {
            return;
        }

        level.addFreshEntity(wingmate);
    }

    private static int getEffectiveSpawnWeight(ServerLevel level, BlockPos pos) {
        int baseWeight = Mth.clamp(Config.generalEndSpawnWeight, 0, SPAWN_WEIGHT_SCALE);
        if (baseWeight == 0) {
            return 0;
        }

        if (!isNearEndCity(level, pos)) {
            return baseWeight;
        }

        int boosted = (int) Math.round(baseWeight * Math.max(0.0D, Config.endCitySpawnDensityMultiplier));
        return Mth.clamp(boosted, 0, SPAWN_WEIGHT_SCALE);
    }

    static boolean isNearEndCity(ServerLevel level, BlockPos pos) {
        BlockPos nearest = level.findNearestMapStructure(END_CITY_BONUS_ZONE, pos, END_CITY_RADIUS_CHUNKS, true);
        return nearest != null && nearest.distSqr(pos) <= (long) END_CITY_RADIUS_BLOCKS * END_CITY_RADIUS_BLOCKS;
    }
}
