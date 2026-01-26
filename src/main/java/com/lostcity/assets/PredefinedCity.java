package com.lostcity.assets;

import java.util.ArrayList;
import java.util.List;

/**
 * Предопределённый город с фиксированным центром и радиусом.
 * Портировано из mcjty.lostcities.worldgen.lost.cityassets.PredefinedCity (оригинальный Forge мод).
 * 
 * Этап 1.3: Базовая поддержка Predefined Assets.
 */
public class PredefinedCity {
    /** Имя/ID предопределённого города. */
    public final String name;
    /** Dimension (например, "minecraft:overworld"). */
    public final String dimension;
    /** X координата центра города (в чанках). */
    public final int chunkX;
    /** Z координата центра города (в чанках). */
    public final int chunkZ;
    /** Радиус города (в блоках). */
    public final int radius;
    /** CityStyle для этого города (может быть null). */
    public final String cityStyle;
    /** Список предопределённых зданий. */
    public final List<PredefinedBuilding> predefinedBuildings;
    /** Список предопределённых улиц. */
    public final List<PredefinedStreet> predefinedStreets;
    
    public PredefinedCity(String name, String dimension, int chunkX, int chunkZ, int radius, String cityStyle,
                         List<PredefinedBuilding> predefinedBuildings, List<PredefinedStreet> predefinedStreets) {
        this.name = name;
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.radius = radius;
        this.cityStyle = cityStyle;
        this.predefinedBuildings = predefinedBuildings != null ? predefinedBuildings : new ArrayList<>();
        this.predefinedStreets = predefinedStreets != null ? predefinedStreets : new ArrayList<>();
    }
    
    /**
     * Получить список предопределённых зданий.
     */
    public List<PredefinedBuilding> getPredefinedBuildings() {
        return predefinedBuildings;
    }
    
    /**
     * Получить список предопределённых улиц.
     */
    public List<PredefinedStreet> getPredefinedStreets() {
        return predefinedStreets;
    }
}
