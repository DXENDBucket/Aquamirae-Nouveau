package dev.aquamiraenouveau;

public enum Ability {
    ARMOR_SHATTER("armor_shatter", "Armor Shatter", 40, 240, 5, true, 2),
    HEALTH_SHATTER("health_shatter", "Health Shatter", 60, 240, 10, true, 3),
    DEPTHS_FURY("fury_of_the_depths", "Fury of the Depths", 60, 600, 5, false, 5),
    DEPTHS_RESILIENCE("resilience_of_the_depths", "Resilience of the Depths", 60, 600, 5, false, 5),
    ABYSSAL_SHARD("abyssal_shard", "Abyssal Shard", 80, 0, 1, false, 2),
    SCYPHOID_STING("scyphoid_sting", "Scyphoid Sting", 40, 60, 5, false, 3),
    TERRIBLE_CHAKRAM("terrible_chakram", "Terrible Chakram", 60, 0, 1, false, 3),
    MAZE_ROSE("maze_rose", "Maze Rose", 70, 0, 1, false, 3),
    ARMOR_BURST("armor_burst", "Armor Burst", 60, 0, 1, false, 3),
    HEALTH_BURST("health_burst", "Health Burst", 60, 0, 1, false, 3);

    public final String id, name;
    public final int mana, duration, maxLevel, cooldownSeconds;
    public final boolean stacking;

    Ability(String id, String name, int mana, int duration, int maxLevel, boolean stacking, int cooldownSeconds) {
        this.id = id;
        this.name = name;
        this.mana = mana;
        this.duration = duration;
        this.maxLevel = maxLevel;
        this.stacking = stacking;
        this.cooldownSeconds = cooldownSeconds;
    }
}
