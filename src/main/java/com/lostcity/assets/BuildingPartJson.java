package com.lostcity.assets;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * DTO для парсинга части здания из JSON.
 * Формат: { "xsize": 16, "zsize": 16, "slices": [ ["###...","###..."], ... ], "refpalette": "default" }
 */
public class BuildingPartJson {
    public int xsize;
    public int zsize;
    public List<List<String>> slices;
    @SerializedName("refpalette")
    public String refPalette;
    /** Мета: support -> char и т.д. */
    public List<MetaEntry> meta;

    public static class MetaEntry {
        public String key;
        public String chr;
    }
}
