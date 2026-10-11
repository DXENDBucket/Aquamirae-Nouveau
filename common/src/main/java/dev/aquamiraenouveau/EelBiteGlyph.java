package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.setup.registry.DamageTypesRegistry;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.arsconflux.api.targeting.Allies;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import dev.obscuria.aquamirae.AquamiraeHelper;
import dev.obscuria.aquamirae.registry.AquamiraeSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.Set;

/** Entity-hit adaptation of the eel's bite and actual-health-loss siphon. */
public final class EelBiteGlyph extends AbstractEffect implements TriggerPaidGlyph, AquamiraeSpell, IDamageEffect {
    public EelBiteGlyph() { super(CarrierRegistry.id("aquamirae_nouveau:glyph_eel_bite"), "Siphoning Bite"); }
    @Override public Ability ability() { return Ability.EEL_BITE; }
    @Override public void onResolve(HitResult hit, Level level, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(level instanceof ServerLevel world) || !isEnabled() || context.isCanceled()) return;
        var frame = CastContext.of(context, hit, null);
        var owner = frame.damageOwner();
        if (frame.caster() == null || !frame.caster().isAlive() || owner == null || !owner.isAlive() || owner.level() != world
                || !(hit instanceof EntityHitResult entity) || !(entity.getEntity() instanceof LivingEntity victim)
                || victim == owner || !victim.isAlive() || victim.level() != world || Allies.areAllies(frame.caster(), victim)
                || !canDamage(frame.caster(), stats, context, resolver, victim)) return;
        double damage = AquamiraeConfig.biteDamage() + Math.max(0, stats.getAmpMultiplier()) * 2;
        double duration = AquamiraeConfig.duration(ability()) + Math.max(0, stats.getDurationMultiplier()) * AquamiraeConfig.extendTicks();
        if (!Double.isFinite(damage) || !Double.isFinite(duration)) return;
        firstCharge(context, stats.getAugments()).trigger(() -> {
            float before = victim.getHealth();
            if (!attemptDamage(world, frame.caster(), stats, context, resolver, victim,
                    DamageUtil.source(world, DamageTypesRegistry.GENERIC_SPELL_DAMAGE, owner), (float)Math.min(1000000, damage))) return false;
            float lost = Math.max(0, before - victim.getHealth());
            if (lost > 0 && owner.isAlive()) owner.heal(lost * 2);
            if (victim.isAlive()) AquamiraeHelper.doPoison(owner, victim, (int)Math.min(72000, duration), 2);
            world.playSound(null, victim, AquamiraeSounds.ENTITY_GENERIC_DEEP_BITE, SoundSource.PLAYERS, 1, 1);
            return true;
        });
    }
    @Override public ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) { return AquamiraeCosts.quote(context, ability(), augments); }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE); }
    @Override public String getBookDescription() { return Component.translatable("aquamirae_nouveau.glyph_desc.glyph_eel_bite").getString(); }
    // Ars 5 calls this hook; Ars 4 has no equivalent method.
    public void addAugmentDescriptions(java.util.Map<AbstractAugment, String> descriptions) {
        for (var augment : getCompatibleAugments()) descriptions.put(augment, Component.translatable(
                "ars_nouveau.augment_desc.glyph_eel_bite_" + augment.getRegistryName().getPath()).getString());
    }
}
