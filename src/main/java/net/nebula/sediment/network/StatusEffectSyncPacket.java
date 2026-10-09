package net.nebula.sediment.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.nebula.sediment.client.ClientStatusEffectStore;
import net.nebula.sediment.sedimentLibMod;

public record StatusEffectSyncPacket(int entityId, List<StatusEffectSnapshot> entries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatusEffectSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_sync"));

    public static final StreamCodec<FriendlyByteBuf, StatusEffectSyncPacket> STREAM_CODEC =
            StreamCodec.of(StatusEffectSyncPacket::encode, StatusEffectSyncPacket::decode);

    public static void encode(FriendlyByteBuf buf, StatusEffectSyncPacket packet) {
        buf.writeVarInt(packet.entityId());
        buf.writeVarInt(packet.entries().size());
        for (StatusEffectSnapshot entry : packet.entries()) {
            buf.writeUtf(entry.id());
            buf.writeVarInt(entry.count());
            buf.writeVarInt(entry.potency());
        }
    }

    public static StatusEffectSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int size = buf.readVarInt();
        List<StatusEffectSnapshot> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new StatusEffectSnapshot(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
        }
        return new StatusEffectSyncPacket(entityId, entries);
    }

    public static void handle(StatusEffectSyncPacket packet, IPayloadContext context) {
        ClientStatusEffectStore.set(packet.entityId(), packet.entries());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}