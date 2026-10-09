package net.nebula.sediment.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nebula.sediment.client.ClientStatusEffectDefinitions;
import net.nebula.sediment.client.StatusEffectToast;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StatusEffectDiscoveredPacket(String effectId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatusEffectDiscoveredPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_discovered"));

    public static final StreamCodec<FriendlyByteBuf, StatusEffectDiscoveredPacket> STREAM_CODEC =
            StreamCodec.of(StatusEffectDiscoveredPacket::encode, StatusEffectDiscoveredPacket::decode);

    public static void encode(FriendlyByteBuf buf, StatusEffectDiscoveredPacket packet) {
        buf.writeUtf(packet.effectId());
    }

    public static StatusEffectDiscoveredPacket decode(FriendlyByteBuf buf) {
        return new StatusEffectDiscoveredPacket(buf.readUtf());
    }

    public static void handle(StatusEffectDiscoveredPacket packet, IPayloadContext context) {
        StatusEffectDefinition def = ClientStatusEffectDefinitions.get(packet.effectId());
        if (def != null) {
            Minecraft.getInstance().getToasts().addToast(new StatusEffectToast(def));
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}