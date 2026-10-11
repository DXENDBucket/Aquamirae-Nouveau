package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import net.minecraft.world.entity.Entity;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Only off-world Ars carriers driving this addon's shards opt in to alliance filtering. */
public final class ShardCarriers {
    private static final Map<EntityProjectileSpell, WeakReference<SpellAbyssalShard>> SHARDS = new WeakHashMap<>();
    static void attach(EntityProjectileSpell carrier, SpellAbyssalShard shard) { SHARDS.put(carrier, new WeakReference<>(shard)); }
    static void detach(EntityProjectileSpell carrier) { SHARDS.remove(carrier); }
    public static boolean ignoresAlly(EntityProjectileSpell carrier, Entity candidate) {
        var reference = SHARDS.get(carrier);
        var shard = reference == null ? null : reference.get();
        return shard != null && shard.ignoresAlly(candidate);
    }
    private ShardCarriers() {}
}
