package org.dreeam.leaf.config.modules.network;

import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

/**
 * ExoLeaf: protection against clients that read slower than the server writes. Netty keeps
 * unsent data in direct memory without a per-connection limit, so a few slow (or deliberately
 * stalled) clients could exhaust it and drop every connection on the server.
 */
public class SlowClientProtection extends ConfigModules {

    public static boolean enabled = true;
    public static long maxPendingBytes = 16L * 1024 * 1024;
    public static long maxUnwritableNanos = 60L * 1_000_000_000L;
    public static int globalPercent = 75;
    public static int globalRecoverPercent = 60;

    public String getBasePath() {
        return EnumConfigCategory.NETWORK.getBaseKeyName() + ".slow-client-protection";
    }

    @Override
    public void onLoaded() {
        enabled = config.getBoolean(getBasePath() + ".enabled", enabled, """
            ExoLeaf: отключать клиентов, которые не успевают принимать данные. Без этого неотправленные
            пакеты копятся в памяти без предела, и несколько медленных (или специально зависших)
            клиентов могут исчерпать direct memory и оборвать соединения всех игроков.""");
        int megabytes = config.getInt(getBasePath() + ".max-pending-megabytes", 16,
            "Предел неотправленных данных на одно соединение, МБ.");
        maxPendingBytes = Math.max(1, megabytes) * 1024L * 1024L;
        int seconds = config.getInt(getBasePath() + ".max-unwritable-seconds", 60,
            "Сколько секунд соединение может не принимать данные, прежде чем будет отключено.");
        maxUnwritableNanos = Math.max(1, seconds) * 1_000_000_000L;
        globalPercent = Math.max(10, Math.min(99, config.getInt(getBasePath() + ".direct-memory-percent", 75, """
            Общий предел: если очереди всех соединений заняли больше этой доли direct memory (%),
            отключаются клиенты с самыми большими очередями, пока занятость не опустится до
            direct-memory-recover-percent.""")));
        globalRecoverPercent = Math.max(5, Math.min(globalPercent - 1, config.getInt(getBasePath() + ".direct-memory-recover-percent", 60,
            "До какой доли direct memory (%) освобождать память при срабатывании общего предела.")));
    }
}
