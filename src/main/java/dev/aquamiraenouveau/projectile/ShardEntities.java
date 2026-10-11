package dev.aquamiraenouveau.projectile;

import dev.aquamiraenouveau.AquamiraeNouveau;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ShardEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AquamiraeNouveau.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<SpellAbyssalShard>> SHARD = TYPES.register("spell_abyssal_shard",
            () -> EntityType.Builder.<SpellAbyssalShard>of(SpellAbyssalShard::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1)
                    .build("aquamirae_nouveau:spell_abyssal_shard"));
    private ShardEntities() {}
}
