package dev.aquamiraenouveau.projectile;

import dev.aquamiraenouveau.AquamiraeNouveau;
import dev.obscuria.aquamirae.client.renderer.entity.TerribleChakramRenderer;
import dev.obscuria.aquamirae.client.renderer.entity.MazeRoseRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AquamiraeNouveau.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ChakramClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChakramEntities.TERRIBLE.get(), TerribleChakramRenderer<SpellTerribleChakram>::new);
        event.registerEntityRenderer(ChakramEntities.ROSE.get(), MazeRoseRenderer<SpellMazeRose>::new);
    }
    private ChakramClient() {}
}
