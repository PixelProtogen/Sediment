package net.nebula.sediment.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.nebula.sediment.common.StatusEffectDiscovery;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.nebula.sediment.common.StatusEffectGameRules;

import java.util.List;

public final class StatusEffectNetworking {

    private static final String PROTOCOL = "8";

    private StatusEffectNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(
                StatusEffectSyncPacket.TYPE,
                StatusEffectSyncPacket.STREAM_CODEC,
                StatusEffectSyncPacket::handle);
        registrar.playToClient(
                StatusEffectRulesPacket.TYPE,
                StatusEffectRulesPacket.STREAM_CODEC,
                StatusEffectRulesPacket::handle);
        registrar.playToClient(
                StatusEffectDefinitionsPacket.TYPE,
                StatusEffectDefinitionsPacket.STREAM_CODEC,
                StatusEffectDefinitionsPacket::handle);
        registrar.playToServer(
                StatusEffectDiscoverPacket.TYPE,
                StatusEffectDiscoverPacket.STREAM_CODEC,
                StatusEffectDiscoverPacket::handle);
        registrar.playToClient(
                StatusEffectDiscoveriesPacket.TYPE,
                StatusEffectDiscoveriesPacket.STREAM_CODEC,
                StatusEffectDiscoveriesPacket::handle);
        registrar.playToServer(
                StatusEffectInspectPacket.TYPE,
                StatusEffectInspectPacket.STREAM_CODEC,
                StatusEffectInspectPacket::handle);
        registrar.playToClient(
                StatusEffectDiscoveredPacket.TYPE,
                StatusEffectDiscoveredPacket.STREAM_CODEC,
                StatusEffectDiscoveredPacket::handle);
        registrar.playToClient(
                StatusEffectBookPacket.TYPE,
                StatusEffectBookPacket.STREAM_CODEC,
                StatusEffectBookPacket::handle);
    }

    public static void sendToTracking(LivingEntity entity, StatusEffectSyncPacket packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
    }

    public static void sendToPlayer(ServerPlayer player, StatusEffectSyncPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendRulesToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, StatusEffectGameRules.packet(player.server));
    }

    public static void sendRulesToAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(StatusEffectGameRules.packet(server));
    }

    public static void sendDefinitionsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, StatusEffectDefinitionsPacket.fromRegistry());
    }

    public static void sendDiscoveriesToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StatusEffectDiscoveriesPacket(List.copyOf(StatusEffectDiscovery.get(player))));
    }

    public static void sendDiscoveredToast(ServerPlayer player, String effectId) {
        PacketDistributor.sendToPlayer(player, new StatusEffectDiscoveredPacket(effectId));
    }

    public static void sendBookState(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StatusEffectBookPacket(StatusEffectDiscovery.isBookUnlocked(player)));
    }
}