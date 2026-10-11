package dev.aquamiraenouveau;

import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.arsconflux.api.targeting.Allies;
import dev.aquamiraenouveau.platform.AuraEffects;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import dev.obscuria.aquamirae.registry.AquamiraeParticleTypes;
import dev.obscuria.aquamirae.registry.AquamiraeSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Original player adapters for actions embedded in native creature ticks, not independent native spells. */
public final class CreatureAuras {
    public record Parameters(Ability ability, int ticks, double range, double healthLimit) {}
    private record Key(UUID host, Ability ability) {}
    private static final Map<Key, ExecutionSession<Parameters>> ACTIVE = new HashMap<>();
    private CreatureAuras() {}

    public static boolean eligible(ServerLevel world, LivingEntity caster, LivingEntity host) {
        return caster != null && caster.isAlive() && !caster.isRemoved() && !caster.isSpectator() && caster.level() == world
                && host.isAlive() && !host.isRemoved() && !host.isSpectator() && host.level() == world
                && world.hasChunkAt(host.blockPosition()) && Allies.areAllies(caster, host);
    }
    public static int remainingForCaster(LivingEntity caster, Ability ability) {
        return ACTIVE.values().stream().filter(s -> s.active() && s.caster() == caster && s.parameters().ability() == ability)
                .mapToInt(ExecutionSession::remainingTicks).max().orElse(0);
    }
    public static boolean start(ServerLevel world, CastContext frame, LivingEntity host, Parameters requested,
                                ResourceCharge charge, BooleanSupplier enabled) {
        if (!requested.ability().isAura()) return false;
        var params = new Parameters(requested.ability(), Math.min(requested.ticks(), AquamiraeConfig.maxAuraTicks(requested.ability())), requested.range(), requested.healthLimit());
        var key = new Key(host.getUUID(), params.ability());
        if (params.ticks() <= 0 || !eligible(world, frame.caster(), host) || ACTIVE.containsKey(key)) return false;
        var driver = new ExecutionDriver<Parameters>() {
            private long lastTick;
            public boolean start(ExecutionSession<Parameters> session) {
                // Keep the marker through the final server tick so entity tick order cannot swallow ejection.
                if (!AuraEffects.apply(host, params.ability(), params.ticks() + 1, frame.caster())) return false;
                ACTIVE.put(key, session); lastTick = world.getGameTime();
                AquamiraeCooldowns.granted(frame.caster(), params.ability());
                world.playSound(null, host, params.ability() == Ability.EEL_ROAR ? AquamiraeSounds.ENTITY_EEL_ROAR
                        : AquamiraeSounds.ENTITY_CAPTAIN_CORNELIA_RAGE, SoundSource.PLAYERS, 1, 1);
                pulse(world, frame, host, params, false);
                return true;
            }
            public boolean tick(ExecutionSession<Parameters> session) {
                if (!eligible(world, frame.caster(), host) || !AuraEffects.has(host, params.ability()) || !enabled.getAsBoolean()) return true;
                long now = world.getGameTime();
                if (now == lastTick) return false;
                boolean finalPulse = session.remainingTicks() <= 1;
                // Reserve each next second before its first action. Idle geometry still has visuals and a lifetime.
                if ((params.ticks() - session.remainingTicks()) % 20 == 0) {
                    if (!session.activate(() -> { pulse(world, frame, host, params, finalPulse); return true; })) return true;
                } else {
                    pulse(world, frame, host, params, finalPulse);
                }
                lastTick = now; return false;
            }
            public void close(ExecutionSession<Parameters> session, ExecutionSession.EndReason reason) {
                if (ACTIVE.remove(key, session)) {
                    AuraEffects.remove(host, params.ability());
                    AquamiraeCooldowns.ended(frame.caster(), params.ability());
                }
            }
        };
        var session = new ExecutionSession<>(world, frame.withExecutor(host), params, params.ticks(), driver, ignored -> {},
                () -> enabled.getAsBoolean() && eligible(world, frame.caster(), host), ExecutionScope.DIRECT);
        // The enclosing transaction prepays the first second; activate prepays subsequent seconds.
        var upkeep = new ResourceCharge(charge.account(), AquamiraeConfig.upkeep(params.ability()));
        session.billing(upkeep::trigger, true);
        return charge.trigger(() -> ExecutionSessions.start(session));
    }
    public static void clearOrphans(LivingEntity host) {
        for (var ability : new Ability[]{Ability.EEL_ROAR, Ability.CORNELIA_VORTEX}) {
            for (var source : java.util.List.copyOf(ACTIVE.values()))
                if (source.caster().getUUID().equals(host.getUUID()) && (source.caster() != host || source.originWorld() != host.level()))
                    source.finish(ExecutionSession.EndReason.OWNER_GONE);
            var session = ACTIVE.get(new Key(host.getUUID(), ability));
            if (session != null && session.active() && session.context().executor() == host && session.world() == host.level()) continue;
            if (session != null) session.finish(ExecutionSession.EndReason.OWNER_GONE);
            AuraEffects.remove(host, ability);
        }
    }
    static void pulse(ServerLevel world, CastContext frame, LivingEntity host, Parameters params, boolean last) {
        var targets = world.getEntitiesOfClass(LivingEntity.class, host.getBoundingBox().inflate(params.range()), target ->
                target != host && target != frame.damageOwner() && target.isAlive() && !target.isSpectator()
                        && !(target instanceof Player player && player.isCreative()) && !Allies.areAllies(frame.caster(), target)
                        && host.hasLineOfSight(target));
        for (var target : targets) {
            if (params.ability() == Ability.EEL_ROAR) {
                var offset = target.position().subtract(host.position());
                double distance = offset.lengthSqr();
                if (distance >= params.range() * params.range() || offset.normalize().dot(host.getLookAngle()) < .7071) continue;
                double pressure = .07 * Math.max(0, 1 - distance / (params.range() * params.range()));
                target.setDeltaMovement(target.getDeltaMovement().add(offset.x * pressure, 0, offset.z * pressure));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1), frame.damageOwner());
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 1), frame.damageOwner());
                target.setTicksFrozen(target.getTicksFrozen() + 1);
            } else {
                if (target.getMaxHealth() > params.healthLimit()) continue;
                Vec3 destination = last ? target.position().subtract(host.position()) : new Vec3(
                        host.getX() + Math.cos(-target.tickCount * .1) * 5, host.getY(), host.getZ() + Math.sin(-target.tickCount * .1) * 5)
                        .subtract(target.position());
                target.setDeltaMovement(destination.scale(.2));
            }
            target.hurtMarked = true;
        }
        if (host.tickCount % 4 == 0) {
            if (params.ability() == Ability.CORNELIA_VORTEX) {
                double angle = -host.tickCount * .1;
                world.sendParticles(AquamiraeParticleTypes.GHOST.get(), host.getX() + Math.cos(angle) * 5, host.getY() + .5,
                        host.getZ() + Math.sin(angle) * 5, 2, .1, .2, .1, .01);
            } else {
                var front = host.getEyePosition().add(host.getLookAngle().scale(2));
                world.sendParticles(ParticleTypes.CLOUD, front.x, front.y, front.z, 3, .25, .25, .25, .05);
            }
        }
    }
}
