package net.nebula.sediment.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.nebula.sediment.network.StatusEffectDefinition;

public class StatusEffectToast implements Toast {

    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");

    private static final long DISPLAY_MS = 5000L;
    private static final int TEXT_X = 30;
    private static final int ICON_SIZE = 16;

    private static final int TITLE_COLOR = 0xFFFFFF00;
    private static final int NAME_COLOR = 0xFFFFFFFF;

    private final StatusEffectDefinition def;
    private boolean playedSound = false;

    public StatusEffectToast(StatusEffectDefinition def) {
        this.def = def;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long timeSinceLastVisible) {
        g.blitSprite(BACKGROUND, 0, 0, this.width(), this.height());

        Font font = toasts.getMinecraft().font;

        StatusEffectHud.drawIcon(g, this.def.icon(), 8, (this.height() - ICON_SIZE) / 2, ICON_SIZE);

        int maxTextWidth = this.width() - TEXT_X - 6;
        g.drawString(font, Component.literal("Effect Discovered"), TEXT_X, 7, TITLE_COLOR, false);
        FormattedCharSequence name = Language.getInstance().getVisualOrder(font.substrByWidth(this.def.info().getName(), maxTextWidth));
        g.drawString(font, name, TEXT_X, 18, NAME_COLOR, false);

        if (!this.playedSound && timeSinceLastVisible > 0L) {
            this.playedSound = true;
            toasts.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 1.0F));
        }

        return timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
    }
}