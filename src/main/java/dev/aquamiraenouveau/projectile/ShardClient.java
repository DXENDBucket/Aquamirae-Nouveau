package dev.aquamiraenouveau.projectile;

import dev.aquamiraenouveau.AquamiraeNouveau;
import dev.obscuria.aquamirae.client.renderer.entity.projectile.AbyssalShardRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AquamiraeNouveau.MOD_ID, value = Dist.CLIENT)
public final class ShardClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ShardEntities.SHARD.get(), AbyssalShardRenderer<SpellAbyssalShard>::new);
    }
    private ShardClient() {}
}
