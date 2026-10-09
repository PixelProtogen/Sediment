package net.nebula.sediment.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.nebula.sediment.StatusEffectRegistry;
import net.nebula.sediment.client.ClientStatusEffectDefinitions;
import net.nebula.sediment.common.IStatusEffect;
import net.nebula.sediment.common.StatusInfoHolder;
import net.nebula.sediment.sedimentLibMod;

public record StatusEffectDefinitionsPacket(List<StatusEffectDefinition> definitions) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatusEffectDefinitionsPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_definitions"));

    public static final StreamCodec<FriendlyByteBuf, StatusEffectDefinitionsPacket> STREAM_CODEC =
            StreamCodec.of(StatusEffectDefinitionsPacket::encode, StatusEffectDefinitionsPacket::decode);

    public static StatusEffectDefinitionsPacket fromRegistry() {
        List<StatusEffectDefinition> list = new ArrayList<>();
        for (String id : StatusEffectRegistry.ids()) {
            IStatusEffect sample = StatusEffectRegistry.create(id);
            if (sample != null) {
                list.add(StatusEffectDefinition.from(sample));
            }
        }
        return new StatusEffectDefinitionsPacket(list);
    }

    public static void encode(FriendlyByteBuf buf, StatusEffectDefinitionsPacket packet) {
        buf.writeVarInt(packet.definitions().size());
        for (StatusEffectDefinition def : packet.definitions()) {
            buf.writeUtf(def.id());
            buf.writeResourceLocation(def.icon());
            buf.writeVarInt(def.maxCount());
            buf.writeVarInt(def.maxPotency());
            buf.writeUtf(def.info().getName().getString());
            buf.writeBoolean(def.info().isLoreTranslatable());
            buf.writeUtf(def.info().getLoreValue());

            List<StatusInfoHolder.Segment> segments = def.info().getSegments();
            buf.writeVarInt(segments.size());
            for (StatusInfoHolder.Segment segment : segments) {
                if (segment.reference() != null) {
                    buf.writeByte(2);
                    buf.writeUtf(segment.reference());
                } else {
                    buf.writeByte(segment.translatable() ? 1 : 0);
                    buf.writeUtf(segment.text());
                }
            }
        }
    }

    public static StatusEffectDefinitionsPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<StatusEffectDefinition> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf();
            ResourceLocation icon = buf.readResourceLocation();
            int maxCount = buf.readVarInt();
            int maxPotency = buf.readVarInt();
            String name = buf.readUtf();
            boolean loreTranslatable = buf.readBoolean();
            String lore = buf.readUtf();

            int segmentCount = buf.readVarInt();
            List<StatusInfoHolder.Segment> segments = new ArrayList<>(segmentCount);
            for (int s = 0; s < segmentCount; s++) {
                byte kind = buf.readByte();
                String value = buf.readUtf();
                segments.add(switch (kind) {
                    case 2 -> new StatusInfoHolder.Segment(null, value, false);
                    case 1 -> new StatusInfoHolder.Segment(value, null, true);
                    default -> new StatusInfoHolder.Segment(value, null, false);
                });
            }

            list.add(new StatusEffectDefinition(
                    id, icon, maxCount, maxPotency,
                    StatusInfoHolder.of(name, segments, lore, loreTranslatable)));
        }
        return new StatusEffectDefinitionsPacket(list);
    }

    public static void handle(StatusEffectDefinitionsPacket packet, IPayloadContext context) {
        ClientStatusEffectDefinitions.set(packet.definitions());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}