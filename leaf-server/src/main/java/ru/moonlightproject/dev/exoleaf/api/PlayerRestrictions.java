package ru.moonlightproject.dev.exoleaf.api;

import java.util.UUID;

/**
 * Hides a player's data from their own client until released: the server sends an empty
 * inventory and holds back the recipe book and advancements. Meant for authentication plugins:
 * restrict in {@code AsyncPlayerPreLoginEvent}, release after a successful login. The server data
 * is untouched; only what the client receives changes. Restrictions are cleared when the player
 * quits.
 */
public interface PlayerRestrictions {

    /**
     * Restricts a player. Safe to call before the player joins.
     *
     * @param playerId player UUID
     */
    void restrict(UUID playerId);

    /**
     * Releases a player and sends the real inventory, recipe book and advancements. Safe to call
     * from any thread; the resync runs on the server thread.
     *
     * @param playerId player UUID
     */
    void release(UUID playerId);

    /**
     * Reports whether a player is restricted.
     *
     * @param playerId player UUID
     * @return true when restricted
     */
    boolean isRestricted(UUID playerId);

    /**
     * Shows an item in a slot of the player's own inventory view without changing server data.
     * Works while the player is restricted, for example to show a CAPTCHA map in the main hand.
     *
     * @param player online player
     * @param slot slot of the player inventory menu (hotbar slot {@code n} is {@code 36 + n})
     * @param item item to show
     */
    void sendVirtualItem(org.bukkit.entity.Player player, int slot, org.bukkit.inventory.ItemStack item);
}
