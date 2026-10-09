package net.nebula.sediment.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

@EventBusSubscriber(modid = sedimentLibMod.MODID)
public final class StatusEffectHooks {

    @FunctionalInterface
    private interface FloatHook {
        float apply(IStatusEffect effect, float value);
    }

    @FunctionalInterface
    private interface VoidHook {
        void run(IStatusEffect effect);
    }

    private StatusEffectHooks() {
    }

    private static StatusContainer containerOf(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return null;
        }
        StatusContainer container = StatusContainer.get(entity.getUUID());
        return container != null && container.getHost() == entity ? container : null;
    }

    private static void each(LivingEntity host, VoidHook hook) {
        StatusContainer container = containerOf(host);
        if (container == null) {
            return;
        }
        for (IStatusEffect effect : container.getEffects().values()) {
            hook.run(effect);
        }
        container.refresh();
    }

    private static float chain(LivingEntity host, float value, FloatHook hook) {
        StatusContainer container = containerOf(host);
        if (container == null) {
            return value;
        }
        for (IStatusEffect effect : container.getEffects().values()) {
            if (value <= 0.0F) {
                break;
            }
            value = hook.apply(effect, value);
        }
        container.refresh();
        return value;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        float original = event.getAmount();
        float amount = original;

        if (source.getEntity() instanceof LivingEntity attacker) {
            amount = chain(attacker, amount, (fx, v) -> fx.onOutgoingDamage(attacker, victim, source, v));
        }

        final float afterOutgoing = amount;
        if (afterOutgoing > 0.0F) {
            amount = chain(victim, afterOutgoing, (fx, v) -> fx.onIncomingDamage(victim, source, v));
        }

        if (amount != original) {
            if (amount <= 0.0F) {
                event.setCanceled(true);
            } else {
                event.setAmount(amount);
            }
        }
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        float damage = event.getNewDamage();

        each(victim, fx -> fx.onDamageTaken(victim, source, damage));

        if (source.getEntity() instanceof LivingEntity attacker) {
            each(attacker, fx -> fx.onDamageDealt(attacker, victim, source, damage));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        each(victim, fx -> fx.onDeath(victim, source));

        if (source.getEntity() instanceof LivingEntity killer) {
            each(killer, fx -> fx.onKill(killer, victim, source));
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity host = event.getEntity();
        float original = event.getAmount();
        float amount = chain(host, original, (fx, v) -> fx.onHeal(host, v));
        if (amount != original) {
            if (amount <= 0.0F) {
                event.setCanceled(true);
            } else {
                event.setAmount(amount);
            }
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity host = event.getEntity();
        each(host, fx -> fx.onJump(host));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity host = event.getEntity();
        float distance = event.getDistance();
        float original = event.getDamageMultiplier();
        float multiplier = chain(host, original, (fx, v) -> fx.onFall(host, distance, v));
        if (multiplier != original) {
            event.setDamageMultiplier(Math.max(0.0F, multiplier));
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity host = event.getEntity();
        float original = event.getStrength();
        float strength = chain(host, original, (fx, v) -> fx.onKnockback(host, v));
        if (strength != original) {
            if (strength <= 0.0F) {
                event.setCanceled(true);
            } else {
                event.setStrength(strength);
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity host = event.getEntity();
        each(host, fx -> fx.onEquipmentChange(host, event.getSlot(), event.getFrom(), event.getTo()));
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        LivingEntity host = event.getEntity();
        StatusContainer container = containerOf(host);
        if (container == null) {
            return;
        }
        for (IStatusEffect effect : container.getEffects().values()) {
            if (!effect.canUseItem(host, event.getItem())) {
                event.setCanceled(true);
                break;
            }
        }
    }
}