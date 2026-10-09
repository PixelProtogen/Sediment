package net.nebula.sediment.common;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.nebula.sediment.StatusEffectRegistry;
import net.nebula.sediment.network.StatusEffectNetworking;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class StatusEffectDiscovery {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, sedimentLibMod.MODID);
    private static final Codec<Set<String>> CODEC = Codec.STRING.listOf().xmap(list -> (Set<String>) new HashSet<>(list), set -> List.copyOf(set));
    public static final Supplier<AttachmentType<Boolean>> BOOK_UNLOCKED = ATTACHMENTS.register("book_unlocked", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    public static boolean isBookUnlocked(ServerPlayer player) {
        return player.getData(BOOK_UNLOCKED);
    }

    public static void unlockBook(ServerPlayer player) {
        player.setData(BOOK_UNLOCKED, true);
        StatusEffectNetworking.sendBookState(player);
    }

    public static final Supplier<AttachmentType<Set<String>>> DISCOVERED = ATTACHMENTS.register("discovered_effects", () -> AttachmentType.<Set<String>>builder(() -> new HashSet<>()).serialize(CODEC).copyOnDeath().build());

    private StatusEffectDiscovery() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    public static boolean has(ServerPlayer player, String effectId) {
        return player.getData(DISCOVERED).contains(effectId);
    }

    private static boolean bookEnabled(MinecraftServer server) {
        return server.getGameRules().getBoolean(StatusEffectGameRules.STATUS_EFFECT_BOOK_ENABLED);
    }

    public static Set<String> get(ServerPlayer player) {
        return Set.copyOf(player.getData(DISCOVERED));
    }

    public static boolean discover(ServerPlayer player, String effectId) {
        if (!StatusEffectRegistry.exists(effectId)) {
            return false;
        }

        Set<String> set = player.getData(DISCOVERED);
        if (!set.add(effectId)) {
            return false;
        }

        player.setData(DISCOVERED, set);
        StatusEffectNetworking.sendDiscoveriesToPlayer(player);
        if (bookEnabled(player.server))
            StatusEffectNetworking.sendDiscoveredToast(player, effectId);
        return true;
    }
}