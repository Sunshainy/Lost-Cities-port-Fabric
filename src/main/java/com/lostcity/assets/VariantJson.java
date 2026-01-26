package com.lostcity.assets;

import java.util.List;

/**
 * DTO для варианта (variant) из JSON.
 * Формат: { "blocks": [ { "random": 9, "block": "..." }, ... ] }
 */
public class VariantJson {
    public List<BlockEntryJson> blocks;
}
