package net.nebula.sediment.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nebula.sediment.client.ClientStatusEffectRules;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.nebula.sediment.sedimentLibMod;

public record StatusEffectRulesPacket(boolean aboveEntities, boolean abovePlayers, boolean bookEnabled) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatusEffectRulesPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_rules"));

    public static final StreamCodec<FriendlyByteBuf, StatusEffectRulesPacket> STREAM_CODEC =
            StreamCodec.of(StatusEffectRulesPacket::encode, StatusEffectRulesPacket::decode);

    public static void encode(FriendlyByteBuf buf, StatusEffectRulesPacket packet) {
        buf.writeBoolean(packet.aboveEntities());
        buf.writeBoolean(packet.abovePlayers());
        buf.writeBoolean(packet.bookEnabled());
    }

    public static StatusEffectRulesPacket decode(FriendlyByteBuf buf) {
        return new StatusEffectRulesPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(StatusEffectRulesPacket packet, IPayloadContext context) {
        ClientStatusEffectRules.set(packet.aboveEntities(), packet.abovePlayers(), packet.bookEnabled());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}