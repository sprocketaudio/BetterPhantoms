package net.sprocketgames.betterphantoms;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

@Mod(BetterPhantomsMod.MOD_ID)
public class BetterPhantomsMod {
    public static final String MOD_ID = "betterphantoms";

    public BetterPhantomsMod(IEventBus modEventBus, ModContainer modContainer) {
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

        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
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

        MobSpawnType spawnType = event.getSpawnType();
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            event.setSpawnCancelled(true);
        }
    }
}
