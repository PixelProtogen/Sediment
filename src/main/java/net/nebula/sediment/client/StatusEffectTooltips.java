package net.nebula.sediment.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.nebula.sediment.common.StatusInfoHolder;
import net.nebula.sediment.network.StatusEffectDefinition;
import net.nebula.sediment.network.StatusEffectDiscoverPacket;
import net.nebula.sediment.network.StatusEffectSnapshot;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StatusEffectTooltips {

    private static final int MAX_WIDTH = 200;
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_SIZE = 9;
    private static final int ICON_GAP = 2;
    private static final int PADDING = 3;
    private static boolean sent = false;

    private record Run(FormattedCharSequence text, @Nullable ResourceLocation icon, int width) {}

    public record Line(List<Run> runs, int width) {}

    private StatusEffectTooltips() {
    }

    public static void awardBookCraft() {
        if (!sent) {
            sent = true;
            PacketDistributor.sendToServer(new StatusEffectDiscoverPacket());
        }
    }

    public static List<Line> build(Font font, StatusEffectDefinition def, @Nullable StatusEffectSnapshot entry) {
        List<Line> lines = new ArrayList<>();

        addWrapped(font, lines, def.info().getName().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

        addDescription(font, lines, def);

        if (def.showCount()) {
            String text = entry != null ? "Count: " + entry.count() + " (max " + def.maxCount() + ")" : "Max Count: " + def.maxCount();
            addWrapped(font, lines, Component.literal(text).withStyle(ChatFormatting.WHITE));
        }
        if (def.showPotency()) {
            String text = entry != null ? "Potency: " + entry.potency() + " (max " + def.maxPotency() + ")" : "Max Potency: " + def.maxPotency();
            addWrapped(font, lines, Component.literal(text).withStyle(ChatFormatting.GOLD));
        }

        Component lore = def.info().getLore();
        if (!lore.getString().isEmpty()) {
            addWrapped(font, lines, lore.copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }

        return lines;
    }

    private static void addWrapped(Font font, List<Line> lines, Component component) {
        for (FormattedCharSequence part : font.split(component, MAX_WIDTH)) {
            int width = font.width(part);
            lines.add(new Line(List.of(new Run(part, null, width)), width));
        }
    }

    private static void addDescription(Font font, List<Line> lines, StatusEffectDefinition def) {
        List<StatusInfoHolder.Segment> segments = def.info().getSegments();
        if (segments.isEmpty()) {
            return;
        }

        List<Run> current = new ArrayList<>();
        int currentWidth = 0;

        for (StatusInfoHolder.Segment segment : segments) {
            List<Run> tokens = new ArrayList<>();

            if (segment.reference() != null) {
                tokens.add(referenceRun(font, segment.reference()));
            } else if (segment.text() != null) {
                for (String word : segment.text().split("(?<= )")) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    FormattedCharSequence seq = Component.literal(word).withStyle(ChatFormatting.GRAY).getVisualOrderText();
                    tokens.add(new Run(seq, null, font.width(seq)));
                }
            }

            for (Run token : tokens) {
                boolean blank = token.icon() == null && token.width() <= font.width(" ");

                if (currentWidth + token.width() > MAX_WIDTH && !current.isEmpty()) {
                    lines.add(new Line(current, currentWidth));
                    current = new ArrayList<>();
                    currentWidth = 0;
                }
                if (blank && current.isEmpty()) {
                    continue;
                }
                current.add(token);
                currentWidth += token.width();
            }
        }

        if (!current.isEmpty()) {
            lines.add(new Line(current, currentWidth));
        }
    }

    private static Run referenceRun(Font font, String referenceId) {
        StatusEffectDefinition other = ClientStatusEffectDefinitions.get(referenceId);

        Component name = other != null ? other.info().getName() : Component.literal(referenceId);
        FormattedCharSequence seq = name.copy().withStyle(ChatFormatting.GOLD).getVisualOrderText();

        if (other == null) {
            return new Run(seq, null, font.width(seq));
        }
        return new Run(seq, other.icon(), ICON_SIZE + ICON_GAP + font.width(seq));
    }

    public static void drawLines(GuiGraphics g, Font font, List<Line> lines, int x, int y) {
        int lineY = y;
        for (Line line : lines) {
            int lineX = x;
            for (Run run : line.runs()) {
                if (run.icon() != null) {
                    StatusEffectHud.drawIcon(g, run.icon(), lineX, lineY, ICON_SIZE);
                    g.drawString(font, run.text(), lineX + ICON_SIZE + ICON_GAP, lineY, 0xFFFFFFFF, true);
                } else {
                    g.drawString(font, run.text(), lineX, lineY, 0xFFFFFFFF, true);
                }
                lineX += run.width();
            }
            lineY += LINE_HEIGHT;
        }
    }

    public static void render(GuiGraphics g, Font font, List<Line> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }

        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, line.width());
        }
        int height = lines.size() * LINE_HEIGHT;

        int x = mouseX + 12;
        int y = mouseY - 12;

        if (x + width + PADDING + 1 > g.guiWidth()) {
            x = mouseX - 16 - width;
        }
        x = Math.max(PADDING + 1, x);
        if (y + height + PADDING + 1 > g.guiHeight()) {
            y = g.guiHeight() - height - PADDING - 1;
        }
        y = Math.max(PADDING + 1, y);

        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 400.0F);

        g.fill(x - PADDING - 1, y - PADDING - 1, x + width + PADDING + 1, y + height + PADDING + 1, 0xF0100010);
        int border = 0xF0501B96;
        g.fill(x - PADDING, y - PADDING, x + width + PADDING, y - PADDING + 1, border);
        g.fill(x - PADDING, y + height + PADDING - 1, x + width + PADDING, y + height + PADDING, border);
        g.fill(x - PADDING, y - PADDING, x - PADDING + 1, y + height + PADDING, border);
        g.fill(x + width + PADDING - 1, y - PADDING, x + width + PADDING, y + height + PADDING, border);
        g.fill(x - PADDING + 1, y - PADDING + 1, x + width + PADDING - 1, y + height + PADDING - 1, 0xF0100010);

        drawLines(g, font, lines, x, y);

        g.pose().popPose();
    }
}