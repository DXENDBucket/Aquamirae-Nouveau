package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import dev.aquamiraenouveau.platform.NativeAbilities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.Set;

/** An effect activation owns its native outcome; subsequent Ars effects retain their original target. */
public final class AquamiraeGlyph extends AbstractEffect implements TriggerPaidGlyph, AquamiraeSpell {
    public final Ability ability;

    public AquamiraeGlyph(Ability ability) {
        super(CarrierRegistry.id("aquamirae_nouveau:glyph_" + ability.id), ability.name);
        this.ability = ability;
        if (ability == Ability.ABYSSAL_SHARD) throw new IllegalArgumentException("Shard is a projectile replacement");
    }

    @Override public Ability ability() { return ability; }

    @Override public void onResolve(HitResult hit, Level level, LivingEntity ignoredCaster,
                                    SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!(level instanceof ServerLevel world) || !isEnabled() || context.isCanceled()) return;
        var frame = CastContext.of(context, hit, null);
        // A selected entity is the target; it never becomes the payer or damage owner.
        if (frame.caster() == null || !frame.caster().isAlive()) return;
        var charge = firstCharge(context, stats.getAugments());
        charge.trigger(() -> activate(world, frame, stats));
    }

    public boolean activate(ServerLevel world, CastContext frame, SpellStats stats) {
        if (!NativeAbilities.supports(ability)) return false;
        if (!(frame.target() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)
                || !target.isAlive() || target.level() != world) return false;
        int levels = 1 + Math.max(0, (int) Math.floor(stats.getAmpMultiplier()));
        if (ability.stacking) levels += NativeAbilities.currentLevel(target, ability);
        levels = Math.min(ability.maxLevel, levels);
        int duration = (int) Math.min(72000, AquamiraeConfig.duration(ability)
                + Math.max(0, stats.getDurationMultiplier()) * AquamiraeConfig.extendTicks());
        return NativeAbilities.apply(target, ability, Math.max(1, duration), levels - 1, frame.damageOwner());
    }

    @Override public ResourceCharge firstCharge(SpellContext context, List<AbstractAugment> augments) {
        return AquamiraeCosts.quote(context, ability, augments);
    }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE); }
    @Override public String getBookDescription() { return Component.translatable("aquamirae_nouveau.glyph_desc.glyph_" + ability.id).getString(); }
    public void addAugmentDescriptions(java.util.Map<AbstractAugment, String> descriptions) {
        descriptions.put(AugmentAmplify.INSTANCE, "Adds a level, up to this effect's native limit.");
        descriptions.put(AugmentExtendTime.INSTANCE, "Extends the native effect duration.");
    }
}
