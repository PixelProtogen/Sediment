package net.nebula.sediment.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.nebula.sediment.StatusEffectRegistry;
import net.nebula.sediment.network.StatusEffectNetworking;
import net.nebula.sediment.network.StatusEffectSnapshot;
import net.nebula.sediment.network.StatusEffectSyncPacket;

public class StatusContainer {

    public static final String PERSIST_KEY = "sediment_effects";

    private static final Map<UUID, StatusContainer> CONTAINERS = new HashMap<>();

    private final Map<String, IStatusEffect> statusEffects = new LinkedHashMap<>();
    private final LivingEntity host;
    private final UUID hostId;
    private List<StatusEffectSnapshot> lastSent = List.of();

    private StatusContainer(LivingEntity host) {
        this.host = host;
        this.hostId = host.getUUID();
    }

    public static boolean apply(LivingEntity host, String effectId) {
        return add(host, effectId, 1, 1);
    }

    public static boolean addCount(LivingEntity host, String effectId, int amount) {
        return add(host, effectId, amount, 0);
    }

    public static boolean addPotency(LivingEntity host, String effectId, int amount) {
        return add(host, effectId, 0, amount);
    }

    private static boolean canModify(LivingEntity host, String effectId) {
        return host != null
                && effectId != null
                && !host.isRemoved()
                && !host.level().isClientSide()
                && StatusEffectRegistry.exists(effectId);
    }

    private static StatusContainer containerFor(LivingEntity host) {
        StatusContainer existing = CONTAINERS.get(host.getUUID());
        if (existing != null && existing.host != host) {
            CONTAINERS.remove(host.getUUID(), existing);
            existing = null;
        }
        if (existing == null) {
            existing = new StatusContainer(host);
            CONTAINERS.put(host.getUUID(), existing);
        }
        return existing;
    }

    public static boolean add(LivingEntity host, String effectId, int countAmount, int potencyAmount) {
        if (!canModify(host, effectId)) {
            return false;
        }
        if (countAmount <= 0 && potencyAmount <= 0) {
            return false;
        }
        StatusContainer container = containerFor(host);
        IStatusEffect target = container.obtain(effectId);
        target.gain(host, countAmount, potencyAmount);
        container.commit();
        return true;
    }

    public static boolean set(LivingEntity host, String effectId, int count, int potency) {
        if (!canModify(host, effectId)) {
            return false;
        }
        StatusContainer container = containerFor(host);
        IStatusEffect target = container.obtain(effectId);
        int wantedCount = Math.max(0, Math.min(count, target.getCount().max()));
        int wantedPotency = Math.max(0, Math.min(potency, target.getPotency().max()));
        int countDelta = wantedCount - target.getCount().get();
        int potencyDelta = wantedPotency - target.getPotency().get();
        if (countDelta > 0 || potencyDelta > 0) {
            target.gain(host, Math.max(0, countDelta), Math.max(0, potencyDelta));
        }
        if (countDelta < 0 || potencyDelta < 0) {
            target.lose(host, Math.max(0, -countDelta), Math.max(0, -potencyDelta));
        }
        container.commit();
        return true;
    }

    public static boolean max(LivingEntity host, String effectId) {
        return set(host, effectId, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public static boolean removeEffect(LivingEntity host, String effectId) {
        if (host == null || host.level().isClientSide()) {
            return false;
        }
        StatusContainer container = CONTAINERS.get(host.getUUID());
        if (container == null || container.host != host) {
            return false;
        }
        return container.remove(effectId);
    }

    public static boolean forceExpire(LivingEntity host, String effectId) {
        if (host == null || host.level().isClientSide()) {
            return false;
        }
        StatusContainer container = CONTAINERS.get(host.getUUID());
        if (container == null || container.host != host) {
            return false;
        }
        IStatusEffect effect = container.statusEffects.remove(effectId);
        if (effect == null) {
            return false;
        }
        effect.forceExpire(host);
        effect.onRemove(host);
        container.commit();
        return true;
    }

    public static boolean clearAll(LivingEntity host) {
        if (host == null || host.level().isClientSide()) {
            return false;
        }
        StatusContainer container = CONTAINERS.get(host.getUUID());
        if (container == null || container.host != host) {
            return false;
        }
        List<IStatusEffect> removed = new ArrayList<>(container.statusEffects.values());
        container.statusEffects.clear();
        for (IStatusEffect effect : removed) {
            effect.onRemove(host);
        }
        container.commit();
        return true;
    }

    @Nullable
    public static StatusContainer get(UUID entityId) {
        StatusContainer container = CONTAINERS.get(entityId);
        if (container != null && container.expireIfNeeded()) {
            return null;
        }
        return container;
    }

    public static void cleanup() {
        for (StatusContainer container : new ArrayList<>(CONTAINERS.values())) {
            container.expireIfNeeded();
        }
    }

    public static void clearRegistry() {
        CONTAINERS.clear();
    }

    public static void sendFull(ServerPlayer player, LivingEntity target) {
        StatusContainer container = CONTAINERS.get(target.getUUID());
        if (container == null || container.host != target) {
            return;
        }
        StatusEffectNetworking.sendToPlayer(player, new StatusEffectSyncPacket(target.getId(), container.snapshot()));
    }

    public static void transfer(LivingEntity from, LivingEntity to, boolean death) {
        if (to.level().isClientSide()) {
            return;
        }

        StatusContainer old = CONTAINERS.get(from.getUUID());
        if (old == null || old.host != from) {
            return;
        }
        CONTAINERS.remove(from.getUUID(), old);

        StatusContainer fresh = null;

        for (IStatusEffect effect : old.statusEffects.values()) {
            if (death && !effect.isEntityPersistent()) {
                continue;
            }
            if (fresh == null) {
                fresh = new StatusContainer(to);
            }
            fresh.statusEffects.put(effect.getId(), effect);
        }

        if (fresh == null) {
            return;
        }

        CONTAINERS.put(to.getUUID(), fresh);

        for (IStatusEffect effect : fresh.statusEffects.values()) {
            effect.onApply(to);
        }

        fresh.writePersistent();
    }

    public static void restore(LivingEntity host) {
        if (host.level().isClientSide()) {
            return;
        }

        StatusContainer existing = CONTAINERS.get(host.getUUID());
        if (existing != null) {
            if (existing.host == host) {
                return;
            }
            CONTAINERS.remove(host.getUUID(), existing);
        }

        CompoundTag data = host.getPersistentData();
        if (!data.contains(PERSIST_KEY, Tag.TAG_LIST)) {
            return;
        }

        ListTag list = data.getList(PERSIST_KEY, Tag.TAG_COMPOUND);
        StatusContainer container = null;

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString("id");

            if (!StatusEffectRegistry.exists(id)) {
                continue;
            }

            IStatusEffect effect = StatusEffectRegistry.create(id);
            if (effect == null || !effect.isWorldPersistent()) {
                continue;
            }

            effect.getCount().set(tag.getInt("count"));
            effect.getPotency().set(tag.getInt("potency"));

            if (effect.isDepleted()) {
                continue;
            }

            if (container == null) {
                container = new StatusContainer(host);
            }
            container.statusEffects.put(id, effect);
        }

        if (container == null) {
            return;
        }

        CONTAINERS.put(host.getUUID(), container);

        for (IStatusEffect effect : container.statusEffects.values()) {
            effect.onApply(host);
        }
    }

    private void writePersistent() {
        ListTag list = new ListTag();

        for (IStatusEffect effect : this.statusEffects.values()) {
            if (!effect.isWorldPersistent()) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("id", effect.getId());
            tag.putInt("count", effect.getCount().get());
            tag.putInt("potency", effect.getPotency().get());
            list.add(tag);
        }

        CompoundTag data = this.host.getPersistentData();
        if (list.isEmpty()) {
            data.remove(PERSIST_KEY);
        } else {
            data.put(PERSIST_KEY, list);
        }
    }

    private IStatusEffect obtain(String effectId) {
        IStatusEffect existing = this.statusEffects.get(effectId);
        if (existing != null) {
            return existing;
        }
        IStatusEffect created = StatusEffectRegistry.create(effectId);
        this.statusEffects.put(effectId, created);
        created.onApply(this.host);
        return created;
    }

    public void lose(String effectId, int countAmount, int potencyAmount) {
        IStatusEffect effect = this.statusEffects.get(effectId);
        if (effect == null) {
            return;
        }
        effect.lose(this.host, countAmount, potencyAmount);
        this.commit();
    }

    public boolean remove(String effectId) {
        IStatusEffect removed = this.statusEffects.remove(effectId);
        if (removed != null) {
            removed.onRemove(this.host);
        }
        this.commit();
        return removed != null;
    }

    public boolean has(String effectId) {
        return this.statusEffects.containsKey(effectId);
    }

    @Nullable
    public IStatusEffect getEffect(String effectId) {
        return this.statusEffects.get(effectId);
    }

    public void tick() {
        for (IStatusEffect effect : new ArrayList<>(this.statusEffects.values())) {
            effect.tick(this.host);
        }
        for (IStatusEffect effect : new ArrayList<>(this.statusEffects.values())) {
            if (effect.tryExpire(this.host)) {
                if (this.statusEffects.remove(effect.getId(), effect)) {
                    effect.onRemove(this.host);
                }
            }
        }
        this.commit();
    }

    public void refresh() {
        this.commit();
    }

    public void iterate(BiConsumer<UUID, IStatusEffect> function) {
        for (IStatusEffect effect : new ArrayList<>(this.statusEffects.values())) {
            function.accept(this.hostId, effect);
        }
        this.commit();
    }

    public boolean canExist() {
        return !this.host.isRemoved() && !this.statusEffects.isEmpty();
    }

    public boolean expireIfNeeded() {
        if (this.canExist()) {
            return false;
        }
        this.expire();
        return true;
    }

    public LivingEntity getHost() {
        return this.host;
    }

    public UUID getHostId() {
        return this.hostId;
    }

    public Map<String, IStatusEffect> getEffects() {
        return new LinkedHashMap<>(this.statusEffects);
    }

    private void commit() {
        List<IStatusEffect> depleted = new ArrayList<>();
        Iterator<IStatusEffect> iterator = this.statusEffects.values().iterator();
        while (iterator.hasNext()) {
            IStatusEffect effect = iterator.next();
            if (effect.isDepleted()) {
                iterator.remove();
                depleted.add(effect);
            }
        }
        for (IStatusEffect effect : depleted) {
            effect.onRemove(this.host);
        }
        if (this.expireIfNeeded()) {
            return;
        }
        this.sync();
    }

    private List<StatusEffectSnapshot> snapshot() {
        List<StatusEffectSnapshot> result = new ArrayList<>(this.statusEffects.size());
        for (IStatusEffect effect : this.statusEffects.values()) {
            result.add(new StatusEffectSnapshot(effect.getId(), effect.getCount().get(), effect.getPotency().get()));
        }
        return result;
    }

    private void sync() {
        List<StatusEffectSnapshot> snapshot = this.snapshot();
        if (snapshot.equals(this.lastSent)) {
            return;
        }
        this.lastSent = snapshot;
        this.writePersistent();

        if (this.host instanceof ServerPlayer player) {
            for (IStatusEffect effect : this.statusEffects.values()) {
                StatusEffectDiscovery.discover(player, effect.getId());
            }
        }
        StatusEffectNetworking.sendToTracking(this.host, new StatusEffectSyncPacket(this.host.getId(), snapshot));
    }

    private void expire() {
        List<IStatusEffect> removed = new ArrayList<>(this.statusEffects.values());
        this.statusEffects.clear();
        CONTAINERS.remove(this.hostId, this);
        if (!this.host.isRemoved()) {
            this.writePersistent();
        }
        if (!this.lastSent.isEmpty() && !this.host.isRemoved()) {
            StatusEffectNetworking.sendToTracking(this.host, new StatusEffectSyncPacket(this.host.getId(), List.of()));
        }
        this.lastSent = List.of();
        for (IStatusEffect effect : removed) {
            effect.onRemove(this.host);
        }
    }
}