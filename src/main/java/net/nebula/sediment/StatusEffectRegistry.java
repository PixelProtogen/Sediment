package net.nebula.sediment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.nebula.sediment.common.*;

public final class StatusEffectRegistry {

    private static final Map<String, Supplier<? extends IStatusEffect>> BUILT_IN = new HashMap<>();
    private static final Map<String, Supplier<? extends IStatusEffect>> CUSTOM = new HashMap<>();
    private static boolean frozen = false;

    private StatusEffectRegistry() {}

    static synchronized void builtIn(String id, Supplier<? extends IStatusEffect> factory) {
        if (frozen) {
            throw new IllegalStateException("Built-in status effects are frozen, cannot register: " + id);
        }
        validate(id, factory);
        if (BUILT_IN.containsKey(id)) {
            throw new IllegalArgumentException("Built-in status effect already registered: " + id);
        }
        BUILT_IN.put(id, factory);
    }

    static synchronized void freeze() {
        frozen = true;
    }

    public static synchronized void register(String id, Supplier<? extends IStatusEffect> factory) {
        if (!frozen) {
            throw new IllegalStateException("Status effect registry is not ready yet, cannot register: " + id);
        }
        validate(id, factory);
        if (BUILT_IN.containsKey(id)) {
            throw new IllegalArgumentException("Cannot override built-in status effect: " + id);
        }
        if (CUSTOM.containsKey(id)) {
            throw new IllegalArgumentException("Custom status effect already registered: " + id);
        }
        CUSTOM.put(id, factory);
    }

    public static synchronized boolean exists(String id) {
        return BUILT_IN.containsKey(id) || CUSTOM.containsKey(id);
    }

    public static synchronized boolean isBuiltIn(String id) {
        return BUILT_IN.containsKey(id);
    }

    @Nullable
    public static synchronized IStatusEffect create(String id) {
        Supplier<? extends IStatusEffect> factory = BUILT_IN.get(id);
        if (factory == null) {
            factory = CUSTOM.get(id);
        }
        return factory != null ? factory.get() : null;
    }

    public static synchronized Set<String> ids() {
        Set<String> result = new HashSet<>(BUILT_IN.keySet());
        result.addAll(CUSTOM.keySet());
        return result;
    }

    private static void validate(String id, Supplier<? extends IStatusEffect> factory) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Status effect id cannot be empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Status effect factory cannot be null: " + id);
        }
        IStatusEffect sample = factory.get();
        if (sample == null || !id.equals(sample.getId())) {
            throw new IllegalArgumentException("Factory must produce an effect with id: " + id);
        }
    }
}