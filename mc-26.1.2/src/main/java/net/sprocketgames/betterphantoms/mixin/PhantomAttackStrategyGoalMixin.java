package net.sprocketgames.betterphantoms.mixin;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomAttackStrategyGoal")
public abstract class PhantomAttackStrategyGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Phantom phantom;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void betterphantoms$allowEndPlayerTargetStrategy(CallbackInfoReturnable<Boolean> cir) {
        if (!phantom.level().dimension().equals(Level.END)) {
            return;
        }

        if (!(phantom.getTarget() instanceof Player player)) {
            return;
        }

        cir.setReturnValue(player.isAlive() && !player.isSpectator() && !player.isCreative());
    }
}
