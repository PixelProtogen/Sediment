package net.nebula.sediment.effects;

import net.minecraft.world.entity.LivingEntity;
import net.nebula.sediment.common.*;

public class BarrierConvertionEffect extends IStatusEffect {

    private static final String CHARGE_ID = "charge";
    private static final String BARRIER_ID = "barrier";

    private static final int MAX_CHARGE_COUNT = 16;
    private static final int MAX_CHARGE_POTENCY = 8;

    private boolean converted = false;

    public BarrierConvertionEffect() {
        super(
                "barrier_generate", "sediment_lib:textures/status_effect/barrier_generate.png", 1,0,0, 0,
                StatusInfoHolder.builder("Convertion: Shield")
                        .text("Consume ").ref("charge").text(" (Count: 16, Potency: 8), convert into ( (X/3 round down) * (Y/4 round up) ) ").ref("barrier").text(". Then expire.")
                        .lore("")
                        .build()
        );
    }

    @Override
    protected boolean expireCondition(LivingEntity host) {
        return this.converted;
    }

    @Override
    protected void expire(LivingEntity host) {
    }

    @Override
    protected void onTick(LivingEntity host) {
        if (this.converted) {
            return;
        }
        this.converted = true;

        StatusContainer container = StatusContainer.get(host.getUUID());
        if (container == null || container.getHost() != host) {
            return;
        }

        IStatusEffect charge = container.getEffect(CHARGE_ID);
        if (charge == null) {
            return;
        }

        int x = Math.min(MAX_CHARGE_COUNT, charge.getCount().get());
        int y = Math.min(MAX_CHARGE_POTENCY, charge.getPotency().get());

        charge.lose(host, x, y);

        int barrier = (x / 3) * ((y + 3) / 4);   // (X/3 rounded down) * (Y/4 rounded up)

        if (barrier > 0) {
            StatusContainer.add(host, BARRIER_ID, barrier, 0);
        }
    }
}