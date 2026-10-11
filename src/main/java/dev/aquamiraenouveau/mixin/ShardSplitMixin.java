package dev.aquamiraenouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aquamiraenouveau.projectile.SpellAbyssalShard;
import dev.obscuria.aquamirae.common.entity.projectile.AbyssalShard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbyssalShard.class, remap = false)
public abstract class ShardSplitMixin {
    @WrapOperation(method = "onBreak", at = @At(value = "INVOKE", target =
            "Ldev/obscuria/aquamirae/common/entity/projectile/AbyssalShard;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ldev/obscuria/aquamirae/common/entity/projectile/AbyssalShard;"))
    private AbyssalShard aquamiraeNouveau$fragment(Level world, Entity owner, Vec3 position, Vec3 velocity, Operation<AbyssalShard> original) {
        if ((Object)this instanceof SpellAbyssalShard shard) return shard.createFragment(world, owner, position, velocity);
        return original.call(world, owner, position, velocity);
    }
}
