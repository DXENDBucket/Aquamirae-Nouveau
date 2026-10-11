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
        dev.aquamiraenouveau.projectile.ShardEntities.TYPES.register(bus);
        dev.aquamiraenouveau.projectile.ChakramEntities.TYPES.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, AquamiraeConfig.SPEC);
        GlyphCatalog.register();
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            var replacements = GlyphCatalog.ALL.stream().filter(dev.arsconflux.api.projectile.ProjectileForm.class::isInstance)
                    .map(com.hollingsworth.arsnouveau.api.spell.AbstractAugment.class::cast).toList();
            for (var profile : dev.arsconflux.api.projectile.CarrierRegistry.profiles()) {
                var form = profile.glyph();
                if (form == null) continue;
                profile.rememberOriginalAugments(form);
                form.compatibleAugments.addAll(replacements);
            }
        }));
        var tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        tabs.register("glyphs", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.aquamirae_nouveau"))
                .icon(() -> GlyphCatalog.ALL.get(0).getGlyph().getDefaultInstance())
                .displayItems((parameters, output) -> GlyphCatalog.ALL.forEach(glyph -> output.accept(glyph.getGlyph()))).build());
        tabs.register(bus);
    }
}
