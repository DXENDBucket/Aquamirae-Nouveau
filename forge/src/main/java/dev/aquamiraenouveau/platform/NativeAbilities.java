package dev.aquamiraenouveau.platform;

import dev.aquamiraenouveau.Ability;
import dev.arsconflux.api.projectile.CarrierRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Forge 7.1.14 has no native AbyssalShard. It is neither linked nor registered here. */
public final class NativeAbilities {
    public static boolean supports(Ability ability) { return ability != Ability.ABYSSAL_SHARD; }
    public static com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart glyph(Ability ability) {
        return switch (ability) {
            case TERRIBLE_CHAKRAM, MAZE_ROSE -> new dev.aquamiraenouveau.projectile.ChakramGlyph(ability);
            case SCYPHOID_STING -> new dev.aquamiraenouveau.ScyphoidStingGlyph();
            case ARMOR_BURST, HEALTH_BURST -> new dev.aquamiraenouveau.ShatterBurstGlyph(ability);
            default -> new dev.aquamiraenouveau.AquamiraeGlyph(ability);
        };
    }
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
    private NativeAbilities() {}
}
