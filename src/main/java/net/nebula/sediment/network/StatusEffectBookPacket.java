package net.nebula.sediment.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nebula.sediment.client.ClientStatusEffectRules;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StatusEffectBookPacket(boolean unlocked) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatusEffectBookPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_book"));

    public static final StreamCodec<FriendlyByteBuf, StatusEffectBookPacket> STREAM_CODEC =
            StreamCodec.of(StatusEffectBookPacket::encode, StatusEffectBookPacket::decode);

    public static void encode(FriendlyByteBuf buf, StatusEffectBookPacket packet) {
        buf.writeBoolean(packet.unlocked());
    }

    public static StatusEffectBookPacket decode(FriendlyByteBuf buf) {
        return new StatusEffectBookPacket(buf.readBoolean());
    }

    public static void handle(StatusEffectBookPacket packet, IPayloadContext context) {
        ClientStatusEffectRules.setBookUnlocked(packet.unlocked());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}