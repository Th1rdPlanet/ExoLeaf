package ru.moonlightproject.dev.exoleaf;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.dreeam.leaf.config.modules.lobby.LobbyMode;
import ru.moonlightproject.dev.exoleaf.api.ExoLeafApi;
import ru.moonlightproject.dev.exoleaf.api.LobbyModeInfo;
import ru.moonlightproject.dev.exoleaf.api.PlayerRestrictions;

/** Server-side implementation of the ExoLeaf API. */
public final class ExoLeafImpl implements ExoLeafApi, PlayerRestrictions, LobbyModeInfo {

    public static final String VERSION = "1.0.0";
    public static final ExoLeafImpl INSTANCE = new ExoLeafImpl();

    private final Set<UUID> restricted = ConcurrentHashMap.newKeySet();

    private ExoLeafImpl() {
    }

    /** Fast check used by the patched send paths. */
    public static boolean restricted(ServerPlayer player) {
        return !INSTANCE.restricted.isEmpty() && INSTANCE.restricted.contains(player.getUUID());
    }

    /** Called when a player leaves. */
    public static void clear(UUID playerId) {
        INSTANCE.restricted.remove(playerId);
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public PlayerRestrictions restrictions() {
        return this;
    }

    @Override
    public LobbyModeInfo lobbyMode() {
        return this;
    }

    @Override
    public void restrict(UUID playerId) {
        restricted.add(playerId);
    }

    @Override
    public void release(UUID playerId) {
        if (!restricted.remove(playerId)) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || restricted.contains(playerId)) {
                return;
            }
            player.inventoryMenu.sendAllDataToRemote();
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.sendAllDataToRemote();
            }
            player.getRecipeBook().sendInitialRecipeBook(player);
            player.getAdvancements().flushDirty(player, false);
        });
    }

    @Override
    public void sendVirtualItem(org.bukkit.entity.Player player, int slot, org.bukkit.inventory.ItemStack item) {
        ServerPlayer handle = ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();
        handle.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
            handle.inventoryMenu.containerId, handle.inventoryMenu.incrementStateId(), slot,
            org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(item)));
    }

    @Override
    public boolean isRestricted(UUID playerId) {
        return restricted.contains(playerId);
    }

    @Override
    public boolean enabled() {
        return LobbyMode.enabled;
    }

    @Override
    public boolean appliesTo(String worldName) {
        return LobbyMode.appliesTo(worldName);
    }

    @Override
    public boolean entitiesFrozen() {
        return LobbyMode.enabled && LobbyMode.freezeEntities;
    }

    @Override
    public boolean redstoneDisabled() {
        return LobbyMode.enabled && LobbyMode.disableRedstone;
    }

    @Override
    public boolean savingDisabled() {
        return LobbyMode.enabled && LobbyMode.disableWorldSaving;
    }
}
