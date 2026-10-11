package dev.aquamiraenouveau.mixin;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.aquamiraenouveau.projectile.AlliedCarriers;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityProjectileSpell.class, remap = false)
public abstract class AlliedCarrierMixin {
    @Inject(method = "canHitEntity", remap = true, at = @At("HEAD"), cancellable = true)
    private void aquamiraeNouveau$skipAlly(Entity candidate, CallbackInfoReturnable<Boolean> result) {
        if (AlliedCarriers.ignores((EntityProjectileSpell)(Object)this, candidate)) result.setReturnValue(false);
    }
}
