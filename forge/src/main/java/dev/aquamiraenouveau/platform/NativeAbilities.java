package dev.aquamiraenouveau.platform;

import dev.aquamiraenouveau.Ability;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Forge 7.1.14 has no native AbyssalShard. It is neither linked nor registered here. */
public final class NativeAbilities {
    public static boolean supports(Ability ability) { return ability != Ability.ABYSSAL_SHARD; }
    public static MobEffect effect(Ability ability) {
        var id = CarrierRegistry.id("aquamirae:" + ability.id);
        if (!BuiltInRegistries.MOB_EFFECT.containsKey(id)) throw new IllegalStateException("Missing native effect: " + id);
        return BuiltInRegistries.MOB_EFFECT.get(id);
    }
    public static int currentLevel(LivingEntity target, Ability ability) {
        var current = target.getEffect(effect(ability));
        return current == null ? 0 : current.getAmplifier() + 1;
    }
    public static boolean apply(LivingEntity target, Ability ability, int ticks, int amplifier, LivingEntity owner) {
        return target.addEffect(new MobEffectInstance(effect(ability), ticks, amplifier), owner);
    }
    public static boolean launchShard(ServerLevel world, CastContext frame, double amplify) { return false; }
    private NativeAbilities() {}
}
