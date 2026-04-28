package net.sprocketgames.betterphantoms;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue DISABLE_OVERWORLD_PHANTOM_SPAWNING = BUILDER
            .comment("Disable natural phantom spawning in the Overworld.")
            .define("disableOverworldPhantomSpawning", true);

    private static final ModConfigSpec.BooleanValue DISABLE_INSOMNIA_PHANTOM_SPAWNING = BUILDER
            .comment("Disable vanilla insomnia-based phantom spawning.")
            .define("disableInsomniaPhantomSpawning", true);

    private static final ModConfigSpec.BooleanValue ENABLE_DRAGON_FIGHT_PHANTOM_WAVES = BUILDER
            .comment("Enable crystal-triggered phantom waves during the dragon fight.")
            .define("enableDragonFightPhantomWaves", true);

    private static final ModConfigSpec.IntValue PHANTOMS_PER_CRYSTAL_WAVE_MIN = BUILDER
            .comment("Minimum phantoms spawned per crystal wave.")
            .defineInRange("phantomsPerCrystalWaveMin", 2, 0, 32);

    private static final ModConfigSpec.IntValue PHANTOMS_PER_CRYSTAL_WAVE_MAX = BUILDER
            .comment("Maximum phantoms spawned per crystal wave.")
            .defineInRange("phantomsPerCrystalWaveMax", 3, 0, 32);

    private static final ModConfigSpec.IntValue ACTIVE_FIGHT_PHANTOM_CAP = BUILDER
            .comment("Maximum number of active fight phantoms.")
            .defineInRange("activeFightPhantomCap", 10, 1, 64);

    private static final ModConfigSpec.IntValue FRENZY_DURATION_SECONDS = BUILDER
            .comment("Frenzy duration in seconds after the final crystal is destroyed.")
            .defineInRange("frenzyDurationSeconds", 12, 1, 120);

    private static final ModConfigSpec.BooleanValue ENABLE_NATURAL_END_SPAWNING_AFTER_DRAGON_DEATH = BUILDER
            .comment("Enable natural End phantom spawning after dragon defeat.")
            .define("enableNaturalEndSpawningAfterDragonDeath", true);

    private static final ModConfigSpec.IntValue GENERAL_END_SPAWN_WEIGHT = BUILDER
            .comment("Baseline natural spawn weight for phantoms in the general End.")
            .defineInRange("generalEndSpawnWeight", 12, 0, 200);

    private static final ModConfigSpec.DoubleValue END_CITY_SPAWN_DENSITY_MULTIPLIER = BUILDER
            .comment("Spawn density multiplier for phantoms near End Cities.")
            .defineInRange("endCitySpawnDensityMultiplier", 1.6D, 0.0D, 10.0D);

    private static final ModConfigSpec.IntValue HIGH_PATROL_ALTITUDE = BUILDER
            .comment("Preferred high patrol altitude for End phantoms.")
            .defineInRange("highPatrolAltitude", 96, 16, 320);

    private static final ModConfigSpec.DoubleValue END_CITY_DIVE_SPEED_BONUS = BUILDER
            .comment("Additional dive speed bonus for phantoms near End Cities.")
            .defineInRange("endCityDiveSpeedBonus", 0.12D, 0.0D, 2.0D);

    private static final ModConfigSpec.DoubleValue PHANTOM_HEALTH_MULTIPLIER = BUILDER
            .comment("Health multiplier for End phantoms.")
            .defineInRange("phantomHealthMultiplier", 0.60D, 0.5D, 1.0D);

    private static final ModConfigSpec.DoubleValue DRAGON_FIGHT_PHANTOM_HEALTH_MULTIPLIER = BUILDER
            .comment("Health multiplier for dragon fight crystal-wave phantoms.")
            .defineInRange("dragonFightPhantomHealthMultiplier", 0.40D, 0.25D, 1.0D);

    private static final ModConfigSpec.DoubleValue ELYTRA_REPAIR_EFFECTIVENESS = BUILDER
            .comment("Repair effectiveness multiplier for Elytra repaired with Phantom Membrane.")
            .defineInRange("elytraRepairEffectiveness", 0.6D, 0.1D, 1.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean disableOverworldPhantomSpawning;
    public static boolean disableInsomniaPhantomSpawning;
    public static boolean enableDragonFightPhantomWaves;
    public static int phantomsPerCrystalWaveMin;
    public static int phantomsPerCrystalWaveMax;
    public static int activeFightPhantomCap;
    public static int frenzyDurationSeconds;
    public static boolean enableNaturalEndSpawningAfterDragonDeath;
    public static int generalEndSpawnWeight;
    public static double endCitySpawnDensityMultiplier;
    public static int highPatrolAltitude;
    public static double endCityDiveSpeedBonus;
    public static double phantomHealthMultiplier;
    public static double dragonFightPhantomHealthMultiplier;
    public static double elytraRepairEffectiveness;

    private Config() {
    }

    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        disableOverworldPhantomSpawning = DISABLE_OVERWORLD_PHANTOM_SPAWNING.get();
        disableInsomniaPhantomSpawning = DISABLE_INSOMNIA_PHANTOM_SPAWNING.get();
        enableDragonFightPhantomWaves = ENABLE_DRAGON_FIGHT_PHANTOM_WAVES.get();
        phantomsPerCrystalWaveMin = PHANTOMS_PER_CRYSTAL_WAVE_MIN.get();
        phantomsPerCrystalWaveMax = Math.max(phantomsPerCrystalWaveMin, PHANTOMS_PER_CRYSTAL_WAVE_MAX.get());
        activeFightPhantomCap = ACTIVE_FIGHT_PHANTOM_CAP.get();
        frenzyDurationSeconds = FRENZY_DURATION_SECONDS.get();
        enableNaturalEndSpawningAfterDragonDeath = ENABLE_NATURAL_END_SPAWNING_AFTER_DRAGON_DEATH.get();
        generalEndSpawnWeight = GENERAL_END_SPAWN_WEIGHT.get();
        endCitySpawnDensityMultiplier = END_CITY_SPAWN_DENSITY_MULTIPLIER.get();
        highPatrolAltitude = HIGH_PATROL_ALTITUDE.get();
        endCityDiveSpeedBonus = END_CITY_DIVE_SPEED_BONUS.get();
        phantomHealthMultiplier = PHANTOM_HEALTH_MULTIPLIER.get();
        dragonFightPhantomHealthMultiplier = DRAGON_FIGHT_PHANTOM_HEALTH_MULTIPLIER.get();
        elytraRepairEffectiveness = ELYTRA_REPAIR_EFFECTIVENESS.get();
    }
}
