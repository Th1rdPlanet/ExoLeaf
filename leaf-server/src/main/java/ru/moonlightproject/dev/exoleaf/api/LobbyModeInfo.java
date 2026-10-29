package ru.moonlightproject.dev.exoleaf.api;

/** Read-only view of the lobby mode configuration. */
public interface LobbyModeInfo {

    /**
     * Whether lobby mode is enabled.
     *
     * @return enabled flag
     */
    boolean enabled();

    /**
     * Whether lobby mode applies to a world.
     *
     * @param worldName world name
     * @return true if the world runs in lobby mode
     */
    boolean appliesTo(String worldName);

    /**
     * Whether non-player entities are frozen.
     *
     * @return flag
     */
    boolean entitiesFrozen();

    /**
     * Whether redstone is disabled.
     *
     * @return flag
     */
    boolean redstoneDisabled();

    /**
     * Whether world and player data saving is disabled.
     *
     * @return flag
     */
    boolean savingDisabled();
}
