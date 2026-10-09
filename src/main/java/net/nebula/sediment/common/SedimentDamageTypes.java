package net.nebula.sediment.common;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.nebula.sediment.sedimentLibMod;

public final class SedimentDamageTypes {

    public static final ResourceKey<DamageType> BLEED = key("bleed");
    public static final ResourceKey<DamageType> DOOM = key("doom");
    public static final TagKey<DamageType> BYPASSES_BARRIER = TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "ignore_shield"));

    private SedimentDamageTypes() {}

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, name));
    }

    public static DamageSource bleed(Level level) {
        return source(level, BLEED, null, null);
    }

    public static DamageSource bleed(Level level, @Nullable Entity cause) {
        return source(level, BLEED, cause, cause);
    }

    public static DamageSource doom(Level level) {
        return source(level, DOOM, null, null);
    }

    public static DamageSource doom(Level level, @Nullable Entity cause) {
        return source(level, DOOM, cause, cause);
    }

    private static DamageSource source(Level level, ResourceKey<DamageType> key,
                                       @Nullable Entity direct, @Nullable Entity causing) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder, direct, causing);
    }
}