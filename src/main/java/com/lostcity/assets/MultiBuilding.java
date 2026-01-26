package com.lostcity.assets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Мульти-здание — структура из нескольких чанков (например, 2x2, 3x3).
 * Оригинал: mcjty.lostcities.worldgen.lost.cityassets.MultiBuilding
 */
public class MultiBuilding {
    private final String name;
    private final int dimX;
    private final int dimZ;
    private final List<List<String>> buildings; // [x][z]
    private final Set<String> buildingSet;

    public MultiBuilding(String name, JsonObject json) {
        this.name = name;
        this.dimX = json.get("dimx").getAsInt();
        this.dimZ = json.get("dimz").getAsInt();
        
        // Parse buildings array
        JsonArray buildingsArray = json.getAsJsonArray("buildings");
        this.buildings = new ArrayList<>();
        for (JsonElement rowElement : buildingsArray) {
            JsonArray row = rowElement.getAsJsonArray();
            List<String> rowList = new ArrayList<>();
            for (JsonElement buildingElement : row) {
                String buildingName = buildingElement.getAsString();
                rowList.add(buildingName);
            }
            buildings.add(rowList);
        }
        
        // Collect unique building names
        this.buildingSet = new HashSet<>();
        for (List<String> row : buildings) {
            for (String building : row) {
                if (building != null && !building.isEmpty()) {
                    buildingSet.add(building);
                }
            }
        }
    }

    /**
     * Получить имя здания в позиции (x, z) внутри мульти-здания.
     * @param x Координата X (0 = левый край)
     * @param z Координата Z (0 = верхний край)
     * @return Имя здания
     */
    public String getBuilding(int x, int z) {
        if (x < 0 || x >= dimX || z < 0 || z >= dimZ) {
            return null;
        }
        return buildings.get(x).get(z);
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimZ() {
        return dimZ;
    }

    public String getName() {
        return name;
    }

    public Set<String> getBuildingSet() {
        return buildingSet;
    }
}
