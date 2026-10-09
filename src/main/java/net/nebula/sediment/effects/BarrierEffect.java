package net.nebula.sediment.effects;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.nebula.sediment.common.*;

public class BarrierEffect extends IStatusEffect {
    public BarrierEffect() {
        super(
                "barrier", "sediment_lib:textures/status_effect/barrier.png", 9999, 0,
                StatusInfoHolder.builder("Shield")
                        .text("Consume X shield based on incoming damage, then reduce that damage based on consumed count. Can be gained via ").ref("barrier_generate")
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
    protected float onIncomingDamage(LivingEntity host, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(SedimentDamageTypes.BYPASSES_BARRIER)) {
            return amount;
        }

        float absorbed = Math.min(amount, this.getCount().get());
        int consumed = (int) Math.ceil(absorbed);

        if (consumed > 0) {
            this.lose(host, consumed, 0);
        }
        return amount - absorbed;
    }
}