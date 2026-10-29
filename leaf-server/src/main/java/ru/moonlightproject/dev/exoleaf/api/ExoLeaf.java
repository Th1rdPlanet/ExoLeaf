package ru.moonlightproject.dev.exoleaf.api;

import java.util.Optional;

/**
 * Entry point of the ExoLeaf API. Available on ExoLeaf servers from plugin load onwards. Plugins
 * that also run on Paper/Leaf should use {@link #find()}.
 */
public final class ExoLeaf {

    private static volatile ExoLeafApi instance;

    private ExoLeaf() {
    }

    /**
     * Returns the API.
     *
     * @return API
     * @throws IllegalStateException when the server is not ExoLeaf
     */
    public static ExoLeafApi api() {
        return find().orElseThrow(() -> new IllegalStateException("This server is not ExoLeaf"));
    }

    /**
     * Returns the API if the server is ExoLeaf.
     *
     * @return API or empty
     */
    public static Optional<ExoLeafApi> find() {
        ExoLeafApi current = instance;
        if (current == null) {
            try {
                current = (ExoLeafApi) Class.forName("ru.moonlightproject.dev.exoleaf.ExoLeafImpl")
                    .getField("INSTANCE").get(null);
                instance = current;
            } catch (ReflectiveOperationException | LinkageError absent) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }
}
