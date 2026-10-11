package dev.aquamiraenouveau;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Visible, dispellable lifetime marker. The bounded Conflux session owns the action. */
public final class CreatureAuraEffect extends MobEffect {
    public CreatureAuraEffect(int color) { super(MobEffectCategory.BENEFICIAL, color); }
}
