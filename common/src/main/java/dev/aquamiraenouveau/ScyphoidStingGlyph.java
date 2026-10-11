package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.arsconflux.api.targeting.Allies;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import dev.obscuria.aquamirae.AquamiraeHelper;
import dev.obscuria.aquamirae.common.entity.AbyssalScyphoid;
import dev.obscuria.aquamirae.common.entity.Anglerfish;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.Set;

/** Adapt the water creature's contact venom through Aquamirae's native poison helper. */
public final class ScyphoidStingGlyph extends AbstractEffect implements TriggerPaidGlyph, AquamiraeSpell {
    public ScyphoidStingGlyph() { super(CarrierRegistry.id("aquamirae_nouveau:glyph_scyphoid_sting"), "Scyphoid Sting"); }
    @Override public Ability ability() { return Ability.SCYPHOID_STING; }
    @Override public void onResolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (world.isClientSide || !isEnabled() || context.isCanceled()) return;
        var frame = CastContext.of(context, hit, null);
        if (frame.caster() == null || !frame.caster().isAlive() || frame.damageOwner() == null
                || !(hit instanceof EntityHitResult entity) || !(entity.getEntity() instanceof LivingEntity victim)
                || !victim.isAlive() || victim.level() != world || victim instanceof AbyssalScyphoid || victim instanceof Anglerfish
                || Allies.areAllies(frame.caster(), victim)) return;
        int ticks = (int)Math.min(72000, AquamiraeConfig.duration(ability()) + Math.max(0, stats.getDurationMultiplier()) * AquamiraeConfig.extendTicks());
        int amplifier = Math.min(ability().maxLevel - 1, 2 + Math.max(0, (int)Math.floor(stats.getAmpMultiplier())));
        firstCharge(context, stats.getAugments()).trigger(() -> {
            if (!AquamiraeHelper.doPoison(frame.damageOwner(), victim, ticks, amplifier)) return false;
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0), frame.damageOwner());
            world.playSound(null, victim, SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, .5f, 1);
            return true;
        });
    }
    @Override public ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) { return AquamiraeCosts.quote(context, ability(), augments); }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE); }
    @Override public String getBookDescription() { return Component.translatable("aquamirae_nouveau.glyph_desc.glyph_scyphoid_sting").getString(); }
    // Ars 5 calls this hook; Ars 4 has no equivalent method.
    public void addAugmentDescriptions(java.util.Map<AbstractAugment, String> descriptions) {
        for (var augment : getCompatibleAugments()) descriptions.put(augment, Component.translatable(
                "ars_nouveau.augment_desc.glyph_scyphoid_sting_" + augment.getRegistryName().getPath()).getString());
    }
}
