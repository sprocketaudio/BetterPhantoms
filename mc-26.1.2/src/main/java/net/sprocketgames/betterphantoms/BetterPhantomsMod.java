package net.sprocketgames.betterphantoms;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@Mod(BetterPhantomsMod.MOD_ID)
public final class BetterPhantomsMod {
    public static final String MOD_ID = "betterphantoms";

    public BetterPhantomsMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(Config::onLoad);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(BetterPhantomsMod::onSpawnPlacementCheck);
        NeoForge.EVENT_BUS.addListener(BetterPhantomsMod::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(ElytraRepairController::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(EndPhantomSpawnController::onSpawnPlacementCheck);
        NeoForge.EVENT_BUS.addListener(EndPhantomSpawnController::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(EndPhantomBehaviorController::onEntityTickPost);
        NeoForge.EVENT_BUS.addListener(EndPhantomBehaviorController::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(DragonFightPhantomManager::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(DragonFightPhantomManager::onServerStopped);
        NeoForge.EVENT_BUS.addListener(EndPhantomSpawnController::onServerStopped);
        NeoForge.EVENT_BUS.addListener(EndPhantomBehaviorController::onServerStopped);
    }

    private static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!Config.disableOverworldPhantomSpawning) {
            return;
        }

        if (event.getEntityType() != EntityType.PHANTOM) {
            return;
        }

        if (!event.getLevel().getLevel().dimension().equals(Level.OVERWORLD)) {
            return;
        }

        EntitySpawnReason spawnType = event.getSpawnType();
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    private static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!Config.disableOverworldPhantomSpawning) {
            return;
        }

        if (event.getEntity().getType() != EntityType.PHANTOM) {
            return;
        }

        if (!event.getLevel().getLevel().dimension().equals(Level.OVERWORLD)) {
            return;
        }

        EntitySpawnReason spawnType = event.getSpawnType();
        if (spawnType == EntitySpawnReason.NATURAL || spawnType == EntitySpawnReason.CHUNK_GENERATION) {
            event.setSpawnCancelled(true);
        }
    }
}
