package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.context.TriggerGeometry;
import dev.arsconflux.api.execution.ExecutionSessions;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.targeting.Allies;
import dev.aquamiraenouveau.projectile.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("aquamirae_nouveau") @PrefixGameTestTemplate(false)
public final class ChakramGameTests {
    private static ServerPlayer player(GameTestHelper h, int y) {
        var uuid = UUID.randomUUID(); var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(uuid, "Aq" + uuid.toString().substring(0, 8)));
        var origin = h.absoluteVec(new Vec3(2, 0, 2)); p.setPos(origin.x, y, origin.z); p.setYRot(-90); p.setXRot(0); p.setGameMode(GameType.SURVIVAL);
        p.getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MAX_MANA.get())).setBaseValue(1000);
        CapabilityRegistry.getMana(p).setMaxMana(1000); CapabilityRegistry.getMana(p).setMana(1000); return p;
    }
    private static LivingEntity target(GameTestHelper h, ServerPlayer p, EntityType<? extends net.minecraft.world.entity.Mob> type, Vec3 offset) {
        var target = type.create(h.getLevel()); target.setNoAi(true); target.setNoGravity(true); target.setPos(p.position().add(offset));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); target.setHealth(100);
        if (type == EntityType.ZOMBIE) target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        h.getLevel().addFreshEntity(target); return target;
    }
    private static ChakramGlyph glyph(Ability ability) {
        return GlyphCatalog.ALL.stream().filter(g -> g instanceof ChakramGlyph c && c.ability() == ability).map(ChakramGlyph.class::cast).findFirst().orElseThrow();
    }
    private static SpellResolver resolver(GameTestHelper h, ServerPlayer p, Spell spell) {
        return new SpellResolver(new SpellContext(h.getLevel(), spell, p, LivingCaster.from(p), ItemStack.EMPTY));
    }
    private static List<Projectile> payloads(GameTestHelper h, ServerPlayer p) {
        return h.getLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(40)).stream().filter(e -> e instanceof ChakramPayload && e.getOwner() == p && !e.isRemoved()).toList();
    }
    private static void cleanup(GameTestHelper h, ServerPlayer p) {
        payloads(h, p).forEach(Entity::discard); p.discard(); ExecutionSessions.tick();
    }
    private static void aim(ServerPlayer p, LivingEntity target) {
        var delta = target.getBoundingBox().getCenter().subtract(p.getEyePosition());
        p.setYRot((float)(-Math.atan2(delta.x, delta.z) * 180 / Math.PI)); p.setXRot((float)(-Math.atan2(delta.y, delta.horizontalDistance()) * 180 / Math.PI));
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=40)
    public static void nativeScyphoidVenomAndExemptions(GameTestHelper h) throws Exception {
        var p = player(h, -48); var cow = target(h, p, EntityType.COW, new Vec3(3, 0, 0));
        var sting = GlyphCatalog.ALL.stream().filter(ScyphoidStingGlyph.class::isInstance).findFirst().orElseThrow();
        try {
            var r = resolver(h, p, new Spell(MethodTouch.INSTANCE, sting)); r.spellContext.nextPart();
            r.onResolveEffect(h.getLevel(), new EntityHitResult(cow));
            h.assertTrue(cow.getEffect(MobEffects.POISON).getAmplifier() == 2 && cow.getEffect(MobEffects.POISON).getDuration() == 60,
                    "Native helper delivers Poison III for sixty ticks");
            h.assertTrue(cow.getEffect(MobEffects.BLINDNESS).getDuration() == 60 && cow.getLastHurtByMob() == p,
                    "Native venom attribution and secondary blindness retain original owner");
            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 960, "One sting quote");
            r = resolver(h, p, new Spell(MethodTouch.INSTANCE, sting, AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE)); r.spellContext.nextPart();
            r.onResolveEffect(h.getLevel(), new EntityHitResult(cow));
            h.assertTrue(cow.getEffect(MobEffects.POISON).getAmplifier() == 3 && cow.getEffect(MobEffects.BLINDNESS).getDuration() == 260,
                    "Amplify strengthens poison and Extend Time prolongs both statuses");
            double mana = CapabilityRegistry.getMana(p).getCurrentMana();
            for (String id : List.of("abyssal_scyphoid", "anglerfish")) {
                var immune = BuiltInRegistries.ENTITY_TYPE.get(CarrierRegistry.id("aquamirae:" + id)).create(h.getLevel());
                var ignored = resolver(h, p, new Spell(MethodTouch.INSTANCE, sting)); ignored.spellContext.nextPart();
                ignored.onResolveEffect(h.getLevel(), new EntityHitResult(immune));
                h.assertTrue(!((LivingEntity)immune).hasEffect(MobEffects.POISON) && !((LivingEntity)immune).hasEffect(MobEffects.BLINDNESS), "Native creature exemption preserved");
            }
            try (var registration = Allies.register(CarrierRegistry.id("aquamirae_nouveau:sting_" + p.getUUID()), (a,b) -> a == p && b == cow)) {
                r = resolver(h, p, new Spell(MethodTouch.INSTANCE, sting)); r.spellContext.nextPart(); r.onResolveEffect(h.getLevel(), new EntityHitResult(cow));
                h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == mana, "Allies and native-exempt creatures consume no quote");
            }
            cow.removeAllEffects(); CapabilityRegistry.getMana(p).setMana(0);
            r = resolver(h, p, new Spell(MethodTouch.INSTANCE, sting)); r.spellContext.nextPart(); r.onResolveEffect(h.getLevel(), new EntityHitResult(cow));
            h.assertTrue(!cow.hasEffect(MobEffects.POISON) && !cow.hasEffect(MobEffects.BLINDNESS), "Empty mana produces neither effect");
            h.succeed();
        } finally { cow.discard(); cleanup(h, p); }
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=40)
    public static void bothWeaponsReplaceAndResolveNativePayload(GameTestHelper h) {
        var players = new ArrayList<ServerPlayer>(); var targets = new ArrayList<LivingEntity>(); var markers = new ArrayList<Marker>();
        try {
            for (var ability : List.of(Ability.TERRIBLE_CHAKRAM, Ability.MAZE_ROSE)) {
                // Keep real flight above both platforms' generated terrain and away from other scenarios.
                var p = player(h, 136); p.setPos(p.position().add(0, 0, players.size() * 8)); players.add(p);
                var victim = target(h, p, EntityType.COW, new Vec3(3, 0, 0)); targets.add(victim); aim(p, victim);
                var marker = new Marker(); markers.add(marker); var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(ability), marker));
                double before = CapabilityRegistry.getMana(p).getCurrentMana();
                h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Real weapon replacement casts");
                h.assertTrue(payloads(h, p).size() == 1, "One native replacement payload");
                var original = BuiltInRegistries.ENTITY_TYPE.get(CarrierRegistry.id("aquamirae:" + ability.id)).create(h.getLevel());
                var replacement = payloads(h, p).get(0);
                h.assertTrue(replacement.getBbWidth() == original.getBbWidth() && replacement.getBbHeight() == original.getBbHeight(),
                        "Replacement retains the native weapon collision dimensions");
                h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, p.getBoundingBox().inflate(8)).stream().noneMatch(e -> e.getOwner() == p), "No duplicate Ars carrier");
                h.assertTrue(Math.abs(before - CapabilityRegistry.getMana(p).getCurrentMana() - r.getExpendedCost() - ability.mana) < .001, "One spawn charge plus normal Ars cost");
            }
            h.runAfterDelay(6, () -> {
                try {
                    for (int i=0; i<targets.size(); i++) {
                        var target = targets.get(i); var marker = markers.get(i);
                        h.assertTrue(target.getHealth() == 97 && target.hasEffect(i == 0 ? MobEffects.POISON : MobEffects.LEVITATION),
                                "Original three-point damage and native secondary status: weapon=" + i + ", health=" + target.getHealth()
                                        + ", effects=" + target.getActiveEffects() + ", hits=" + marker.hits + ", target=" + target.position()
                                        + ", projectiles=" + payloads(h, players.get(i)).stream().map(e -> e.position() + "/" + e.getDeltaMovement()).toList());
                        h.assertTrue(marker.hits == 1 && marker.victim == target && marker.owner == players.get(i), "Successful hurt continues once on the original target and owner");
                    }
                    h.succeed();
                } finally { targets.forEach(Entity::discard); players.forEach(p -> cleanup(h, p)); }
            });
        } catch (RuntimeException error) { targets.forEach(Entity::discard); players.forEach(p -> cleanup(h, p)); throw error; }
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=40)
    public static void roseUsesNativeHomingWithAlliedCandidatesExcluded(GameTestHelper h) throws Exception {
        var p = player(h, 32); var friend = target(h, p, EntityType.ZOMBIE, new Vec3(3, 0, 2));
        try (var registration = Allies.register(CarrierRegistry.id("aquamirae_nouveau:rose_" + p.getUUID()), (a,b) -> a == p && b == friend)) {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(Ability.MAZE_ROSE), new Marker()));
            h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Rose casts");
            var rose = payloads(h, p).get(0); rose.tickCount = 10;
            var nativeType = BuiltInRegistries.ENTITY_TYPE.get(CarrierRegistry.id("aquamirae:maze_rose"));
            var ordinary = nativeType.create(h.getLevel()); ordinary.setPos(rose.position()); ordinary.tickCount = 10;
            var homing = rose.getClass().getSuperclass().getDeclaredMethod("computeMonsterHoming", Vec3.class); homing.setAccessible(true);
            var motion = new Vec3(.8, 0, 0);
            h.assertTrue(((Vec3)homing.invoke(rose, motion)).equals(motion), "Opted-in native homing cannot lock an allied monster");
            h.assertTrue(!((Vec3)homing.invoke(ordinary, motion)).equals(motion), "Ordinary native rose keeps its original candidate selection");
            var mana = CapabilityRegistry.getMana(p).getCurrentMana();
            ((ChakramPayload)rose).control().onHit(new EntityHitResult(friend), () -> {});
            h.assertTrue(friend.getHealth() == 100 && !friend.hasEffect(MobEffects.LEVITATION) && CapabilityRegistry.getMana(p).getCurrentMana() == mana,
                    "Defensive ally dispatch applies no native hurt or levitation");
            h.succeed();
        } finally { friend.discard(); cleanup(h, p); }
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=40)
    public static void managedWeaponBillsOnlyAcceptedHitsAndCancelsOnEmptyMana(GameTestHelper h) {
        var p = player(h, 72); var cow = target(h, p, EntityType.COW, new Vec3(4, 0, 0)); var marker = new Marker();
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(Ability.TERRIBLE_CHAKRAM), marker)); r.castType = MethodProjectile.INSTANCE;
            var carrier = new EntityProjectileSpell(h.getLevel(), r); carrier.setOwner(p); carrier.setPos(p.getEyePosition());
            TriggerGeometry.write(r.spellContext, new Vec3(1, 0, 0));
            var profile = new CarrierProfile(CarrierRegistry.id("aquamirae_nouveau:managed_chakram"), true, true, null, Set::of);
            h.assertTrue(glyph(Ability.TERRIBLE_CHAKRAM).replace(carrier, profile), "Managed replacement starts");
            var payload = payloads(h, p).get(0); var start = payload.position(); payload.tick();
            h.assertTrue(payload.position().equals(start) && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Managed native tick supplies neither extra motion nor spawn payment");
            var hit = new EntityHitResult(cow); var callback = ArsSpellAccess.resolver(carrier);
            callback.onResolveEffect(h.getLevel(), hit); callback.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(marker.hits == 1 && cow.getHealth() == 97 && CapabilityRegistry.getMana(p).getCurrentMana() == 960,
                    "Invulnerability rejection cannot charge or continue a second time");
            cow.invulnerableTime = 0; callback.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(marker.hits == 2 && CapabilityRegistry.getMana(p).getCurrentMana() == 920, "A second accepted impact bills once");
            cow.invulnerableTime = 0; CapabilityRegistry.getMana(p).setMana(0); callback.onResolveEffect(h.getLevel(), hit);
            h.assertTrue(payload.isRemoved() && carrier.isRemoved() && marker.hits == 2 && cow.getHealth() == 94, "Denied quote cleans both entities before hurt");
            h.succeed();
        } finally { cow.discard(); cleanup(h, p); }
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=40)
    public static void nativeSweepAndReturnKeepOneSpawnFee(GameTestHelper h) {
        int fixture = 0;
        for (var ability : List.of(Ability.TERRIBLE_CHAKRAM, Ability.MAZE_ROSE)) {
            var p = player(h, 4096 + fixture++ * 48); var first = target(h, p, EntityType.COW, new Vec3(3, 0, 0));
            var second = target(h, p, EntityType.COW, new Vec3(6, 0, 0)); var marker = new Marker();
            try {
                var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(ability), marker)); r.castType = MethodProjectile.INSTANCE;
                var carrier = new EntityProjectileSpell(h.getLevel(), r); carrier.setOwner(p); carrier.setPos(p.getEyePosition());
                TriggerGeometry.write(r.spellContext, new Vec3(1, 0, 0));
                glyph(ability).replace(carrier, new CarrierProfile(CarrierRegistry.id("aquamirae_nouveau:sweep_test"), false, false, null, Set::of));
                var payload = (ChakramPayload)payloads(h, p).get(0); double paid = CapabilityRegistry.getMana(p).getCurrentMana();
                h.assertTrue(paid == 1000 - ability.mana, "Ordinary native weapon pays its revised launch quote");
                for (var victim : List.of(first, first, second)) payload.control().onHit(new EntityHitResult(victim), () -> { throw new AssertionError("Native callback must pass through the controller"); });
                first.invulnerableTime = 0;
                payload.control().onHit(new EntityHitResult(first), () -> { throw new AssertionError("Return contact uses the controller"); });
                h.assertTrue(marker.hits == 3 && first.getHealth() == 94 && second.getHealth() == 97, "Two victims and a later return deliver three accepted native contacts; immediate repetition is rejected");
                h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == paid, "Ordinary sweep and return retain one launch fee despite multiple accepted continuations");
            } finally { first.discard(); second.discard(); cleanup(h, p); }
        }
        h.succeed();
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=40)
    public static void realScatterPaysEachWeaponAndRespectsAvailableMana(GameTestHelper h) {
        int fixture = 0;
        for (var ability : List.of(Ability.TERRIBLE_CHAKRAM, Ability.MAZE_ROSE)) {
            var p = player(h, 8192 + fixture++ * 48);
            try {
                var spell = new Spell(MethodProjectile.INSTANCE, glyph(ability), com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit.INSTANCE);
                var r = resolver(h, p, spell); h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Real scatter spell casts");
                h.assertTrue(payloads(h, p).size() == 2 && CapabilityRegistry.getMana(p).getCurrentMana() == 1000 - r.getExpendedCost() - 2 * ability.mana, "One Split adds one native weapon; each pays a quote in addition to one Ars launch price: ability=" + ability + ", count=" + payloads(h, p).size() + ", mana=" + CapabilityRegistry.getMana(p).getCurrentMana() + ", ars=" + r.getExpendedCost() + ", payload=" + ability.mana);
                payloads(h, p).forEach(Entity::discard); ExecutionSessions.tick();
                CapabilityRegistry.getMana(p).setMana(30 + ability.mana);
                r = resolver(h, p, spell); h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Affordable scatter still releases");
                h.assertTrue(payloads(h, p).size() == 1 && CapabilityRegistry.getMana(p).getCurrentMana() == 0, "After paying Projectile plus Split, only one affordable payload appears; no unpaid scatter copies");
            } finally { cleanup(h, p); }
        }
        h.succeed();
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=120)
    public static void nativeWeaponsReturnToTheirOwner(GameTestHelper h) {
        var p = player(h, 272);
        try {
            for (var ability : List.of(Ability.TERRIBLE_CHAKRAM, Ability.MAZE_ROSE)) {
                var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(ability))); h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Native returning weapon launches");
            }
            var launched = payloads(h, p); h.assertTrue(launched.size() == 2, "Both weapon types start");
            h.runAfterDelay(100, () -> { try { h.assertTrue(launched.stream().allMatch(Entity::isRemoved), "Original homing-to-owner removes the weapon before the cap"); h.succeed(); } finally { cleanup(h, p); } });
        } catch (RuntimeException error) { cleanup(h, p); throw error; }
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=150)
    public static void zeroMotionWeaponIsBoundedAndOrphanIsRemoved(GameTestHelper h) {
        var p = player(h, 216);
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(Ability.TERRIBLE_CHAKRAM))); h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Bounded weapon launches");
            var stuck = payloads(h, p).get(0); stuck.setPos(p.position().add(2, 0, 0)); stuck.setDeltaMovement(Vec3.ZERO);
            var orphan = ChakramEntities.create(Ability.MAZE_ROSE, h.getLevel()).entity(); orphan.setPos(p.position()); h.getLevel().addFreshEntity(orphan); orphan.tick();
            h.assertTrue(orphan.isRemoved(), "Reload without runtime controller removes an orphan");
            h.runAfterDelay(20, () -> h.assertTrue(!stuck.isRemoved(), "Zero velocity cannot silently pretend the native return completed"));
            h.runAfterDelay(125, () -> { try { h.assertTrue(stuck.isRemoved(), "Execution session removes stalled weapons at the 120-tick cap"); h.succeed(); } finally { cleanup(h, p); } });
        } catch (RuntimeException error) { cleanup(h, p); throw error; }
    }
    @GameTest(template="empty", batch="aqua_new", timeoutTicks=40)
    public static void deniedSpawnAndOwnerLossCleanNativeWeapons(GameTestHelper h) {
        var p = player(h, 176);
        try {
            var r = resolver(h, p, new Spell(MethodProjectile.INSTANCE, glyph(Ability.MAZE_ROSE))); r.castType = MethodProjectile.INSTANCE;
            var carrier = new EntityProjectileSpell(h.getLevel(), r); carrier.setOwner(p); carrier.setPos(p.getEyePosition());
            CapabilityRegistry.getMana(p).setMana(0);
            h.assertTrue(glyph(Ability.MAZE_ROSE).replace(carrier, CarrierRegistry.of(MethodProjectile.INSTANCE)) && payloads(h, p).isEmpty(), "Empty account spawns no payload");
            CapabilityRegistry.getMana(p).setMana(1000); h.assertTrue(r.onCast(ItemStack.EMPTY, h.getLevel()), "Funded payload spawns");
            var launched = payloads(h, p).get(0); p.discard(); ExecutionSessions.tick();
            h.assertTrue(launched.isRemoved(), "Owner loss closes native payload"); h.succeed();
        } finally { cleanup(h, p); }
    }
    private static final class Marker extends AbstractEffect {
        int hits; LivingEntity victim, owner;
        Marker() { super(CarrierRegistry.id("aquamirae_nouveau:glyph_chakram_marker"), "Marker"); }
        @Override public int getDefaultManaCost() { return 0; }
        @Override public int getCastingCost() { return 0; }
        @Override public boolean isEnabled() { return true; }
        @Override public SpellTier defaultTier() { return SpellTier.ONE; }
        @Override public SpellTier getConfigTier() { return SpellTier.ONE; }
        @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
        @Override public void onResolve(HitResult hit, net.minecraft.world.level.Level level, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            hits++; owner = CastContexts.current().damageOwner();
            if (hit instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity target) victim = target;
        }
    }
}
