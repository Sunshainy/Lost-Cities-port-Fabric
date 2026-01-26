package com.lostcity.assets;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * JSON представление Style для загрузки из JSON.
 * Этап 3.1: Palettes через CityStyle
 */
public class StyleJson {
    
    /** Списки палитр с весами для случайного выбора. */
    @SerializedName("randompalettes")
    public List<List<PaletteSelectorJson>> randomPalettes;
    
    /**
     * Селектор палитры с весом.
     */
    public static class PaletteSelectorJson {
        public Float factor;
        public String palette;
    }
}
