package com.lostcity.assets;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO для парсинга записи палитры из JSON.
 * Соответствует формату Lost Cities: char → block / blocks / variant / damaged.
 */
public class PaletteEntryJson {
    @SerializedName("char")
    public String chr;       // один символ (в JSON называется "char")
    public String block;     // "minecraft:stone_bricks" (одиночный блок)
    public List<BlockEntryJson> blocks;  // Массив блоков с random весами
    public String variant;   // "stonebrick" — ссылка на вариант
    public String damaged;   // блок для разрушенного состояния
    @SerializedName("frompalette")
    public String fromPalette;
    /** Условие лута (напр. "chestloot"). Оригинал: loot. */
    public String loot;
    /** Идентификатор моба для спавнера (напр. "minecraft:zombie"). Оригинал: mobid. */
    public String mobid;
    /** Является ли факелом (отложенная расстановка). Оригинал: torch. */
    public Boolean torch;

    public char getChar() {
        return chr != null && !chr.isEmpty() ? chr.charAt(0) : '\0';
    }
}
