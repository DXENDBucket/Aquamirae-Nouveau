package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.validation.SpellPhraseValidator;
import dev.arsconflux.api.projectile.CarrierRegistry;
import dev.arsconflux.api.projectile.ProjectileForm;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import java.util.List;

/** Allow shard power modifiers without granting ineffective Amplify to ordinary Ars or our weapons. */
public final class AquamiraePayloadRules {
    private AquamiraePayloadRules() {}
    public static List<SpellValidationError> validate(List<AbstractSpellPart> recipe) {
        var errors = new ArrayList<SpellValidationError>();
        for (var phrase : SpellPhraseValidator.splitSpellIntoPhrases(recipe)) {
            var profile = CarrierRegistry.of(phrase.getAction());
            if (profile == null || !phrase.getAugments().contains(AugmentAmplify.INSTANCE)) continue;
            var payload = phrase.getAugments().stream().filter(ProjectileForm.class::isInstance).findFirst().orElse(null);
            if (payload != null && !(payload instanceof AquamiraeSpell)) continue;
            boolean allowed = payload instanceof AquamiraeSpell own ? own.ability() == Ability.ABYSSAL_SHARD : profile.originallySupported(AugmentAmplify.INSTANCE);
            if (!allowed) errors.add(new Error(phrase.getFirstPosition(), phrase.getAction()));
        }
        return errors;
    }
    private record Error(int position, AbstractSpellPart spellPart) implements SpellValidationError {
        public int getPosition() { return position; }
        public AbstractSpellPart getSpellPart() { return spellPart; }
        public MutableComponent makeTextComponentExisting() { return Component.translatable("aquamirae_nouveau.validation.amplify"); }
        public MutableComponent makeTextComponentAdding() { return makeTextComponentExisting(); }
    }
}
