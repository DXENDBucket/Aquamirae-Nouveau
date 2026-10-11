package dev.aquamiraenouveau.projectile;

import dev.aquamiraenouveau.AquamiraeNouveau;
import dev.obscuria.aquamirae.client.renderer.entity.TerribleChakramRenderer;
import dev.obscuria.aquamirae.client.renderer.entity.MazeRoseRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AquamiraeNouveau.MOD_ID, value = Dist.CLIENT)
public final class ChakramClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChakramEntities.TERRIBLE.get(), TerribleChakramRenderer<SpellTerribleChakram>::new);
        event.registerEntityRenderer(ChakramEntities.ROSE.get(), MazeRoseRenderer<SpellMazeRose>::new);
    }
    private ChakramClient() {}
}
