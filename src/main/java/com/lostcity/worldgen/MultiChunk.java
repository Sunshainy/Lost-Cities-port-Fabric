package com.lostcity.worldgen;

import com.lostcity.assets.AssetRegistries;
import com.lostcity.assets.Building;
import com.lostcity.assets.CityStyle;
import com.lostcity.assets.MultiBuilding;
import com.lostcity.config.LostCityConfig;
import com.lostcity.config.MultiSettings;
import com.lostcity.config.ProfileConfig;
import com.lostcity.util.ModLogger;
import com.lostcity.util.TimedCache;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.*;
import java.util.function.Function;

/**
 * Область NxN чанков для размещения мульти-зданий.
 * Оригинал: mcjty.lostcities.worldgen.lost.MultiChunk — строго 1:1, без фолбеков.
 */
public class MultiChunk {

    record MB(String name, int offsetX, int offsetZ) {}

    private static final TimedCache<ChunkPos, MultiChunk> MULTICHUNKS = new TimedCache<>(() -> 60, 5000);

    public static void cleanCache() {
        MULTICHUNKS.clear();
    }

    private final ChunkPos mc;
    private final ChunkPos topleft;
    private final int areasize;
    private final MB[][] buildingGrid;

    public MultiChunk(ChunkPos mc, int areasize) {
        this.mc = mc;
        this.topleft = new ChunkPos(mc.x * areasize, mc.z * areasize);
        this.areasize = areasize;
        this.buildingGrid = new MB[areasize][areasize];
        for (int x = 0; x < areasize; x++) {
            for (int z = 0; z < areasize; z++) {
                buildingGrid[x][z] = null;
            }
        }
    }

    public static synchronized MultiChunk getOrCreate(StructureWorldAccess world, ChunkPos coord, LostCityConfig config) {
        MultiSettings settings = MultiSettings.DEFAULT;
        int areasize = settings.areasize();
        ChunkPos mc = getMultiCoord(coord, areasize);
        return MULTICHUNKS.computeIfAbsent(mc, k -> new MultiChunk(mc, areasize).calculateBuildings(world, config, settings));
    }

    public MB getMultiBuilding(ChunkPos coord) {
        int x = coord.x - topleft.x;
        int z = coord.z - topleft.z;
        if (x < 0 || x >= areasize || z < 0 || z >= areasize) return null;
        return buildingGrid[x][z];
    }

    private static ChunkPos getMultiCoord(ChunkPos coord, int areasize) {
        return new ChunkPos(Math.floorDiv(coord.x, areasize), Math.floorDiv(coord.z, areasize));
    }

    private static <T> T getRandomFromList(Random rand, List<T> list, Function<T, Float> weightGetter) {
        if (list == null || list.isEmpty()) return null;
        float total = 0;
        for (T t : list) total += weightGetter.apply(t);
        if (total <= 0) return null;
        float r = rand.nextFloat() * total;
        for (T t : list) {
            r -= weightGetter.apply(t);
            if (r <= 0) return t;
        }
        return list.get(list.size() - 1);
    }

    private MultiChunk calculateBuildings(StructureWorldAccess world, LostCityConfig config, MultiSettings settings) {
        Random rand = new Random(mc.x * 797013493L + mc.z * 295085213L);
        ProfileConfig profile = config.getActiveProfile();

        int min = settings.minimum();
        int max = settings.maximum();
        int cnt = min + rand.nextInt(max - min + 1);
        if (cnt <= 0) return this;

        int cityLevel = ChunkHeightmap.getCityLevel(topleft, profile, world);

        Map<String, Integer> styleCount = new HashMap<>();
        List<CityStyle> styleList = new ArrayList<>();
        for (int x = 0; x < areasize; x++) {
            for (int z = 0; z < areasize; z++) {
                ChunkPos c = new ChunkPos(topleft.x + x, topleft.z + z);
                CityStyle cs = City.getCityStyle(c, config, world);
                if (cs == null) continue;
                String id = cs.getId();
                styleCount.merge(id, 1, Integer::sum);
                if (styleList.stream().noneMatch(s -> s.getId().equals(id))) {
                    styleList.add(cs);
                }
            }
        }
        styleList.sort(Comparator.comparing(CityStyle::getId));

        List<MultiBuilding> multiBuildings = new ArrayList<>();
        List<CityStyle> styleForBuilding = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            CityStyle cityStyle = getRandomFromList(rand, styleList, s -> (float) styleCount.getOrDefault(s.getId(), 0));
            if (cityStyle == null || !cityStyle.hasMultiBuildings()) continue;
            String mbName = cityStyle.getRandomMultiBuilding(rand, topleft);
            if (mbName == null) continue;
            MultiBuilding mb = AssetRegistries.getMultiBuilding(mbName);
            if (mb == null) {
                ModLogger.warn("Multibuilding not found: {}", mbName);
                continue;
            }
            multiBuildings.add(mb);
            styleForBuilding.add(cityStyle);
        }

        multiBuildings.sort((a, b) -> Integer.compare(b.getDimX() + b.getDimZ(), a.getDimX() + a.getDimZ()));

        for (int i = 0; i < multiBuildings.size(); i++) {
            MultiBuilding mb = multiBuildings.get(i);
            CityStyle buildingCityStyle = styleForBuilding.get(i);
            int maxCellars = 0;
            int profileMax = Math.max(0, profile.getBuildingMaxCellars());
            for (String b : mb.getBuildingSet()) {
                Building building = AssetRegistries.getBuilding(b);
                if (building == null) continue;
                int mc = building.getMaxCellars() >= 0 ? building.getMaxCellars() : profileMax;
                if (mc > maxCellars) maxCellars = mc;
            }

            int dimX = mb.getDimX();
            int dimZ = mb.getDimZ();
            int attempts = settings.attempts();
            for (int att = 0; att < attempts; att++) {
                int x = rand.nextInt(areasize - dimX + 1);
                int z = rand.nextInt(areasize - dimZ + 1);
                if (canPlaceBuilding(world, config, profile, settings, buildingCityStyle, mb, cityLevel, maxCellars, x, z)) {
                    placeBuilding(mb, x, z);
                    break;
                }
            }
        }
        return this;
    }

    private boolean canPlaceBuilding(StructureWorldAccess world, LostCityConfig config, ProfileConfig profile,
                                     MultiSettings settings, CityStyle buildingCityStyle, MultiBuilding building,
                                     int cityLevel, int maxCellars, int x, int z) {
        int correctStyle = 0;
        for (int xx = 0; xx < building.getDimX(); xx++) {
            for (int zz = 0; zz < building.getDimZ(); zz++) {
                if (buildingGrid[x + xx][z + zz] != null) return false;
                ChunkPos coord = new ChunkPos(topleft.x + x + xx, topleft.z + z + zz);
                if (City.isChunkOccupied(world, coord)) return false;
                if (!BuildingInfo.isCityRaw(coord, config, profile, world)) return false;
                if (BuildingInfo.hasHighway(coord, config, profile, world)) return false;
                CityStyle cs = City.getCityStyle(coord, config, world);
                if (cs != null && cs.getId().equals(buildingCityStyle.getId())) correctStyle++;
            }
        }
        float factor = settings.correctStyleFactor();
        int total = building.getDimX() * building.getDimZ();
        if (correctStyle < total * factor) return false;
        return true;
    }

    private void placeBuilding(MultiBuilding building, int x, int z) {
        for (int xx = 0; xx < building.getDimX(); xx++) {
            for (int zz = 0; zz < building.getDimZ(); zz++) {
                buildingGrid[x + xx][z + zz] = new MB(building.getName(), xx, zz);
            }
        }
    }
}
