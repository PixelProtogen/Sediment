package net.nebula.sediment.effects;

import net.minecraft.world.entity.LivingEntity;
import net.nebula.sediment.common.*;

public class SedimentedEffect extends IStatusEffect {
    public SedimentedEffect() {
        super(
                "sedimented", "sediment_lib:textures/status_effect/sediment.png", 1, 0,
                StatusInfoHolder.builder("The Sedimented").text("").lore("Rays of radiance wash over you").build()
        );
    }

    @Override
    protected boolean expireCondition(LivingEntity host) {
        return !host.getName().getString().equals("N9bula");
    }

    @Override
    protected void expire(LivingEntity host) {
    }

    @Override
    public boolean isEntityPersistent() {
        return true;
    }
}