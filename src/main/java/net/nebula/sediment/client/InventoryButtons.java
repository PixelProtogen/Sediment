package net.nebula.sediment.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = sedimentLibMod.MODID, value = Dist.CLIENT)
public final class InventoryButtons {

    private static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_book/book"),
            ResourceLocation.fromNamespaceAndPath(sedimentLibMod.MODID, "status_effect_book/book_highlighted")
    );

    private static final int WIDTH = 20;
    private static final int HEIGHT = 18;

    private static final int OFFSET_X = 104 + 22;
    private static final int OFFSET_Y = -22;

    private static ImageButton button;

    private InventoryButtons() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            button = null;
            return;
        }

        button = new ImageButton(screen.getGuiLeft() + OFFSET_X, screen.height / 2 + OFFSET_Y, WIDTH, HEIGHT, SPRITES, b -> open());
        button.setTooltip(Tooltip.create(Component.literal("Status Effects")));
        button.visible = ClientStatusEffectRules.bookAvailable();
        event.addListener(button);
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (button != null && event.getScreen() instanceof InventoryScreen screen) {
            boolean allowed = ClientStatusEffectRules.bookAvailable();
            button.visible = allowed;
            button.active = allowed;
            button.setX(screen.getGuiLeft() + OFFSET_X);
            button.setY(screen.height / 2 + OFFSET_Y);
        }
    }

    private static void open() {
        if (!ClientStatusEffectRules.bookAvailable()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new StatusEffectBookScreen(mc.screen));
    }
}