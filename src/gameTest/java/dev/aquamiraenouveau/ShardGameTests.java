package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectOrbit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.execution.ExecutionSessions;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.*;
import dev.aquamiraenouveau.projectile.*;
import dev.obscuria.aquamirae.common.entity.projectile.AbyssalShard;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("aquamirae_nouveau") @PrefixGameTestTemplate(false)
public final class ShardGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var base = h.absoluteVec(new Vec3(2, 0, 2));
        var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ShardTester"));
        p.setPos(base.x, h.getLevel().getMaxBuildHeight() - 24, base.z);
        p.setYRot(-90); p.setXRot(0); p.setGameMode(GameType.SURVIVAL);
        p.getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MAX_MANA.get())).setBaseValue(1000);
        CapabilityRegistry.getMana(p).setMaxMana(1000); CapabilityRegistry.getMana(p).setMana(1000);
        return p;
    }
    private static AbyssalShardGlyph glyph() {
        return GlyphCatalog.ALL.stream().filter(AbyssalShardGlyph.class::isInstance).map(AbyssalShardGlyph.class::cast).findFirst().orElseThrow();
    }
    private static AABB area(ServerPlayer p) { return p.getBoundingBox().inflate(24); }
    private static SpellAbyssalShard shard(GameTestHelper h, ServerPlayer p) {
        return h.getLevel().getEntitiesOfClass(SpellAbyssalShard.class, area(p)).stream().filter(e -> e.getOwner() == p && !e.isRemoved()).findFirst().orElseThrow();
    }
    private static void cleanup(GameTestHelper h, ServerPlayer p) {
        h.getLevel().getEntitiesOfClass(AbyssalShard.class, area(p)).stream().filter(e -> e.getOwner() == p).forEach(Entity::discard);
        h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, area(p)).stream().filter(e -> e.getOwner() == p).forEach(Entity::discard);
        p.discard();
    }
    private static SpellResolver resolver(GameTestHelper h, ServerPlayer p, Spell spell) {
        return new SpellResolver(new SpellContext(h.getLevel(), spell, p, LivingCaster.from(p), ItemStack.EMPTY));
    }
    private static void aimAt(ServerPlayer p, LivingEntity target) {
        var direction = target.getBoundingBox().getCenter().subtract(p.getEyePosition());
        p.setYRot((float) (-Math.atan2(direction.x, direction.z) * 180 / Math.PI));
        p.setXRot((float) (-Math.atan2(direction.y, direction.horizontalDistance()) * 180 / Math.PI));
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void bareShardSpellDamagesIdleSlime(GameTestHelper h) {
        assertBareSpellHits(h, EntityType.SLIME);
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void bareShardSpellDamagesIdleMagmaCube(GameTestHelper h) {
        assertBareSpellHits(h, EntityType.MAGMA_CUBE);
    }
    private static void assertBareSpellHits(GameTestHelper h, EntityType<? extends net.minecraft.world.entity.monster.Slime> type) {
        var p = player(h); var target = type.create(h.getLevel());
        target.setSize(2, true); target.setNoAi(true); target.setNoGravity(true);
        target.setPos(p.position().add(3, 0, 0));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); target.setHealth(100);
        h.getLevel().addFreshEntity(target); aimAt(p, target);
        try {
            h.assertTrue(target.getTarget() == null, "Idle slime has no attacking target");
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph()));
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Projectile plus shard casts without a damage glyph");
            var primary = shard(h, p);
            h.runAfterDelay(8, () -> {
                try {
                    h.assertTrue(target.getHealth() < 100, "Bare native payload damages idle target without an Ars damage effect");
                    if (type == EntityType.SLIME)
                        h.assertTrue(Math.abs(target.getHealth() - 90) < .001, "Unarmored slime receives ten native damage");
                    h.assertTrue(target.hasEffect(MobEffects.WEAKNESS) && primary.isRemoved(), "Native impact inflicts Weakness and breaks the shard");
                    h.succeed();
                } finally { target.discard(); cleanup(h, p); }
            });
        } catch (RuntimeException error) { target.discard(); cleanup(h, p); throw error; }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=80)
    public static void idleSlimeIsTrackedOffTheOriginalFlightLine(GameTestHelper h) {
        var p = player(h); var target = EntityType.SLIME.create(h.getLevel());
        target.setSize(2, true); target.setNoAi(true); target.setNoGravity(true);
        target.setPos(p.position().add(3, 0, 3));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); target.setHealth(100);
        h.getLevel().addFreshEntity(target);
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph()));
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Bare shard shoots past off-axis idle slime");
            var primary = shard(h, p);
            // Begin at the native mature homing strength, without changing the aim toward the slime.
            primary.tickCount = 30;
            h.runAfterDelay(60, () -> {
                try {
                    h.assertTrue(target.getHealth() < 100 && primary.isRemoved(), "Native homing acquires Enemy implementations without an attack target");
                    h.succeed();
                } finally { target.discard(); cleanup(h, p); }
            });
        } catch (RuntimeException error) { target.discard(); cleanup(h, p); throw error; }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void passiveAnimalAndVillagerReceiveContactAndContinuation(GameTestHelper h) {
        var players = new ArrayList<ServerPlayer>();
        var targets = new ArrayList<LivingEntity>();
        var markers = new ArrayList<Marker>();
        try {
            for (var type : List.of(EntityType.COW, EntityType.VILLAGER)) {
                var p = player(h); p.setPos(p.position().add(0, 0, players.size() * 10)); players.add(p);
                var target = type.create(h.getLevel()); targets.add(target);
                target.setNoAi(true); target.setNoGravity(true); target.setPos(p.position().add(3, 0, 0));
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); target.setHealth(100);
                h.getLevel().addFreshEntity(target); aimAt(p, target);
                var marker = new Marker(); markers.add(marker);
                var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(), marker));
                h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Real shot aims at passive target");
            }
            h.runAfterDelay(8, () -> {
                try {
                    for (int i = 0; i < targets.size(); i++) {
                        var target = targets.get(i); var marker = markers.get(i);
                        h.assertTrue(target.getHealth() < 100 && target.hasEffect(MobEffects.WEAKNESS), "Contact damages passive target");
                        h.assertTrue(marker.hits == 1 && marker.victim == target && marker.owner == players.get(i), "Passive contact continues once with original ownership");
                    }
                    h.succeed();
                } finally { targets.forEach(Entity::discard); players.forEach(p -> cleanup(h, p)); }
            });
        } catch (RuntimeException error) { targets.forEach(Entity::discard); players.forEach(p -> cleanup(h, p)); throw error; }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void realSpawnReplacementBillsOnceAndVanillaIsUnaffected(GameTestHelper h) {
        var p = player(h); var marker = new Marker();
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(), AugmentAmplify.INSTANCE, marker));
            var mana = CapabilityRegistry.getMana(p); double before = mana.getCurrentMana();
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Real Ars cast succeeds");
            var shard = shard(h, p);
            h.assertTrue(Math.abs(before - mana.getCurrentMana() - r.getExpendedCost() - 90) < .001, "One native spawn quote plus normal Ars cost");
            h.assertTrue(Math.abs(shard.getShardScale() - 1.2) < .001, "Native scale enhancement");
            h.assertTrue(shard.getOwner() == p && shard.getY() > p.getY() + 1, "Original owner and eye-height launch");
            h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, area(p)).isEmpty(), "Original carrier is not spawned");
            h.assertTrue(marker.hits == 0, "Following effects wait for shard impact");
            var plain = resolver(h, p, new Spell(MethodProjectile.INSTANCE, marker));
            h.assertTrue(plain.onCast(ItemStack.EMPTY, h.getLevel()), "Plain Ars cast still succeeds");
            h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, area(p)).size() == 1, "Unmanaged projectile is unchanged");
            h.succeed();
        } finally { cleanup(h, p); }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void nativeHitResolvesFollowingEffectAtVictimAndSplits(GameTestHelper h) {
        var p = player(h); var marker = new Marker();
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.setNoAi(true); zombie.setNoGravity(true); zombie.setPos(p.position().add(3, 0, 0));
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); zombie.setHealth(100);
        h.getLevel().addFreshEntity(zombie);
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(), marker));
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Real cast launches shard");
            var primary = shard(h, p); double afterSpawn = CapabilityRegistry.getMana(p).getCurrentMana();
            h.runAfterDelay(8, () -> {
                try {
                    h.assertTrue(zombie.getHealth() < 100 && zombie.hasEffect(MobEffects.WEAKNESS), "Native damage and Weakness happen on real collision");
                    h.assertTrue(primary.isRemoved(), "Native primary shard breaks");
                    h.assertTrue(marker.hits == 1 && marker.victim == zombie && marker.owner == p, "Ars continuation resolves once with correct victim and owner");
                    h.assertTrue(Math.abs(afterSpawn - CapabilityRegistry.getMana(p).getCurrentMana()) < .001, "Impact and split do not rebill a single-hit payload");
                    h.assertTrue(h.getLevel().getEntitiesOfClass(AbyssalShard.class, area(p)).stream().filter(e -> e.getOwner() == p && !(e instanceof SpellAbyssalShard)).count() == 3, "Three native children retain original ownership");
                    h.succeed();
                } finally { zombie.discard(); cleanup(h, p); }
            });
        } catch (RuntimeException error) { zombie.discard(); cleanup(h, p); throw error; }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void nativeLifetimeAndChildrenAreBounded(GameTestHelper h) {
        var p = player(h);
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(), new Marker()));
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Shard casts");
            var primary = shard(h, p); primary.tickCount = 201; primary.tick();
            h.assertTrue(primary.isRemoved(), "Native primary expires after 200 ticks");
            var children = h.getLevel().getEntitiesOfClass(AbyssalShard.class, area(p)).stream().filter(e -> e.getOwner() == p && !e.isRemoved()).toList();
            h.assertTrue(children.size() == 3, "Native expiry splits into three children");
            for (var child : children) {
                h.assertTrue(Math.abs(child.getShardScale() - .5) < .001, "Child retains native half scale");
                child.tickCount = 201; child.tick();
                h.assertTrue(child.isRemoved(), "Child expires without further splitting");
            }
            h.succeed();
        } finally { cleanup(h, p); }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void managedTrajectoryHasOneDriverAndRepeatedImpactsPay(GameTestHelper h) {
        var p = player(h); var marker = new Marker();
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.setNoAi(true); zombie.setNoGravity(true); zombie.setPos(p.position().add(8, 0, 0));
        h.getLevel().addFreshEntity(zombie);
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(), marker));
            r.castType = MethodProjectile.INSTANCE;
            var carrier = new EntityProjectileSpell(h.getLevel(), r);
            carrier.setOwner(p); carrier.setPos(p.getEyePosition()); carrier.setDeltaMovement(new Vec3(.8, 0, 0));
            var repeated = new CarrierProfile(CarrierRegistry.id("aquamirae_nouveau:test_repeated"), true, true, null, Set::of);
            var mana = CapabilityRegistry.getMana(p); double before = mana.getCurrentMana();
            h.assertTrue(glyph().replace(carrier, repeated), "Managed replacement consumes carrier");
            var primary = shard(h, p); var start = primary.position();
            primary.tick();
            h.assertTrue(primary.position().equals(start), "Native tick does not also drive Ars motion");
            h.assertTrue(Math.abs(mana.getCurrentMana() - before) < .001, "Repeated carrier does not bill at spawn");
            var hit = new EntityHitResult(zombie, zombie.getBoundingBox().getCenter());
            ArsSpellAccess.resolver(carrier).onResolveEffect(h.getLevel(), hit);
            h.assertTrue(!primary.isRemoved() && marker.hits == 1, "Repeated payload remains intact and continues once");
            ArsSpellAccess.resolver(carrier).onResolveEffect(h.getLevel(), hit);
            h.assertTrue(marker.hits == 2 && Math.abs(before - mana.getCurrentMana() - 160) < .001, "Each repeated impact pays exactly once");
            h.assertTrue(h.getLevel().getEntitiesOfClass(AbyssalShard.class, area(p)).stream().noneMatch(e -> e.getOwner() == p && !(e instanceof SpellAbyssalShard)), "Repeated impact cannot multiply split payloads");
            mana.setMana(0);
            ArsSpellAccess.resolver(carrier).onResolveEffect(h.getLevel(), hit);
            h.assertTrue(primary.isRemoved() && carrier.isRemoved() && marker.hits == 2, "Empty mana cancels both entities before native hit or continuation");
            h.succeed();
        } finally { zombie.discard(); cleanup(h, p); }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void realOrbitRetainsItsArsMotion(GameTestHelper h) {
        var p = player(h);
        try {
            var r = resolver(h, p, new Spell(MethodSelf.INSTANCE, EffectOrbit.INSTANCE, glyph(), new Marker()));
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Real orbit cast succeeds");
            var primary = shard(h, p); var start = primary.position();
            h.runAfterDelay(5, () -> {
                try {
                    h.assertTrue(!primary.isRemoved() && primary.position().distanceTo(start) > .01, "Managed shard follows live Ars orbit");
                    h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, area(p)).isEmpty(), "No extra visible Ars carrier");
                    h.succeed();
                } finally { cleanup(h, p); }
            });
        } catch (RuntimeException error) { cleanup(h, p); throw error; }
    }
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void emptyManaAndOwnerLossLeaveNoReplacement(GameTestHelper h) {
        var p = player(h);
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(), new Marker()));
            r.castType = MethodProjectile.INSTANCE;
            CapabilityRegistry.getMana(p).setMana(0);
            var carrier = new EntityProjectileSpell(h.getLevel(), r); carrier.setOwner(p); carrier.setPos(p.getEyePosition());
            h.assertTrue(glyph().replace(carrier, CarrierRegistry.of(MethodProjectile.INSTANCE)), "Denied payload still consumes original carrier");
            h.assertTrue(h.getLevel().getEntitiesOfClass(SpellAbyssalShard.class, area(p)).stream().noneMatch(e -> e.getOwner() == p), "No unaffordable native entity for this caster");
            CapabilityRegistry.getMana(p).setMana(1000);
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Funded cast launches");
            var primary = shard(h, p); p.discard(); ExecutionSessions.tick();
            h.assertTrue(primary.isRemoved(), "Owner loss cleans up transient managed payload");
            h.succeed();
        } finally { cleanup(h, p); }
    }
    private static final class Marker extends AbstractEffect {
        int hits; LivingEntity victim, owner;
        Marker() { super(CarrierRegistry.id("aquamirae_nouveau:glyph_shard_marker"), "Marker"); }
        @Override public int getDefaultManaCost() { return 0; }
        @Override public int getCastingCost() { return 0; }
        @Override public boolean isEnabled() { return true; }
        @Override public SpellTier defaultTier() { return SpellTier.ONE; }
        @Override public SpellTier getConfigTier() { return SpellTier.ONE; }
        @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
        @Override public void onResolve(HitResult hit, net.minecraft.world.level.Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            hits++; owner = CastContexts.current().damageOwner();
            if (hit instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity living) victim = living;
        }
    }
}
