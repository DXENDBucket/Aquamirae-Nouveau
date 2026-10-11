package dev.aquamiraenouveau;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import dev.arsconflux.api.resource.ArsManaAccount;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.aquamiraenouveau.platform.AquamiraeConfig;
import java.util.List;

public final class AquamiraeCosts {
    public static ResourceCharge quote(SpellContext context, Ability ability, List<AbstractAugment> augments) {
        long extra = augments.stream().filter(a -> a == AugmentAmplify.INSTANCE).count() * AquamiraeConfig.amplifyCost();
        return new ResourceCharge(new ArsManaAccount(context.getCaster()),
                (int) Math.min(Integer.MAX_VALUE, AquamiraeConfig.cost(ability) + extra));
    }
    private AquamiraeCosts() {}
}
