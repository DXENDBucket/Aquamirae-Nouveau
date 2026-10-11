package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.casting.ActiveCastSource;
import dev.arsconflux.api.execution.ExecutionSessions;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.targeting.Allies;
import dev.aquamiraenouveau.platform.AuraEffects;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("aquamirae_nouveau") @PrefixGameTestTemplate(false)
public final class CreatureGameTests {
    private static final java.util.concurrent.atomic.AtomicInteger FIXTURES = new java.util.concurrent.atomic.AtomicInteger();
    private static ServerPlayer player(GameTestHelper h) {
        var id = UUID.randomUUID();
        var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(id, "Aq" + id.toString().substring(0, 8)));
        var pos = h.absoluteVec(new Vec3(2, 0, 2)); p.setPos(pos.x, 512 + 48 * FIXTURES.getAndIncrement(), pos.z); p.setYRot(-90); p.setXRot(0);
        p.setGameMode(GameType.SURVIVAL);
        p.getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MAX_MANA.get())).setBaseValue(1000);
        CapabilityRegistry.getMana(p).setMaxMana(1000); CapabilityRegistry.getMana(p).setMana(1000); return p;
    }
    private static LivingEntity mob(GameTestHelper h, ServerPlayer p, Vec3 offset, int health) {
        var cow = EntityType.COW.create(h.getLevel()); cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(p.position().add(offset));
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health); cow.setHealth(health); h.getLevel().addFreshEntity(cow); return cow;
    }
    private static AbstractSpellPart glyph(Ability ability) { return GlyphCatalog.ALL.stream().filter(g -> ((AquamiraeSpell)g).ability() == ability).findFirst().orElseThrow(); }
    private static SpellResolver resolver(GameTestHelper h, ServerPlayer p, Spell spell) {
        return new SpellResolver(new SpellContext(h.getLevel(), spell, p, LivingCaster.from(p), ItemStack.EMPTY));
    }
    private static void apply(GameTestHelper h, ServerPlayer p, LivingEntity target, Ability ability, AbstractAugment... augments) {
        var parts = new ArrayList<AbstractSpellPart>(); parts.add(MethodTouch.INSTANCE); parts.add(glyph(ability)); parts.addAll(List.of(augments));
        var r = resolver(h, p, new Spell(parts)); r.spellContext.nextPart(); r.onResolveEffect(h.getLevel(), new EntityHitResult(target));
    }
    private static void cleanup(LivingEntity... entities) { for (var e : entities) e.discard(); ExecutionSessions.tick(); ActiveCasts.tick(); AquamiraePreparation.tick(); }

    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void biteUsesActualLossAndOriginalOwner(GameTestHelper h) {
        var p = player(h); var owner = player(h); var victim = mob(h, p, new Vec3(3, 0, 0), 20);
        owner.setHealth(2); victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.ABSORPTION, 200, 1)); victim.setAbsorptionAmount(5);
        try {
            var r = resolver(h, p, new Spell(List.of(MethodTouch.INSTANCE, glyph(Ability.EEL_BITE))));
            CastContexts.bind(r.spellContext, CastContext.of(r.spellContext, null, null).withDamageOwner(owner));
            r.spellContext.nextPart(); r.onResolveEffect(h.getLevel(), new EntityHitResult(victim));
            h.assertTrue(victim.getHealth() == 17 && owner.getHealth() == 8 && p.getHealth() == 20, "Only actual health loss heals the actual owner, excluding absorption: victim=" + victim.getHealth() + ", owner=" + owner.getHealth() + ", caster=" + p.getHealth());
            h.assertTrue(victim.hasEffect(MobEffects.POISON) && victim.getLastHurtByMob() == owner, "Native poison and damage retain owner");
            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 900 && CapabilityRegistry.getMana(owner).getCurrentMana() == 1000, "Original caster pays the bite"); h.succeed();
        } finally { cleanup(p, owner, victim); }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void biteRejectsCancellationAlliesAndOverkill(GameTestHelper h) throws Exception {
        var p = player(h); var victim = mob(h, p, new Vec3(3, 0, 0), 20); p.setHealth(2);
        Consumer<SpellDamageEvent.Pre> cancel = e -> { if (e.target == victim) e.setCanceled(true); };
        try {
            NeoForge.EVENT_BUS.addListener(cancel); apply(h, p, victim, Ability.EEL_BITE); NeoForge.EVENT_BUS.unregister(cancel);
            h.assertTrue(victim.getHealth() == 20 && p.getHealth() == 2 && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Cancellation has no poison, healing or charge");
            try (var ignored = Allies.register(CarrierRegistry.id("aquamirae_nouveau:test_bite_ally"), (a,b) -> a == p && b == victim)) {
                apply(h, p, victim, Ability.EEL_BITE);
                h.assertTrue(victim.getHealth() == 20 && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Allies are rejected without payment");
            }
            victim.setInvulnerable(true); apply(h, p, victim, Ability.EEL_BITE);
            h.assertTrue(p.getHealth() == 2 && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Rejected damage cannot heal or charge");
            victim.setInvulnerable(false); victim.setHealth(2); apply(h, p, victim, Ability.EEL_BITE);
            h.assertTrue(victim.isDeadOrDying() && p.getHealth() == 6 && CapabilityRegistry.getMana(p).getCurrentMana() == 900, "Overkill heals only twice the two remaining health"); h.succeed();
        } finally { NeoForge.EVENT_BUS.unregister(cancel); cleanup(p, victim); }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void biteAugmentsDoNotBecomeAChannel(GameTestHelper h) {
        var p = player(h); var victim = mob(h, p, new Vec3(3, 0, 0), 40); p.setHealth(2);
        try {
            apply(h, p, victim, Ability.EEL_BITE, AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE);
            h.assertTrue(victim.getHealth() == 30 && p.getHealth() == 20, "Amplify adds bite damage, healing is capped at maximum health");
            h.assertTrue(victim.getEffect(MobEffects.POISON).getDuration() == 300 && CapabilityRegistry.getMana(p).getCurrentMana() == 890, "Extend affects poison, one charge only"); h.succeed();
        } finally { cleanup(p, victim); }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=60)
    public static void roarTracksAlliedHostAndStopsWhenRemoved(GameTestHelper h) throws Exception {
        var p = player(h); var host = mob(h, p, new Vec3(4, 0, 0), 40); host.setYRot(-90); host.setXRot(0);
        var enemy = mob(h, p, new Vec3(7, 0, 0), 40); var friend = mob(h, p, new Vec3(7, 0, 1), 40); var behind = mob(h, p, new Vec3(1, 0, 0), 40);
        var relation = Allies.register(CarrierRegistry.id("aquamirae_nouveau:test_roar_host"), (a,b) -> a == p && (b == host || b == friend));
        try {
            apply(h, p, host, Ability.EEL_ROAR);
            h.assertTrue(AuraEffects.has(host, Ability.EEL_ROAR) && !AuraEffects.has(p, Ability.EEL_ROAR), "Selected ally hosts the buff");
            h.assertTrue(enemy.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && enemy.hasEffect(MobEffects.DARKNESS) && enemy.getDeltaMovement().x > 0 && enemy.getTicksFrozen() > 0, "Facing cone repels and applies native statuses");
            h.assertTrue(!friend.hasEffect(MobEffects.DARKNESS) && !behind.hasEffect(MobEffects.DARKNESS), "Source allies and targets behind the host are excluded");
            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 920, "Caster pays initial quote");
            h.runAfterDelay(22, () -> {
                try {
                    h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 908, "Upkeep belongs to original caster");
                    AuraEffects.remove(host, Ability.EEL_ROAR); ExecutionSessions.tick();
                    enemy.removeAllEffects(); enemy.setDeltaMovement(Vec3.ZERO);
                    h.runAfterDelay(3, () -> {
                        try { h.assertTrue(!enemy.hasEffect(MobEffects.DARKNESS), "Dispelled benefit stops emissions"); h.succeed(); }
                        finally { cleanup(p, host, enemy, friend, behind); try { relation.close(); } catch (Exception e) { throw new RuntimeException(e); } }
                    });
                } catch (Throwable e) { cleanup(p, host, enemy, friend, behind); try { relation.close(); } catch (Exception suppressed) { e.addSuppressed(suppressed); } throw e; }
            });
        } catch (Throwable e) { relation.close(); cleanup(p, host, enemy, friend, behind); throw e; }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=45)
    public static void auraPrepaidSecondExpiresOnEmptyMana(GameTestHelper h) {
        var p = player(h); CapabilityRegistry.getMana(p).setMana(80);
        var enemy = mob(h, p, new Vec3(3, 0, 0), 40);
        apply(h, p, p, Ability.EEL_ROAR);
        h.assertTrue(AuraEffects.has(p, Ability.EEL_ROAR) && enemy.hasEffect(MobEffects.DARKNESS), "The paid first second emits even when it exhausts mana");
        h.runAfterDelay(22, () -> {
            try { h.assertTrue(!AuraEffects.has(p, Ability.EEL_ROAR) && CapabilityRegistry.getMana(p).getCurrentMana() == 0, "Failed upkeep removes the marker and session without negative mana"); h.succeed(); }
            finally { cleanup(p, enemy); }
        });
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void vortexHealthRangeAndDurationAugments(GameTestHelper h) throws Exception {
        var p = player(h); var small = mob(h, p, new Vec3(3, 0, 0), 100); var large = mob(h, p, new Vec3(4, 0, 0), 150);
        var outside = mob(h, p, new Vec3(11, 0, 0), 100); var friend = mob(h, p, new Vec3(3, 0, 1), 100);
        try (var ignored = Allies.register(CarrierRegistry.id("aquamirae_nouveau:test_vortex_ally"), (a,b) -> a == p && b == friend)) {
            apply(h, p, p, Ability.CORNELIA_VORTEX);
            h.assertTrue(small.getDeltaMovement().lengthSqr() > 0 && large.getDeltaMovement().lengthSqr() == 0 && outside.getDeltaMovement().lengthSqr() == 0 && friend.getDeltaMovement().lengthSqr() == 0, "Default health cap, range and friend exclusions");
            AuraEffects.remove(p, Ability.CORNELIA_VORTEX); ExecutionSessions.tick();
            apply(h, p, p, Ability.CORNELIA_VORTEX, AugmentAmplify.INSTANCE, AugmentAOE.INSTANCE, AugmentExtendTime.INSTANCE);
            h.assertTrue(large.getDeltaMovement().lengthSqr() > 0 && outside.getDeltaMovement().lengthSqr() > 0, "Amplify raises cap to 150; AOE reaches outside the original range");
            h.assertTrue(AuraEffects.remainingTicks(p, Ability.CORNELIA_VORTEX) == 401 && CapabilityRegistry.getMana(p).getCurrentMana() == 750, "Extend doubles the default duration; two initial casts and one Amplify fee"); h.succeed();
        } finally { cleanup(p, small, large, outside, friend); }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=45)
    public static void auraExpiryEjectsAndCleansHostDeath(GameTestHelper h) {
        var p = player(h); var target = mob(h, p, new Vec3(3, 0, 0), 40);
        var r = resolver(h, p, new Spell(List.of(MethodSelf.INSTANCE, glyph(Ability.CORNELIA_VORTEX))));
        var frame = CastContext.of(r.spellContext, new EntityHitResult(p), null);
        var quote = ((CreatureAuraGlyph)glyph(Ability.CORNELIA_VORTEX)).firstCharge(r.spellContext, List.of());
        CreatureAuras.start(h.getLevel(), frame, p, new CreatureAuras.Parameters(Ability.CORNELIA_VORTEX, 3, 10, 100), quote, () -> true);
        h.runAfterDelay(4, () -> {
            try {
                h.assertTrue(!AuraEffects.has(p, Ability.CORNELIA_VORTEX), "Natural expiry removes aura marker");
                h.assertTrue(target.getDeltaMovement().subtract(target.position().subtract(p.position()).scale(.2)).lengthSqr() < .05, "Final tick applies radial ejection");
                apply(h, p, p, Ability.EEL_ROAR); p.setHealth(0); ExecutionSessions.tick();
                h.assertTrue(!AuraEffects.has(p, Ability.EEL_ROAR), "Caster death cleans granted actions"); h.succeed();
            } finally { cleanup(p, target); }
        });
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void orphanAndDuplicateBenefitsCannotStack(GameTestHelper h) {
        var p = player(h); var hostileHost = mob(h, p, new Vec3(3, 0, 0), 40);
        try {
            apply(h, p, hostileHost, Ability.EEL_ROAR);
            h.assertTrue(!AuraEffects.has(hostileHost, Ability.EEL_ROAR) && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "An enemy cannot become a benefit host");
            var r = resolver(h, p, new Spell(List.of(MethodSelf.INSTANCE, glyph(Ability.EEL_ROAR)))); r.onCast(ItemStack.EMPTY, h.getLevel());
            h.assertTrue(AuraEffects.has(p, Ability.EEL_ROAR), "Real Ars Self grants the registered effect");
            double paid = CapabilityRegistry.getMana(p).getCurrentMana(); apply(h, p, p, Ability.EEL_ROAR);
            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == paid, "Duplicate grants do not stack or charge");
            AuraEffects.apply(hostileHost, Ability.CORNELIA_VORTEX, 200, p); CreatureAuras.clearOrphans(hostileHost);
            h.assertTrue(!AuraEffects.has(hostileHost, Ability.CORNELIA_VORTEX), "Reloaded marker without transient session is removed");
            var reloaded = EntityType.COW.create(h.getLevel()); reloaded.setUUID(p.getUUID());
            AuraEffects.apply(reloaded, Ability.EEL_ROAR, 200, p); CreatureAuras.clearOrphans(reloaded);
            h.assertTrue(!AuraEffects.has(reloaded, Ability.EEL_ROAR) && !AuraEffects.has(p, Ability.EEL_ROAR), "A new entity with the same UUID cannot inherit the old execution session");
            reloaded.discard(); h.succeed();
        } finally { cleanup(p, hostileHost); }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=60)
    public static void preparationCanCancelWithoutSpendingThenRelease(GameTestHelper h) {
        var p = player(h); var spell = new Spell(List.of(MethodSelf.INSTANCE, glyph(Ability.EEL_ROAR)));
        java.util.function.Supplier<Boolean> cast = () -> resolver(h, p, spell).onCast(ItemStack.EMPTY, h.getLevel());
        try {
            ActiveCasts.perform(spell, p, InteractionHand.MAIN_HAND, ActiveCastSource.CASTER_TOOL, cast, false, true);
            h.assertTrue(ActiveCasts.isChanting(p) && AquamiraePreparation.visible(p), "Aura prepares with a server-owned progress bar");
            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 1000 && AquamiraeCooldowns.remaining(p, Ability.EEL_ROAR) == 0, "Preparation itself neither pays nor starts cooldown");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.PAPER));
            h.runAfterDelay(2, () -> {
                try {
                    h.assertTrue(!ActiveCasts.isChanting(p) && !AquamiraePreparation.visible(p) && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Changing the held item cancels preparation and its display without payment");
                    ActiveCasts.perform(spell, p, InteractionHand.MAIN_HAND, ActiveCastSource.CASTER_TOOL, cast, false, true);
                    h.runAfterDelay(15, () -> {
                        try {
                            h.assertTrue(!ActiveCasts.isChanting(p) && !AquamiraePreparation.visible(p) && AuraEffects.has(p, Ability.EEL_ROAR), "Completed thirteen-tick preparation releases the real Ars spell once");
                            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 910 && AquamiraeCooldowns.remaining(p, Ability.EEL_ROAR) >= 590, "Release pays Self plus the new eighty mana quote and records thirty seconds");
                            h.succeed();
                        } finally { cleanup(p); }
                    });
                } catch (Throwable e) { cleanup(p); throw e; }
            });
        } catch (Throwable e) { cleanup(p); throw e; }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void mixedAuraPreparationKeepsOneRelease(GameTestHelper h) {
        var p = player(h); var spell = new Spell(List.of(MethodSelf.INSTANCE, glyph(Ability.EEL_ROAR), glyph(Ability.CORNELIA_VORTEX)));
        var provider = new AquamiraeCooldowns();
        try {
            h.assertTrue(!provider.chantingEnabled(new dev.arsconflux.api.casting.ActiveCastRequest(new Spell(List.of(MethodSelf.INSTANCE, glyph(Ability.DEPTHS_FURY))), p, ActiveCastSource.CASTER_TOOL)), "Ordinary buffs keep immediate casting");
            ActiveCasts.perform(spell, p, InteractionHand.MAIN_HAND, ActiveCastSource.CASTER_TOOL,
                    () -> resolver(h, p, spell).onCast(ItemStack.EMPTY, h.getLevel()), false, true);
            h.runAfterDelay(18, () -> {
                h.assertTrue(ActiveCasts.isChanting(p) && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "MAXIMUM aggregation waits for the longer twenty-tick preparation");
            });
            h.runAfterDelay(22, () -> {
                try {
                    h.assertTrue(AuraEffects.has(p, Ability.EEL_ROAR) && AuraEffects.has(p, Ability.CORNELIA_VORTEX), "One mixed release grants both distinct actions");
                    h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 790 && !AquamiraePreparation.visible(p), "One Self price and both payload prices, without repeated preparation"); h.succeed();
                } finally { cleanup(p); }
            });
        } catch (Throwable e) { cleanup(p); throw e; }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void parallelAuraHostsPayIndependentlyDespiteActiveCooldown(GameTestHelper h) throws Exception {
        var p = player(h); var first = mob(h, p, new Vec3(3, 0, 0), 40); var second = mob(h, p, new Vec3(6, 0, 0), 40);
        var relation = Allies.register(CarrierRegistry.id("aquamirae_nouveau:test_parallel_" + p.getUUID()), (a,b) -> a == p && (b == first || b == second));
        try {
            apply(h, p, first, Ability.EEL_ROAR); apply(h, p, second, Ability.EEL_ROAR); apply(h, p, first, Ability.EEL_ROAR);
            h.assertTrue(AuraEffects.has(first, Ability.EEL_ROAR) && AuraEffects.has(second, Ability.EEL_ROAR) && CapabilityRegistry.getMana(p).getCurrentMana() == 840, "Multiple allied hosts are permitted; each pays eighty once, duplicate same-host grants are free");
            h.assertTrue(!ActiveCasts.allowed(new Spell(List.of(MethodSelf.INSTANCE, glyph(Ability.EEL_ROAR))), p, ActiveCastSource.CASTER_TOOL), "Long cooldown gates new active releases, not these already dispatched grants");
            h.runAfterDelay(22, () -> {
                try {
                    h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 816, "Both hosts independently prepay twelve mana upkeep");
                    AuraEffects.remove(first, Ability.EEL_ROAR); ExecutionSessions.tick();
                    h.assertTrue(AuraEffects.has(second, Ability.EEL_ROAR), "Dispelling one host does not cancel the other"); h.succeed();
                } finally { cleanup(p, first, second); try { relation.close(); } catch (Exception e) { throw new RuntimeException(e); } }
            });
        } catch (Throwable e) { relation.close(); cleanup(p, first, second); throw e; }
    }
    @GameTest(template="empty", batch="aqua_creatures", timeoutTicks=40)
    public static void boundedAuraLifetimeRecoveryAndCloneState(GameTestHelper h) {
        var p = player(h); var replacement = player(h);
        try {
            var five = new AbstractAugment[]{AugmentExtendTime.INSTANCE, AugmentExtendTime.INSTANCE, AugmentExtendTime.INSTANCE, AugmentExtendTime.INSTANCE, AugmentExtendTime.INSTANCE};
            apply(h, p, p, Ability.EEL_ROAR, five); apply(h, p, p, Ability.CORNELIA_VORTEX, five);
            h.assertTrue(AuraEffects.remainingTicks(p, Ability.EEL_ROAR) == 301 && AuraEffects.remainingTicks(p, Ability.CORNELIA_VORTEX) == 401, "Even long compositions stop at fifteen and twenty seconds, with the existing final-tick marker guard");
            var data = p.getPersistentData().getCompound(AquamiraeCooldowns.DATA); data.putLong(Ability.EEL_ROAR.id, 0); data.putLong(Ability.CORNELIA_VORTEX.id, 0);
            h.assertTrue(AquamiraeCooldowns.remaining(p, Ability.EEL_ROAR) == 700 && AquamiraeCooldowns.remaining(p, Ability.CORNELIA_VORTEX) == 700, "Active auras preserve an end-relative recovery window even if the release deadline has elapsed");
            AuraEffects.remove(p, Ability.EEL_ROAR); AuraEffects.remove(p, Ability.CORNELIA_VORTEX); ExecutionSessions.tick();
            h.assertTrue(AquamiraeCooldowns.remaining(p, Ability.EEL_ROAR) == 400 && AquamiraeCooldowns.remaining(p, Ability.CORNELIA_VORTEX) == 300, "Ending actions persists twenty and fifteen seconds of recovery");
            AquamiraeCooldowns.copyState(p, replacement);
            h.assertTrue(AquamiraeCooldowns.remaining(replacement, Ability.EEL_ROAR) == 400 && AquamiraeCooldowns.remaining(replacement, Ability.CORNELIA_VORTEX) == 300, "Respawn preserves only this addon's cooldown state"); h.succeed();
        } finally { cleanup(p, replacement); }
    }
}
