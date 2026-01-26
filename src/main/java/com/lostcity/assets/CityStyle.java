package com.lostcity.assets;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Стиль города - определяет палитры, шансы зданий, этажи и другие параметры.
 * Портирован из CityStyle (оригинальный Forge мод).
 * 
 * Этап 3.1: Palettes через CityStyle
 */
public class CityStyle {
    
    /** Выбор multibuilding с весом. Оригинал: ObjectSelector. */
    public static class MultiBuildingChoice {
        public final float factor;
        public final String value;
        public MultiBuildingChoice(float factor, String value) {
            this.factor = factor;
            this.value = value;
        }
    }
    
    private final Identifier name;
    private String style;  // Имя Style для получения палитр
    private final String inherit;  // Наследование от другого CityStyle
    private final List<MultiBuildingChoice> multibuildingChoices = new ArrayList<>();
    
    // Building settings
    private Float buildingChance;  // Переопределение BUILDING_CHANCE
    private Integer minFloorCount;
    private Integer maxFloorCount;
    private Integer minCellarCount;
    private Integer maxCellarCount;
    
    // Street settings
    private Character streetBlock;
    private Character streetBaseBlock;
    private Character streetVariantBlock;
    private Character borderBlock;
    private Character wallBlock;
    
    // Park settings
    private Float parkChance;
    private Boolean avoidFoliage;
    private Boolean parkBorder;
    private Boolean parkElevation;
    private Integer parkStreetThreshold;
    private Character parkElevationBlock;
    private Character grassBlock;
    
    // Corridor settings
    private Float corridorChance;
    private Character corridorRoofBlock;
    private Character corridorGlassBlock;
    
    // Rail settings
    private Character railMainBlock;
    
    // General settings
    private Character ironbarsBlock;
    private Character glowstoneBlock;
    private Character leavesBlock;
    private Character rubbleDirtBlock;
    
    // Fountain settings
    private Float fountainChance;
    private Float frontChance;
    
    // Sphere settings (для space профиля)
    private Character sphereBlock;
    private Character sphereSideBlock;
    private Character sphereGlassBlock;
    
    /**
     * Конструктор из JSON объекта.
     */
    public CityStyle(Identifier name, CityStyleJson json) {
        this.name = name;
        this.style = json.style;
        this.inherit = json.inherit;
        
        // Building settings
        if (json.buildingSettings != null) {
            this.buildingChance = json.buildingSettings.buildingChance;
            this.minFloorCount = json.buildingSettings.minFloorCount;
            this.maxFloorCount = json.buildingSettings.maxFloorCount;
            this.minCellarCount = json.buildingSettings.minCellarCount;
            this.maxCellarCount = json.buildingSettings.maxCellarCount;
        } else {
            this.buildingChance = null;
            this.minFloorCount = null;
            this.maxFloorCount = null;
            this.minCellarCount = null;
            this.maxCellarCount = null;
        }
        
        // Street settings
        if (json.streetBlocks != null) {
            this.streetBlock = json.streetBlocks.street;
            this.streetBaseBlock = json.streetBlocks.streetBase;
            this.streetVariantBlock = json.streetBlocks.streetVariant;
            this.borderBlock = json.streetBlocks.border;
            this.wallBlock = json.streetBlocks.wall;
        } else {
            this.streetBlock = null;
            this.streetBaseBlock = null;
            this.streetVariantBlock = null;
            this.borderBlock = null;
            this.wallBlock = null;
        }
        
        // Park settings
        if (json.parkBlocks != null) {
            this.parkElevationBlock = json.parkBlocks.elevation;
            this.grassBlock = json.parkBlocks.grass;
        } else {
            this.parkElevationBlock = null;
            this.grassBlock = null;
        }
        if (json.parkSettings != null) {
            this.parkChance = json.parkSettings.parkChance;
            this.avoidFoliage = json.parkSettings.avoidFoliage;
            this.parkBorder = json.parkSettings.parkBorder;
            this.parkElevation = json.parkSettings.parkElevation;
            this.parkStreetThreshold = json.parkSettings.parkStreetThreshold;
        } else {
            this.parkChance = null;
            this.avoidFoliage = null;
            this.parkBorder = null;
            this.parkElevation = null;
            this.parkStreetThreshold = null;
        }
        
        // Corridor settings
        if (json.corridorBlocks != null) {
            this.corridorRoofBlock = json.corridorBlocks.roof;
            this.corridorGlassBlock = json.corridorBlocks.glass;
        } else {
            this.corridorRoofBlock = null;
            this.corridorGlassBlock = null;
        }
        if (json.corridorSettings != null) {
            this.corridorChance = json.corridorSettings.corridorChance;
        } else {
            this.corridorChance = null;
        }
        
        // Rail settings
        if (json.railBlocks != null) {
            this.railMainBlock = json.railBlocks.railMain;
        } else {
            this.railMainBlock = null;
        }
        
        // General settings
        if (json.generalBlocks != null) {
            this.ironbarsBlock = json.generalBlocks.ironbars;
            this.glowstoneBlock = json.generalBlocks.glowstone;
            this.leavesBlock = json.generalBlocks.leaves;
            this.rubbleDirtBlock = json.generalBlocks.rubbleDirt;
        } else {
            this.ironbarsBlock = null;
            this.glowstoneBlock = null;
            this.leavesBlock = null;
            this.rubbleDirtBlock = null;
        }
        
        // Fountain settings
        if (json.fountainSettings != null) {
            this.fountainChance = json.fountainSettings.fountainChance;
        } else {
            this.fountainChance = null;
        }
        if (json.frontSettings != null) {
            this.frontChance = json.frontSettings.frontChance;
        } else {
            this.frontChance = null;
        }
        
        // Sphere settings
        if (json.sphereBlocks != null) {
            this.sphereBlock = json.sphereBlocks.inner;
            this.sphereSideBlock = json.sphereBlocks.border;
            this.sphereGlassBlock = json.sphereBlocks.glass;
        } else {
            this.sphereBlock = null;
            this.sphereSideBlock = null;
            this.sphereGlassBlock = null;
        }
        if (json.selectors != null && json.selectors.multibuildings != null) {
            for (CityStyleJson.FactorValueJson fv : json.selectors.multibuildings) {
                if (fv != null && fv.value != null && !fv.value.isBlank()) {
                    float fac = fv.factor != null ? fv.factor : 1.0f;
                    multibuildingChoices.add(new MultiBuildingChoice(fac, fv.value));
                }
            }
        }
    }
    
    /**
     * Применить наследование от другого CityStyle.
     * Вызывается после загрузки всех CityStyle.
     * Оригинал: CityStyle.init() -> resolveInherit().
     */
    public void resolveInherit(CityStyle inheritFrom) {
        if (inheritFrom == null) return;
        
        // Наследуем style если не задан
        if (this.style == null && inheritFrom.style != null) {
            this.style = inheritFrom.style;
        }
        
        // Наследуем building settings если не заданы
        if (this.buildingChance == null) this.buildingChance = inheritFrom.buildingChance;
        if (this.minFloorCount == null) this.minFloorCount = inheritFrom.minFloorCount;
        if (this.maxFloorCount == null) this.maxFloorCount = inheritFrom.maxFloorCount;
        if (this.minCellarCount == null) this.minCellarCount = inheritFrom.minCellarCount;
        if (this.maxCellarCount == null) this.maxCellarCount = inheritFrom.maxCellarCount;
        
        // Наследуем street settings если не заданы
        if (this.streetBlock == null) this.streetBlock = inheritFrom.streetBlock;
        if (this.streetBaseBlock == null) this.streetBaseBlock = inheritFrom.streetBaseBlock;
        if (this.streetVariantBlock == null) this.streetVariantBlock = inheritFrom.streetVariantBlock;
        if (this.borderBlock == null) this.borderBlock = inheritFrom.borderBlock;
        if (this.wallBlock == null) this.wallBlock = inheritFrom.wallBlock;
        
        // Наследуем park settings если не заданы
        if (this.parkChance == null) this.parkChance = inheritFrom.parkChance;
        if (this.avoidFoliage == null) this.avoidFoliage = inheritFrom.avoidFoliage;
        if (this.parkBorder == null) this.parkBorder = inheritFrom.parkBorder;
        if (this.parkElevation == null) this.parkElevation = inheritFrom.parkElevation;
        if (this.parkStreetThreshold == null) this.parkStreetThreshold = inheritFrom.parkStreetThreshold;
        if (this.parkElevationBlock == null) this.parkElevationBlock = inheritFrom.parkElevationBlock;
        if (this.grassBlock == null) this.grassBlock = inheritFrom.grassBlock;
        
        // Наследуем corridor settings если не заданы
        if (this.corridorChance == null) this.corridorChance = inheritFrom.corridorChance;
        if (this.corridorRoofBlock == null) this.corridorRoofBlock = inheritFrom.corridorRoofBlock;
        if (this.corridorGlassBlock == null) this.corridorGlassBlock = inheritFrom.corridorGlassBlock;
        
        // Наследуем rail settings если не заданы
        if (this.railMainBlock == null) this.railMainBlock = inheritFrom.railMainBlock;
        
        // Наследуем general settings если не заданы
        if (this.ironbarsBlock == null) this.ironbarsBlock = inheritFrom.ironbarsBlock;
        if (this.glowstoneBlock == null) this.glowstoneBlock = inheritFrom.glowstoneBlock;
        if (this.leavesBlock == null) this.leavesBlock = inheritFrom.leavesBlock;
        if (this.rubbleDirtBlock == null) this.rubbleDirtBlock = inheritFrom.rubbleDirtBlock;
        
        // Наследуем fountain settings если не заданы
        if (this.fountainChance == null) this.fountainChance = inheritFrom.fountainChance;
        if (this.frontChance == null) this.frontChance = inheritFrom.frontChance;
        
        // Наследуем sphere settings если не заданы
        if (this.sphereBlock == null) this.sphereBlock = inheritFrom.sphereBlock;
        if (this.sphereSideBlock == null) this.sphereSideBlock = inheritFrom.sphereSideBlock;
        if (this.sphereGlassBlock == null) this.sphereGlassBlock = inheritFrom.sphereGlassBlock;
        if (multibuildingChoices.isEmpty() && !inheritFrom.multibuildingChoices.isEmpty()) {
            multibuildingChoices.addAll(inheritFrom.multibuildingChoices);
        }
    }
    
    /**
     * Случайный multibuilding по весам. Оригинал: getRandomMultiBuilding(rand, pos).
     * pos не используется (в оригинале — для distance в ObjectSelector; у нас упрощено).
     */
    public String getRandomMultiBuilding(Random random, ChunkPos pos) {
        if (multibuildingChoices.isEmpty()) return null;
        float total = 0;
        for (MultiBuildingChoice c : multibuildingChoices) total += c.factor;
        if (total <= 0) return null;
        float r = random.nextFloat() * total;
        for (MultiBuildingChoice c : multibuildingChoices) {
            r -= c.factor;
            if (r <= 0) {
                String v = c.value;
                return (v != null && !v.contains(":")) ? "lostcities:" + v : v;
            }
        }
        String v = multibuildingChoices.get(multibuildingChoices.size() - 1).value;
        return (v != null && !v.contains(":")) ? "lostcity:" + v : v;
    }
    
    public boolean hasMultiBuildings() {
        return !multibuildingChoices.isEmpty();
    }
    
    public Identifier getName() {
        return name;
    }
    
    public String getId() {
        return name.toString();
    }
    
    /**
     * Получить имя Style для получения палитр. Оригинал: getStyle().
     */
    public String getStyle() {
        return style;
    }
    
    /**
     * Получить buildingChance (переопределение BUILDING_CHANCE). Оригинал: getBuildingChance().
     */
    public Float getBuildingChance() {
        return buildingChance;
    }
    
    /**
     * Получить maxFloorCount (переопределение BUILDING_MAXFLOORS). Оригинал: getMaxFloorCount().
     */
    public Integer getMaxFloorCount() {
        return maxFloorCount;
    }
    
    /**
     * Получить minFloorCount (переопределение BUILDING_MINFLOORS). Оригинал: getMinFloorCount().
     */
    public Integer getMinFloorCount() {
        return minFloorCount;
    }
    
    /**
     * Получить maxCellarCount (переопределение BUILDING_MAXCELLARS). Оригинал: getMaxCellarCount().
     */
    public Integer getMaxCellarCount() {
        return maxCellarCount;
    }
    
    /**
     * Получить minCellarCount (переопределение BUILDING_MINCELLARS). Оригинал: getMinCellarCount().
     */
    public Integer getMinCellarCount() {
        return minCellarCount;
    }
    
    /**
     * Получить streetBlock. Оригинал: getStreetBlock().
     */
    public Character getStreetBlock() {
        return streetBlock;
    }
    
    /**
     * Получить parkStreetThreshold. Оригинал: getParkStreetThreshold().
     */
    public Integer getParkStreetThreshold() {
        return parkStreetThreshold;
    }
    
    /**
     * Получить avoidFoliage. Оригинал: getAvoidFoliage().
     */
    public Boolean getAvoidFoliage() {
        return avoidFoliage;
    }
    
    /**
     * Получить inherit. Оригинал: getInherit().
     */
    public String getInherit() {
        return inherit;
    }
}
