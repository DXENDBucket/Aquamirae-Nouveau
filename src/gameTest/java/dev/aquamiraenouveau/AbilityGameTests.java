package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.api.casting.ActiveCasts;
import dev.arsconflux.api.casting.ActiveCastSource;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.context.TriggerGeometry;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import dev.aquamiraenouveau.platform.NativeAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.UUID;

@GameTestHolder("aquamirae_nouveau") @PrefixGameTestTemplate(false)
public final class AbilityGameTests {
    private static Vec3 origin(GameTestHelper h) {
        var vec = h.absoluteVec(new Vec3(2, 0, 2));
        return new Vec3(vec.x, h.getLevel().getMaxBuildHeight() - 32, vec.z);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "AquaTester"));
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(origin(h));
        p.getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MAX_MANA.get())).setBaseValue(1000);
        CapabilityRegistry.getMana(p).setMaxMana(1000);
        CapabilityRegistry.getMana(p).setMana(1000);
        return p;
    }
    private static LivingEntity target(GameTestHelper h) {
        var mob = EntityType.COW.create(h.getLevel());
        mob.setNoAi(true); mob.setNoGravity(true); mob.setPos(origin(h).add(0, 0, 3));
        mob.getAttribute(Attributes.ARMOR).setBaseValue(20);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); mob.setHealth(100);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }
    private static AquamiraeGlyph glyph(Ability ability) {
        return GlyphCatalog.ALL.stream().filter(g -> g instanceof AquamiraeGlyph effect && effect.ability == ability)
                .map(AquamiraeGlyph.class::cast).findFirst().orElseThrow();
    }
    private static SpellResolver resolve(GameTestHelper h, ServerPlayer payer, Spell spell, HitResult hit) {
        var context = new SpellContext(h.getLevel(), spell, payer, LivingCaster.from(payer), ItemStack.EMPTY);
        var resolver = new SpellResolver(context);
        context.nextPart();
        TriggerGeometry.write(context, new Vec3(1, 0, 0));
        resolver.onResolveEffect(h.getLevel(), hit);
        return resolver;
    }
    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.001, message + ": " + actual + " != " + expected);
    }

    @GameTest(template="empty", batch="aqua_status", timeoutTicks=40)
    public static void nativeAttributesStackingAndDuration(GameTestHelper h) {
        var p = player(h); var target = target(h);
        try {
            var armor = glyph(Ability.ARMOR_SHATTER);
            resolve(h, p, new Spell(MethodTouch.INSTANCE, armor), new EntityHitResult(target));
            close(h, target.getAttributeValue(Attributes.ARMOR), 18, "Native armor modifier applied");
            resolve(h, p, new Spell(MethodTouch.INSTANCE, armor, AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE), new EntityHitResult(target));
            close(h, NativeAbilities.currentLevel(target, Ability.ARMOR_SHATTER), 3, "Native stack plus amplified activation");
            close(h, target.getAttributeValue(Attributes.ARMOR), 14, "Native level III armor");
            close(h, target.getEffect(NativeAbilities.effect(Ability.ARMOR_SHATTER)).getDuration(), 440, "Extend Time adds configured ticks");
            for (int i=0; i<6; i++) armor.activate(h.getLevel(), CastContext.detached(p, new EntityHitResult(target), null), new SpellStats.Builder().build());
            close(h, NativeAbilities.currentLevel(target, Ability.ARMOR_SHATTER), 5, "Native five-stack cap");
            target.removeEffect(NativeAbilities.effect(Ability.ARMOR_SHATTER));
            close(h, target.getAttributeValue(Attributes.ARMOR), 20, "Removal restores armor");
            resolve(h, p, new Spell(MethodTouch.INSTANCE, glyph(Ability.HEALTH_SHATTER)), new EntityHitResult(target));
            close(h, target.getMaxHealth(), 95, "Native maximum health modifier");
            target.removeEffect(NativeAbilities.effect(Ability.HEALTH_SHATTER));
            close(h, target.getMaxHealth(), 100, "Health shatter cleans up its modifier");
            h.succeed();
        } finally { target.discard(); p.discard(); }
    }

    @GameTest(template="empty", batch="aqua_billing", timeoutTicks=40)
    public static void triggerBillingAndPayerRemainWithCaster(GameTestHelper h) {
        var p = player(h); var target = target(h);
        try {
            var mana = CapabilityRegistry.getMana(p);
            var armor = glyph(Ability.ARMOR_SHATTER);
            var spell = new Spell(MethodTouch.INSTANCE, armor);
            resolve(h, p, spell, new EntityHitResult(target));
            close(h, mana.getCurrentMana(), 1000 - AquamiraeConfig.cost(Ability.ARMOR_SHATTER), "Selected target does not become payer");
            resolve(h, p, spell, new EntityHitResult(target));
            close(h, mana.getCurrentMana(), 1000 - 2*AquamiraeConfig.cost(Ability.ARMOR_SHATTER), "Repeated real activations pay again");
            double before = mana.getCurrentMana();
            resolve(h, p, spell, new BlockHitResult(origin(h), Direction.UP, BlockPos.containing(origin(h)), false));
            close(h, mana.getCurrentMana(), before, "Invalid target releases reservation");
            mana.setMana(0);
            resolve(h, p, new Spell(MethodTouch.INSTANCE, glyph(Ability.HEALTH_SHATTER)), new EntityHitResult(target));
            close(h, NativeAbilities.currentLevel(target, Ability.HEALTH_SHATTER), 0, "Empty mana prevents native activation");
            p.setGameMode(GameType.CREATIVE);
            resolve(h, p, spell, new EntityHitResult(target));
            close(h, NativeAbilities.currentLevel(target, Ability.ARMOR_SHATTER), 3, "Creative still activates");
            close(h, mana.getCurrentMana(), 0, "Creative is free");
            h.succeed();
        } finally { target.discard(); p.discard(); }
    }

    @GameTest(template="empty", batch="aqua_billing", timeoutTicks=40)
    public static void cappedRefreshDoesNotSpendOnUnchangedEffect(GameTestHelper h) {
        var p = player(h); var target = target(h);
        try {
            var buff = glyph(Ability.DEPTHS_FURY);
            var spell = new Spell(MethodTouch.INSTANCE, buff);
            resolve(h, p, spell, new EntityHitResult(target));
            var mana = CapabilityRegistry.getMana(p);
            double before = mana.getCurrentMana();
            resolve(h, p, spell, new EntityHitResult(target));
            close(h, mana.getCurrentMana(), before, "Identical same-tick native addEffect failure is uncharged");
            close(h, NativeAbilities.currentLevel(target, Ability.DEPTHS_FURY), 1, "Nonstacking buff stays at one level");
            resolve(h, p, new Spell(MethodTouch.INSTANCE, glyph(Ability.DEPTHS_RESILIENCE), AugmentAmplify.INSTANCE), new EntityHitResult(target));
            close(h, NativeAbilities.currentLevel(target, Ability.DEPTHS_RESILIENCE), 2, "Native resilience supports amplification");
            close(h, mana.getCurrentMana(), before - AquamiraeConfig.cost(Ability.DEPTHS_RESILIENCE) - AquamiraeConfig.amplifyCost(), "Amplified activation has one fee");
            h.succeed();
        } finally { target.discard(); p.discard(); }
    }

    @GameTest(template="empty", batch="aqua_cooldown", timeoutTicks=40)
    public static void activeReleaseCooldownDoesNotGateExistingHits(GameTestHelper h) {
        var p = player(h); var target = target(h);
        try {
            var spell = new Spell(MethodSelf.INSTANCE, glyph(Ability.DEPTHS_FURY));
            var source = ActiveCastSource.CASTER_TOOL;
            h.assertTrue(ActiveCasts.allowed(spell, p, source), "Initial active use allowed");
            ActiveCasts.execute(spell, p, source, () -> {
                var context = new SpellContext(h.getLevel(), spell, p, LivingCaster.from(p), ItemStack.EMPTY);
                return new SpellResolver(context).onCast(ItemStack.EMPTY, h.getLevel());
            }, false);
            h.assertTrue(AquamiraeCooldowns.remaining(p, Ability.DEPTHS_FURY) > 0, "Acknowledged release commits cooldown");
            h.assertTrue(!ActiveCasts.allowed(spell, p, source), "New active use denied");
            resolve(h, p, new Spell(MethodTouch.INSTANCE, glyph(Ability.DEPTHS_FURY)), new EntityHitResult(target));
            close(h, NativeAbilities.currentLevel(target, Ability.DEPTHS_FURY), 1, "Already dispatched hit still executes");
            p.setGameMode(GameType.CREATIVE);
            h.assertTrue(ActiveCasts.allowed(spell, p, source), "Creative bypasses cooldown");
            h.succeed();
        } finally { target.discard(); p.discard(); }
    }

    @GameTest(template="empty", batch="aqua_chain", timeoutTicks=40)
    public static void contextAndSubsequentEffectsKeepOriginalTarget(GameTestHelper h) {
        var p = player(h); var target = target(h);
        try {
            var marker = new Marker();
            var resolver = resolve(h, p, new Spell(MethodTouch.INSTANCE, glyph(Ability.ARMOR_SHATTER), marker), new EntityHitResult(target));
            h.assertTrue(marker.last == target, "Native effect does not swallow or retarget following effects");
            var origin = CastContext.of(resolver.spellContext, new EntityHitResult(target), null);
            CastContexts.bind(resolver.spellContext, origin);
            var cloned = resolver.spellContext.clone();
            var copied = CastContext.of(cloned, new EntityHitResult(target), null);
            h.assertTrue(copied.caster() == p && copied.damageOwner() == p, "Cloned continuation keeps original attribution");
            h.succeed();
        } finally { target.discard(); p.discard(); }
    }

    @GameTest(template="empty", batch="aqua_recipe", timeoutTicks=40)
    public static void registeredRecipesUseNativeMaterials(GameTestHelper h) {
        for (var g : GlyphCatalog.ALL) {
            var key = g.getRegistryName();
            h.assertTrue(h.getLevel().getRecipeManager().byKey(key).isPresent(), "Native glyph recipe loads: " + key);
            h.assertTrue(!g.getGlyph().getDefaultInstance().isEmpty(), "Glyph item exists: " + key);
        }
        h.assertTrue(GlyphCatalog.ALL.size() == (NativeAbilities.supports(Ability.ABYSSAL_SHARD) ? 5 : 4), "Only native platform capabilities are registered");
        h.succeed();
    }

    private static final class Marker extends AbstractEffect {
        LivingEntity last;
        Marker() { super(dev.arsconflux.api.projectile.CarrierRegistry.id("aquamirae_nouveau:glyph_test_marker"), "Test marker"); }
        @Override public int getDefaultManaCost() { return 0; }
        @Override public int getCastingCost() { return 0; }
        @Override public boolean isEnabled() { return true; }
        @Override public SpellTier defaultTier() { return SpellTier.ONE; }
        @Override public SpellTier getConfigTier() { return SpellTier.ONE; }
        @Override protected java.util.Set<AbstractAugment> getCompatibleAugments() { return java.util.Set.of(); }
        @Override public void onResolve(HitResult hit, net.minecraft.world.level.Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            if (hit instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity living) last = living;
        }
    }
}
