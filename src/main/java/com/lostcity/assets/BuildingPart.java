package com.lostcity.assets;

import java.util.HashMap;
import java.util.Map;

/**
 * Часть здания: срезы (слои по Y) из символов.
 * Портирован из BuildingPart (оригинальный Forge мод).
 * Срезы — массив строк, каждая строка = одна горизонтальная линия (x), все строки = срез (x*z).
 */
public class BuildingPart {
    private final String name;
    private final int xSize;
    private final int zSize;
    /** Срезы по Y. slices[y][z] = строка длины xSize. */
    private final String[] slices;
    private final String refPaletteName;
    /** Метаданные части (dontconnect и т.д.). */
    private final Map<String, Object> metadata = new HashMap<>();

    public BuildingPart(String name, int xSize, int zSize, String[] slices, String refPaletteName) {
        this.name = name;
        this.xSize = xSize;
        this.zSize = zSize;
        this.slices = slices;
        this.refPaletteName = refPaletteName;
    }
    
    /** Добавить метаданные (используется при загрузке из JSON). */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /** Получить boolean метаданные (оригинал: getMetaBoolean). */
    public boolean getMetaBoolean(String key) {
        Object o = metadata.get(key);
        return o instanceof Boolean ? (Boolean) o : false;
    }

    /** Символ палитры по ключу метаданных (support и т.д.). Оригинал: getMetaChar. */
    public Character getMetaChar(String key) {
        Object o = metadata.get(key);
        if (o instanceof Character) return (Character) o;
        if (o instanceof String && ((String) o).length() == 1) return ((String) o).charAt(0);
        return null;
    }
    
    /** Получить integer метаданные (оригинал: getMetaInteger). */
    public Integer getMetaInteger(String key) {
        Object o = metadata.get(key);
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof Number) return ((Number) o).intValue();
        return null;
    }

    public String getName() {
        return name;
    }

    public int getXSize() {
        return xSize;
    }

    public int getZSize() {
        return zSize;
    }

    public int getSliceCount() {
        return slices.length;
    }

    /** Символ в локальных координатах (x, y, z). */
    public char getChar(int x, int y, int z) {
        if (y < 0 || y >= slices.length) return ' ';
        String row = slices[y];
        int idx = z * xSize + x;
        if (idx < 0 || idx >= row.length()) return ' ';
        return row.charAt(idx);
    }

    public String[] getSlices() {
        return slices;
    }

    public String getRefPaletteName() {
        return refPaletteName;
    }
}
