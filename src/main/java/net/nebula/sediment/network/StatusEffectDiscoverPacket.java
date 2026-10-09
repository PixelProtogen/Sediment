package net.nebula.sediment.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class StatusEffectDiscoverPacket implements CustomPacketPayload {
    private static final ResourceLocation RECIPE_ID = ResourceLocation.fromNamespaceAndPath("sediment_lib", "status_effect_book");

    public static final Type<StatusEffectDiscoverPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("sediment", "discover_effect"));

    public static final StreamCodec<ByteBuf, StatusEffectDiscoverPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {}, buf -> new StatusEffectDiscoverPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StatusEffectDiscoverPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (!player.getRecipeBook().contains(RECIPE_ID)) {
                player.awardRecipesByKey(List.of(RECIPE_ID));
            }
        }
    }
}
