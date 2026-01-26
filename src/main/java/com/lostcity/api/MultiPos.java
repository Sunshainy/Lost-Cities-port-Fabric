package com.lostcity.api;

/**
 * Позиция чанка в мульти-здании (multibuilding).
 * Оригинал: mcjty.lostcities.api.MultiPos
 * 
 * @param x Координата X внутри мульти-здания (0 = левый край)
 * @param z Координата Z внутри мульти-здания (0 = верхний край)
 * @param w Ширина мульти-здания (количество чанков по X)
 * @param h Высота мульти-здания (количество чанков по Z)
 */
public record MultiPos(int x, int z, int w, int h) {
    /** Одиночное здание (не мульти). */
    public static final MultiPos SINGLE = new MultiPos(-1, -1, 1, 1);

    /** Является ли это одиночным зданием (не мульти). */
    public boolean isSingle() {
        return x == -1;
    }

    /** Является ли это частью мульти-здания. */
    public boolean isMulti() {
        return x != -1;
    }

    /** Является ли это верхним левым углом мульти-здания. */
    public boolean isTopLeft() {
        return x == 0 && z == 0;
    }

    /** Является ли это правым краем мульти-здания. */
    public boolean isRightSide() {
        return x == w - 1;
    }

    /** Является ли это нижним краем мульти-здания. */
    public boolean isBottomSide() {
        return z == h - 1;
    }
}
