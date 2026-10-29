package org.dreeam.leaf.config.modules.lobby;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

/**
 * ExoLeaf lobby mode: a server that only hosts players, blocks, portals and plugins (lobby or
 * Selene auth node) skips the simulation it does not need. Every part is a separate switch; the
 * whole mode is off unless {@code lobby-mode.enabled} is true.
 */
public class LobbyMode extends ConfigModules {

    public static boolean enabled = false;
    public static List<String> worlds = new ArrayList<>();

    public static boolean freezeEntities = true;
    public static Set<String> tickingEntityTypes = new HashSet<>();
    public static boolean disableMobSpawning = true;
    public static boolean disableRandomTicks = true;
    public static boolean disableFluids = true;
    public static boolean disableWeather = true;

    public static boolean disableRedstone = true;

    public static boolean hideNametags = true;
    public static boolean hideLatency = true;

    public static boolean disableWorldSaving = true;
    public static boolean disablePlayerDataSaving = true;

    public static boolean voidWorld = false;
    public static boolean spawnPlatform = true;
    public static String platformBlock = "minecraft:bedrock";
    public static int platformRadius = 2;

    public String getBasePath() {
        return EnumConfigCategory.LOBBY.getBaseKeyName();
    }

    @Override
    public void onLoaded() {
        String base = getBasePath();
        config.addComment(base, """
            ExoLeaf: режим лобби / сервера авторизации. Сервер обслуживает только игроков,
            блоки, порталы и плагины и не тратит ресурсы на остальную симуляцию.
            Каждая часть включается отдельно; весь режим работает только при enabled: true.""");
        enabled = config.getBoolean(base + ".enabled", enabled, "Включить режим лобби.");
        worlds = config.getList(base + ".worlds", worlds,
            "Миры, к которым применяется режим. Пусто — ко всем мирам.");

        freezeEntities = config.getBoolean(base + ".entities.freeze", freezeEntities, """
            Не тикать сущности, кроме игроков и типов из tick-types: мобы, предметы, снаряды
            стоят на месте. NPC и голограммы плагинов на пакетах это не затрагивает.""");
        List<String> types = config.getList(base + ".entities.tick-types", List.of("minecraft:armor_stand"),
            "Типы сущностей, которые продолжают тикать (например minecraft:armor_stand для NPC на стойках).");
        Set<String> normalized = new HashSet<>();
        for (String type : types) {
            String value = type.trim().toLowerCase(Locale.ROOT);
            normalized.add(value.contains(":") ? value : "minecraft:" + value);
        }
        tickingEntityTypes = normalized;
        tickingCache = new java.util.concurrent.ConcurrentHashMap<>();
        disableMobSpawning = config.getBoolean(base + ".entities.disable-natural-spawning", disableMobSpawning,
            "Не спавнить мобов естественным образом.");
        disableRandomTicks = config.getBoolean(base + ".world.disable-random-ticks", disableRandomTicks,
            "Без случайных тиков блоков: трава не растёт, лёд не тает, листва не опадает.");
        disableFluids = config.getBoolean(base + ".world.disable-fluids", disableFluids,
            "Вода и лава не растекаются.");
        disableWeather = config.getBoolean(base + ".world.disable-weather", disableWeather,
            "Без смены погоды и молний.");

        disableRedstone = config.getBoolean(base + ".redstone.disable", disableRedstone, """
            Редстоун-компоненты (провод, повторители, компараторы, поршни, наблюдатели, раздатчики,
            TNT, факелы) не реагируют на сигналы. Нажатия кнопок и плит по-прежнему приходят плагинам
            событиями.""");

        hideNametags = config.getBoolean(base + ".players.hide-nametags", hideNametags, """
            Не показывать ники над игроками (служебная команда скорборда с невидимыми никами).
            Если плагин сам раздаёт игрокам команды скорборда, его настройки ников главнее.""");
        hideLatency = config.getBoolean(base + ".players.hide-latency", hideLatency,
            "Не рассылать обновления пинга в списке игроков.");

        disableWorldSaving = config.getBoolean(base + ".saving.disable-world", disableWorldSaving,
            "Не сохранять чанки и сущности: изменения живут до перезапуска.");
        disablePlayerDataSaving = config.getBoolean(base + ".saving.disable-player-data", disablePlayerDataSaving,
            "Не сохранять данные игроков, статистику и достижения.");

        voidWorld = config.getBoolean(base + ".void-world.enabled", voidWorld, """
            Генерировать новые чанки пустыми (как пресет The Void). Уже созданные чанки не
            меняются: для чистого пустого мира удалите папку мира перед запуском.""");
        spawnPlatform = config.getBoolean(base + ".void-world.spawn-platform", spawnPlatform,
            "Поставить платформу под точкой спавна, чтобы игроки не падали в пустоту.");
        platformBlock = config.getString(base + ".void-world.platform-block", platformBlock,
            "Блок платформы.");
        platformRadius = config.getInt(base + ".void-world.platform-radius", platformRadius,
            "Радиус платформы в блоках (0 — один блок).");
    }

    @Override
    public void onPostLoaded() {
        if (!enabled) {
            return;
        }
        List<String> parts = new ArrayList<>();
        if (freezeEntities) parts.add("entities:frozen");
        if (disableMobSpawning) parts.add("spawn:off");
        if (disableRandomTicks) parts.add("random-ticks:off");
        if (disableFluids) parts.add("fluids:off");
        if (disableWeather) parts.add("weather:off");
        if (disableRedstone) parts.add("redstone:off");
        if (hideNametags) parts.add("nametags:hidden");
        if (hideLatency) parts.add("latency:hidden");
        if (disableWorldSaving || disablePlayerDataSaving) parts.add("saving:off");
        if (voidWorld) parts.add("void-world");
        LOGGER.info(ru.moonlightproject.dev.exoleaf.Ansi.line("[*] lobby    » {}  worlds:{}"), String.join("  ", parts), worlds.isEmpty() ? "all" : String.join(",", worlds));
    }

    /** Whether lobby mode applies to the world with this name. */
    public static boolean appliesTo(String worldName) {
        return enabled && (worlds.isEmpty() || worlds.contains(worldName));
    }

    private static volatile java.util.Map<net.minecraft.world.entity.EntityType<?>, Boolean> tickingCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    /** Whether an entity type keeps ticking when entities are frozen (cached per type). */
    public static boolean keepsTicking(net.minecraft.world.entity.EntityType<?> type) {
        return tickingCache.computeIfAbsent(type,
            t -> tickingEntityTypes.contains(net.minecraft.world.entity.EntityType.getKey(t).toString()));
    }

    /** Redstone components and blocks that only react to redstone power. */
    public static boolean isRedstone(net.minecraft.world.level.block.Block block) {
        return block instanceof net.minecraft.world.level.block.RedStoneWireBlock
            || block instanceof net.minecraft.world.level.block.DiodeBlock
            || block instanceof net.minecraft.world.level.block.piston.PistonBaseBlock
            || block instanceof net.minecraft.world.level.block.ObserverBlock
            || block instanceof net.minecraft.world.level.block.DispenserBlock
            || block instanceof net.minecraft.world.level.block.RedstoneTorchBlock
            || block instanceof net.minecraft.world.level.block.TntBlock
            || block instanceof net.minecraft.world.level.block.PoweredRailBlock
            || block instanceof net.minecraft.world.level.block.RedstoneLampBlock
            || block instanceof net.minecraft.world.level.block.NoteBlock
            || block instanceof net.minecraft.world.level.block.DoorBlock
            || block instanceof net.minecraft.world.level.block.TrapDoorBlock
            || block instanceof net.minecraft.world.level.block.FenceGateBlock;
    }
}
