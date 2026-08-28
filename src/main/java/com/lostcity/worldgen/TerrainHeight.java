package com.lostcity.worldgen;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * Сэмплирование высоты террейна НЕ через чанки, а напрямую через шум генератора.
 *
 * Оригинал (Forge): LostCityTerrainFeature.generateHeightmap() / ChunkHeightmap.calculateAccurateHeight()
 * используют generator.getBaseHeight(...) — в yarn это ChunkGenerator.getHeight(...).
 *
 * Почему это важно: генерация города обращается к высоте СОСЕДНИХ чанков (соседи для границ,
 * усреднение USE_AVG_HEIGHTMAP, поиск концов магистрали в Highway). Эти чанки лежат далеко за
 * пределами региона (ChunkRegion), выданного фиче. Любой запрос через world.getTopY() ->
 * ChunkRegion.getChunk() для чанка вне региона печатает в лог
 *   "Requested chunk : X Z" / "Region bounds : ..."
 * и бросает RuntimeException("We are asking a region for a chunk out of bound").
 *
 * ChunkGenerator.getHeight() читает только шум и HeightLimitView, никаких чанков не трогает,
 * поэтому работает для ЛЮБЫХ координат и даёт одинаковый результат независимо от порядка
 * генерации (важно для Chunky и вообще для многопоточной догенерации).
 */
public final class TerrainHeight {

    private TerrainHeight() {
    }

    /**
     * Высота поверхности (OCEAN_FLOOR_WG) в блоковых координатах, из шума генератора.
     *
     * @return высота, либо Integer.MIN_VALUE если генератор недоступен (вызывающий должен
     *         подставить свой fallback и НЕ кэшировать результат).
     */
    public static int sample(StructureWorldAccess world, int blockX, int blockZ) {
        if (world == null) {
            return Integer.MIN_VALUE;
        }
        try {
            ServerWorld serverWorld = world.toServerWorld();
            if (serverWorld == null) {
                return Integer.MIN_VALUE;
            }
            ChunkManager chunkManager = serverWorld.getChunkManager();
            if (!(chunkManager instanceof ServerChunkManager serverChunkManager)) {
                return Integer.MIN_VALUE;
            }
            ChunkGenerator generator = serverChunkManager.getChunkGenerator();
            NoiseConfig noiseConfig = serverChunkManager.getNoiseConfig();
            if (generator == null || noiseConfig == null) {
                return Integer.MIN_VALUE;
            }
            // world используется только как HeightLimitView (bottomY/height) — чанки не запрашиваются.
            return generator.getHeight(blockX, blockZ, Heightmap.Type.OCEAN_FLOOR_WG, world, noiseConfig);
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    /** Высота в центре чанка — то, что оригинал берёт как высоту чанка (cx + 8, cz + 8). */
    public static int sampleChunkCenter(StructureWorldAccess world, int chunkX, int chunkZ) {
        return sample(world, (chunkX << 4) + 8, (chunkZ << 4) + 8);
    }

    /**
     * Биом в блоковых координатах, из BiomeSource генератора, без обращения к чанкам.
     *
     * Ровно то же, что делает оригинал в DefaultDimensionInfo.getBiome():
     * generator.getBiomeSource().getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler).
     *
     * world.getBiome() здесь не подходит по той же причине, что и world.getTopY():
     * он идёт в ChunkRegion.getChunk(), и для соседнего чанка за границей региона
     * на 1.20.6+ это уже не предупреждение, а IllegalStateException
     * "Requested chunk unavailable during world generation", который валит генерацию чанка.
     *
     * @return биом, либо null если генератор недоступен (вызывающий подставляет своё поведение)
     */
    public static RegistryEntry<Biome> sampleBiome(StructureWorldAccess world, int blockX, int blockY, int blockZ) {
        if (world == null) {
            return null;
        }
        try {
            ServerWorld serverWorld = world.toServerWorld();
            if (serverWorld == null) {
                return null;
            }
            ChunkManager chunkManager = serverWorld.getChunkManager();
            if (!(chunkManager instanceof ServerChunkManager serverChunkManager)) {
                return null;
            }
            ChunkGenerator generator = serverChunkManager.getChunkGenerator();
            NoiseConfig noiseConfig = serverChunkManager.getNoiseConfig();
            if (generator == null || noiseConfig == null) {
                return null;
            }
            // getBiome здесь — это getNoiseBiome оригинала: координаты в биомных единицах (блок >> 2).
            return generator.getBiomeSource().getBiome(
                    blockX >> 2, blockY >> 2, blockZ >> 2, noiseConfig.getMultiNoiseSampler());
        } catch (Exception e) {
            return null;
        }
    }
}
