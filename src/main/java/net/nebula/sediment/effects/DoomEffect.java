package net.nebula.sediment.effects;
import net.nebula.sediment.common.*;

import net.minecraft.world.entity.LivingEntity;

public class DoomEffect extends IStatusEffect {
    public final int DEPLETION_TICK_SPEED = 60;

    public DoomEffect() {
        super(
                "doom", "sediment_lib:textures/status_effect/doom.png",99,0,0,0,
                StatusInfoHolder.builder("Doom")
                        .text("Unobservable")
                        .lore("Something bad creeps over you...")
                        .build()
        );
    }

    @Override
    protected boolean expireCondition(LivingEntity host) {
        return this.getCount().get() <= 0;
    }

    @Override
    protected void expire(LivingEntity host) {
        host.invulnerableTime = 0;
        host.hurt(SedimentDamageTypes.doom(host.level()), host.getMaxHealth() + 1);
    }

    @Override
    protected void onTick(LivingEntity host) {
        if (host.tickCount % DEPLETION_TICK_SPEED != 0) {
            return;
        }
        this.lose(host, 1, 0);
    }
}