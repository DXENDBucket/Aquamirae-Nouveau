package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.CarrierProxy;
import dev.obscuria.aquamirae.common.entity.projectile.AbyssalShard;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import java.util.function.Consumer;

/** Native shard payload with an opt-in Ars controller; ordinary Aquamirae shards are untouched. */
public final class SpellAbyssalShard extends AbyssalShard implements CarrierProxy {
    private static final EntityDataAccessor<Boolean> ARS_DRIVEN = SynchedEntityData.defineId(SpellAbyssalShard.class, EntityDataSerializers.BOOLEAN);
    private EntityProjectileSpell carrier;
    private Consumer<HitResult> impact;
    private boolean repeated;
    private boolean resolving;

    public SpellAbyssalShard(EntityType<? extends SpellAbyssalShard> type, Level world) { super(type, world); }
    public void configure(EntityProjectileSpell carrier, boolean arsDriven, boolean repeated, Consumer<HitResult> impact) {
        this.carrier = carrier;
        this.repeated = repeated;
        this.impact = impact;
        entityData.set(ARS_DRIVEN, arsDriven);
    }
    @Override public EntityProjectileSpell confluxTrajectory() { return carrier; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARS_DRIVEN, false);
    }
    @Override public void tick() {
        if (!level().isClientSide && impact == null) { discard(); return; } // No resolver survives a world reload.
        if (!entityData.get(ARS_DRIVEN)) { super.tick(); return; }
        // Conflux ticks the off-world Ars carrier. Never run native movement or collision as well.
        baseTick();
        if (level().isClientSide) {
            setPos(position().add(getDeltaMovement()));
            ProjectileUtil.rotateTowardsMovement(this, 0.2f);
            localXRotO = localXRot; localYRotO = localYRot;
            localXRot = getXRot(); localYRot = getYRot();
        }
    }
    @Override protected void onHit(HitResult hit) {
        if (level().isClientSide || resolving || impact == null) return;
        impact.accept(hit);
    }
    public void nativeImpact(HitResult hit) {
        resolving = true;
        try { super.onHit(hit); } finally { resolving = false; }
    }
    @Override public void onBreak() {
        // Persistent Ars carriers keep their payload; a single-hit carrier retains native splitting.
        if (!(resolving && repeated)) super.onBreak();
    }
    @Override public void remove(RemovalReason reason) {
        if (!(resolving && repeated && reason == RemovalReason.DISCARDED)) super.remove(reason);
    }
    public void detach() { carrier = null; impact = null; }
}
