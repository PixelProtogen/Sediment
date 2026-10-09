package net.nebula.sediment.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nebula.sediment.client.ClientStatusEffectDiscoveries;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StatusEffectDiscoveriesPacket(List<String> ids) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatusEffectDiscoveriesPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_discoveries"));

    public static final StreamCodec<FriendlyByteBuf, StatusEffectDiscoveriesPacket> STREAM_CODEC =
            StreamCodec.of(StatusEffectDiscoveriesPacket::encode, StatusEffectDiscoveriesPacket::decode);

    public static void encode(FriendlyByteBuf buf, StatusEffectDiscoveriesPacket packet) {
        buf.writeVarInt(packet.ids().size());
        for (String id : packet.ids()) {
            buf.writeUtf(id);
        }
    }

    public static StatusEffectDiscoveriesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readUtf());
        }
        return new StatusEffectDiscoveriesPacket(ids);
    }

    public static void handle(StatusEffectDiscoveriesPacket packet, IPayloadContext context) {
        ClientStatusEffectDiscoveries.set(packet.ids());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}