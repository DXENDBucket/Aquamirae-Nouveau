package dev.aquamiraenouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.aquamiraenouveau.projectile.ChakramPayload;
import dev.obscuria.aquamirae.common.entity.projectile.MazeRose;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

/** Filter the native candidate list without reproducing the rose's homing algorithm. */
@Mixin(value = MazeRose.class, remap = false)
public abstract class RoseHomingMixin {
    @WrapOperation(method = "computeMonsterHoming", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private <T extends Entity> List<T> aquamiraeNouveau$candidates(Level world, Class<T> type, AABB bounds, Operation<List<T>> original) {
        var candidates = original.call(world, type, bounds);
        if ((Object)this instanceof ChakramPayload payload && payload.control() != null)
            return candidates.stream().filter(entity -> entity.isAlive() && !payload.control().ignores(entity)).toList();
        return candidates;
    }
}
