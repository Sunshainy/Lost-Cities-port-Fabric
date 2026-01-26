package com.lostcity.assets;

/**
 * Предопределённая улица в PredefinedCity.
 * Портировано из mcjty.lostcities.worldgen.lost.regassets.data.PredefinedStreet (оригинальный Forge мод).
 * 
 * Этап 1.3: Базовая поддержка Predefined Assets.
 */
public class PredefinedStreet {
    /** Относительная координата X чанка от центра PredefinedCity. */
    public final int relChunkX;
    /** Относительная координата Z чанка от центра PredefinedCity. */
    public final int relChunkZ;
    
    public PredefinedStreet(int relChunkX, int relChunkZ) {
        this.relChunkX = relChunkX;
        this.relChunkZ = relChunkZ;
    }
}
