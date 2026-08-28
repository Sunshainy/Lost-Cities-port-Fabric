package com.lostcity.worldgen;

import net.minecraft.util.BlockRotation;

import java.util.Random;

/**
 * Трансформация части при размещении (повороты для улиц и зданий).
 * Портировано из Transform (оригинальный Lost Cities).
 */
public enum Transform {
    ROTATE_NONE(BlockRotation.NONE),
    ROTATE_90(BlockRotation.CLOCKWISE_90),
    ROTATE_180(BlockRotation.CLOCKWISE_180),
    ROTATE_270(BlockRotation.COUNTERCLOCKWISE_90);

    private final BlockRotation mcRotation;

    Transform(BlockRotation mcRotation) {
        this.mcRotation = mcRotation;
    }

    /** Для поворота BlockState (оригинал: transform.getMcRotation()). */
    public BlockRotation getBlockRotation() {
        return mcRotation;
    }

    /** Случайный поворот 0/90/180/270 (оригинал: Transform.randomRotation). */
    public static Transform randomRotation(Random r) {
        return switch (r.nextInt(4)) {
            case 1 -> ROTATE_90;
            case 2 -> ROTATE_180;
            case 3 -> ROTATE_270;
            default -> ROTATE_NONE;
        };
    }

    /** Локальные координаты (px, pz) части → координаты в чанке. В оригинале ВСЕГДА используется 16x16 (mx=15, mz=15). */
    public int rotateX(int x, int z) {
        return switch (this) {
            case ROTATE_NONE -> x;
            case ROTATE_90 -> 15 - z;
            case ROTATE_180 -> 15 - x;
            case ROTATE_270 -> z;
        };
    }

    public int rotateZ(int x, int z) {
        return switch (this) {
            case ROTATE_NONE -> z;
            case ROTATE_90 -> x;
            case ROTATE_180 -> 15 - z;
            case ROTATE_270 -> 15 - x;
        };
    }
}
