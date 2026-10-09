package net.nebula.sediment.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.nebula.sediment.network.StatusEffectDefinition;
import net.nebula.sediment.network.StatusEffectSnapshot;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = sedimentLibMod.MODID, value = Dist.CLIENT)
public final class StatusEffectHud {

    private static final int SIZE = 16;
    private static final int GAP = 2;
    private static final int MARGIN = 4;
    private static final float TEXT_SCALE = 0.7F;

    private static final int COUNT_COLOR = 0xFFFFFFFF;
    private static final int POTENCY_COLOR = 0xFFFFD35A;

    private StatusEffectHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (mc.screen instanceof AbstractContainerScreen<?>) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        drawOwnList(g, mc, -1, -1);

        if (mc.screen == null && StatusEffectKeys.INSPECT.isDown()) {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            Looked looked = findLookedAt(mc, partial);
            if (looked != null) {
                drawTooltip(g, mc, looked.def, looked.entry, g.guiWidth() / 2, g.guiHeight() / 2);
                ClientStatusEffectDiscoveries.reportInspected(looked.entityId, looked.entry.id());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }

        GuiGraphics g = event.getGuiGraphics();
        int hovered = drawOwnList(g, mc, event.getMouseX(), event.getMouseY());

        if (hovered >= 0) {
            StatusEffectSnapshot entry = ClientStatusEffectStore.get(mc.player).get(hovered);
            StatusEffectDefinition def = ClientStatusEffectDefinitions.get(entry.id());
            if (def != null) {
                drawTooltip(g, mc, def, entry, event.getMouseX(), event.getMouseY());
            }
        }
    }

    private static int drawOwnList(GuiGraphics g, Minecraft mc, int mouseX, int mouseY) {
        List<StatusEffectSnapshot> entries = ClientStatusEffectStore.get(mc.player);
        if (entries.isEmpty()) {
            return -1;
        }

        Font font = mc.font;
        int screenHeight = g.guiHeight();
        int hovered = -1;

        for (int i = 0; i < entries.size(); i++) {
            StatusEffectSnapshot entry = entries.get(i);
            StatusEffectDefinition def = ClientStatusEffectDefinitions.get(entry.id());
            if (def == null) {
                continue;
            }

            int[] pos = slotPos(i, entries.size(), screenHeight);
            int x = pos[0];
            int y = pos[1];

            drawIcon(g, def.icon(), x, y, SIZE);

            if (def.showPotency()) {
                drawCornerText(g, font, Integer.toString(entry.potency()), x, y + SIZE, POTENCY_COLOR);
            }
            if (def.showCount()) {
                drawCornerText(g, font, Integer.toString(entry.count()), x + SIZE, y + SIZE, COUNT_COLOR);
            }

            if (mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE) {
                hovered = i;
            }
        }

        return hovered;
    }

    private static int[] slotPos(int index, int count, int screenHeight) {
        int step = SIZE + GAP;
        int perColumn = Math.max(1, (screenHeight - 2 * MARGIN) / step);
        int rowsInFirst = Math.min(count, perColumn);
        int startY = (screenHeight - (rowsInFirst * step - GAP)) / 2;

        int col = index / perColumn;
        int row = index % perColumn;

        return new int[]{MARGIN + col * step, startY + row * step};
    }

    static void drawIcon(GuiGraphics g, ResourceLocation icon, int x, int y, int size) {
        Matrix4f m = g.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, icon);
        RenderSystem.enableBlend();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(m, x, y + size, 0.0F).setUv(0.0F, 1.0F);
        buffer.addVertex(m, x + size, y + size, 0.0F).setUv(1.0F, 1.0F);
        buffer.addVertex(m, x + size, y, 0.0F).setUv(1.0F, 0.0F);
        buffer.addVertex(m, x, y, 0.0F).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.disableBlend();
    }

    private static void drawCornerText(GuiGraphics g, Font font, String text, int cornerX, int cornerY, int color) {
        float width = font.width(text) * TEXT_SCALE;
        float height = font.lineHeight * TEXT_SCALE;

        g.pose().pushPose();
        g.pose().translate(cornerX - width / 2.0F, cornerY - height / 2.0F, 200.0F);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        g.drawString(font, text, 0, 0, color, true);
        g.pose().popPose();
    }

    private static void drawTooltip(GuiGraphics g, Minecraft mc, StatusEffectDefinition def,
                                    StatusEffectSnapshot entry, int x, int y) {
        StatusEffectTooltips.render(g, mc.font, StatusEffectTooltips.build(mc.font, def, entry), x, y);
    }

    private record Looked(StatusEffectDefinition def, StatusEffectSnapshot entry, int entityId) {}

    private static Looked findLookedAt(Minecraft mc, float partial) {
        if (mc.level == null || mc.player == null) {
            return null;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f right = new Vector3f(camera.getLeftVector()).negate();

        Vec3 lookVec = new Vec3(look.x(), look.y(), look.z());
        Vec3 upVec = new Vec3(up.x(), up.y(), up.z());
        Vec3 rightVec = new Vec3(right.x(), right.y(), right.z());

        Looked best = null;
        double bestT = Double.MAX_VALUE;

        for (Entity candidate : mc.level.entitiesForRendering()) {
            if (!(candidate instanceof LivingEntity entity) || entity == mc.player) {
                continue;
            }
            if (!ClientStatusEffectRules.shouldDisplay(entity) || entity.isInvisibleTo(mc.player)) {
                continue;
            }
            if (mc.getEntityRenderDispatcher().distanceToSqr(entity) > StatusEffectRenderer.MAX_DISTANCE_SQR) {
                continue;
            }

            List<StatusEffectSnapshot> entries = ClientStatusEffectStore.get(entity);
            if (entries.isEmpty()) {
                continue;
            }

            Vec3 origin = entity.getPosition(partial)
                    .add(StatusEffectRenderer.anchorOffset(entity, mc, mc.getEntityRenderDispatcher(), partial));

            double t = origin.subtract(camPos).dot(lookVec);
            if (t <= 0.0 || t >= bestT) {
                continue;
            }

            Vec3 hit = camPos.add(lookVec.scale(t));
            Vec3 rel = hit.subtract(origin);

            double lx = rel.dot(rightVec) / StatusEffectRenderer.WORLD_SCALE;
            double ly = -rel.dot(upVec) / StatusEffectRenderer.WORLD_SCALE;

            int shown = Math.min(entries.size(), StatusEffectRenderer.MAX_SHOWN);
            for (int i = 0; i < shown; i++) {
                float x = StatusEffectRenderer.slotX(i, shown);
                float top = StatusEffectRenderer.slotTop(i);
                int size = StatusEffectRenderer.ICON_SIZE;

                if (lx >= x - 1 && lx <= x + size + 1 && ly >= top - 1 && ly <= top + size + 1) {
                    StatusEffectSnapshot entry = entries.get(i);
                    StatusEffectDefinition def = ClientStatusEffectDefinitions.get(entry.id());
                    if (def != null) {
                        best = new Looked(def, entry, entity.getId());
                        bestT = t;
                    }
                    break;
                }
            }
        }

        return best;
    }
}