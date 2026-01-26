package com.lostcity.assets;

/**
 * Предопределённое здание в PredefinedCity.
 * Портировано из mcjty.lostcities.worldgen.lost.regassets.data.PredefinedBuilding (оригинальный Forge мод).
 * 
 * Этап 1.3: Базовая поддержка Predefined Assets.
 */
public class PredefinedBuilding {
    /** ID здания (например, "lostcity:building1"). */
    public final String building;
    /** Относительная координата X чанка от центра PredefinedCity. */
    public final int relChunkX;
    /** Относительная координата Z чанка от центра PredefinedCity. */
    public final int relChunkZ;
    /** Является ли это multibuilding (занимает несколько чанков). */
    public final boolean multi;
    /** Предотвращать разрушения (ruins) для этого здания. */
    public final boolean preventRuins;
    
    public PredefinedBuilding(String building, int relChunkX, int relChunkZ, boolean multi, boolean preventRuins) {
        this.building = building;
        this.relChunkX = relChunkX;
        this.relChunkZ = relChunkZ;
        this.multi = multi;
        this.preventRuins = preventRuins;
    }
}
