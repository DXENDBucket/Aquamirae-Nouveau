package dev.aquamiraenouveau.projectile;

import dev.obscuria.aquamirae.common.entity.projectile.MazeRose;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Inherit the full native weapon. Common control only supplies Ars context and trajectory selection. */
public final class SpellMazeRose extends MazeRose implements ChakramPayload {
    private static final EntityDataAccessor<Boolean> ARS_DRIVEN = SynchedEntityData.defineId(SpellMazeRose.class, EntityDataSerializers.BOOLEAN);
    private ChakramControl control;
    public SpellMazeRose(EntityType<? extends SpellMazeRose> type, Level world) { super(type, world); }
    @Override public ChakramControl control() { return control; }
    @Override public void configure(ChakramControl control, boolean arsDriven) {
        this.control = control; entityData.set(ARS_DRIVEN, arsDriven);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(ARS_DRIVEN, false);
    }
    @Override public void tick() { ChakramControl.tick(this, entityData.get(ARS_DRIVEN), control, super::tick); }
    @Override protected boolean canHitEntity(Entity candidate) {
        return (control == null || !control.ignores(candidate)) && super.canHitEntity(candidate);
    }
    @Override protected void onHit(HitResult hit) {
        if (!level().isClientSide && control != null) control.onHit(hit, () -> super.onHit(hit));
    }
    @Override protected void onHurtEntity(EntityHitResult hit) {
        super.onHurtEntity(hit); if (control != null) control.didHurt();
    }
    @Override public boolean nativeImpact(HitResult hit) { return control.nativeImpact(() -> super.onHit(hit)); }
}
