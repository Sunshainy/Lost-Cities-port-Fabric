package com.lostcity.assets;

/**
 * DTO для ссылки на часть здания в JSON.
 * Поддерживает top, floor, range, part.
 */
public class PartRefJson {
    public boolean top;
    public Integer floor;
    public String range;
    public String part;
}
