package com.lostcity.worldgen;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;

/**
 * Ключ кэша "измерение + чанк".
 *
 * Оригинал использует mcjty.lostcities.varia.ChunkCoord, который тоже включает dimension.
 * В порту кэши высот/уровней были ключевались только по (x,z) — при одновременно загруженных
 * измерениях с Lost Cities значения из одного измерения подставлялись в другое.
 *
 * RegistryKey — синглтон на идентификатор, так что сравнение/хеш дешёвые и строки не аллоцируются.
 */
public record DimChunk(RegistryKey<World> dimension, int x, int z) {

    public static DimChunk of(StructureWorldAccess world, int chunkX, int chunkZ) {
        RegistryKey<World> key = null;
        if (world != null) {
            try {
                key = world.toServerWorld().getRegistryKey();
            } catch (Exception e) {
                key = null;
            }
        }
        return new DimChunk(key, chunkX, chunkZ);
    }
}
