package net.nebula.sediment.network;

import net.minecraft.resources.ResourceLocation;
import net.nebula.sediment.common.IStatusEffect;
import net.nebula.sediment.common.StatusInfoHolder;

public record StatusEffectDefinition(
        String id,
        ResourceLocation icon,
        int maxCount,
        int maxPotency,
        StatusInfoHolder info
) {

    public static StatusEffectDefinition from(IStatusEffect effect) {
        return new StatusEffectDefinition(
                effect.getId(),
                effect.getIconPath(),
                effect.getCount().max(),
                effect.getPotency().max(),
                effect.getInfo());
    }

    public boolean showCount() {
        return maxCount > 1;
    }

    public boolean showPotency() {
        return maxPotency > 0;
    }
}