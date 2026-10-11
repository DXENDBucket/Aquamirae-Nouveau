package dev.aquamiraenouveau.projectile;

import dev.arsconflux.api.projectile.CarrierProxy;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

public interface ChakramPayload extends CarrierProxy {
    ChakramControl control();
    void configure(ChakramControl control, boolean arsDriven);
    boolean nativeImpact(HitResult hit);
    default Projectile entity() { return (Projectile)this; }
    @Override default EntityProjectileSpell confluxTrajectory() { return control() == null ? null : control().carrier(); }
}
