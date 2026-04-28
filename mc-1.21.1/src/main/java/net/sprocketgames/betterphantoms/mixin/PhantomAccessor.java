package net.sprocketgames.betterphantoms.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Phantom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Phantom.class)
public interface PhantomAccessor {
    @Accessor("anchorPoint")
    void betterphantoms$setAnchorPoint(BlockPos pos);
}
