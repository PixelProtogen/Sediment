package net.nebula.sediment.effects;
import net.nebula.sediment.common.*;

import net.minecraft.world.entity.LivingEntity;

public class BleedEffect extends IStatusEffect {

    public BleedEffect() {
        super(
                "bleed", "sediment_lib:textures/status_effect/bleed.png",64,64,0,1,
                StatusInfoHolder.builder("Bleed")
                        .text("Every second, take damage equal to potency, then lose 1 count.")
                        .lore("")
                        .build()
        );
    }

    @Override
    protected boolean expireCondition(LivingEntity host) {
        return this.getCount().get() <= 0;
    }

    @Override
    protected void expire(LivingEntity host) {
    }

    @Override
    protected void onTick(LivingEntity host) {
        if (host.tickCount % 20 != 0) {
            return;
        }
        int damage = this.getPotency().get();
        if (damage > 0) {
            host.invulnerableTime = 0;
            host.hurt(SedimentDamageTypes.bleed(host.level()), damage);
        }
        this.lose(host, 1, 0);
    }
}