package dev.aquamiraenouveau;

import dev.aquamiraenouveau.platform.AquamiraeConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(AquamiraeNouveau.MOD_ID)
public final class AquamiraeNouveau {
    public static final String MOD_ID = "aquamirae_nouveau";
    public AquamiraeNouveau(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, AquamiraeConfig.SPEC);
        GlyphCatalog.register();
        var tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        tabs.register("glyphs", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.aquamirae_nouveau"))
                .icon(() -> GlyphCatalog.ALL.get(0).getGlyph().getDefaultInstance())
                .displayItems((parameters, output) -> GlyphCatalog.ALL.forEach(glyph -> output.accept(glyph.getGlyph()))).build());
        tabs.register(bus);
    }
}
