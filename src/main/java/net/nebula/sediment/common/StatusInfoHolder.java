package net.nebula.sediment.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class StatusInfoHolder {
    public record Segment(@Nullable String text, @Nullable String reference, boolean translatable) {}

    private final Component name;
    private final List<Segment> description;
    private final String lore;
    private final boolean loreTranslatable;

    private StatusInfoHolder(String name, List<Segment> description, String lore, boolean loreTranslatable) {
        this.name = Component.literal(name);
        this.description = List.copyOf(description);
        this.lore = lore;
        this.loreTranslatable = loreTranslatable;
    }

    public static StatusInfoHolder of(String name, List<Segment> description, String lore, boolean loreTranslatable) {
        return new StatusInfoHolder(name, description, lore, loreTranslatable);
    }

    public static StatusInfoHolder simple(String name) {
        return builder(name).build();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public Component getName() {
        return this.name;
    }

    public Component getLore() {
        return this.loreTranslatable ? Component.translatable(this.lore) : Component.literal(this.lore);
    }

    public String getLoreValue() {
        return this.lore;
    }

    public boolean isLoreTranslatable() {
        return this.loreTranslatable;
    }

    public List<Segment> getSegments() {
        return this.description;
    }

    public Component getDescription() {
        return this.getDescription(id -> null);
    }

    public Component getDescription(Function<String, Component> resolver) {
        MutableComponent result = Component.empty();
        for (Segment segment : this.description) {
            if (segment.text() != null) {
                result.append(segment.translatable()
                        ? Component.translatable(segment.text())
                        : Component.literal(segment.text()));
                continue;
            }
            String reference = segment.reference();
            Component resolved = resolver.apply(reference);
            MutableComponent part = resolved != null ? resolved.copy() : Component.literal(reference);
            result.append(part.withStyle(ChatFormatting.GOLD));
        }
        return result;
    }

    public List<String> getReferences() {
        List<String> references = new ArrayList<>();
        for (Segment segment : this.description) {
            if (segment.reference() != null) {
                references.add(segment.reference());
            }
        }
        return references;
    }

    public static class Builder {

        private final String name;
        private final List<Segment> description = new ArrayList<>();
        private String lore = "";
        private boolean loreTranslatable = false;

        private Builder(String name) {
            this.name = name;
        }

        public Builder text(String text) {
            this.description.add(new Segment(text, null, false));
            return this;
        }

        public Builder textTranslatable(String key) {
            this.description.add(new Segment(key, null, true));
            return this;
        }

        public Builder ref(String id) {
            this.description.add(new Segment(null, id, false));
            return this;
        }

        public Builder lore(String lore) {
            this.lore = lore;
            this.loreTranslatable = false;
            return this;
        }

        public Builder loreTranslatable(String key) {
            this.lore = key;
            this.loreTranslatable = true;
            return this;
        }

        public StatusInfoHolder build() {
            return new StatusInfoHolder(this.name, this.description, this.lore, this.loreTranslatable);
        }
    }
}