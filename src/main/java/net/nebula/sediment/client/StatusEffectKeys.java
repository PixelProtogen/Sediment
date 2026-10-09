package net.nebula.sediment.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.nebula.sediment.sedimentLibMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = sedimentLibMod.MODID, value = Dist.CLIENT)
public final class StatusEffectKeys {
    public static final KeyMapping INSPECT = new KeyMapping("key.sediment_lib.inspect_effects", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, "key.categories.sediment_lib");
    private StatusEffectKeys() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(INSPECT);
    }
}