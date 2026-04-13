package net.sprocketgames.betterphantoms.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.sprocketgames.betterphantoms.DragonFightPhantomManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndDragonFight.class)
public class EndDragonFightMixin {
    @Inject(method = "onCrystalDestroyed", at = @At("TAIL"))
    private void betterphantoms$onCrystalDestroyed(EndCrystal crystal, DamageSource source, CallbackInfo ci) {
        DragonFightPhantomManager.onCrystalDestroyed((EndDragonFight) (Object) this, crystal);
    }
}
