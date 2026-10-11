package dev.aquamiraenouveau.projectile;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.ResourceCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ChakramCasting {
    public static boolean replace(ChakramGlyph glyph, EntityProjectileSpell carrier, CarrierProfile profile) {
        if (!(carrier.level() instanceof ServerLevel world)) return false;
        var resolver = ArsSpellAccess.resolver(carrier);
        if (resolver == null) return false;
        var frame = CastContext.of(resolver.spellContext, null, null);
        if (frame.caster() == null || !frame.caster().isAlive() || frame.damageOwner() == null) return true;
        var payload = ChakramEntities.create(glyph.ability(), world);
        payload.entity().setOwner(frame.damageOwner());
        var quote = glyph.initialCharge(resolver.spellContext, resolver.getCastStats().getAugments());
        var continuation = resolver.clone();
        var flight = new Flight(payload, carrier, profile, frame, quote, resolver.getCastStats().getAccMultiplier());
        int lifetime = profile.arsDriven() ? Math.max(1, Math.min(1200, carrier.getExpirationTime() + 1)) : 120;
        var session = new ExecutionSession<>(world, frame.withAccount(quote.account()), payload, lifetime, flight,
                hit -> {
                    var child = continuation.clone();
                    CastContexts.bind(child.spellContext, CastContexts.current().withAccount(frame.account()));
                    child.onResolveEffect(payload.entity().level(), hit);
                }, glyph::isEnabled, ExecutionScope.DIRECT).billing(quote::trigger, profile.repeated());
        ExecutionSessions.start(session);
        return true;
    }
    private static final class Flight implements ExecutionDriver<ChakramPayload> {
        private final ChakramPayload payload;
        private final EntityProjectileSpell carrier;
        private final ArsTrajectory trajectory;
        private final CastContext frame;
        private final ResourceCharge quote;
        private final boolean repeated;
        private final double acceleration;
        private Vec3 direction;
        Flight(ChakramPayload payload, EntityProjectileSpell carrier, CarrierProfile profile, CastContext frame, ResourceCharge quote, double acceleration) {
            this.payload = payload; this.carrier = carrier; this.frame = frame; this.quote = quote;
            this.repeated = profile.repeated(); this.acceleration = acceleration;
            trajectory = profile.arsDriven() ? new ArsTrajectory(payload.entity(), carrier) : null;
            direction = TriggerGeometry.direction(carrier);
        }
        @Override public Vec3 incomingDirection() { return direction; }
        @Override public boolean start(ExecutionSession<ChakramPayload> session) {
            payload.configure(new ChakramControl(frame.caster(), carrier, hit -> impact(session, hit)), trajectory != null);
            if (trajectory != null) {
                AlliedCarriers.attach(carrier, frame.caster());
                return trajectory.start(session.world(), () -> {}, hit -> impact(session, hit));
            }
            payload.entity().setPos(carrier.position());
            payload.entity().setDeltaMovement(direction.scale(.8 * Math.max(.1, 1 + acceleration / .75)));
            return session.world().hasChunkAt(payload.entity().blockPosition()) && session.world().addFreshEntity(payload.entity());
        }
        private void impact(ExecutionSession<ChakramPayload> session, HitResult hit) {
            if (hit.getType() == HitResult.Type.MISS) return;
            if (hit instanceof EntityHitResult entity && payload.control().ignores(entity.getEntity())) return;
            direction = trajectory == null ? payload.entity().getDeltaMovement().normalize() : trajectory.incomingDirection();
            boolean success = session.activate(() -> {
                if (!payload.nativeImpact(hit)) return false;
                session.impact(hit); return true;
            });
            // Rejected hurt (e.g. invulnerability frames) is not a paid activation or a reason to discard a returning weapon.
            if (!success && (!session.permitted() || repeated && quote.affordableCount(1) == 0))
                session.finish(ExecutionSession.EndReason.DENIED);
        }
        @Override public boolean tick(ExecutionSession<ChakramPayload> session) {
            if (trajectory != null) return trajectory.tick(session.world());
            return payload.entity().isRemoved() || !session.world().hasChunkAt(payload.entity().blockPosition())
                    || !session.world().hasChunkAt(BlockPos.containing(payload.entity().position().add(payload.entity().getDeltaMovement())));
        }
        @Override public void close(ExecutionSession<ChakramPayload> session, ExecutionSession.EndReason reason) {
            AlliedCarriers.detach(carrier);
            if (trajectory != null) trajectory.close();
            else { payload.entity().discard(); carrier.discard(); }
        }
    }
    private ChakramCasting() {}
}
