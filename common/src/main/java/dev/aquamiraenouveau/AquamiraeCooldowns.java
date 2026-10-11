package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.ActiveCastProvider;
import dev.arsconflux.api.casting.ActiveCastRequest;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;
import java.util.List;

/** Actor-owned deadlines are gated only at Core's active-release boundary. */
public final class AquamiraeCooldowns implements ActiveCastProvider {
    private static final String DATA = "aquamirae_nouveau_cooldowns";
    private static EnumSet<Ability> used(List<Spell> spells) {
        var abilities = EnumSet.noneOf(Ability.class);
        for (var spell : spells) for (var part : ArsSpellAccess.parts(spell))
            if (part instanceof AquamiraeGlyph glyph) abilities.add(glyph.ability);
        return abilities;
    }
    private static boolean applies(LivingEntity actor) {
        return actor.level() instanceof ServerLevel && AquamiraeConfig.cooldownsEnabled()
                && (!(actor instanceof Player player) || !player.isCreative() || AquamiraeConfig.creativeCooldown());
    }
    private static long clock(LivingEntity actor) { return actor.level().getServer().overworld().getGameTime(); }
    public static long remaining(LivingEntity actor, Ability ability) {
        if (!applies(actor) || AquamiraeConfig.cooldownTicks(ability) == 0) return 0;
        return Math.max(0, actor.getPersistentData().getCompound(DATA).getLong(ability.id) - clock(actor));
    }
    @Override public boolean supports(Spell spell) { return !used(List.of(spell)).isEmpty(); }
    @Override public boolean allowed(ActiveCastRequest request) {
        for (var ability : used(List.of(request.spell()))) {
            long ticks = remaining(request.caster(), ability);
            if (ticks == 0) continue;
            if (request.caster() instanceof ServerPlayer player) player.displayClientMessage(Component.translatable(
                    "aquamirae_nouveau.cooldown", Component.translatable("aquamirae_nouveau.name." + ability.id),
                    String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0)), true);
            return false;
        }
        return true;
    }
    @Override public void released(ActiveCastRequest request, List<Spell> recipes) {
        var actor = request.caster();
        if (!applies(actor)) return;
        var data = actor.getPersistentData().getCompound(DATA);
        for (var ability : used(recipes)) {
            int ticks = AquamiraeConfig.cooldownTicks(ability);
            if (ticks > 0) data.putLong(ability.id, Math.max(data.getLong(ability.id), clock(actor) + ticks));
        }
        actor.getPersistentData().put(DATA, data);
    }
}
