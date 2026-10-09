package net.nebula.sediment.effects;
import net.nebula.sediment.common.*;

import net.minecraft.world.entity.LivingEntity;

public class ChargeEffect extends IStatusEffect {
    public final int DEPLETION_TICK_SPEED = 120;

    public ChargeEffect() {
        super(
                "charge", "sediment_lib:textures/status_effect/charge.png",32,16,
                StatusInfoHolder.builder("Charge")
                        .text("Empowers attack")
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
        if (host.tickCount % DEPLETION_TICK_SPEED != 0) {
            return;
        }
        this.lose(host, 1, 0);
    }
}