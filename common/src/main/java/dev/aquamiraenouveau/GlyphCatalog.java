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
        dev.arsconflux.api.casting.ActiveCasts.register(
                dev.arsconflux.api.projectile.CarrierRegistry.id("aquamirae_nouveau:cooldowns"), new AquamiraeCooldowns());
    }
    private GlyphCatalog() {}
}
