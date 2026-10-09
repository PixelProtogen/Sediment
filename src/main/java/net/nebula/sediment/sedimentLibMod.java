package net.nebula.sediment;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.nebula.sediment.common.StatusEffectDiscovery;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.nebula.sediment.common.StatusEffectGameRules;
import net.nebula.sediment.network.StatusEffectNetworking;
import net.nebula.sediment.effects.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(sedimentLibMod.MODID)
public class sedimentLibMod {

    public static final String MODID = "sediment_lib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> STATUS_EFFECT_BOOK = ITEMS.register("status_effect_book", net.nebula.sediment.items.StatusEffectBook::new);

    public sedimentLibMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(StatusEffectNetworking::register);
        StatusEffectGameRules.register();
        registerBuiltInEffects();
        StatusEffectDiscovery.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addToTab);
    }

    private void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(STATUS_EFFECT_BOOK);
        }
    }

    private static void registerBuiltInEffects() {
        StatusEffectRegistry.builtIn("bleed", BleedEffect::new);
        StatusEffectRegistry.builtIn("charge", ChargeEffect::new);
        StatusEffectRegistry.builtIn("barrier", BarrierEffect::new);
        StatusEffectRegistry.builtIn("barrier_generate", BarrierConvertionEffect::new);
        StatusEffectRegistry.builtIn("doom", DoomEffect::new);
        StatusEffectRegistry.builtIn("sedimented", SedimentedEffect::new);
        StatusEffectRegistry.freeze();
    }
}