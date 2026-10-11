package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ShardCasting {
    public static boolean replace(AbyssalShardGlyph glyph, EntityProjectileSpell carrier, CarrierProfile profile) {
        if (!(carrier.level() instanceof ServerLevel world)) return false;
        var resolver = ArsSpellAccess.resolver(carrier);
        if (resolver == null) return false;
        var frame = CastContext.of(resolver.spellContext, null, null);
        if (frame.caster() == null || !frame.caster().isAlive() || frame.damageOwner() == null) return true;
        var stats = resolver.getCastStats();
        var quote = glyph.initialCharge(resolver.spellContext, stats.getAugments());
        var shard = new SpellAbyssalShard(ShardEntities.SHARD.get(), world);
        shard.setOwner(frame.damageOwner());
        shard.setShardScale((float) Math.min(2.0, 1.0 + Math.max(0, stats.getAmpMultiplier()) * 0.2));
        var flight = new Flight(shard, carrier, profile, stats.getAccMultiplier());
        var continuation = resolver.clone();
        int lifetime = profile.arsDriven() ? Math.max(1, Math.min(1200, carrier.getExpirationTime() + 1)) : 202;
        var session = new ExecutionSession<>(world, frame.withAccount(quote.account()), shard, lifetime, flight,
                hit -> {
                    var child = continuation.clone();
                    CastContexts.bind(child.spellContext, CastContexts.current().withAccount(frame.account()));
                    child.onResolveEffect(shard.level(), hit);
                }, glyph::isEnabled, ExecutionScope.DIRECT).billing(quote::trigger, profile.repeated());
        ExecutionSessions.start(session);
        return true;
    }

    private static final class Flight implements ExecutionDriver<SpellAbyssalShard> {
        private final SpellAbyssalShard shard;
        private final EntityProjectileSpell carrier;
        private final ArsTrajectory trajectory;
        private final boolean repeated;
        private final double acceleration;
        private Vec3 direction;
        Flight(SpellAbyssalShard shard, EntityProjectileSpell carrier, CarrierProfile profile, double acceleration) {
            this.shard = shard; this.carrier = carrier; this.repeated = profile.repeated(); this.acceleration = acceleration;
            trajectory = profile.arsDriven() ? new ArsTrajectory(shard, carrier) : null;
            direction = TriggerGeometry.direction(carrier);
        }
        @Override public Vec3 incomingDirection() { return direction; }
        @Override public boolean start(ExecutionSession<SpellAbyssalShard> session) {
            shard.configure(carrier, trajectory != null, repeated, hit -> impact(session, hit));
            if (trajectory != null) return trajectory.start(session.world(), () -> {}, hit -> impact(session, hit));
            shard.setPos(carrier.position());
            shard.setDeltaMovement(direction.scale(0.8 * Math.max(0.1, 1 + acceleration / 0.75)));
            return session.world().hasChunkAt(shard.blockPosition()) && session.world().addFreshEntity(shard);
        }
        private void impact(ExecutionSession<SpellAbyssalShard> session, HitResult hit) {
            if (hit.getType() == HitResult.Type.MISS) return;
            direction = trajectory == null ? shard.getDeltaMovement().normalize() : trajectory.incomingDirection();
            if (!session.activate(() -> {
                shard.setPos(hit.getLocation());
                shard.nativeImpact(hit);
                session.impact(hit);
                return true;
            })) session.finish(ExecutionSession.EndReason.DENIED);
        }
        @Override public boolean tick(ExecutionSession<SpellAbyssalShard> session) {
            if (trajectory != null) return trajectory.tick(session.world());
            return shard.isRemoved() || !session.world().hasChunkAt(shard.blockPosition());
        }
        @Override public void close(ExecutionSession<SpellAbyssalShard> session, ExecutionSession.EndReason reason) {
            shard.detach();
            if (trajectory != null) trajectory.close();
            else { shard.discard(); carrier.discard(); }
        }
    }
    private ShardCasting() {}
}
