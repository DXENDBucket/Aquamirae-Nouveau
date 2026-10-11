package dev.aquamiraenouveau;

import dev.arsconflux.api.context.CastContext;
import dev.aquamiraenouveau.platform.NativeAbilities;
import dev.obscuria.aquamirae.common.entity.projectile.AbyssalShard;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("aquamirae_nouveau") @PrefixGameTestTemplate(false)
public final class ShardGameTests {
    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void nativeOwnershipScaleSplitAndExpiry(GameTestHelper h) {
        var base = h.absoluteVec(new Vec3(2, 0, 2));
        var point = new Vec3(base.x, h.getLevel().getMaxBuildHeight() - 16, base.z);
        var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ShardTester"));
        p.setPos(point);
        var area = new AABB(point, point).inflate(12);
        try {
            var frame = CastContext.detached(p, new EntityHitResult(p, point), null).withIncomingDirection(new Vec3(1, 0, 0));
            h.assertTrue(NativeAbilities.launchShard(h.getLevel(), frame, 1), "Native entity was added");
            var shard = h.getLevel().getEntitiesOfClass(AbyssalShard.class, area).stream().filter(e -> e.getOwner() == p).findFirst().orElseThrow();
            h.assertTrue(shard.getOwner() == p, "Original caster retains native ownership");
            h.assertTrue(Math.abs(shard.getY()-p.getEyeY()) < .001, "Self launches from eye height rather than colliding with the floor");
            h.assertTrue(Math.abs(shard.getShardScale()-1.2) < .001 && Math.abs(shard.getDamage(p)-12) < .001, "Amplification uses native scale and damage");
            shard.tickCount = 201;
            shard.tick();
            h.assertTrue(shard.isRemoved(), "Native 200-tick lifetime is retained");
            var children = h.getLevel().getEntitiesOfClass(AbyssalShard.class, area).stream().filter(e -> e.getOwner() == p && !e.isRemoved()).toList();
            h.assertTrue(children.size() == 3, "Native break creates exactly three children");
            for (var child : children) {
                h.assertTrue(Math.abs(child.getShardScale()-.5) < .001, "Native child scale is retained");
                child.tickCount=201; child.tick();
                h.assertTrue(child.isRemoved(), "Native child lifetime expires without splitting again");
            }
            h.assertTrue(h.getLevel().getEntitiesOfClass(AbyssalShard.class, area).stream().noneMatch(e -> e.getOwner() == p && !e.isRemoved()), "No live shard remains after native expiry");
            h.succeed();
        } finally {
            h.getLevel().getEntitiesOfClass(AbyssalShard.class, area).stream().filter(e -> e.getOwner() == p).forEach(e -> e.discard());
            p.discard();
        }
    }

    @GameTest(template="empty", batch="aqua_shard", timeoutTicks=40)
    public static void nativeCollisionTargetsHostilesAndAppliesWeakness(GameTestHelper h) {
        var base = h.absoluteVec(new Vec3(2, 0, 2));
        var point = new Vec3(base.x, h.getLevel().getMaxBuildHeight() - 16, base.z);
        var p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ShardTester"));
        p.setPos(point.add(-4, 0, 0));
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.setNoAi(true); zombie.setNoGravity(true); zombie.setPos(point.add(2, 0, 0));
        h.getLevel().addFreshEntity(zombie);
        var area = new AABB(point, point).inflate(12);
        try {
            var hit = new net.minecraft.world.phys.BlockHitResult(point.add(0, 1, 0), net.minecraft.core.Direction.UP, net.minecraft.core.BlockPos.containing(point), false);
            var frame = CastContext.detached(p, hit, null).withIncomingDirection(new Vec3(1, 0, 0));
            h.assertTrue(NativeAbilities.launchShard(h.getLevel(), frame, 0), "Native shard launches");
            var shard = h.getLevel().getEntitiesOfClass(AbyssalShard.class, area).stream().filter(e -> e.getOwner() == p).findFirst().orElseThrow();
            // Observe actual server ticks after the newly added target enters entity tracking.
            h.runAfterDelay(8, () -> {
              try {
            h.assertTrue(zombie.getHealth() < zombie.getMaxHealth(), "Native collision deals damage to a monster");
            h.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS), "Native post-hit Weakness is retained");
            h.assertTrue(shard.isRemoved(), "Native collision breaks primary shard");
            h.succeed();
              } finally {
                zombie.discard();
                h.getLevel().getEntitiesOfClass(AbyssalShard.class, area).stream().filter(e -> e.getOwner() == p).forEach(e -> e.discard());
                p.discard();
              }
            });
        } catch (RuntimeException error) {
            zombie.discard();
            h.getLevel().getEntitiesOfClass(AbyssalShard.class, area).stream().filter(e -> e.getOwner() == p).forEach(e -> e.discard());
            p.discard();
            throw error;
        }
    }
}
