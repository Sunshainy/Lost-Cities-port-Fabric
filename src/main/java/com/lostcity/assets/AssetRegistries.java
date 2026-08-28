package com.lostcity.assets;

import com.lostcity.LostCityMod;
import com.lostcity.util.ModLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реестры ассетов: здания, палитры, части зданий.
 * Портирован из AssetRegistries (оригинальный Forge мод).
 *
 * Загрузка через AssetLoader (ResourceManager) при старте data packs.
 */
public class AssetRegistries {

    private static final Map<String, Building> BUILDINGS = new HashMap<>();
    private static final Map<String, Palette> PALETTES = new HashMap<>();
    private static final Map<String, BuildingPart> PARTS = new HashMap<>();
    private static final Map<String, VariantJson> VARIANTS = new HashMap<>();  // Варианты (stonebrick, etc.)
    private static final Map<String, MultiBuilding> MULTIBUILDINGS = new HashMap<>();  // Мульти-здания
    /** Этап 1.3: Предопределённые города. */
    private static final Map<String, PredefinedCity> PREDEFINED_CITIES = new HashMap<>();
    /** Этап 3.1: CityStyle для палитр и настроек. */
    private static final Map<String, CityStyle> CITYSTYLES = new HashMap<>();
    /** Этап 3.1: Style для получения палитр. */
    private static final Map<String, Style> STYLES = new HashMap<>();
    private static final Map<String, List<StuffObject>> STUFF_BY_TAG = new HashMap<>();

    private static boolean loaded = false;
    /** Кэш уличной палитры (common+default). Сбрасывается при clear(). */
    private static CompiledPalette streetPaletteCache = null;

    public static void clear() {
        BUILDINGS.clear();
        PALETTES.clear();
        PARTS.clear();
        VARIANTS.clear();
        MULTIBUILDINGS.clear();
        PREDEFINED_CITIES.clear();
        CITYSTYLES.clear();
        STYLES.clear();
        STUFF_BY_TAG.clear();
        streetPaletteCache = null;
        loaded = false;
        ModLogger.info("AssetRegistries cleared");
    }

    public static void putStuff(StuffObject stuff) {
        for (String tag : stuff.tags) {
            STUFF_BY_TAG.computeIfAbsent(tag, k -> new ArrayList<>()).add(stuff);
        }
    }

    public static List<StuffObject> getStuffByTag(String tag) {
        return STUFF_BY_TAG.get(tag);
    }

    /** Уличная палитра (common+default). Кэшируется после загрузки ассетов. */
    public static CompiledPalette getStreetPalette() {
        if (streetPaletteCache != null) return streetPaletteCache;
        Palette common = getPalette("lostcities:common");
        Palette def = getPalette("lostcities:default");
        if (common == null && def == null) return null;
        List<Palette> list = new ArrayList<>();
        if (common != null) list.add(common);
        if (def != null) list.add(def);
        streetPaletteCache = new CompiledPalette(list.toArray(new Palette[0]));
        return streetPaletteCache;
    }

    public static void setLoaded() {
        loaded = true;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void putBuilding(String id, Building b) {
        BUILDINGS.put(id, b);
    }

    public static void putPalette(String id, Palette p) {
        PALETTES.put(id, p);
    }

    public static void putPart(String id, BuildingPart p) {
        PARTS.put(id, p);
    }
    
    public static void putVariant(String id, VariantJson v) {
        VARIANTS.put(id, v);
    }
    
    public static void putCityStyle(String id, CityStyle cs) {
        CITYSTYLES.put(id, cs);
    }
    
    public static void putStyle(String id, Style s) {
        STYLES.put(id, s);
    }
    
    public static VariantJson getVariant(String name) {
        if (name == null || name.isBlank()) return null;
        
        // Пробуем разные варианты имени
        String id = normalizeId(name);
        VariantJson variant = VARIANTS.get(id);
        if (variant != null) return variant;
        
        // Пробуем без нормализации
        variant = VARIANTS.get(name);
        if (variant != null) return variant;
        
        // Пробуем с namespace
        if (!name.contains(":")) {
            variant = VARIANTS.get(LostCityMod.MOD_ID + ":" + name);
            if (variant != null) return variant;
        }
        
        return null;
    }

    public static Building getBuilding(String name) {
        if (name == null || name.isBlank()) return null;
        String id = normalizeId(name);
        return BUILDINGS.get(id);
    }

    public static Palette getPalette(String name) {
        if (name == null || name.isBlank()) return null;
        String id = normalizeId(name);
        return PALETTES.get(id);
    }

    public static BuildingPart getPart(String name) {
        if (name == null || name.isBlank()) return null;
        String id = normalizeId(name);
        return PARTS.get(id);
    }
    
    public static CityStyle getCityStyle(String name) {
        if (name == null || name.isBlank()) return null;
        String id = normalizeId(name);
        return CITYSTYLES.get(id);
    }
    
    /**
     * Получить CityStyle с обработкой отсутствия (возвращает null вместо исключения).
     * Оригинал: AssetRegistries.CITYSTYLES.get().
     */
    public static CityStyle getCityStyleOrNull(String name) {
        return getCityStyle(name);
    }
    
    public static Style getStyle(String name) {
        if (name == null || name.isBlank()) return null;
        String id = normalizeId(name);
        return STYLES.get(id);
    }
    
    /**
     * Получить Style с обработкой отсутствия (возвращает null вместо исключения).
     * Оригинал: AssetRegistries.STYLES.get().
     */
    public static Style getStyleOrNull(String name) {
        return getStyle(name);
    }

    /** Поддержка lostcities:building5 и building5. */
    private static String normalizeId(String name) {
        name = name.trim();
        if (name.contains(":")) {
            String[] s = name.split(":", 2);
            return (s[0].equals("lostcity") || s[0].equals("lostcities") ? LostCityMod.MOD_ID : s[0]) + ":" + s[1];
        }
        return LostCityMod.MOD_ID + ":" + name;
    }

    public static Map<String, Building> getBuildings() {
        return Collections.unmodifiableMap(BUILDINGS);
    }

    public static Map<String, Palette> getPalettes() {
        return Collections.unmodifiableMap(PALETTES);
    }

    public static Map<String, BuildingPart> getParts() {
        return Collections.unmodifiableMap(PARTS);
    }

    public static int getBuildingCount() {
        return BUILDINGS.size();
    }

    public static int getPaletteCount() {
        return PALETTES.size();
    }

    public static int getPartCount() {
        return PARTS.size();
    }
    
    // === MultiBuilding ===
    
    public static void putMultiBuilding(String id, MultiBuilding mb) {
        MULTIBUILDINGS.put(id, mb);
    }
    
    public static MultiBuilding getMultiBuilding(String name) {
        if (name == null || name.isBlank()) return null;
        String id = normalizeId(name);
        MultiBuilding mb = MULTIBUILDINGS.get(id);
        if (mb != null) return mb;
        mb = MULTIBUILDINGS.get("lostcities:" + id);
        if (mb != null) return mb;
        return MULTIBUILDINGS.get(name);
    }
    
    public static int getMultiBuildingCount() {
        return MULTIBUILDINGS.size();
    }
    
    // === ЭТАП 1.3: PREDEFINED CITIES ===
    
    public static void putPredefinedCity(String id, PredefinedCity city) {
        PREDEFINED_CITIES.put(id, city);
    }
    
    public static PredefinedCity getPredefinedCity(String name) {
        if (name == null || name.isBlank()) return null;
        String id = normalizeId(name);
        PredefinedCity city = PREDEFINED_CITIES.get(id);
        if (city != null) return city;
        city = PREDEFINED_CITIES.get(name);
        if (city != null) return city;
        if (!name.contains(":")) {
            city = PREDEFINED_CITIES.get(LostCityMod.MOD_ID + ":" + name);
            if (city != null) return city;
        }
        return null;
    }
    
    /**
     * Получить все предопределённые города.
     */
    public static java.util.Collection<PredefinedCity> getAllPredefinedCities() {
        return Collections.unmodifiableCollection(PREDEFINED_CITIES.values());
    }
    
    /**
     * Этап 3.1: Получить все CityStyle для разрешения наследования.
     */
    public static Map<String, CityStyle> getAllCityStyles() {
        return Collections.unmodifiableMap(CITYSTYLES);
    }
}
