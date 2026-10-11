package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.arsconflux.api.glyph.GlyphPresentation;
import dev.aquamiraenouveau.platform.NativeAbilities;
import java.util.Arrays;
import java.util.List;

public final class GlyphCatalog {
    public static final List<AbstractSpellPart> ALL = Arrays.stream(Ability.values())
            .filter(NativeAbilities::supports).map(NativeAbilities::glyph).toList();

    public static void register() {
        GlyphPresentation.register("aquamirae_nouveau", part ->
                String.format(java.util.Locale.ROOT, "%02d", ((AquamiraeSpell) part).ability().ordinal()));
        ALL.forEach(GlyphRegistry::registerSpell);
        dev.arsconflux.api.glyph.SpellValidators.register(
                dev.arsconflux.api.projectile.CarrierRegistry.id("aquamirae_nouveau:payload_rules"), AquamiraePayloadRules::validate);
        dev.arsconflux.api.casting.ActiveCasts.register(
                dev.arsconflux.api.projectile.CarrierRegistry.id("aquamirae_nouveau:cooldowns"), new AquamiraeCooldowns());
    }
    public static void configureCarriers() {
        var replacements = ALL.stream().filter(dev.arsconflux.api.projectile.ProjectileForm.class::isInstance)
                .map(com.hollingsworth.arsnouveau.api.spell.AbstractAugment.class::cast).toList();
        for (var profile : dev.arsconflux.api.projectile.CarrierRegistry.profiles()) {
            var form = profile.glyph();
            if (form == null) continue;
            profile.rememberOriginalAugments(form);
            form.compatibleAugments.addAll(replacements);
            if (NativeAbilities.supports(Ability.ABYSSAL_SHARD)) form.compatibleAugments.add(com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE);
            for (var replacement : replacements) NativeAbilities.describeReplacement(form, replacement);
        }
    }
    private GlyphCatalog() {}
}
