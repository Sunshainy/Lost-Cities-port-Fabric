package com.lostcity.assets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostcity.LostCityMod;
import com.lostcity.util.ModLogger;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Загрузчик ассетов из JSON (data/lostcities/lostcities/).
 * Портировано из RegistryAssetRegistry.loadAll + Forge data pack registry.
 *
 * Загрузка: 1) palettes, 2) parts, 3) buildings.
 */
public class AssetLoader implements SimpleSynchronousResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    /** Path для findResources: lostcities (Identifier path для data/lostcities/lostcities/...) */
    private static final String ASSETS_PATH = "lostcities";

    @Override
    public Identifier getFabricId() {
        return Identifier.of(LostCityMod.MOD_ID, "assets");
    }

    @Override
    public void reload(ResourceManager manager) {
        AssetRegistries.clear();
        long start = System.nanoTime();
        ModLogger.info("=== AssetRegistries.load START ===");

        // Как в оригинале: загрузка из любых датапаков (data/<namespace>/lostcities/...).
        // Позволяет добавлять свои здания/палитры/части через датапак.
        Map<Identifier, net.minecraft.resource.Resource> all = manager.findResources(
            ASSETS_PATH,
            id -> id.getPath().endsWith(".json")
        );

        ModLogger.info("Found {} JSON assets in 'lostcities' folder (from mod resources and datapacks)", all.size());
        
        int palettes = 0, parts = 0, buildings = 0, variants = 0, multibuildings = 0, predefinedCities = 0, citystyles = 0, stuffs = 0;

        // Загружаем варианты ПЕРВЫМИ (они нужны для разрешения variant в палитрах)
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/variants/")) continue;
            if (loadVariant(id, path, e.getValue())) variants++;
        }

        // Затем палитры (могут использовать варианты)
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/palettes/")) continue;
            if (loadPalette(id, path, e.getValue())) palettes++;
        }
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/parts/")) continue;
            if (loadPart(id, path, e.getValue())) parts++;
        }
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/buildings/")) continue;
            if (loadBuilding(id, path, e.getValue())) buildings++;
        }
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/multibuildings/")) continue;
            if (loadMultiBuilding(id, path, e.getValue())) multibuildings++;
        }
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/stuff/")) continue;
            if (loadStuff(id, path, e.getValue())) stuffs++;
        }
        
        // Этап 1.3: Загружаем predefined cities
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/predefinedcities/")) continue;
            if (loadPredefinedCity(id, path, e.getValue())) predefinedCities++;
        }
        
        // Этап 3.1: Загружаем Style (нужны для CityStyle)
        int styles = 0;
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/styles/")) continue;
            if (loadStyle(id, path, e.getValue())) styles++;
        }
        
        // Этап 3.1: Загружаем CityStyle (после Style, чтобы можно было использовать наследование)
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> e : all.entrySet()) {
            Identifier id = e.getKey();
            String path = id.getPath();
            if (!path.contains("/citystyles/")) continue;
            if (loadCityStyle(id, path, e.getValue())) citystyles++;
        }
        
        // Разрешаем наследование в CityStyle (после загрузки всех)
        resolveCityStyleInheritance();

        AssetRegistries.setLoaded();
        long ms = (System.nanoTime() - start) / 1_000_000;
        ModLogger.info("=== AssetRegistries.load END ({}ms) ===", ms);
        ModLogger.info("Loaded {} buildings, {} palettes, {} parts, {} variants, {} multibuildings, {} predefined cities, {} styles, {} citystyles", 
            buildings, palettes, parts, variants, multibuildings, predefinedCities, styles, citystyles);
        
        // Логируем доступные палитры для отладки
        if (palettes > 0) {
            Palette common = AssetRegistries.getPalette("lostcities:common");
            Palette defaultPal = AssetRegistries.getPalette("lostcities:default");
            ModLogger.info("Available palettes: common={} ({} symbols), default={} ({} symbols), bricks_standard={}", 
                common != null, common != null ? common.size() : 0,
                defaultPal != null, defaultPal != null ? defaultPal.size() : 0,
                AssetRegistries.getPalette("lostcities:bricks_standard") != null);
        }
    }

    private static boolean loadStuff(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "stuff");
        try {
            try (java.io.Reader reader = new java.io.InputStreamReader(r.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                StuffJson j = GSON.fromJson(reader, StuffJson.class);
                if (j == null) {
                    ModLogger.warn("Empty stuff JSON for {}", name);
                    return false;
                }
                StuffObject stuff = new StuffObject(name, j);
                AssetRegistries.putStuff(stuff);
                return true;
            }
        } catch (Exception ex) {
            ModLogger.warn("Failed to load stuff {}: {}", name, ex.getMessage());
            return false;
        }
    }

    private static String assetId(Identifier full, String folder) {
        String path = full.getPath();
        int i = path.indexOf(folder + "/");
        if (i < 0) {
            String base = path.replace(".json", "").replace(ASSETS_PATH + "/", "").replace("/", ".");
            return full.getNamespace() + ":" + base;
        }
        String sub = path.substring(i + folder.length() + 1).replace(".json", "");
        return full.getNamespace() + ":" + sub;
    }

    private static boolean loadPalette(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "palettes");
        try {
            try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                PaletteJson j = GSON.fromJson(reader, PaletteJson.class);
                if (j == null || j.palette == null) return false;
                Palette p = new Palette(name);
                int loaded = 0;
                int variantEntries = 0;
                int blocksEntries = 0;
                
                for (PaletteEntryJson e : j.palette) {
                    if (e == null || e.chr == null || e.chr.isEmpty()) {
                        ModLogger.warn("Skipping invalid palette entry (chr is null or empty)");
                        continue;
                    }
                    char c = e.getChar();
                    if (c == '\0') {
                        ModLogger.warn("Skipping palette entry with invalid char");
                        continue;
                    }
                    
                    boolean entryProcessed = false;
                    
                    // Обрабатываем blocks (массив с random) - приоритет
                    if (e.blocks != null && !e.blocks.isEmpty()) {
                        p.putBlocks(c, e.blocks);
                        blocksEntries++;
                        loaded++;
                        entryProcessed = true;
                        ModLogger.debug("  Character '{}': added blocks array ({} entries)", c, e.blocks.size());
                    }
                    // Обрабатываем block (одиночный)
                    else if (e.block != null && !e.block.isBlank()) {
                        p.put(c, e.block);
                        loaded++;
                        entryProcessed = true;
                        ModLogger.debug("  Character '{}': added block '{}'", c, e.block);
                    } 
                    // Обрабатываем variant (ссылка на вариант)
                    else if (e.variant != null && !e.variant.isBlank()) {
                        p.putVariant(c, e.variant);
                        variantEntries++;
                        loaded++;
                        entryProcessed = true;
                        ModLogger.debug("  Character '{}': added variant '{}'", c, e.variant);
                    }
                    // Обрабатываем frompalette (ссылка на другой символ)
                    else if (e.fromPalette != null && !e.fromPalette.isBlank()) {
                        p.putFromPalette(c, e.fromPalette);
                        loaded++;
                        entryProcessed = true;
                        ModLogger.debug("  Character '{}': added frompalette reference to '{}'", c, e.fromPalette);
                    }
                    
                    if (!entryProcessed) {
                        ModLogger.warn("  Character '{}': no block/blocks/variant specified!", c);
                    }
                    
                    if (e.damaged != null && !e.damaged.isBlank()) {
                        p.putDamaged(c, e.damaged);
                    }
                    if (e.loot != null && !e.loot.isBlank()) {
                        p.putLoot(c, e.loot);
                    }
                    if (e.mobid != null && !e.mobid.isBlank()) {
                        p.putMobId(c, e.mobid);
                    }
                    if (e.torch != null) {
                        p.putTorch(c, e.torch);
                    }
                }
                AssetRegistries.putPalette(name, p);
                int fromPaletteCount = p.getFromPaletteCount();
                ModLogger.info("Palette '{}' loaded: {} total entries ({} single blocks, {} blocks arrays, {} variants, {} frompalette)", 
                    name, loaded, loaded - blocksEntries - variantEntries - fromPaletteCount, blocksEntries, variantEntries, fromPaletteCount);
                ModLogger.info("  Palette '{}' final size: {} (blocks: {}, arrays: {}, variants: {}, frompalette: {})", 
                    name, p.size(), p.getBlockCount(), p.getBlocksCount(), p.getVariantCount(), fromPaletteCount);
                return true;
            }
        } catch (Exception ex) {
            ModLogger.warn("Failed to load palette {}: {}", name, ex.getMessage());
            return false;
        }
    }

    private static boolean loadPart(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "parts");
        try {
            try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                BuildingPartJson j = GSON.fromJson(reader, BuildingPartJson.class);
                if (j == null || j.slices == null) return false;
                StringBuilder[] slices = new StringBuilder[j.slices.size()];
                for (int i = 0; i < j.slices.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    for (String row : j.slices.get(i)) sb.append(row);
                    slices[i] = sb;
                }
                String[] sliceStrs = new String[slices.length];
                for (int i = 0; i < slices.length; i++) sliceStrs[i] = slices[i].toString();
                String ref = (j.refPalette != null && !j.refPalette.isEmpty()) ? j.refPalette : "default";
                BuildingPart p = new BuildingPart(name, j.xsize, j.zsize, sliceStrs, ref);
                if (j.meta != null) {
                    for (BuildingPartJson.MetaEntry e : j.meta) {
                        if (e != null && e.key != null && e.chr != null && !e.chr.isEmpty())
                            p.setMetadata(e.key, Character.valueOf(e.chr.charAt(0)));
                    }
                }
                AssetRegistries.putPart(name, p);
                return true;
            }
        } catch (Exception ex) {
            ModLogger.warn("Failed to load part {}: {}", name, ex.getMessage());
            return false;
        }
    }

    private static boolean loadBuilding(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "buildings");
        try {
            try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                BuildingJson j = GSON.fromJson(reader, BuildingJson.class);
                if (j == null || j.parts == null) return false;
                char fill = (j.filler != null && !j.filler.isEmpty()) ? j.filler.charAt(0) : '#';
                Character rub = (j.rubble != null && !j.rubble.isEmpty()) ? j.rubble.charAt(0) : null;
                boolean allowDoors = j.allowDoors != null ? j.allowDoors : true;
                float prefersLonely = j.prefersLonely != null ? j.prefersLonely : 0.0f;
                int maxCellars = j.maxCellars != null ? j.maxCellars : -1;
                int minFloors = j.minFloors != null ? j.minFloors : -1;
                int maxFloors = j.maxFloors != null ? j.maxFloors : -1;
                Building b = new Building(name, fill, rub, j.refPalette, allowDoors, prefersLonely, maxCellars, minFloors, maxFloors);
                for (PartRefJson pr : j.parts) {
                    if (pr != null && pr.part != null) b.addPart(pr.top, pr.part, pr.floor, pr.range);
                }
                if (j.parts2 != null) {
                    for (PartRefJson pr : j.parts2) {
                        if (pr != null && pr.part != null) b.addPart2(pr.top, pr.part, pr.floor, pr.range);
                    }
                }
                AssetRegistries.putBuilding(name, b);
                return true;
            }
        } catch (Exception ex) {
            ModLogger.warn("Failed to load building {}: {}", name, ex.getMessage());
            return false;
        }
    }
    
    private static boolean loadVariant(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "variants");
        try {
            try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                VariantJson j = GSON.fromJson(reader, VariantJson.class);
                if (j == null || j.blocks == null || j.blocks.isEmpty()) {
                    ModLogger.warn("Variant '{}' has no blocks", name);
                    return false;
                }
                AssetRegistries.putVariant(name, j);
                // Также регистрируем без namespace для удобства
                String shortName = name.contains(":") ? name.split(":")[1] : name;
                if (!shortName.equals(name)) {
                    AssetRegistries.putVariant(shortName, j);
                }
                ModLogger.info("Variant '{}' loaded: {} blocks (also registered as '{}')", 
                    name, j.blocks.size(), shortName);
                return true;
            }
        } catch (Exception ex) {
            ModLogger.warn("Failed to load variant {}: {}", name, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
    
    private static boolean loadMultiBuilding(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "multibuildings");
        try {
            try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                com.google.gson.JsonObject j = GSON.fromJson(reader, com.google.gson.JsonObject.class);
                if (j == null) return false;
                MultiBuilding mb = new MultiBuilding(name, j);
                AssetRegistries.putMultiBuilding(name, mb);
                ModLogger.info("MultiBuilding '{}' loaded: {}x{}", name, mb.getDimX(), mb.getDimZ());
                return true;
            }
        } catch (Exception ex) {
            ModLogger.warn("Failed to load multibuilding {}: {}", name, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Этап 1.3: Загрузить PredefinedCity из JSON.
     */
    private static boolean loadPredefinedCity(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "predefinedcities");
        try {
            try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                PredefinedCityJson j = GSON.fromJson(reader, PredefinedCityJson.class);
                if (j == null || j.dimension == null || j.chunkx == null || j.chunkz == null || j.radius == null) {
                    ModLogger.warn("PredefinedCity '{}' missing required fields (dimension, chunkx, chunkz, radius)", name);
                    return false;
                }
                
                // Конвертируем JSON в объекты
                java.util.List<PredefinedBuilding> buildings = new java.util.ArrayList<>();
                if (j.buildings != null) {
                    for (PredefinedBuildingJson bj : j.buildings) {
                        if (bj == null || bj.building == null || bj.chunkx == null || bj.chunkz == null) {
                            ModLogger.warn("Skipping invalid PredefinedBuilding in city '{}'", name);
                            continue;
                        }
                        buildings.add(new PredefinedBuilding(
                            bj.building,
                            bj.chunkx,
                            bj.chunkz,
                            bj.multi != null ? bj.multi : false,
                            bj.preventruins != null ? bj.preventruins : false
                        ));
                    }
                }
                
                java.util.List<PredefinedStreet> streets = new java.util.ArrayList<>();
                if (j.streets != null) {
                    for (PredefinedStreetJson sj : j.streets) {
                        if (sj == null || sj.chunkx == null || sj.chunkz == null) {
                            ModLogger.warn("Skipping invalid PredefinedStreet in city '{}'", name);
                            continue;
                        }
                        streets.add(new PredefinedStreet(sj.chunkx, sj.chunkz));
                    }
                }
                
                PredefinedCity city = new PredefinedCity(
                    name,
                    j.dimension,
                    j.chunkx,
                    j.chunkz,
                    j.radius,
                    j.citystyle,
                    buildings,
                    streets
                );
                
                AssetRegistries.putPredefinedCity(name, city);
                ModLogger.info("PredefinedCity '{}' loaded: dimension={}, center=({}, {}), radius={}, buildings={}, streets={}",
                    name, j.dimension, j.chunkx, j.chunkz, j.radius, buildings.size(), streets.size());
                return true;
            }
        } catch (Exception ex) {
            ModLogger.warn("Failed to load predefined city {}: {}", name, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Этап 3.1: Загрузить Style из JSON.
     */
    private static boolean loadStyle(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "styles");
        if (name == null) return false;
        
        try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
            StyleJson json = GSON.fromJson(reader, StyleJson.class);
            if (json == null) {
                ModLogger.warn("Style JSON is null for {}", name);
                return false;
            }
            
            Style style = new Style(id, json);
            AssetRegistries.putStyle(name, style);
            ModLogger.info("Style '{}' loaded", name);
            return true;
        } catch (Exception ex) {
            ModLogger.warn("Failed to load style {}: {}", name, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Этап 3.1: Загрузить CityStyle из JSON.
     */
    private static boolean loadCityStyle(Identifier id, String path, net.minecraft.resource.Resource r) {
        String name = assetId(id, "citystyles");
        if (name == null) return false;
        
        try (Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
            CityStyleJson json = GSON.fromJson(reader, CityStyleJson.class);
            if (json == null) {
                ModLogger.warn("CityStyle JSON is null for {}", name);
                return false;
            }
            
            CityStyle cityStyle = new CityStyle(id, json);
            AssetRegistries.putCityStyle(name, cityStyle);
            ModLogger.info("CityStyle '{}' loaded: style={}, inherit={}", name, json.style, json.inherit);
            return true;
        } catch (Exception ex) {
            ModLogger.warn("Failed to load citystyle {}: {}", name, ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * Этап 3.1: Разрешить наследование в CityStyle (после загрузки всех).
     */
    private static void resolveCityStyleInheritance() {
        for (Map.Entry<String, CityStyle> entry : AssetRegistries.getAllCityStyles().entrySet()) {
            CityStyle cityStyle = entry.getValue();
            String inherit = cityStyle.getInherit();
            if (inherit != null && !inherit.isBlank()) {
                CityStyle inheritFrom = AssetRegistries.getCityStyle(inherit);
                if (inheritFrom != null) {
                    cityStyle.resolveInherit(inheritFrom);
                    ModLogger.debug("CityStyle '{}' resolved inheritance from '{}'", entry.getKey(), inherit);
                } else {
                    ModLogger.warn("CityStyle '{}' inherits from '{}' but it was not found", entry.getKey(), inherit);
                }
            }
        }
    }
}
