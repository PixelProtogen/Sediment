package net.nebula.sediment.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public abstract class IStatusEffect {

    private static final int DEFAULT_COUNT_MAX = 64;
    private static final int DEFAULT_POTENCY_MAX = 64;

    private final String id;
    private final ResourceLocation iconPath;
    private final DoubleConstrainedValue count;
    private final DoubleConstrainedValue potency;
    private final StatusInfoHolder info;
    private final boolean PERSISTENT = true;

    protected IStatusEffect(String id, String icon, int maxCount, int maxPotency, int minCount, int minPotency, StatusInfoHolder info) {
        this.id = id;
        this.iconPath = ResourceLocation.parse(icon);
        this.count = new DoubleConstrainedValue(minCount,maxCount);
        this.potency = new DoubleConstrainedValue(minPotency,maxPotency);
        this.info = info;
    }

    protected IStatusEffect(String id, String icon, StatusInfoHolder info) {
        this(id, icon, DEFAULT_COUNT_MAX, DEFAULT_POTENCY_MAX, info);
    }

    protected IStatusEffect(String id, String icon, int maxCount, int maxPotency, StatusInfoHolder info) {
        this(id, icon, maxCount, maxPotency, 0, 0, info);
    }

    protected abstract boolean expireCondition(LivingEntity host);

    protected abstract void expire(LivingEntity host);

    protected void onApply(LivingEntity host) {
    }

    protected void onRemove(LivingEntity host) {
    }

    protected void onTick(LivingEntity host) {
    }

    protected void onGain(LivingEntity host, int gainedCount, int gainedPotency) {
    }

    protected void onLose(LivingEntity host, int lostCount, int lostPotency) {
    }

    protected float onIncomingDamage(LivingEntity host, DamageSource source, float amount) {
        return amount;
    }

    protected void onDamageTaken(LivingEntity host, DamageSource source, float amount) {
    }

    protected float onOutgoingDamage(LivingEntity host, LivingEntity target, DamageSource source, float amount) {
        return amount;
    }

    protected void onDamageDealt(LivingEntity host, LivingEntity target, DamageSource source, float amount) {
    }

    protected void onKill(LivingEntity host, LivingEntity victim, DamageSource source) {
    }

    protected void onDeath(LivingEntity host, DamageSource source) {
    }

    protected float onHeal(LivingEntity host, float amount) {
        return amount;
    }

    protected void onJump(LivingEntity host) {
    }

    protected float onFall(LivingEntity host, float distance, float damageMultiplier) {
        return damageMultiplier;
    }

    protected float onKnockback(LivingEntity host, float strength) {
        return strength;
    }

    protected void onEquipmentChange(LivingEntity host, EquipmentSlot slot, ItemStack from, ItemStack to) {
    }

    protected boolean canUseItem(LivingEntity host, ItemStack stack) {
        return true;
    }

    public final void tick(LivingEntity host) {
        this.onTick(host);
    }

    public final void gain(LivingEntity host, int countAmount, int potencyAmount) {
        int oldCount = this.count.get();
        int oldPotency = this.potency.get();
        this.count.add(Math.max(0, countAmount));
        this.potency.add(Math.max(0, potencyAmount));
        int gainedCount = this.count.get() - oldCount;
        int gainedPotency = this.potency.get() - oldPotency;
        if (gainedCount > 0 || gainedPotency > 0) {
            this.onGain(host, gainedCount, gainedPotency);
        }
    }

    public final void lose(LivingEntity host, int countAmount, int potencyAmount) {
        int oldCount = this.count.get();
        int oldPotency = this.potency.get();
        this.count.sub(Math.max(0, countAmount));
        this.potency.sub(Math.max(0, potencyAmount));
        int lostCount = oldCount - this.count.get();
        int lostPotency = oldPotency - this.potency.get();
        if (lostCount > 0 || lostPotency > 0) {
            this.onLose(host, lostCount, lostPotency);
        }
    }

    public final boolean tryExpire(LivingEntity host) {
        if (!this.expireCondition(host)) {
            return false;
        }
        this.expire(host);
        return true;
    }

    public final void forceExpire(LivingEntity host) {
        this.expire(host);
    }

    public boolean isDepleted() {
        return this.count.get() <= 0 && this.potency.get() <= 0;
    }

    public String getId() {
        return this.id;
    }

    public ResourceLocation getIconPath() {
        return this.iconPath;
    }

    public DoubleConstrainedValue getCount() {
        return this.count;
    }

    public DoubleConstrainedValue getPotency() {
        return this.potency;
    }

    public StatusInfoHolder getInfo() {
        return this.info;
    }

    public boolean isWorldPersistent() {
        return true;
    }

    public boolean isEntityPersistent() {
        return false;
    }
}