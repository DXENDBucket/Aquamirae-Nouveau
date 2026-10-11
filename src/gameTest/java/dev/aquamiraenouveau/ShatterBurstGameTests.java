package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.targeting.Allies;
import dev.aquamiraenouveau.platform.NativeAbilities;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
public final class ShatterBurstGameTests {
    private static final List<Ability> BURSTS = List.of(Ability.ARMOR_BURST, Ability.HEALTH_BURST);
    private static ServerPlayer player(GameTestHelper h) {
        var id = UUID.randomUUID(); var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(id, "Aq" + id.toString().substring(0, 8)));
        var pos = h.absoluteVec(new Vec3(2, 0, 2)); p.setPos(pos.x, 320, pos.z); p.setGameMode(GameType.SURVIVAL);
        p.getAttribute(net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(com.hollingsworth.arsnouveau.api.perk.PerkAttributes.MAX_MANA.get())).setBaseValue(1000);
        CapabilityRegistry.getMana(p).setMaxMana(1000); CapabilityRegistry.getMana(p).setMana(1000); return p;
    }
    private static LivingEntity target(GameTestHelper h, ServerPlayer p) {
        var cow = EntityType.COW.create(h.getLevel()); cow.setNoAi(true); cow.setNoGravity(true); cow.setPos(p.position().add(3, 0, 0));
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); cow.setHealth(100); h.getLevel().addFreshEntity(cow); return cow;
    }
    private static Ability state(Ability burst) { return burst == Ability.ARMOR_BURST ? Ability.ARMOR_SHATTER : Ability.HEALTH_SHATTER; }
    private static int threshold(Ability burst) { return burst == Ability.ARMOR_BURST ? 5 : 10; }
    private static AbstractSpellPart glyph(Ability ability) { return GlyphCatalog.ALL.stream().filter(g -> ((AquamiraeSpell)g).ability() == ability).findFirst().orElseThrow(); }
    private static SpellResolver resolver(GameTestHelper h, ServerPlayer p, Spell spell) { return new SpellResolver(new SpellContext(h.getLevel(), spell, p, LivingCaster.from(p), ItemStack.EMPTY)); }
    private static void resolve(GameTestHelper h, ServerPlayer p, LivingEntity victim, Ability ability) {
        var r = resolver(h, p, new Spell(MethodTouch.INSTANCE, glyph(ability))); r.spellContext.nextPart(); r.onResolveEffect(h.getLevel(), new EntityHitResult(victim));
    }
    private static void full(LivingEntity victim, Ability burst, ServerPlayer owner) { NativeAbilities.apply(victim, state(burst), 240, threshold(burst) - 1, owner); }

    @GameTest(template="empty", batch="aqua_burst", timeoutTicks=40)
    public static void nativeThresholdsDamageAndSuccessfulPayment(GameTestHelper h) {
        var p = player(h); var victim = target(h, p);
        try {
            for (var burst : BURSTS) {
                victim.removeAllEffects(); victim.invulnerableTime = 0; CapabilityRegistry.getMana(p).setMana(1000);
                NativeAbilities.apply(victim, state(burst), 240, threshold(burst) - 2, p);
                float before = victim.getHealth(); resolve(h, p, victim, burst);
                h.assertTrue(victim.getHealth() == before && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "One level below the native threshold produces no damage or charge");
                full(victim, burst, p); before = victim.getHealth(); resolve(h, p, victim, burst);
                float amount = burst == Ability.ARMOR_BURST ? 6 : 4;
                h.assertTrue(Math.abs(before - victim.getHealth() - amount) < .001 && CapabilityRegistry.getMana(p).getCurrentMana() == (burst == Ability.ARMOR_BURST ? 980 : 985), "Native bonus damage and one payload payment");
                h.assertTrue(NativeAbilities.currentLevel(victim, state(burst)) == threshold(burst), "Native burst does not consume or reset stacks");
                before = victim.getHealth(); resolve(h, p, victim, burst);
                h.assertTrue(victim.getHealth() == before && CapabilityRegistry.getMana(p).getCurrentMana() == (burst == Ability.ARMOR_BURST ? 980 : 985), "Invulnerability rejects a second payment");
            }
            h.succeed();
        } finally { victim.discard(); p.discard(); }
    }
    @GameTest(template="empty", batch="aqua_burst", timeoutTicks=40)
    public static void alliesEmptyManaAndInvalidCasterAreFree(GameTestHelper h) throws Exception {
        var p = player(h); var victim = target(h, p);
        try {
            for (var burst : BURSTS) {
                victim.removeAllEffects(); victim.invulnerableTime = 0; CapabilityRegistry.getMana(p).setMana(1000); full(victim, burst, p);
                float before = victim.getHealth();
                try (var registration = Allies.register(CarrierRegistry.id("aquamirae_nouveau:burst_" + p.getUUID()), (a,b) -> a == p && b == victim)) {
                    resolve(h, p, victim, burst);
                    h.assertTrue(victim.getHealth() == before && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Core provider protects allies without charge");
                }
                CapabilityRegistry.getMana(p).setMana(0); resolve(h, p, victim, burst);
                h.assertTrue(victim.getHealth() == before, "Empty account cannot produce native bonus damage");
            }
            CapabilityRegistry.getMana(p).setMana(1000); p.setHealth(0); resolve(h, p, victim, Ability.HEALTH_BURST);
            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Dead caster cannot activate a paid effect"); h.succeed();
        } finally { victim.discard(); p.discard(); }
    }
    @GameTest(template="empty", batch="aqua_burst", timeoutTicks=40)
    public static void realArsCompositionPreservesTargetOwnerAndPayer(GameTestHelper h) {
        var p = player(h); var owner = player(h); var victim = target(h, p); var marker = new Marker();
        try {
            var parts = new ArrayList<AbstractSpellPart>(); parts.add(MethodTouch.INSTANCE);
            for (int i = 0; i < 5; i++) parts.add(glyph(Ability.ARMOR_SHATTER));
            parts.add(glyph(Ability.ARMOR_BURST)); parts.add(marker);
            var r = resolver(h, p, new Spell(parts));
            CastContexts.bind(r.spellContext, CastContext.of(r.spellContext, null, null).withDamageOwner(owner));
            double before = CapabilityRegistry.getMana(p).getCurrentMana();
            h.assertTrue(r.onCastOnEntity(ItemStack.EMPTY, victim, InteractionHand.MAIN_HAND), "Real touch spell casts");
            h.assertTrue(victim.getHealth() == 94 && NativeAbilities.currentLevel(victim, Ability.ARMOR_SHATTER) == 5, "Preceding state effects reach the native threshold before burst");
            h.assertTrue(victim.getLastHurtByMob() == owner && marker.hits == 1 && marker.victim == victim && marker.owner == owner,
                    "Native source and following Ars effect retain the explicit damage owner and target: actualAttacker=" + victim.getLastHurtByMob()
                            + ", owner=" + owner + ", payer=" + p + ", hits=" + marker.hits + ", markerOwner=" + marker.owner + ", markerVictim=" + marker.victim);
            h.assertTrue(Math.abs(before - CapabilityRegistry.getMana(p).getCurrentMana() - r.getExpendedCost() - 220) < .001
                    && CapabilityRegistry.getMana(owner).getCurrentMana() == 1000, "Original caster pays five state activations and one burst; owner is not the payer");
            h.succeed();
        } finally { victim.discard(); p.discard(); owner.discard(); }
    }
    @GameTest(template="empty", batch="aqua_burst", timeoutTicks=40)
    public static void arsDamageCancellationPreventsPayment(GameTestHelper h) {
        var p = player(h); var victim = target(h, p); full(victim, Ability.ARMOR_BURST, p);
        Consumer<SpellDamageEvent.Pre> cancellation = event -> { if (event.target == victim) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancellation);
        try {
            float before = victim.getHealth(); resolve(h, p, victim, Ability.ARMOR_BURST);
            h.assertTrue(victim.getHealth() == before && CapabilityRegistry.getMana(p).getCurrentMana() == 1000, "Canceled Ars damage is not a paid activation"); h.succeed();
        } finally { NeoForge.EVENT_BUS.unregister(cancellation); victim.discard(); p.discard(); }
    }
    private static final class Marker extends AbstractEffect {
        int hits; LivingEntity victim, owner;
        Marker() { super(CarrierRegistry.id("aquamirae_nouveau:glyph_burst_marker"), "Marker"); }
        @Override public int getDefaultManaCost() { return 0; }
        @Override public int getCastingCost() { return 0; }
        @Override public boolean isEnabled() { return true; }
        @Override public SpellTier defaultTier() { return SpellTier.ONE; }
        @Override public SpellTier getConfigTier() { return SpellTier.ONE; }
        @Override protected Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
        @Override public void onResolve(HitResult hit, net.minecraft.world.level.Level level, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) {
            hits++; victim = (LivingEntity)((EntityHitResult)hit).getEntity(); owner = CastContext.of(context, hit, null).damageOwner();
        }
    }
}
