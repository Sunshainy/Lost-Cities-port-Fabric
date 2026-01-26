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

    public Building(String name, char filler, Character rubble, String refPaletteName, boolean allowDoors) {
        this(name, filler, rubble, refPaletteName, allowDoors, 0.0f, -1);
    }
    
    public Building(String name, char filler, Character rubble, String refPaletteName, boolean allowDoors, float prefersLonely) {
        this(name, filler, rubble, refPaletteName, allowDoors, prefersLonely, -1);
    }
    
    public Building(String name, char filler, Character rubble, String refPaletteName, boolean allowDoors, float prefersLonely, int maxCellars) {
        this.name = name;
        this.filler = filler;
        this.rubble = rubble;
        this.refPaletteName = refPaletteName;
        this.allowDoors = allowDoors;
        this.prefersLonely = prefersLonely;
        this.maxCellars = maxCellars;
    }

    /** Разрешать проёмы к соседним чанкам (оригинал: allowDoors). */
    public boolean getAllowDoors() {
        return allowDoors;
    }

    public void addPart(boolean top, String partName) {
        parts.add(new PartRef(top, partName));
    }

    public void addPart2(boolean top, String partName) {
        parts2.add(new PartRef(top, partName));
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

    public static class PartRef {
        public final boolean top;
        public final String part;

        public PartRef(boolean top, String part) {
            this.top = top;
            this.part = part;
        }
    }
}
