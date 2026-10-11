package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.Set;

/** Grant a creature action to an allied host without changing caster or payment ownership. */
public final class CreatureAuraGlyph extends AbstractEffect implements TriggerPaidGlyph, AquamiraeSpell {
    private final Ability ability;
    public CreatureAuraGlyph(Ability ability) {
        super(CarrierRegistry.id("aquamirae_nouveau:glyph_" + ability.id), ability.name);
        if (ability != Ability.EEL_ROAR && ability != Ability.CORNELIA_VORTEX) throw new IllegalArgumentException("Not an aura");
        this.ability = ability;
    }
    @Override public Ability ability() { return ability; }
    @Override public void onResolve(HitResult hit, Level level, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(level instanceof ServerLevel world) || !isEnabled() || context.isCanceled()
                || !(hit instanceof EntityHitResult entity) || !(entity.getEntity() instanceof LivingEntity host)) return;
        var frame = CastContext.of(context, hit, null);
        if (!CreatureAuras.eligible(world, frame.caster(), host)) return;
        double duration = AquamiraeConfig.duration(ability) + Math.max(0, stats.getDurationMultiplier()) * AquamiraeConfig.extendTicks();
        if (!Double.isFinite(duration) || duration <= 0) return;
        int ticks = (int)Math.min(AquamiraeConfig.maxAuraTicks(ability), duration);
        double range = Math.min(32, (ability == Ability.EEL_ROAR ? Math.sqrt(150) : 10) + 2 * bounded(stats.getAoeMultiplier(), 16));
        double healthLimit = 100 + 50 * bounded(stats.getAmpMultiplier(), 18);
        CreatureAuras.start(world, frame, host, new CreatureAuras.Parameters(ability, ticks, range, healthLimit),
                firstCharge(context, stats.getAugments()), this::isEnabled);
    }
    private static double bounded(double value, int max) { return Double.isFinite(value) ? Math.min(max, Math.max(0, Math.floor(value))) : 0; }
    @Override public ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) { return AquamiraeCosts.quote(context, ability, augments); }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override protected Set<AbstractAugment> getCompatibleAugments() {
        // Ars calls this from its constructor, before subclass fields are assigned.
        return getRegistryName().getPath().equals("glyph_cornelia_vortex") ? Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentAOE.INSTANCE)
                : Set.of(AugmentExtendTime.INSTANCE, AugmentAOE.INSTANCE);
    }
    @Override public String getBookDescription() { return Component.translatable("aquamirae_nouveau.glyph_desc.glyph_" + ability.id).getString(); }
    // Ars 5 calls this hook; Ars 4 has no equivalent method.
    public void addAugmentDescriptions(java.util.Map<AbstractAugment, String> descriptions) {
        for (var augment : getCompatibleAugments()) descriptions.put(augment, Component.translatable(
                "ars_nouveau.augment_desc." + getRegistryName().getPath() + "_" + augment.getRegistryName().getPath()).getString());
    }
}
