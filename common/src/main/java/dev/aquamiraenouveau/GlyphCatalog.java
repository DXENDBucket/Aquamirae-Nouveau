package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import dev.arsconflux.api.glyph.GlyphPresentation;
import dev.aquamiraenouveau.platform.NativeAbilities;
import java.util.Arrays;
import java.util.List;

public final class GlyphCatalog {
    public static final List<AquamiraeGlyph> ALL = Arrays.stream(Ability.values())
            .filter(NativeAbilities::supports).map(AquamiraeGlyph::new).toList();

    public static void register() {
        GlyphPresentation.register("aquamirae_nouveau", part ->
                String.format(java.util.Locale.ROOT, "%02d", ((AquamiraeGlyph) part).ability.ordinal()));
        ALL.forEach(GlyphRegistry::registerSpell);
        dev.arsconflux.api.casting.ActiveCasts.register(
                dev.arsconflux.api.projectile.CarrierRegistry.id("aquamirae_nouveau:cooldowns"), new AquamiraeCooldowns());
    }
    private GlyphCatalog() {}
}
