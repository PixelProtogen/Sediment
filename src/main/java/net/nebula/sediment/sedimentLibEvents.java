package net.nebula.sediment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.nebula.sediment.client.ClientStatusEffectStore;
import net.nebula.sediment.command.StatusEffectCommand;
import net.nebula.sediment.common.*;
import net.nebula.sediment.network.StatusEffectNetworking;

@EventBusSubscriber(modid = sedimentLibMod.MODID)
public final class sedimentLibEvents {

    private sedimentLibEvents() {
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity living) {
            StatusContainer.restore(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().remove(StatusContainer.PERSIST_KEY);
        StatusContainer.transfer(event.getOriginal(), event.getEntity(), event.isWasDeath());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatusContainer.sendFull(player, player);
            StatusEffectNetworking.sendBookState(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        StatusContainer.clearRegistry();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StatusEffectCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        StatusContainer container = StatusContainer.get(living.getUUID());
        if (container != null && container.getHost() == living) {
            container.tick();
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof LivingEntity target) {
            StatusContainer.sendFull(player, target);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatusEffectNetworking.sendRulesToPlayer(player);
            StatusEffectNetworking.sendDefinitionsToPlayer(player);
            StatusEffectNetworking.sendDiscoveriesToPlayer(player);
            StatusEffectNetworking.sendBookState(player);
            StatusContainer.sendFull(player, player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatusContainer.sendFull(player, player);
        }
    }

    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ClientStatusEffectStore.remove(event.getEntity().getId());
        }
    }
}