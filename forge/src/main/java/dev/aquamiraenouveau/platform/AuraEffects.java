package dev.aquamiraenouveau.platform;

import dev.aquamiraenouveau.Ability;
import dev.aquamiraenouveau.AquamiraeNouveau;
import dev.aquamiraenouveau.CreatureAuraEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AuraEffects {
    public static final DeferredRegister<MobEffect> TYPES = DeferredRegister.create(Registries.MOB_EFFECT, AquamiraeNouveau.MOD_ID);
    private static final RegistryObject<MobEffect> ROAR = TYPES.register("eel_roar", () -> new CreatureAuraEffect(0x597D8C));
    private static final RegistryObject<MobEffect> VORTEX = TYPES.register("cornelia_vortex", () -> new CreatureAuraEffect(0x87DFCA));
    private static MobEffect effect(Ability ability) { return (ability == Ability.EEL_ROAR ? ROAR : VORTEX).get(); }
    public static boolean apply(LivingEntity host, Ability ability, int ticks, LivingEntity source) {
        return host.addEffect(new MobEffectInstance(effect(ability), ticks), source);
    }
    public static boolean has(LivingEntity host, Ability ability) { return host.hasEffect(effect(ability)); }
    public static int remainingTicks(LivingEntity host, Ability ability) {
        var current = host.getEffect(effect(ability)); return current == null ? 0 : current.getDuration();
    }
    public static void remove(LivingEntity host, Ability ability) { host.removeEffect(effect(ability)); }
    private AuraEffects() {}
}
