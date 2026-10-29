package ru.moonlightproject.dev.exoleaf;

import io.netty.channel.ChannelOutboundBuffer;
import io.netty.util.internal.PlatformDependent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.dreeam.leaf.config.modules.network.SlowClientProtection;
import org.slf4j.Logger;

/**
 * Network-wide guard: when queued outbound data fills Netty's direct memory, the connections
 * with the largest queues are dropped before allocation failures break every connection.
 */
public final class SlowClients {

    private static final Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private SlowClients() {
    }

    /** Called every tick with the connection list (under its lock). */
    public static void enforce(List<Connection> connections) {
        if (!SlowClientProtection.enabled) {
            return;
        }
        long max = PlatformDependent.maxDirectMemory();
        long used = PlatformDependent.usedDirectMemory();
        if (max <= 0 || used < 0 || used * 100 < max * SlowClientProtection.globalPercent) {
            return;
        }
        List<long[]> pending = new ArrayList<>();
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            if (connection.channel == null || !connection.channel.isOpen()) {
                continue;
            }
            ChannelOutboundBuffer buffer = connection.channel.unsafe().outboundBuffer();
            long bytes = buffer == null ? 0L : buffer.totalPendingWriteBytes();
            if (bytes > 0) {
                pending.add(new long[] {bytes, i});
            }
        }
        pending.sort((a, b) -> Long.compare(b[0], a[0]));
        long target = max * SlowClientProtection.globalRecoverPercent / 100;
        long projected = used;
        int dropped = 0;
        for (long[] entry : pending) {
            if (projected <= target) {
                break;
            }
            Connection connection = connections.get((int) entry[1]);
            connection.disconnect(Component.literal("Connection too slow"));
            projected -= entry[0];
            dropped++;
        }
        if (dropped > 0) {
            LOGGER.warn(Ansi.line("[!] network  » direct-memory:{}/{}MiB  slow-clients-dropped:{}"),
                used / 1048576, max / 1048576, dropped);
        }
    }
}
