package net.nebula.sediment.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.nebula.sediment.network.StatusEffectNetworking;
import net.nebula.sediment.network.StatusEffectRulesPacket;

public final class StatusEffectGameRules {

    public static GameRules.Key<GameRules.BooleanValue> DISPLAY_STATUS_EFFECT_ABOVE_ENTITIES;
    public static GameRules.Key<GameRules.BooleanValue> DISPLAY_STATUS_EFFECT_ABOVE_PLAYERS;
    public static GameRules.Key<GameRules.BooleanValue> STATUS_EFFECT_BOOK_ENABLED;

    private static boolean registered = false;

    private StatusEffectGameRules() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        DISPLAY_STATUS_EFFECT_ABOVE_ENTITIES = GameRules.register("displayStatusEffectAboveEntities", GameRules.Category.MISC, GameRules.BooleanValue.create(true, (server, value) -> StatusEffectNetworking.sendRulesToAll(server)));
        DISPLAY_STATUS_EFFECT_ABOVE_PLAYERS = GameRules.register("displayStatusEffectAbovePlayers", GameRules.Category.MISC, GameRules.BooleanValue.create(true, (server, value) -> StatusEffectNetworking.sendRulesToAll(server)));
        STATUS_EFFECT_BOOK_ENABLED = GameRules.register("isStatusEffectBookEnabled", GameRules.Category.MISC, GameRules.BooleanValue.create(true, (server, value) -> StatusEffectNetworking.sendRulesToAll(server)));
    }

    public static StatusEffectRulesPacket packet(MinecraftServer server) {
        GameRules rules = server.getGameRules();
        return new StatusEffectRulesPacket(
                rules.getBoolean(DISPLAY_STATUS_EFFECT_ABOVE_ENTITIES),
                rules.getBoolean(DISPLAY_STATUS_EFFECT_ABOVE_PLAYERS),
                rules.getBoolean(STATUS_EFFECT_BOOK_ENABLED));
    }
}