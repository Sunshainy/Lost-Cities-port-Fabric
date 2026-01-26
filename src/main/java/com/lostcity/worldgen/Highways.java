package com.lostcity.worldgen;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

/**
 * Вспомогательные методы для магистралей. Оригинал: mcjty.lostcities.worldgen.gen.Highways.
 */
public final class Highways {

    public static boolean isEmpty(BlockState state) {
        return state.isAir() || state.getBlock() == Blocks.STRUCTURE_VOID 
            || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA;
    }

    /** Не очищать листья и бревна над магистралью. */
    public static boolean isClearableAboveHighway(BlockState state) {
        return !state.isIn(BlockTags.LEAVES) && !state.isIn(BlockTags.LOGS);
    }
    
    /**
     * Проверка, нужны ли опоры под магистралью в данном чанке.
     * Опоры нужны, если под магистралью есть пустота или вода/лава.
     */
    public static boolean needsSupports(ChunkDriver driver, int highwayGroundLevel) {
        // Проверяем несколько точек под магистралью
        int checkDepth = 5; // Проверяем 5 блоков вниз
        int checks = 0;
        int emptyChecks = 0;
        
        // Проверяем в нескольких точках чанка
        for (int x = 2; x < 14; x += 4) {
            for (int z = 2; z < 14; z += 4) {
                checks++;
                boolean hasEmpty = false;
                // Проверяем вниз от уровня магистрали
                for (int dy = 1; dy <= checkDepth; dy++) {
                    int y = highwayGroundLevel - dy;
                    if (y < driver.world.getBottomY()) break;
                    BlockState state = driver.getBlock(x, y, z);
                    if (isEmpty(state)) {
                        hasEmpty = true;
                        break;
                    }
                }
                if (hasEmpty) {
                    emptyChecks++;
                }
            }
        }
        
        // Если больше половины проверок показали пустоту - нужны опоры
        return emptyChecks > checks / 2;
    }
}
