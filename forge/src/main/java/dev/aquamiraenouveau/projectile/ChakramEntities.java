package dev.aquamiraenouveau.projectile;

import dev.aquamiraenouveau.Ability;
import dev.aquamiraenouveau.AquamiraeNouveau;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ChakramEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AquamiraeNouveau.MOD_ID);
    public static final RegistryObject<EntityType<SpellTerribleChakram>> TERRIBLE = TYPES.register("spell_terrible_chakram",
            () -> EntityType.Builder.<SpellTerribleChakram>of(SpellTerribleChakram::new, MobCategory.MISC)
                    .sized(1.2f, .6f).clientTrackingRange(64).updateInterval(1).build("aquamirae_nouveau:spell_terrible_chakram"));
    public static final RegistryObject<EntityType<SpellMazeRose>> ROSE = TYPES.register("spell_maze_rose",
            () -> EntityType.Builder.<SpellMazeRose>of(SpellMazeRose::new, MobCategory.MISC)
                    .sized(1.2f, .6f).clientTrackingRange(64).updateInterval(1).build("aquamirae_nouveau:spell_maze_rose"));
    public static ChakramPayload create(Ability ability, Level world) {
        return switch (ability) {
            case TERRIBLE_CHAKRAM -> new SpellTerribleChakram(TERRIBLE.get(), world);
            case MAZE_ROSE -> new SpellMazeRose(ROSE.get(), world);
            default -> throw new IllegalArgumentException("Not a native chakram: " + ability);
        };
    }
    private ChakramEntities() {}
}
