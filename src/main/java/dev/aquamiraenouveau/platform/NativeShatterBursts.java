package dev.aquamiraenouveau.platform;

import dev.aquamiraenouveau.Ability;
import dev.obscuria.aquamirae.common.ability.weapon.ShatterbladeFullAbility;
import dev.obscuria.aquamirae.common.ability.weapon.ShatterspikeFullAbility;
import dev.obscuria.aquamirae.integration.DamageContext;
import net.minecraft.world.entity.LivingEntity;

/** Keep thresholds, configurable native variables and sound in the actual weapon abilities. */
public final class NativeShatterBursts {
    public static float damage(Ability ability, LivingEntity owner, LivingEntity victim) {
        var damage = new DamageContext(0);
        switch (ability) {
            case ARMOR_BURST -> ShatterspikeFullAbility.SHARED.modifyOutgoingDamage(owner, victim, damage);
            case HEALTH_BURST -> ShatterbladeFullAbility.SHARED.modifyOutgoingDamage(owner, victim, damage);
            default -> throw new IllegalArgumentException("Not a shatter burst: " + ability);
        }
        return damage.result();
    }
    private NativeShatterBursts() {}
}
