package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.CarrierProxy;
import dev.arsconflux.api.targeting.Allies;
import dev.obscuria.aquamirae.common.entity.projectile.AbyssalShard;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.Consumer;

/** Native shard payload with an opt-in Ars controller; ordinary Aquamirae shards are untouched. */
public final class SpellAbyssalShard extends AbyssalShard implements CarrierProxy {
    private static final EntityDataAccessor<Boolean> ARS_DRIVEN = SynchedEntityData.defineId(SpellAbyssalShard.class, EntityDataSerializers.BOOLEAN);
    private EntityProjectileSpell carrier;
    private Consumer<HitResult> impact;
    private boolean repeated;
    private boolean resolving;
    private boolean selectingHomingTarget;
    private Entity allianceCaster;
    private boolean nativeFragment;

    public SpellAbyssalShard(EntityType<? extends SpellAbyssalShard> type, Level world) { super(type, world); }
    public void setAllianceCaster(Entity caster) { allianceCaster = caster; }
    public boolean ignoresAlly(Entity entity) { return Allies.areAllies(allianceCaster, entity); }
    /** Called only by the opt-in native split hook; native onBreak still chooses count, motion and scale. */
    public SpellAbyssalShard createFragment(Level world, Entity owner, Vec3 position, Vec3 velocity) {
        var fragment = new SpellAbyssalShard(ShardEntities.SHARD.get(), world);
        fragment.nativeFragment = true;
        fragment.allianceCaster = allianceCaster;
        fragment.setOwner(owner);
        fragment.setPos(position);
        fragment.setDeltaMovement(velocity);
        world.addFreshEntity(fragment);
        return fragment;
    }
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
    @Override protected boolean canTargetEntity(Entity entity) {
        // Slimes and magma cubes implement Enemy but do not extend the native Monster check.
        return acceptsContact(entity) && (super.canTargetEntity(entity) || entity instanceof Enemy && hasLineOfSight(entity));
    }
    private boolean acceptsContact(Entity entity) {
        var owner = getOwner();
        return entity != owner && !ignoresAlly(entity) && entity.canBeHitByProjectile()
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
                && (owner == null || !owner.isPassengerOfSameVehicle(entity));
    }
    @Override protected boolean canHitEntity(Entity entity) {
        // Native findTarget also uses this predicate. Keep homing selection separate from contact.
        return selectingHomingTarget ? super.canHitEntity(entity) : acceptsContact(entity);
    }
    @Override protected LivingEntity findTarget() {
        selectingHomingTarget = true;
        try { return super.findTarget(); } finally { selectingHomingTarget = false; }
    }
    @Override public void tick() {
        if (!level().isClientSide && (allianceCaster == null || !allianceCaster.isAlive()
                || !nativeFragment && impact == null)) { discard(); return; } // No context survives a world reload.
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
        if (hit instanceof EntityHitResult entity && ignoresAlly(entity.getEntity())) return;
        if (nativeFragment) { super.onHit(hit); return; }
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
