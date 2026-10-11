package dev.aquamiraenouveau.platform;

import dev.aquamiraenouveau.Ability;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class NativeAbilities {
    public static boolean supports(Ability ability) { return true; }
    public static com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart glyph(Ability ability) {
        return switch (ability) {
            case ABYSSAL_SHARD -> new dev.aquamiraenouveau.projectile.AbyssalShardGlyph();
            case TERRIBLE_CHAKRAM, MAZE_ROSE -> new dev.aquamiraenouveau.projectile.ChakramGlyph(ability);
            case SCYPHOID_STING -> new dev.aquamiraenouveau.ScyphoidStingGlyph();
            default -> new dev.aquamiraenouveau.AquamiraeGlyph(ability);
        };
    }
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
    private NativeAbilities() {}
}
