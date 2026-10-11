package dev.aquamiraenouveau.platform;

import dev.aquamiraenouveau.Ability;
import java.util.EnumMap;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class AquamiraeConfig {
    public static final ModConfigSpec SPEC;
    private static final EnumMap<Ability, ModConfigSpec.IntValue> COSTS = new EnumMap<>(Ability.class);
    private static final EnumMap<Ability, ModConfigSpec.IntValue> DURATIONS = new EnumMap<>(Ability.class);
    private static final EnumMap<Ability, ModConfigSpec.IntValue> COOLDOWNS = new EnumMap<>(Ability.class);
    private static final EnumMap<Ability, ModConfigSpec.IntValue> UPKEEP = new EnumMap<>(Ability.class);
    private static final ModConfigSpec.IntValue AMPLIFY, EXTEND;
    private static final ModConfigSpec.DoubleValue BITE_DAMAGE;
    private static final ModConfigSpec.BooleanValue COOLDOWN_ENABLED, CREATIVE_COOLDOWN;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("mana");
        AMPLIFY = builder.comment("Additional mana per written Amplify augment, paid on each successful activation.")
                .defineInRange("amplify", 10, 0, 1000000);
        for (var ability : Ability.values()) if (NativeAbilities.supports(ability))
            COSTS.put(ability, builder.defineInRange(ability.id, ability.mana, 0, 1000000));
        builder.pop().push("duration");
        EXTEND = builder.comment("Ticks added per Extend Time augment to status effects and granted creature actions.")
                .defineInRange("extendTicks", 200, 0, 72000);
        for (var ability : Ability.values()) if (NativeAbilities.supports(ability) && ability.duration > 0)
            DURATIONS.put(ability, builder.defineInRange(ability.id, ability.duration, 1, 72000));
        builder.pop().push("creatureActions");
        BITE_DAMAGE = builder.comment("Siphoning Bite base damage; the native eel's default attack damage is 8.")
                .defineInRange("biteDamage", 8.0, 0.0, 1000000.0);
        UPKEEP.put(Ability.EEL_ROAR, builder.comment("Mana prepaid per additional second of Eel Roar.").defineInRange("roarUpkeep", 4, 0, 1000000));
        UPKEEP.put(Ability.CORNELIA_VORTEX, builder.comment("Mana prepaid per additional second of Cornelia's Vortex.").defineInRange("vortexUpkeep", 8, 0, 1000000));
        builder.pop().push("cooldowns");
        COOLDOWN_ENABLED = builder.comment("Gate new active casts through Ars Conflux. Ongoing branches are unaffected.").define("enabled", true);
        CREATIVE_COOLDOWN = builder.define("creativeCooldown", false);
        builder.push("seconds");
        for (var ability : Ability.values()) if (NativeAbilities.supports(ability))
            COOLDOWNS.put(ability, builder.defineInRange(ability.id, ability.cooldownSeconds, 0, 3600));
        builder.pop().pop();
        SPEC = builder.build();
    }
    public static int cost(Ability ability) { return COSTS.get(ability).get(); }
    public static double biteDamage() { return BITE_DAMAGE.get(); }
    public static int upkeep(Ability ability) { return UPKEEP.get(ability).get(); }
    public static int amplifyCost() { return AMPLIFY.get(); }
    public static int duration(Ability ability) { return DURATIONS.get(ability).get(); }
    public static int extendTicks() { return EXTEND.get(); }
    public static boolean cooldownsEnabled() { return COOLDOWN_ENABLED.get(); }
    public static boolean creativeCooldown() { return CREATIVE_COOLDOWN.get(); }
    public static int cooldownTicks(Ability ability) { return COOLDOWNS.get(ability).get() * 20; }
    private AquamiraeConfig() {}
}
