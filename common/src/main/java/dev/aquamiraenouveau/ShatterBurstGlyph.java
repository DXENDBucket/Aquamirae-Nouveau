package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import com.hollingsworth.arsnouveau.setup.registry.DamageTypesRegistry;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.arsconflux.api.targeting.Allies;
import dev.aquamiraenouveau.platform.NativeShatterBursts;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.Set;

/** Expose only the native full-stack bonus as an Ars damage effect. */
public final class ShatterBurstGlyph extends AbstractEffect implements TriggerPaidGlyph, AquamiraeSpell, IDamageEffect {
    private final Ability ability;
    public ShatterBurstGlyph(Ability ability) {
        super(CarrierRegistry.id("aquamirae_nouveau:glyph_" + ability.id), ability.name);
        if (ability != Ability.ARMOR_BURST && ability != Ability.HEALTH_BURST) throw new IllegalArgumentException("Not a shatter burst");
        this.ability = ability;
    }
    @Override public Ability ability() { return ability; }
    @Override public void onResolve(HitResult hit, Level level, LivingEntity ignoredCaster, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(level instanceof ServerLevel world) || !isEnabled() || context.isCanceled()) return;
        var frame = CastContext.of(context, hit, null);
        if (frame.caster() == null || !frame.caster().isAlive() || frame.damageOwner() == null
                || !(hit instanceof EntityHitResult entity) || !(entity.getEntity() instanceof LivingEntity victim)
                || !victim.isAlive() || victim.level() != world || Allies.areAllies(frame.caster(), victim)
                || !canDamage(frame.caster(), stats, context, resolver, victim)) return;
        firstCharge(context, stats.getAugments()).trigger(() -> {
            float damage = NativeShatterBursts.damage(ability, frame.damageOwner(), victim);
            return Float.isFinite(damage) && damage > 0 && attemptDamage(world, frame.caster(), stats, context, resolver, victim,
                    DamageUtil.source(world, DamageTypesRegistry.GENERIC_SPELL_DAMAGE, frame.damageOwner()), damage);
        });
    }
    @Override public ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) { return AquamiraeCosts.quote(context, ability, List.of()); }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
    @Override public String getBookDescription() { return Component.translatable("aquamirae_nouveau.glyph_desc.glyph_" + ability.id).getString(); }
}
