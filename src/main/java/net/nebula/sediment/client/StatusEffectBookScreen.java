package net.nebula.sediment.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.nebula.sediment.network.StatusEffectDefinition;

public class StatusEffectBookScreen extends Screen {
    private Button backButton;
    private static final int SLOT = 18;
    private static final int ICON = 16;
    private static final int COLS = 6;
    private static final int GRID_ROWS = 6;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;

    private static final int SCROLL_X = GRID_X + COLS * SLOT + 2;
    private static final int SCROLL_W = 12;
    private static final int THUMB_H = 15;

    private static final int DETAIL_X = SCROLL_X + SCROLL_W + 6;
    private static final int WIDTH = 392;
    private static final int DETAIL_W = WIDTH - DETAIL_X - 8;
    private static final int BUTTON_H = 20;
    private static final int HEIGHT = GRID_Y + GRID_ROWS * SLOT + 8 + BUTTON_H + 4;

    private static final int BLACK = 0xFF000000;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int INSET_FILL = 0xFF1B1B1F;
    private static final int TEXT_DARK = 0xFF404040;

    private final Screen parent;

    private final List<StatusEffectDefinition> all = new ArrayList<>();
    private final List<StatusEffectDefinition> visible = new ArrayList<>();

    private int left;
    private int top;
    private int scrollRow = 0;
    private String selectedId = null;
    private boolean draggingScroll = false;

    public StatusEffectBookScreen(Screen parent) {
        super(Component.literal("Status Effects"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;

        this.all.clear();
        this.all.addAll(ClientStatusEffectDefinitions.all());
        this.all.sort(Comparator.comparing(StatusEffectDefinition::id));

        this.backButton = Button.builder(Component.literal("Back"), b -> this.onClose()).bounds(this.left + (WIDTH - 100) / 2, this.top + HEIGHT - BUTTON_H - 6, 100, BUTTON_H).build();
        this.addWidget(this.backButton);

        this.refreshVisible();
    }

    @Override
    public void tick() {
        if (!ClientStatusEffectRules.bookAvailable()) {
            this.onClose();
            return;
        }
        this.refreshVisible();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshVisible() {
        this.visible.clear();
        for (StatusEffectDefinition def : this.all) {
            if (ClientStatusEffectDiscoveries.has(def.id())) {
                this.visible.add(def);
            }
        }

        if (this.selectedId != null && this.indexOf(this.selectedId) < 0) {
            this.selectedId = null;
        }
        this.clampScroll();
    }

    private int indexOf(String id) {
        for (int i = 0; i < this.visible.size(); i++) {
            if (this.visible.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        this.refreshVisible();

        Font font = this.font;

        drawPanel(g, this.left, this.top, WIDTH, HEIGHT);

        g.drawString(font, this.title, this.left + GRID_X, this.top + 6, TEXT_DARK, false);

        String counter = this.visible.size() + " discovered";
        g.drawString(font, counter, this.left + WIDTH - 8 - font.width(counter), this.top + 6, TEXT_DARK, false);

        StatusEffectDefinition hovered = this.renderGrid(g, mouseX, mouseY);
        this.renderScrollbar(g);
        this.renderDetail(g);

        this.backButton.render(g, mouseX, mouseY, partialTick);

        if (hovered != null) {
            g.renderTooltip(font, hovered.info().getName(), mouseX, mouseY);
        }

    }

    private StatusEffectDefinition renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        StatusEffectDefinition hovered = null;

        int first = this.scrollRow * COLS;

        for (int cell = 0; cell < COLS * GRID_ROWS; cell++) {
            int col = cell % COLS;
            int row = cell / COLS;
            int x = this.left + GRID_X + col * SLOT;
            int y = this.top + GRID_Y + row * SLOT;

            drawSlot(g, x, y);

            int index = first + cell;
            if (index >= this.visible.size()) {
                continue;
            }

            StatusEffectDefinition def = this.visible.get(index);
            StatusEffectHud.drawIcon(g, def.icon(), x + 1, y + 1, ICON);

            if (def.id().equals(this.selectedId)) {
                g.renderOutline(x, y, SLOT, SLOT, 0xFFFFD35A);
            }

            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x80FFFFFF);
                hovered = def;
            }
        }

        return hovered;
    }

    private void renderScrollbar(GuiGraphics g) {
        int x = this.left + SCROLL_X;
        int y = this.top + GRID_Y;
        int trackH = GRID_ROWS * SLOT;

        drawInset(g, x, y, SCROLL_W, trackH, SLOT_FILL);

        int maxScroll = this.maxScroll();
        int thumbY = maxScroll > 0 ? y + 1 + (trackH - 2 - THUMB_H) * this.scrollRow / maxScroll : y + 1;

        int fill = maxScroll > 0 ? PANEL : 0xFFA0A0A0;
        g.fill(x + 1, thumbY, x + SCROLL_W - 1, thumbY + THUMB_H, fill);
        g.fill(x + 1, thumbY, x + SCROLL_W - 1, thumbY + 1, WHITE);
        g.fill(x + 1, thumbY, x + 2, thumbY + THUMB_H, WHITE);
        g.fill(x + 1, thumbY + THUMB_H - 1, x + SCROLL_W - 1, thumbY + THUMB_H, SHADOW);
        g.fill(x + SCROLL_W - 2, thumbY, x + SCROLL_W - 1, thumbY + THUMB_H, SHADOW);
    }

    private void renderDetail(GuiGraphics g) {
        int x = this.left + DETAIL_X;
        int y = this.top + GRID_Y;
        int h = GRID_ROWS * SLOT;

        drawInset(g, x, y, DETAIL_W, h, INSET_FILL);

        if (this.visible.isEmpty()) {
            g.drawString(this.font, "No effects discovered yet.", x + 8, y + 8, 0xFF888899, false);
            return;
        }

        StatusEffectDefinition def = this.selectedId == null ? null : this.visible.get(this.indexOf(this.selectedId));
        if (def == null) {
            g.drawString(this.font, "Select an effect", x + 8, y + 8, 0xFF888899, false);
            return;
        }

        g.enableScissor(x + 1, y + 1, x + DETAIL_W - 1, y + h - 1);

        StatusEffectHud.drawIcon(g, def.icon(), x + 6, y + 6, 24);
        StatusEffectTooltips.drawLines(g, this.font, StatusEffectTooltips.build(this.font, def, null), x + 36, y + 8);

        g.disableScissor();
    }

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, BLACK);
        g.fill(x, y + 1, x + w, y + h - 1, BLACK);

        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);

        g.fill(x + 1, y + 1, x + w - 2, y + 3, WHITE);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, WHITE);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, SHADOW);
    }

    private static void drawSlot(GuiGraphics g, int x, int y) {
        drawInset(g, x, y, SLOT, SLOT, SLOT_FILL);
    }

    private static void drawInset(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w - 1, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + h - 1, SLOT_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, WHITE);
        g.fill(x + w - 1, y + 1, x + w, y + h, WHITE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int sx = this.left + SCROLL_X;
            int sy = this.top + GRID_Y;
            if (mouseX >= sx && mouseX < sx + SCROLL_W && mouseY >= sy && mouseY < sy + GRID_ROWS * SLOT) {
                this.draggingScroll = this.maxScroll() > 0;
                this.scrollToMouse(mouseY);
                return true;
            }

            int gx = this.left + GRID_X;
            int gy = this.top + GRID_Y;
            if (mouseX >= gx && mouseX < gx + COLS * SLOT && mouseY >= gy && mouseY < gy + GRID_ROWS * SLOT) {
                int col = (int) ((mouseX - gx) / SLOT);
                int row = (int) ((mouseY - gy) / SLOT);
                int index = (this.scrollRow + row) * COLS + col;

                if (index < this.visible.size()) {
                    this.selectedId = this.visible.get(index).id();
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScroll) {
            this.scrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingScroll = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollRow -= (int) Math.signum(scrollY);
        this.clampScroll();
        return true;
    }

    private void scrollToMouse(double mouseY) {
        int maxScroll = this.maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int trackY = this.top + GRID_Y + 1;
        int range = GRID_ROWS * SLOT - 2 - THUMB_H;
        double fraction = (mouseY - trackY - THUMB_H / 2.0) / range;
        this.scrollRow = (int) Math.round(fraction * maxScroll);
        this.clampScroll();
    }

    private int totalRows() {
        return (this.visible.size() + COLS - 1) / COLS;
    }

    private int maxScroll() {
        return Math.max(0, this.totalRows() - GRID_ROWS);
    }

    private void clampScroll() {
        this.scrollRow = Math.max(0, Math.min(this.scrollRow, this.maxScroll()));
    }
}