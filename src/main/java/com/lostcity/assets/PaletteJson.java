package com.lostcity.assets;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * DTO для парсинга палитры из JSON.
 * Формат: { "palette": [ { "char": "R", "block": "...", "damaged": "..." }, ... ] }
 */
public class PaletteJson {
    public List<PaletteEntryJson> palette;
}
