package mcjty.lostcities.worldgen;

import mcjty.lostcities.config.LandscapeType;
import mcjty.lostcities.setup.Config;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * A heightmap for a chunk
 */
public class ChunkHeightmap {
    private int height;
    private final LandscapeType type;
    private final int groundLevel;

    // Only valid when 'calculateAccurateHeight()' is called
    private int minHeight;
    private int maxHeight;

    public ChunkHeightmap(LandscapeType type, int groundLevel) {
        this.groundLevel = groundLevel;
        this.type = type;
        height = Short.MIN_VALUE;
    }

    // Make a copy
    public ChunkHeightmap(ChunkHeightmap other) {
        this.height = other.height;
        this.type = other.type;
        this.groundLevel = other.groundLevel;
        this.minHeight = other.minHeight;
        this.maxHeight = other.maxHeight;
    }

    public void update(int y) {
        int current = height;
        if (y <= current) {
            return;
        }

        if (type == LandscapeType.CAVERN || type == LandscapeType.CAVERNSPHERES) {
            // Here we try to find the height inside the cavern itself. Ignoring the top layer
            int base = Math.max(groundLevel - 20, 1);
            if (y > 100 || y < base) {
                return;
            }
            if (y == 100) {
                y = 127;
            }
        }
        height = y;
    }

    public int getHeight() {
        if (height < -4000) {
            return groundLevel;
        }
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void calculateAccurateHeight(WorldGenLevel region, int chunkX, int chunkZ) {
        ServerChunkCache chunkProvider = region.getLevel().getChunkSource();
        ChunkGenerator generator = chunkProvider.getGenerator();
        int cx = chunkX << 4;
        int cz = chunkZ << 4;
        RandomState randomState = chunkProvider.randomState();
        // Average of height and 4 other points
        int height0;
        int height1;
        int height2;
        int height3;
        if (Config.OPTIMIZED_HEIGHTMAP.get()) {
            height0 = HeightGenOpt.getBaseHeight((NoiseBasedChunkGenerator) generator,cx + 2, cz + 2, region, randomState);
            height1 = HeightGenOpt.getBaseHeight((NoiseBasedChunkGenerator) generator,cx + 2, cz + 14,region, randomState);
            height2 = HeightGenOpt.getBaseHeight((NoiseBasedChunkGenerator) generator,cx + 14, cz + 2, region, randomState);
            height3 = HeightGenOpt.getBaseHeight((NoiseBasedChunkGenerator) generator,cx + 14, cz + 14, region, randomState);
        } else {
            height0 = generator.getBaseHeight(cx + 2, cz + 2, Heightmap.Types.OCEAN_FLOOR_WG, region, randomState);
            height1 = generator.getBaseHeight(cx + 2, cz + 14, Heightmap.Types.OCEAN_FLOOR_WG, region, randomState);
            height2 = generator.getBaseHeight(cx + 14, cz + 2, Heightmap.Types.OCEAN_FLOOR_WG, region, randomState);
            height3 = generator.getBaseHeight(cx + 14, cz + 14, Heightmap.Types.OCEAN_FLOOR_WG, region, randomState);
        }
        minHeight = Math.min(height, Math.min(height0, Math.min(height1, Math.min(height2, height3))));
        maxHeight = Math.max(height, Math.max(height0, Math.max(height1, Math.max(height2, height3))));
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }
}
