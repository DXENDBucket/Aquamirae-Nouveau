package dev.aquamiraenouveau.platform;

import dev.aquamiraenouveau.Ability;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.obscuria.aquamirae.common.entity.projectile.AbyssalShard;
import dev.obscuria.aquamirae.registry.AquamiraeEntityTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public final class NativeAbilities {
    public static boolean supports(Ability ability) { return true; }
    public static Holder<MobEffect> effect(Ability ability) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(CarrierRegistry.id("aquamirae:" + ability.id)).orElseThrow();
    }
    public static int currentLevel(LivingEntity target, Ability ability) {
        var current = target.getEffect(effect(ability));
        return current == null ? 0 : current.getAmplifier() + 1;
    }
    public static boolean apply(LivingEntity target, Ability ability, int ticks, int amplifier, LivingEntity owner) {
        return target.addEffect(new MobEffectInstance(effect(ability), ticks, amplifier), owner);
    }
    public static boolean launchShard(ServerLevel world, CastContext frame, double amplify) {
        var owner = frame.damageOwner();
        if (owner == null || !owner.isAlive() || owner.level() != world) return false;
        var direction = frame.incomingDirection();
        if (direction.lengthSqr() < 1.0e-8) direction = frame.executor().getLookAngle();
        direction = direction.normalize();
        var origin = frame.origin();
        // Ars Self resolves at the actor's feet; lift only that activation to eye height.
        if (frame.target() instanceof EntityHitResult hit && hit.getEntity() == frame.executor())
            origin = frame.executor().getEyePosition();
        // Start outside a struck surface; never hide a Boss.
        if (frame.target() instanceof BlockHitResult block)
            origin = origin.add(net.minecraft.world.phys.Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(0.3));
        else origin = origin.add(direction.scale(0.3));
        if (!world.hasChunkAt(net.minecraft.core.BlockPos.containing(origin))) return false;
        var shard = new AbyssalShard(AquamiraeEntityTypes.ABYSSAL_SHARD.get(), world);
        shard.setOwner(owner);
        shard.setPos(origin);
        shard.setDeltaMovement(direction.scale(0.8));
        shard.setShardScale((float) Math.min(2.0, 1.0 + Math.max(0, amplify) * 0.2));
        return world.addFreshEntity(shard);
    }
    private NativeAbilities() {}
}
