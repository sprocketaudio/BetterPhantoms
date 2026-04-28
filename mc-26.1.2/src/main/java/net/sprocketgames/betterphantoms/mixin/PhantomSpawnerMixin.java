package net.sprocketgames.betterphantoms.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.sprocketgames.betterphantoms.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void betterphantoms$disableInsomniaPhantoms(ServerLevel level, boolean spawnEnemies, CallbackInfo ci) {
        if (Config.disableInsomniaPhantomSpawning) {
            ci.cancel();
        }
    }
}
