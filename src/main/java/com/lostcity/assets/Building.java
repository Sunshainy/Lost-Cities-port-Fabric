package com.lostcity.assets;

import java.util.ArrayList;
import java.util.List;

/**
 * Здание: filler, rubble, список частей (part refs).
 * Портирован из Building (оригинальный Forge мод).
 */
public class Building {
    private final String name;
    private final char filler;
    private final Character rubble;
    private final List<PartRef> parts = new ArrayList<>();
    private final List<PartRef> parts2 = new ArrayList<>();
    private final String refPaletteName;
    private final boolean allowDoors;
    /** Шанс того, что это здание предпочитает быть одиноким. Оригинал: prefersLonely. */
    private final float prefersLonely;
    /** Макс. подвалов, -1 = из профиля. Оригинал: getMaxCellars. */
    private final int maxCellars;
    /** Мин. этажей, -1 = из профиля. Оригинал: minFloors. */
    private final int minFloors;
    /** Макс. этажей, -1 = из профиля. Оригинал: maxFloors. */
    private final int maxFloors;

    public Building(String name, char filler, Character rubble, String refPaletteName, boolean allowDoors) {
        this(name, filler, rubble, refPaletteName, allowDoors, 0.0f, -1, -1, -1);
    }
    
    public Building(String name, char filler, Character rubble, String refPaletteName, boolean allowDoors, float prefersLonely) {
        this(name, filler, rubble, refPaletteName, allowDoors, prefersLonely, -1, -1, -1);
    }
    
    public Building(String name, char filler, Character rubble, String refPaletteName, boolean allowDoors, float prefersLonely, int maxCellars) {
        this(name, filler, rubble, refPaletteName, allowDoors, prefersLonely, maxCellars, -1, -1);
    }

    public Building(String name, char filler, Character rubble, String refPaletteName, boolean allowDoors, float prefersLonely, int maxCellars, int minFloors, int maxFloors) {
        this.name = name;
        this.filler = filler;
        this.rubble = rubble;
        this.refPaletteName = refPaletteName;
        this.allowDoors = allowDoors;
        this.prefersLonely = prefersLonely;
        this.maxCellars = maxCellars;
        this.minFloors = minFloors;
        this.maxFloors = maxFloors;
    }

    /** Разрешать проёмы к соседним чанкам (оригинал: allowDoors). */
    public boolean getAllowDoors() {
        return allowDoors;
    }

    public void addPart(boolean top, String partName, Integer floor, String range) {
        parts.add(new PartRef(top, partName, floor, range));
    }

    public void addPart(boolean top, String partName) {
        addPart(top, partName, null, null);
    }

    public void addPart2(boolean top, String partName, Integer floor, String range) {
        parts2.add(new PartRef(top, partName, floor, range));
    }

    public void addPart2(boolean top, String partName) {
        addPart2(top, partName, null, null);
    }

    public List<PartRef> getParts2() {
        return parts2;
    }

    public String getName() {
        return name;
    }

    public char getFiller() {
        return filler;
    }

    public Character getRubble() {
        return rubble;
    }

    public List<PartRef> getParts() {
        return parts;
    }

    public String getRefPaletteName() {
        return refPaletteName;
    }
    
    /**
     * Получить шанс того, что это здание предпочитает быть одиноким. Оригинал: getPrefersLonely().
     * Если 1.0f, здание хочет быть одиноким всегда.
     */
    public float getPrefersLonely() {
        return prefersLonely;
    }
    
    /** Макс. подвалов; -1 = из профиля. Оригинал: getMaxCellars. */
    public int getMaxCellars() {
        return maxCellars;
    }

    /** Мин. этажей; -1 = из профиля. Оригинал: getMinFloors. */
    public int getMinFloors() {
        return minFloors;
    }

    /** Макс. этажей; -1 = из профиля. Оригинал: getMaxFloors. */
    public int getMaxFloors() {
        return maxFloors;
    }

    public static class PartRef {
        public final boolean top;
        public final String part;
        public final Integer floor;
        public final String range;

        public PartRef(boolean top, String part, Integer floor, String range) {
            this.top = top;
            this.part = part;
            this.floor = floor;
            this.range = range;
        }

        public boolean isValidForFloor(int currentFloor, boolean isTopFloor) {
            if (top && !isTopFloor) return false;
            if (!top && isTopFloor && floor == null && range == null) return false;
            if (floor != null && floor != currentFloor) return false;
            if (range != null && !range.isBlank()) {
                String[] split = range.split(",");
                if (split.length == 2) {
                    try {
                        int min = Integer.parseInt(split[0].trim());
                        int max = Integer.parseInt(split[1].trim());
                        if (currentFloor < min || currentFloor > max) return false;
                    } catch (NumberFormatException ignored) {}
                }
            }
            return true;
        }
    }
}
