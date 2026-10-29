package ru.moonlightproject.dev.exoleaf;

import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.dreeam.leaf.config.modules.lobby.LobbyMode;
import org.jspecify.annotations.Nullable;

/**
 * Lobby-mode generator: every chunk is empty except an optional platform under the spawn point
 * (0, {@value #SPAWN_Y}, 0). No noise, caves, decorations, structures or mobs, and a single
 * fixed biome: without it the world would still sample the climate noise for every new chunk,
 * which is most of the cost of generating a chunk.
 */
public final class VoidChunkGenerator extends ChunkGenerator {

    static final int SPAWN_Y = 64;

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        if (!LobbyMode.spawnPlatform) {
            return;
        }
        int radius = Math.max(0, Math.min(32, LobbyMode.platformRadius));
        Material material = Material.matchMaterial(LobbyMode.platformBlock);
        if (material == null || !material.isBlock()) {
            material = Material.BEDROCK;
        }
        int y = SPAWN_Y - 1;
        if (y < chunkData.getMinHeight() || y >= chunkData.getMaxHeight()) {
            return;
        }
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.floorDiv(x, 16) == chunkX && Math.floorDiv(z, 16) == chunkZ) {
                    chunkData.setBlock(Math.floorMod(x, 16), y, Math.floorMod(z, 16), material);
                }
            }
        }
    }

    private static final BiomeProvider SINGLE_BIOME = new BiomeProvider() {
        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            return Biome.PLAINS;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return List.of(Biome.PLAINS);
        }
    };

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return SINGLE_BIOME;
    }

    @Override
    public @Nullable Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, SPAWN_Y, 0.5);
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
