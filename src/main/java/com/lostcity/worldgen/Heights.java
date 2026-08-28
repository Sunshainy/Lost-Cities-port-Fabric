package com.lostcity.worldgen;

import net.minecraft.world.HeightLimitView;

/**
 * Границы мира по высоте, одинаково на всех поддерживаемых версиях.
 *
 * В 1.21.2 HeightLimitView.getTopY() (эксклюзивная верхняя граница) заменили на
 * getTopYInclusive() (инклюзивная, то есть на единицу меньше). Прямая замена
 * одного вызова на другой сдвинула бы всю арифметику высот на один блок.
 *
 * getBottomY() и getHeight() есть во всех версиях с неизменным смыслом, поэтому
 * считаем границу через них — без версионных веток и без риска сдвига.
 */
public final class Heights {

    private Heights() {
    }

    /** Верхняя граница мира, эксклюзивная — прямой аналог старого getTopY(). */
    public static int topY(HeightLimitView view) {
        return view.getBottomY() + view.getHeight();
    }

    /** Нижняя граница мира, инклюзивная. */
    public static int bottomY(HeightLimitView view) {
        return view.getBottomY();
    }
}
