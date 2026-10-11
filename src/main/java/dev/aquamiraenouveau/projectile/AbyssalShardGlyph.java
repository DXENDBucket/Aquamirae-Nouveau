package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.aquamiraenouveau.*;
import net.minecraft.network.chat.Component;
import java.util.List;

/** A replacement augment directly after an Ars projectile form. */
public final class AbyssalShardGlyph extends AbstractAugment implements ProjectileForm, AquamiraeSpell {
    public AbyssalShardGlyph() {
        super(CarrierRegistry.id("aquamirae_nouveau:glyph_abyssal_shard"), "Abyssal Shard");
    }
    @Override public Ability ability() { return Ability.ABYSSAL_SHARD; }
    @Override public boolean replace(EntityProjectileSpell carrier, CarrierProfile profile) {
        return ShardCasting.replace(this, carrier, profile);
    }
    @Override public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) {
        return AquamiraeCosts.quote(context, ability(), augments);
    }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override public String getBookDescription() {
        return Component.translatable("aquamirae_nouveau.glyph_desc.glyph_abyssal_shard").getString();
    }
}
