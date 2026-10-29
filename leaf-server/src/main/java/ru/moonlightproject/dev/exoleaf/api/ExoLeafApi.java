package ru.moonlightproject.dev.exoleaf.api;

/** Services of ExoLeaf. */
public interface ExoLeafApi {

    /**
     * Returns the ExoLeaf version.
     *
     * @return version
     */
    String version();

    /**
     * Returns player data restrictions (inventory, recipes, advancements hidden from the client).
     *
     * @return restrictions
     */
    PlayerRestrictions restrictions();

    /**
     * Returns the lobby mode state from {@code leaf-global.yml → lobby-mode}.
     *
     * @return lobby mode
     */
    LobbyModeInfo lobbyMode();
}
