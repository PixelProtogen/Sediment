package net.nebula.sediment.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.nebula.sediment.network.StatusEffectDefinition;

public final class ClientStatusEffectDefinitions {

    private static final Map<String, StatusEffectDefinition> DEFINITIONS = new HashMap<>();

    private ClientStatusEffectDefinitions() {
    }

    public static void set(List<StatusEffectDefinition> definitions) {
        DEFINITIONS.clear();
        for (StatusEffectDefinition def : definitions) {
            DEFINITIONS.put(def.id(), def);
        }
    }

    @Nullable
    public static StatusEffectDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static void clear() {
        DEFINITIONS.clear();
    }

    public static java.util.Collection<StatusEffectDefinition> all() {
        return java.util.List.copyOf(DEFINITIONS.values());
    }
}