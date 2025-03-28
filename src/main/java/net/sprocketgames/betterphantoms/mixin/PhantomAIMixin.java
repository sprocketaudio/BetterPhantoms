package net.sprocketgames.betterphantoms.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Phantom.class)
public abstract class PhantomAIMixin extends Mob implements NeutralMob {

    protected PhantomAIMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    //Disable Phantom Attack AI
    @Redirect(method = "registerGoals",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V",
                    ordinal = 3))
    private void disableGoal(GoalSelector instance, int priority, Goal goal) {

    }



}
