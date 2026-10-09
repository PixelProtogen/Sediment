package net.nebula.sediment.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.nebula.sediment.network.StatusEffectDefinition;
import net.nebula.sediment.network.StatusEffectSnapshot;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = sedimentLibMod.MODID, value = Dist.CLIENT)
public final class StatusEffectRenderer {

    static final int ICON_SIZE = 12;
    private static final int GAP = 7;
    private static final int PER_ROW = 6;
    static final int MAX_SHOWN = 18;

    static final float WORLD_SCALE = 0.025F;
    private static final float TEXT_SCALE = 0.7F;
    private static final float GLYPH_HEIGHT = 7.0F;
    private static final float TEXT_Z = 0.5F;

    private static final double NAMETAG_OFFSET = 0.5;
    private static final double NAMETAG_TOP = WORLD_SCALE;
    private static final double GAP_ABOVE_NAMETAG = 0.06;
    private static final double GAP_ABOVE_HEAD = 0.20;

    static final double MAX_DISTANCE_SQR = 32.0 * 32.0;

    private static final int COUNT_COLOR = 0xFFFFFFFF;
    private static final int POTENCY_COLOR = 0xFFFFD35A;

    private StatusEffectRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        if (!ClientStatusEffectRules.shouldDisplay(entity)) {
            return;
        }

        List<StatusEffectSnapshot> entries = ClientStatusEffectStore.get(entity);

        if (entries.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();

        if (dispatcher.distanceToSqr(entity) > MAX_DISTANCE_SQR) {
            return;
        }

        if (minecraft.player != null && entity.isInvisibleTo(minecraft.player)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        Font font = minecraft.font;

        int light = LightTexture.FULL_BRIGHT;
        int shown = Math.min(entries.size(), MAX_SHOWN);

        Vec3 anchor = anchorOffset(entity, minecraft, dispatcher, event.getPartialTick());

        if (anchor == null) {
            anchor = new Vec3(0.0, entity.getBbHeight(), 0.0);
        }

        double y = anchor.y + (isNameTagVisible(entity, minecraft, dispatcher) ? NAMETAG_OFFSET + NAMETAG_TOP + GAP_ABOVE_NAMETAG : GAP_ABOVE_HEAD);

        poseStack.pushPose();

        poseStack.translate(anchor.x, anchor.y, anchor.z);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(WORLD_SCALE, -WORLD_SCALE, WORLD_SCALE);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        for (int i = 0; i < shown; i++) {
            StatusEffectDefinition def = ClientStatusEffectDefinitions.get(entries.get(i).id());
            if (def == null) {
                continue;
            }

            drawIcon(pose, buffers, def.icon(), slotX(i, shown), slotTop(i), light);

            if (buffers instanceof MultiBufferSource.BufferSource source) {
                source.endBatch(RenderType.entityTranslucent(def.icon()));
            }
        }

        for (int i = 0; i < shown; i++) {
            StatusEffectSnapshot entry = entries.get(i);
            StatusEffectDefinition def = ClientStatusEffectDefinitions.get(entry.id());

            if (def == null) {
                continue;
            }

            float x = slotX(i, shown);
            float bottom = slotTop(i) + ICON_SIZE;

            if (def.showPotency()) {
                drawText(font, matrix, buffers, Integer.toString(entry.potency()), x, bottom, POTENCY_COLOR, light);
            }

            if (def.showCount()) {
                drawText(font, matrix, buffers, Integer.toString(entry.count()), x + ICON_SIZE, bottom, COUNT_COLOR, light);
            }
        }

        poseStack.popPose();
        StatusEffectTooltips.awardBookCraft();
    }

    public static float slotX(int index, int shown) {
        int row = index / PER_ROW;
        int col = index % PER_ROW;
        int inRow = Math.min(PER_ROW, shown - row * PER_ROW);
        float rowWidth = inRow * ICON_SIZE + (inRow - 1) * GAP;
        return -rowWidth / 2.0F + col * (ICON_SIZE + GAP);
    }

    public static float slotTop(int index) {
        int row = index / PER_ROW;
        return -(row + 1) * ICON_SIZE - row * GAP;
    }

    static Vec3 anchorOffset(LivingEntity entity, Minecraft minecraft, EntityRenderDispatcher dispatcher, float partialTick) {
        Vec3 attach = entity.getAttachments().getNullable(
                EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        if (attach == null) {
            attach = new Vec3(0.0, entity.getBbHeight(), 0.0);
        }

        double y = attach.y + (isNameTagVisible(entity, minecraft, dispatcher) ? NAMETAG_OFFSET + NAMETAG_TOP + GAP_ABOVE_NAMETAG : GAP_ABOVE_HEAD);

        return new Vec3(attach.x, y, attach.z);
    }

    private static void drawText(
            Font font,
            Matrix4f matrix,
            MultiBufferSource buffers,
            String text,
            float centerX,
            float centerY,
            int color,
            int light
    ) {
        float width = font.width(text) * TEXT_SCALE;
        float height = GLYPH_HEIGHT * TEXT_SCALE;

        Matrix4f textMatrix = new Matrix4f(matrix).translate(centerX - width / 2.0F, centerY - height / 2.0F, TEXT_Z).scale(TEXT_SCALE);

        font.drawInBatch(text, 0.0F, 0.0F, color | 0xFF000000, true, textMatrix, buffers, Font.DisplayMode.NORMAL, 0,light);
    }

    private static boolean isNameTagVisible(LivingEntity entity, Minecraft mc, EntityRenderDispatcher dispatcher) {
        if (!Minecraft.renderNames()) {
            return false;
        }
        if (entity == mc.getCameraEntity() || entity.isVehicle()) {
            return false;
        }
        double max = entity.isDiscrete() ? 32.0 : 64.0;
        if (dispatcher.distanceToSqr(entity) >= max * max) {
            return false;
        }
        return entity.shouldShowName()
                || (entity.hasCustomName() && entity == dispatcher.crosshairPickEntity);
    }

    private static void drawIcon(PoseStack.Pose pose, MultiBufferSource buffers, ResourceLocation icon,
                                 float x, float y, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(icon));

        vertex(consumer, pose, x,             y + ICON_SIZE, 0.0F, 1.0F, light);
        vertex(consumer, pose, x + ICON_SIZE, y + ICON_SIZE, 1.0F, 1.0F, light);
        vertex(consumer, pose, x + ICON_SIZE, y,             1.0F, 0.0F, light);
        vertex(consumer, pose, x,             y,             0.0F, 0.0F, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float u, float v, int light) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}