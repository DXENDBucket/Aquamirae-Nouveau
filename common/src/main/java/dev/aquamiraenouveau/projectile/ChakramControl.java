package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.targeting.Allies;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.HitResult;
import java.util.function.Consumer;

/** Shared runtime glue; movement, damage, poison and levitation stay in the native weapon classes. */
public final class ChakramControl {
    private final LivingEntity caster;
    private final EntityProjectileSpell carrier;
    private final Consumer<HitResult> impact;
    private boolean resolving, hurt;
    public ChakramControl(LivingEntity caster, EntityProjectileSpell carrier, Consumer<HitResult> impact) {
        this.caster = caster; this.carrier = carrier; this.impact = impact;
    }
    public EntityProjectileSpell carrier() { return carrier; }
    public boolean ignores(Entity candidate) { return Allies.areAllies(caster, candidate); }
    public void onHit(HitResult hit, Runnable nativeHit) { if (resolving) nativeHit.run(); else impact.accept(hit); }
    public void didHurt() { hurt = true; }
    public boolean nativeImpact(Runnable action) {
        resolving = true; hurt = false;
        try { action.run(); return hurt; } finally { resolving = false; }
    }
    public static void tick(Projectile projectile, boolean arsDriven, ChakramControl control, Runnable nativeTick) {
        if (!projectile.level().isClientSide && (control == null || !control.caster.isAlive() || control.caster.isRemoved())) {
            projectile.discard(); return;
        }
        if (!arsDriven) { nativeTick.run(); return; }
        projectile.baseTick();
        if (projectile.level().isClientSide) {
            projectile.setPos(projectile.position().add(projectile.getDeltaMovement()));
            ProjectileUtil.rotateTowardsMovement(projectile, .2f);
        }
    }
}
