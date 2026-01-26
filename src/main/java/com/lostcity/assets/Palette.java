package com.lostcity.assets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Палитра: маппинг символов на блоки (одиночные или массивы).
 * Портирован из Palette (оригинальный Forge мод).
 */
public class Palette {
    private final String name;
    private final Map<Character, String> charToBlock = new HashMap<>();  // Одиночные блоки
    private final Map<Character, List<BlockEntryJson>> charToBlocks = new HashMap<>();  // Массивы блоков с random
    private final Map<Character, String> charToVariant = new HashMap<>();  // Variant ссылки
    private final Map<Character, String> charToFromPalette = new HashMap<>();
    private final Map<Character, String> charToDamaged = new HashMap<>();
    private final Map<Character, String> charToLoot = new HashMap<>();
    private final Map<Character, Boolean> charToTorch = new HashMap<>();

    public Palette(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void put(char c, String blockId) {
        charToBlock.put(c, blockId);
    }
    
    public void putBlocks(char c, List<BlockEntryJson> blocks) {
        charToBlocks.put(c, blocks);
    }
    
    public void putVariant(char c, String variantName) {
        charToVariant.put(c, variantName);
    }
    
    public void putFromPalette(char c, String refChar) {
        charToFromPalette.put(c, refChar);
    }

    public void putDamaged(char c, String blockId) {
        charToDamaged.put(c, blockId);
    }

    public void putLoot(char c, String condition) {
        charToLoot.put(c, condition);
    }

    public void putTorch(char c, boolean isTorch) {
        charToTorch.put(c, isTorch);
    }

    public String getLoot(char c) {
        return charToLoot.get(c);
    }

    public boolean isTorch(char c) {
        return Boolean.TRUE.equals(charToTorch.get(c));
    }

    public String getBlock(char c) {
        return charToBlock.get(c);
    }
    
    public List<BlockEntryJson> getBlocks(char c) {
        return charToBlocks.get(c);
    }
    
    public String getVariant(char c) {
        return charToVariant.get(c);
    }

    public String getDamaged(char c) {
        return charToDamaged.getOrDefault(c, charToBlock.get(c));
    }

    public int size() {
        // Возвращаем общее количество символов (одиночные блоки + массивы + варианты + frompalette)
        return charToBlock.size() + charToBlocks.size() + charToVariant.size() + charToFromPalette.size();
    }
    
    public int getBlockCount() {
        return charToBlock.size();
    }
    
    public int getBlocksCount() {
        return charToBlocks.size();
    }
    
    public int getVariantCount() {
        return charToVariant.size();
    }
    
    public int getFromPaletteCount() {
        return charToFromPalette.size();
    }

    public Map<Character, String> getCharToBlock() {
        return new HashMap<>(charToBlock);
    }
    
    public Map<Character, List<BlockEntryJson>> getCharToBlocks() {
        return new HashMap<>(charToBlocks);
    }
    
    public Map<Character, String> getCharToVariant() {
        return new HashMap<>(charToVariant);
    }
    
    public Map<Character, String> getCharToFromPalette() {
        return new HashMap<>(charToFromPalette);
    }
    
    /**
     * Объединить эту палитру с другой. Оригинал: Palette.merge().
     * Палитры из other перезаписывают/дополняют эту палитру.
     * 
     * @param other Другая палитра для объединения
     * @return Эта палитра (для цепочки вызовов)
     */
    public Palette merge(Palette other) {
        if (other == null) return this;
        
        // Объединяем одиночные блоки (other перезаписывает)
        charToBlock.putAll(other.charToBlock);
        
        // Объединяем массивы блоков (other перезаписывает)
        charToBlocks.putAll(other.charToBlocks);
        
        // Объединяем варианты (other перезаписывает)
        charToVariant.putAll(other.charToVariant);
        
        // Объединяем fromPalette (other перезаписывает)
        charToFromPalette.putAll(other.charToFromPalette);
        
        // Объединяем damaged (other перезаписывает)
        charToDamaged.putAll(other.charToDamaged);
        
        // Объединяем loot (other перезаписывает)
        charToLoot.putAll(other.charToLoot);
        
        // Объединяем torch (other перезаписывает)
        charToTorch.putAll(other.charToTorch);
        
        return this;
    }
}
