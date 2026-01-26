package com.lostcity.assets;

import net.minecraft.util.Identifier;
import net.minecraft.world.StructureWorldAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Style - определяет случайные палитры для генерации.
 * Портирован из Style (оригинальный Forge мод).
 * 
 * Этап 3.1: Palettes через CityStyle
 */
public class Style {
    
    private final Identifier name;
    private final List<List<PaletteChoice>> randomPaletteChoices;
    
    /**
     * Выбор палитры с весом.
     */
    public static class PaletteChoice {
        public final float factor;
        public final String palette;
        
        public PaletteChoice(float factor, String palette) {
            this.factor = factor;
            this.palette = palette;
        }
    }
    
    /**
     * Конструктор из JSON.
     */
    public Style(Identifier name, StyleJson json) {
        this.name = name;
        this.randomPaletteChoices = new ArrayList<>();
        
        if (json.randomPalettes != null) {
            for (List<StyleJson.PaletteSelectorJson> array : json.randomPalettes) {
                List<PaletteChoice> choices = new ArrayList<>();
                for (StyleJson.PaletteSelectorJson selector : array) {
                    choices.add(new PaletteChoice(selector.factor, selector.palette));
                }
                randomPaletteChoices.add(choices);
            }
        }
    }
    
    public Identifier getName() {
        return name;
    }
    
    public String getId() {
        return name.toString();
    }
    
    /**
     * Получить случайную палитру на основе весов. Оригинал: getRandomPalette().
     * Объединяет несколько палитр в одну.
     * 
     * @param world Мир (для получения палитр из реестра)
     * @param random Генератор случайных чисел
     * @return Объединенная палитра
     */
    public Palette getRandomPalette(StructureWorldAccess world, Random random) {
        Palette palette = new Palette("__random__");
        
        for (List<PaletteChoice> choices : randomPaletteChoices) {
            // Вычисляем общий вес
            float totalWeight = 0;
            for (PaletteChoice choice : choices) {
                totalWeight += choice.factor;
            }
            
            if (totalWeight <= 0) continue;
            
            // Выбираем случайную палитру на основе весов
            float r = random.nextFloat() * totalWeight;
            Palette toMerge = null;
            
            for (PaletteChoice choice : choices) {
                r -= choice.factor;
                if (r <= 0) {
                    String paletteName = choice.palette;
                    // Нормализуем имя палитры
                    if (!paletteName.contains(":")) {
                        paletteName = "lostcities:" + paletteName;
                    }
                    toMerge = AssetRegistries.getPalette(paletteName);
                    if (toMerge == null) {
                        // Пробуем с другим namespace
                        toMerge = AssetRegistries.getPalette("lostcities:" + choice.palette);
                    }
                    break;
                }
            }
            
            if (toMerge != null) {
                palette.merge(toMerge);
            }
        }
        
        return palette;
    }
}
