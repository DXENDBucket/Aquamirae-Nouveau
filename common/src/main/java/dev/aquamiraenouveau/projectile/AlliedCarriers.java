package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.targeting.Allies;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Opt in only this addon's off-world Ars carriers. Registration is scoped to their execution session. */
public final class AlliedCarriers {
    private static final Map<EntityProjectileSpell, WeakReference<LivingEntity>> CASTERS = new WeakHashMap<>();
    public static void attach(EntityProjectileSpell carrier, LivingEntity caster) { CASTERS.put(carrier, new WeakReference<>(caster)); }
    public static void detach(EntityProjectileSpell carrier) { CASTERS.remove(carrier); }
    public static boolean ignores(EntityProjectileSpell carrier, Entity candidate) {
        if (carrier.level().isClientSide) return false;
        var reference = CASTERS.get(carrier);
        return reference != null && Allies.areAllies(reference.get(), candidate);
    }
    private AlliedCarriers() {}
}
