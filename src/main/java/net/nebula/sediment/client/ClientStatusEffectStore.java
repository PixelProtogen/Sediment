package net.nebula.sediment.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.nebula.sediment.network.StatusEffectSnapshot;

public final class ClientStatusEffectStore {

    private static final Map<Integer, List<StatusEffectSnapshot>> ENTITIES = new HashMap<>();

    private ClientStatusEffectStore() {
    }

    public static void set(int entityId, List<StatusEffectSnapshot> entries) {
        if (entries.isEmpty()) {
            ENTITIES.remove(entityId);
            return;
        }
        ENTITIES.put(entityId, List.copyOf(entries));
    }

    public static List<StatusEffectSnapshot> get(int entityId) {
        return ENTITIES.getOrDefault(entityId, List.of());
    }

    public static List<StatusEffectSnapshot> get(Entity entity) {
        return get(entity.getId());
    }

    public static void remove(int entityId) {
        ENTITIES.remove(entityId);
    }

    public static void clear() {
        ENTITIES.clear();
    }
}