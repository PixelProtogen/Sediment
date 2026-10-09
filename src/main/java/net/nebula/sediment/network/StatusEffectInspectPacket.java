package net.nebula.sediment.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.nebula.sediment.common.StatusContainer;
import net.nebula.sediment.common.StatusEffectDiscovery;
import net.nebula.sediment.common.StatusEffectGameRules;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StatusEffectInspectPacket(int entityId, String effectId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatusEffectInspectPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_inspect"));

    public static final StreamCodec<FriendlyByteBuf, StatusEffectInspectPacket> STREAM_CODEC =
            StreamCodec.of(StatusEffectInspectPacket::encode, StatusEffectInspectPacket::decode);

    private static final double MAX_DISTANCE_SQR = 34.0 * 34.0;   // client uses 32, with a little slack

    public static void encode(FriendlyByteBuf buf, StatusEffectInspectPacket packet) {
        buf.writeVarInt(packet.entityId());
        buf.writeUtf(packet.effectId());
    }

    public static StatusEffectInspectPacket decode(FriendlyByteBuf buf) {
        return new StatusEffectInspectPacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(StatusEffectInspectPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = player.level().getEntity(packet.entityId());
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        if (player.distanceToSqr(living) > MAX_DISTANCE_SQR) {
            return;
        }

        boolean visible = living instanceof Player
                ? player.server.getGameRules().getBoolean(StatusEffectGameRules.DISPLAY_STATUS_EFFECT_ABOVE_PLAYERS)
                : player.server.getGameRules().getBoolean(StatusEffectGameRules.DISPLAY_STATUS_EFFECT_ABOVE_ENTITIES);
        if (!visible) {
            return;
        }

        StatusContainer container = StatusContainer.get(living.getUUID());
        if (container == null || container.getHost() != living || !container.has(packet.effectId())) {
            return;
        }

        StatusEffectDiscovery.discover(player, packet.effectId());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}