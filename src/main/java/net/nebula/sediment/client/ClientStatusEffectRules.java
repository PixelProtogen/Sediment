package net.nebula.sediment.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ClientStatusEffectRules {

    private static boolean aboveEntities = true;
    private static boolean abovePlayers = true;
    private static boolean bookEnabled = true;

    private ClientStatusEffectRules() {
    }

    private static boolean bookUnlocked = false;

    public static void setBookUnlocked(boolean unlocked) {
        bookUnlocked = unlocked;
    }

    public static boolean bookUnlocked() {
        return bookUnlocked;
    }

    public static boolean bookAvailable() {
        return bookUseAllowed() && bookUnlocked();
    }

    public static void set(boolean newAboveEntities, boolean newAbovePlayers, boolean newBookEnabled) {
        aboveEntities = newAboveEntities;
        abovePlayers = newAbovePlayers;
        bookEnabled = newBookEnabled;
    }

    public static boolean displayAboveEntities() {
        return aboveEntities;
    }

    public static boolean displayAbovePlayers() {
        return abovePlayers;
    }

    public static boolean bookUseAllowed() {
        return bookEnabled;
    }

    public static boolean shouldDisplay(Entity entity) {
        return entity instanceof Player ? abovePlayers : aboveEntities;
    }
}