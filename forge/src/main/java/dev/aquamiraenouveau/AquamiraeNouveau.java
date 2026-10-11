package dev.aquamiraenouveau;

import dev.aquamiraenouveau.platform.AquamiraeConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

@Mod(AquamiraeNouveau.MOD_ID)
public final class AquamiraeNouveau {
    public static final String MOD_ID = "aquamirae_nouveau";
    public AquamiraeNouveau() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, AquamiraeConfig.SPEC);
        GlyphCatalog.register();
        var tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        tabs.register("glyphs", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.aquamirae_nouveau"))
                .icon(() -> GlyphCatalog.ALL.get(0).getGlyph().getDefaultInstance())
                .displayItems((parameters, output) -> GlyphCatalog.ALL.forEach(glyph -> output.accept(glyph.getGlyph()))).build());
        tabs.register(bus);
    }
}
