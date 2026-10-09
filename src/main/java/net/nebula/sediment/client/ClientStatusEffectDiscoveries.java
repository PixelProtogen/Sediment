package net.nebula.sediment.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.nebula.sediment.network.StatusEffectInspectPacket;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClientStatusEffectDiscoveries {

    private static final long RETRY_MS = 2000L;

    private static final Set<String> DISCOVERED = new HashSet<>();
    private static final Map<String, Long> PENDING = new HashMap<>();

    private ClientStatusEffectDiscoveries() {
    }

    public static void set(List<String> ids) {
        DISCOVERED.clear();
        DISCOVERED.addAll(ids);
        PENDING.clear();
    }

    public static boolean has(String effectId) {
        return DISCOVERED.contains(effectId);
    }

    public static Set<String> all() {
        return Set.copyOf(DISCOVERED);
    }

    public static void reportInspected(int entityId, String effectId) {
        if (DISCOVERED.contains(effectId)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = PENDING.get(effectId);
        if (last != null && now - last < RETRY_MS) {
            return;
        }

        PENDING.put(effectId, now);
        PacketDistributor.sendToServer(new StatusEffectInspectPacket(entityId, effectId));
    }
}