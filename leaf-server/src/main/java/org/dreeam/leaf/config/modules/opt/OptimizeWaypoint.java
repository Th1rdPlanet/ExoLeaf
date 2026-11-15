package org.dreeam.leaf.config.modules.opt;

import org.dreeam.leaf.config.ConfigModule;
import org.dreeam.leaf.config.ConfigCategory;
import org.dreeam.leaf.config.annotations.Experimental;

public class OptimizeWaypoint extends ConfigModule {
    public String basePath() {
        return ConfigCategory.PERF.basePath() + ".optimize-waypoint";
    }

    @Experimental
    public static boolean enabled = false;

    // Leaf - waypoint update throttling: base ticks between per-player /
    // per-waypoint connection re-evaluations; scaled up under tick pressure.
    public static int updateIntervalTicks = 5;

    @Override
    public void onLoaded() {
        enabled = globalConfig.getBoolean(basePath(), enabled);
        updateIntervalTicks = Math.max(1, globalConfig.getInt(ConfigCategory.PERF.basePath() + ".optimize-waypoint-interval", updateIntervalTicks));
    }
}
