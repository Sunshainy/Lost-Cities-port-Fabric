package com.lostcity.assets;

/**
 * DTO для одного блока в массиве blocks (с random весом).
 */
public class BlockEntryJson {
    public Integer random;  // Вес блока (1-128)
    public String block;    // ID блока
}
