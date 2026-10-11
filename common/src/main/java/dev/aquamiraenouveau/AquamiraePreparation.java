package dev.aquamiraenouveau;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vanilla progress display; Core owns preparation, validity and release. No client-only classes. */
public final class AquamiraePreparation {
    private record Progress(UUID token, ServerPlayer player, ServerLevel world, long finish, int duration, ServerBossEvent bar) {}
    private static final Map<UUID, Progress> ACTIVE = new HashMap<>();
    private AquamiraePreparation() {}
    public static boolean visible(LivingEntity actor) { return ACTIVE.containsKey(actor.getUUID()); }
    public static void update(LivingEntity actor, UUID token, int duration, int remaining) {
        if (!(actor instanceof ServerPlayer player) || !(actor.level() instanceof ServerLevel world)) return;
        var previous = ACTIVE.get(actor.getUUID());
        if (remaining <= 0) {
            if (previous == null || !previous.token().equals(token)) return;
            close(previous);
            if (world.getGameTime() < previous.finish() && player.isAlive())
                player.displayClientMessage(Component.translatable("aquamirae_nouveau.preparation_cancelled"), true);
            return;
        }
        if (previous != null && previous.token().equals(token)) return;
        if (previous != null) close(previous);
        var bar = new ServerBossEvent(label(remaining), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        bar.setProgress(Math.min(1, remaining / (float)Math.max(1, duration))); bar.addPlayer(player);
        ACTIVE.put(actor.getUUID(), new Progress(token, player, world, world.getGameTime() + remaining, Math.max(1, duration), bar));
    }
    private static Component label(int remaining) {
        return Component.translatable("aquamirae_nouveau.preparation", String.format(java.util.Locale.ROOT, "%.2f", remaining / 20.0));
    }
    private static void close(Progress progress) {
        if (ACTIVE.remove(progress.player().getUUID(), progress)) progress.bar().removeAllPlayers();
    }
    public static void tick() {
        for (var progress : List.copyOf(ACTIVE.values())) {
            var player = progress.player();
            if (!player.isAlive() || player.isRemoved() || player.level() != progress.world() || player.isSpectator()) { close(progress); continue; }
            int remaining = (int)Math.max(0, progress.finish() - progress.world().getGameTime());
            progress.bar().setName(label(remaining)); progress.bar().setProgress(Math.min(1, remaining / (float)progress.duration()));
            if (remaining == 0) close(progress);
        }
    }
    public static void stop() { for (var progress : List.copyOf(ACTIVE.values())) close(progress); }
}
