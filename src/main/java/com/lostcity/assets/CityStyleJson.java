package com.lostcity.assets;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * JSON представление CityStyle для загрузки из JSON.
 * Этап 3.1: Palettes через CityStyle
 */
public class CityStyleJson {
    
    /** Имя Style для получения палитр. */
    public String style;
    
    /** Наследование от другого CityStyle. */
    public String inherit;
    
    /** Теги для фильтрации. */
    @SerializedName("stuff_tags")
    public String[] stuffTags;
    
    /** Настройки зданий. */
    @SerializedName("buildingsettings")
    public BuildingSettingsJson buildingSettings;
    
    /** Блоки улиц. */
    @SerializedName("streetblocks")
    public StreetBlocksJson streetBlocks;
    
    /** Блоки парков. */
    @SerializedName("parkblocks")
    public ParkBlocksJson parkBlocks;
    
    /** Настройки парков. */
    @SerializedName("parksettings")
    public ParkSettingsJson parkSettings;
    
    /** Блоки коридоров. */
    @SerializedName("corridorblocks")
    public CorridorBlocksJson corridorBlocks;
    
    /** Настройки коридоров. */
    @SerializedName("corridorsettings")
    public CorridorSettingsJson corridorSettings;
    
    /** Блоки рельсов. */
    @SerializedName("railblocks")
    public RailBlocksJson railBlocks;
    
    /** Общие блоки. */
    @SerializedName("generalblocks")
    public GeneralBlocksJson generalBlocks;
    
    /** Настройки фонтанов. */
    @SerializedName("fountainsettings")
    public FountainSettingsJson fountainSettings;
    
    /** Настройки фасадов. */
    @SerializedName("frontsettings")
    public FrontSettingsJson frontSettings;
    
    /** Блоки сфер (для space профиля). */
    @SerializedName("sphereblocks")
    public SphereBlocksJson sphereBlocks;
    
    /** Селекторы: buildings, multibuildings (оригинал: selectors). */
    public SelectorsJson selectors;
    
    /**
     * selectors.multibuildings[]. Оригинал: ObjectSelector factor+value.
     */
    public static class FactorValueJson {
        public Float factor;
        public String value;
    }
    
    public static class SelectorsJson {
        public List<FactorValueJson> multibuildings;
    }
    
    /**
     * Настройки зданий.
     */
    public static class BuildingSettingsJson {
        @SerializedName("buildingchance")
        public Float buildingChance;
        
        @SerializedName("minfloorcount")
        public Integer minFloorCount;
        
        @SerializedName("maxfloorcount")
        public Integer maxFloorCount;
        
        @SerializedName("mincellarcount")
        public Integer minCellarCount;
        
        @SerializedName("maxcellarcount")
        public Integer maxCellarCount;
    }
    
    /**
     * Блоки улиц.
     */
    public static class StreetBlocksJson {
        public Character street;
        @SerializedName("streetbase")
        public Character streetBase;
        @SerializedName("streetvariant")
        public Character streetVariant;
        public Character border;
        public Character wall;
    }
    
    /**
     * Блоки парков.
     */
    public static class ParkBlocksJson {
        public Character elevation;
        public Character grass;
    }
    
    /**
     * Настройки парков.
     */
    public static class ParkSettingsJson {
        @SerializedName("parkchance")
        public Float parkChance;
        
        @SerializedName("avoidfoliage")
        public Boolean avoidFoliage;
        
        @SerializedName("parkborder")
        public Boolean parkBorder;
        
        @SerializedName("parkelevation")
        public Boolean parkElevation;
        
        @SerializedName("parkstreetthreshold")
        public Integer parkStreetThreshold;
    }
    
    /**
     * Блоки коридоров.
     */
    public static class CorridorBlocksJson {
        public Character roof;
        public Character glass;
    }
    
    /**
     * Настройки коридоров.
     */
    public static class CorridorSettingsJson {
        @SerializedName("corridorchance")
        public Float corridorChance;
    }
    
    /**
     * Блоки рельсов.
     */
    public static class RailBlocksJson {
        @SerializedName("railmain")
        public Character railMain;
    }
    
    /**
     * Общие блоки.
     */
    public static class GeneralBlocksJson {
        public Character ironbars;
        public Character glowstone;
        public Character leaves;
        @SerializedName("rubbledirt")
        public Character rubbleDirt;
    }
    
    /**
     * Настройки фонтанов.
     */
    public static class FountainSettingsJson {
        @SerializedName("fountainchance")
        public Float fountainChance;
    }
    
    /**
     * Настройки фасадов.
     */
    public static class FrontSettingsJson {
        @SerializedName("frontchance")
        public Float frontChance;
    }
    
    /**
     * Блоки сфер.
     */
    public static class SphereBlocksJson {
        public Character inner;
        public Character border;
        public Character glass;
    }
}
