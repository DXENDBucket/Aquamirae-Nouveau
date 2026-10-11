package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAccelerate;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.aquamiraenouveau.*;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Set;

/** Native returning weapons are replacement augments, not an extra spell effect projectile. */
public final class ChakramGlyph extends AbstractAugment implements ProjectileForm, AquamiraeSpell {
    private final Ability ability;
    public ChakramGlyph(Ability ability) {
        super(CarrierRegistry.id("aquamirae_nouveau:glyph_" + ability.id), ability.name);
        if (ability != Ability.TERRIBLE_CHAKRAM && ability != Ability.MAZE_ROSE) throw new IllegalArgumentException("Not a chakram");
        this.ability = ability;
    }
    @Override public Ability ability() { return ability; }
    @Override public boolean replace(EntityProjectileSpell carrier, CarrierProfile profile) { return ChakramCasting.replace(this, carrier, profile); }
    @Override public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) { return AquamiraeCosts.quote(context, ability, List.of()); }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override public Set<AbstractAugment> getCompatibleAugments() { return Set.of(AugmentAccelerate.INSTANCE); }
    @Override public String getBookDescription() { return Component.translatable("aquamirae_nouveau.glyph_desc.glyph_" + ability.id).getString(); }
}
