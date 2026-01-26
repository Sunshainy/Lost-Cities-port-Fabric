package com.lostcity.assets;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * DTO для парсинга здания из JSON.
 * Формат: { "filler": "#", "rubble": "}", "parts": [ { "top": false, "part": "building5_1" }, ... ] }
 */
public class BuildingJson {
    public String filler;
    public String rubble;
    public List<PartRefJson> parts;
    /** Внутренние части (лестницы и т.п.). Оригинал: parts2. */
    public List<PartRefJson> parts2;
    @SerializedName("refpalette")
    public String refPalette;
    /** true = разрешать проёмы/двери к соседям (оригинал: allowDoors). По умолчанию true. */
    @SerializedName("allowdoors")
    public Boolean allowDoors;
    /** Шанс того, что это здание предпочитает быть одиноким. Оригинал: prefersLonely. По умолчанию 0.0f. */
    @SerializedName("preferslonely")
    public Float prefersLonely;
    /** Макс. подвалов (-1 = из профиля). Оригинал: maxcellars. */
    @SerializedName("maxcellars")
    public Integer maxCellars;
}
