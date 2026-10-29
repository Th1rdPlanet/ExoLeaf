package org.dreeam.leaf.config;

public enum ConfigCategory {
    ASYNC("async"),
    PERF("performance"),
    FIXES("fixes"),
    GAMEPLAY("gameplay-mechanisms"),
    NETWORK("network"),
    MISC("misc"),
    LOBBY("lobby-mode"); // ExoLeaf - lobby mode

    private final String basePath;

    private static final ConfigCategory[] VALUES = ConfigCategory.values();

    ConfigCategory(String basePath) {
        this.basePath = basePath;
    }

    public String basePath() {
        return this.basePath;
    }

    public static ConfigCategory[] categoryValues() {
        return VALUES;
    }
}
