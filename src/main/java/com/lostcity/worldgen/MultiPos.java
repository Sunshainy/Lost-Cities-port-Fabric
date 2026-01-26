package com.lostcity.worldgen;

/**
 * Позиция чанка внутри мультиздания (2×2, 3×3 и т.д.).
 * Портировано из MultiPos (оригинальный Lost Cities).
 */
public record MultiPos(int x, int z, int w, int h) {

    public static final MultiPos SINGLE = new MultiPos(-1, -1, 1, 1);

    public boolean isSingle() {
        return x == -1;
    }

    public boolean isMulti() {
        return x != -1;
    }

    public boolean isTopLeft() {
        return x == 0 && z == 0;
    }

    /** Правая сторона мультиздания — нет связи по X с востоком. */
    public boolean isRightSide() {
        return x == w - 1;
    }

    /** Нижняя сторона мультиздания — нет связи по Z с югом. */
    public boolean isBottomSide() {
        return z == h - 1;
    }
}
