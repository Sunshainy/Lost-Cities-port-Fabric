package com.lostcity.assets;

import com.lostcity.util.ModLogger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Скомпилированная палитра: эффективное представление палитры для генерации.
 * Портирован из CompiledPalette (оригинальный Forge мод).
 * 
 * Поддерживает:
 * - Прямые блоки (Character → BlockState)
 * - Случайные блоки (Character → BlockState[])
 * - Ссылки на другие символы (Character → Character)
 */
public class CompiledPalette {
    
    private final Map<Character, Object> palette = new HashMap<>();
    private final Map<BlockState, BlockState> damagedToBlock = new HashMap<>();
    private final Map<Character, String> lootMap = new HashMap<>();
    private final Map<Character, Boolean> torchMap = new HashMap<>();
    
    /**
     * Создать CompiledPalette из одной или нескольких палитр
     */
    public CompiledPalette(Palette... palettes) {
        ModLogger.info("Creating CompiledPalette from {} palettes", palettes.length);
        
        // Первый проход: добавляем прямые блоки, массивы блоков и variant ссылки
        for (Palette p : palettes) {
            if (p != null) {
                ModLogger.info("  Processing palette '{}' ({} total: {} blocks, {} arrays, {} variants, {} frompalette)...", 
                    p.getName(), p.size(), p.getBlockCount(), p.getBlocksCount(), p.getVariantCount(), p.getFromPaletteCount());
                
                // Одиночные блоки
                Map<Character, String> charToBlock = p.getCharToBlock();
                int singleBlocksAdded = 0;
                for (Map.Entry<Character, String> entry : charToBlock.entrySet()) {
                    char c = entry.getKey();
                    String blockId = entry.getValue();
                    
                    if (blockId == null || blockId.isBlank()) {
                        continue;
                    }
                    
                    BlockState state = parseBlockId(blockId);
                    if (state != null) {
                        palette.put(c, state);
                        if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                        if (p.isTorch(c)) torchMap.put(c, true);
                        singleBlocksAdded++;
                        ModLogger.debug("    Character '{}': block '{}'", c, blockId);
                    } else {
                        ModLogger.warn("    Character '{}': failed to parse block '{}'", c, blockId);
                    }
                }
                if (singleBlocksAdded > 0) {
                    ModLogger.info("    Added {} single blocks from palette '{}'", singleBlocksAdded, p.getName());
                }
                
                // Массивы блоков с random
                Map<Character, List<BlockEntryJson>> charToBlocks = p.getCharToBlocks();
                int arraysAdded = 0;
                for (Map.Entry<Character, List<BlockEntryJson>> entry : charToBlocks.entrySet()) {
                    char c = entry.getKey();
                    List<BlockEntryJson> blocks = entry.getValue();
                    
                    BlockState[] randomBlocks = buildRandomBlocksArray(blocks);
                    if (randomBlocks != null && randomBlocks.length > 0) {
                        palette.put(c, randomBlocks);
                        if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                        if (p.isTorch(c)) torchMap.put(c, true);
                        arraysAdded++;
                        ModLogger.info("    Character '{}': random blocks array ({} blocks)", c, randomBlocks.length);
                    } else {
                        ModLogger.warn("    Character '{}': failed to build random blocks array", c);
                    }
                }
                if (arraysAdded > 0) {
                    ModLogger.info("    Added {} random block arrays from palette '{}'", arraysAdded, p.getName());
                }
                
                // Variant ссылки (разрешаем их)
                Map<Character, String> charToVariant = p.getCharToVariant();
                int variantsResolved = 0;
                for (Map.Entry<Character, String> entry : charToVariant.entrySet()) {
                    char c = entry.getKey();
                    String variantName = entry.getValue();
                    
                    // Пробуем разные варианты имени
                    VariantJson variant = AssetRegistries.getVariant(variantName);
                    if (variant == null) {
                        variant = AssetRegistries.getVariant("lostcities:" + variantName);
                    }
                    if (variant == null && variantName.contains(":")) {
                        variant = AssetRegistries.getVariant(variantName.split(":")[1]);
                    }
                    
                    if (variant != null && variant.blocks != null && !variant.blocks.isEmpty()) {
                        BlockState[] randomBlocks = buildRandomBlocksArray(variant.blocks);
                        if (randomBlocks != null && randomBlocks.length > 0) {
                            palette.put(c, randomBlocks);
                            if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                            if (p.isTorch(c)) torchMap.put(c, true);
                            variantsResolved++;
                            ModLogger.info("    Character '{}': variant '{}' resolved to {} blocks",
                                c, variantName, randomBlocks.length);
                        } else {
                            ModLogger.warn("    Character '{}': variant '{}' resolved but no valid blocks", c, variantName);
                        }
                    } else {
                        ModLogger.warn("    Character '{}': variant '{}' not found (tried: {}, lostcities:{})", 
                            c, variantName, variantName, variantName);
                    }
                }
                if (variantsResolved > 0) {
                    ModLogger.info("    Resolved {} variants from palette '{}'", variantsResolved, p.getName());
                }
                
                // frompalette ссылки (сохраняем для разрешения во втором проходе)
                Map<Character, String> charToFromPalette = p.getCharToFromPalette();
                for (Map.Entry<Character, String> entry : charToFromPalette.entrySet()) {
                    char c = entry.getKey();
                    String refChar = entry.getValue();
                    if (refChar != null && !refChar.isEmpty()) {
                        char ref = refChar.charAt(0);
                        palette.put(c, ref);
                        if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                        if (p.isTorch(c)) torchMap.put(c, true);
                        ModLogger.debug("    Character '{}': frompalette reference to '{}' (will resolve later)", c, ref);
                    }
                }
            }
        }
        
        // Второй проход: разрешаем frompalette ссылки (итеративно)
        resolveFromPaletteReferences();
        
        ModLogger.info("CompiledPalette created: {} entries", palette.size());
        if (palette.size() == 0) {
            ModLogger.error("WARNING: CompiledPalette is EMPTY! No symbols loaded!");
        }
    }
    
    /**
     * Разрешить frompalette ссылки (Character → Character) итеративно.
     * Логика из оригинала: разрешаем ссылки, пока все не будут разрешены.
     */
    private void resolveFromPaletteReferences() {
        boolean dirty = true;
        int iteration = 0;
        int maxIterations = 10;
        int totalReferences = 0;
        
        // Считаем начальное количество ссылок
        for (Object value : palette.values()) {
            if (value instanceof Character) {
                totalReferences++;
            }
        }
        
        if (totalReferences == 0) {
            return; // Нет ссылок для разрешения
        }
        
        ModLogger.debug("  Starting reference resolution ({} references to resolve)...", totalReferences);
        
        while (dirty && iteration < maxIterations) {
            dirty = false;
            iteration++;
            int resolvedThisIteration = 0;
            
            // Ищем ссылки (Character → Character) и разрешаем их
            Map<Character, Object> toResolve = new HashMap<>();
            for (Map.Entry<Character, Object> entry : palette.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Character refChar) {
                    // Это ссылка - проверяем, можем ли её разрешить
                    Object target = resolveReferenceRecursive(refChar, 0, 10);
                    if (target != null && !(target instanceof Character)) {
                        // Целевой символ уже разрешён - можем разрешить ссылку
                        toResolve.put(entry.getKey(), target);
                        dirty = true;
                        resolvedThisIteration++;
                    }
                }
            }
            
            // Применяем разрешённые ссылки
            for (Map.Entry<Character, Object> entry : toResolve.entrySet()) {
                palette.put(entry.getKey(), entry.getValue());
                ModLogger.debug("      ✓ Resolved '{}' -> '{}'", entry.getKey(), 
                    entry.getValue() instanceof BlockState ? "BlockState" : 
                    entry.getValue() instanceof BlockState[] ? "BlockState[]" : "unknown");
            }
            
            if (resolvedThisIteration > 0) {
                ModLogger.debug("    Reference resolution iteration {}: resolved {} references", 
                    iteration, resolvedThisIteration);
            }
        }
        
        if (iteration >= maxIterations && dirty) {
            ModLogger.warn("  ⚠ Reference resolution stopped after {} iterations (possible circular reference)", maxIterations);
        } else if (totalReferences > 0) {
            ModLogger.info("  ✓ Reference resolution complete after {} iterations", iteration);
        }
    }
    
    /**
     * Рекурсивно разрешить ссылку (для обработки цепочек ссылок).
     */
    private Object resolveReferenceRecursive(char refChar, int depth, int maxDepth) {
        if (depth >= maxDepth) {
            return null; // Предотвращаем бесконечную рекурсию
        }
        
        Object target = palette.get(refChar);
        if (target == null) {
            return null;
        }
        
        if (target instanceof Character nextRef) {
            // Это ссылка на другую ссылку - рекурсивно разрешаем
            return resolveReferenceRecursive(nextRef, depth + 1, maxDepth);
        }
        
        // Это финальное значение (BlockState или BlockState[])
        return target;
    }
    
    /**
     * Построить массив BlockState из списка BlockEntryJson с random весами.
     * Массив должен содержать ровно 128 элементов.
     * 
     * Логика из оригинала (CompiledPalette.addEntries):
     * - Каждый блок добавляется `random` раз в массив
     * - Если сумма весов < 128 - ошибка
     * - Если сумма весов >= 128 - обрезаем до 128
     */
    private BlockState[] buildRandomBlocksArray(List<BlockEntryJson> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return null;
        }
        
        BlockState[] result = new BlockState[128];
        int idx = 0;
        
        for (BlockEntryJson entry : blocks) {
            if (entry.random == null || entry.random <= 0 || entry.block == null) {
                continue;
            }
            
            BlockState state = parseBlockId(entry.block);
            if (state == null) {
                ModLogger.warn("buildRandomBlocksArray: failed to parse block '{}'", entry.block);
                continue;
            }
            
            // Добавляем блок `random` раз (как в оригинале addEntries)
            for (int i = 0; i < entry.random && idx < 128; i++) {
                result[idx++] = state;
            }
            
            if (idx >= 128) {
                break; // Массив заполнен
            }
        }
        
        // Проверяем, что массив заполнен до 128
        if (idx < 128) {
            ModLogger.warn("buildRandomBlocksArray: not enough blocks (got {}, need 128). Filling with last block.", idx);
            // Заполняем остаток последним блоком
            if (idx > 0) {
                BlockState last = result[idx - 1];
                for (int i = idx; i < 128; i++) {
                    result[i] = last;
                }
            } else {
                // Если ничего не добавлено, возвращаем null
                return null;
            }
        }
        
        return result;
    }
    
    /**
     * Создать CompiledPalette, объединяя существующую с новыми палитрами
     */
    public CompiledPalette(CompiledPalette other, Palette... palettes) {
        this.palette.putAll(other.palette);
        this.damagedToBlock.putAll(other.damagedToBlock);
        this.lootMap.putAll(other.lootMap);
        for (Map.Entry<Character, Boolean> e : other.torchMap.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) this.torchMap.put(e.getKey(), true);
        }

        ModLogger.debug("Merging {} palettes into existing compiled palette ({} entries)",
            palettes.length, other.palette.size());

        for (Palette p : palettes) {
            if (p != null) {
                Map<Character, String> charToBlock = p.getCharToBlock();
                for (Map.Entry<Character, String> entry : charToBlock.entrySet()) {
                    char c = entry.getKey();
                    String blockId = entry.getValue();
                    if (blockId != null && !blockId.isBlank()) {
                        BlockState state = parseBlockId(blockId);
                        if (state != null) {
                            palette.put(c, state);
                            if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                            if (p.isTorch(c)) torchMap.put(c, true);
                        }
                    }
                }
                Map<Character, List<BlockEntryJson>> charToBlocks = p.getCharToBlocks();
                for (Map.Entry<Character, List<BlockEntryJson>> entry : charToBlocks.entrySet()) {
                    char c = entry.getKey();
                    BlockState[] randomBlocks = buildRandomBlocksArray(entry.getValue());
                    if (randomBlocks != null && randomBlocks.length > 0) {
                        palette.put(c, randomBlocks);
                        if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                        if (p.isTorch(c)) torchMap.put(c, true);
                    }
                }
                Map<Character, String> charToVariant = p.getCharToVariant();
                for (Map.Entry<Character, String> entry : charToVariant.entrySet()) {
                    char c = entry.getKey();
                    String variantName = entry.getValue();
                    VariantJson variant = AssetRegistries.getVariant(variantName);
                    if (variant != null && variant.blocks != null && !variant.blocks.isEmpty()) {
                        BlockState[] randomBlocks = buildRandomBlocksArray(variant.blocks);
                        if (randomBlocks != null && randomBlocks.length > 0) {
                            palette.put(c, randomBlocks);
                            if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                            if (p.isTorch(c)) torchMap.put(c, true);
                        }
                    }
                }
                Map<Character, String> charToFromPalette = p.getCharToFromPalette();
                for (Map.Entry<Character, String> entry : charToFromPalette.entrySet()) {
                    char c = entry.getKey();
                    String refChar = entry.getValue();
                    if (refChar != null && !refChar.isEmpty()) {
                        char ref = refChar.charAt(0);
                        palette.put(c, ref);
                        if (p.getLoot(c) != null) lootMap.put(c, p.getLoot(c));
                        if (p.isTorch(c)) torchMap.put(c, true);
                    }
                }
            }
        }

        resolveFromPaletteReferences();
        ModLogger.debug("Merged CompiledPalette: {} entries", palette.size());
    }
    
    /**
     * Получить BlockState для символа (с случайным выбором, если массив)
     */
    public BlockState get(char c) {
        Object o = palette.get(c);
        if (o == null) {
            return null;
        }
        
        if (o instanceof BlockState state) {
            return state;
        } else if (o instanceof BlockState[] randomBlocks) {
            // Случайный выбор из массива (используем ThreadLocalRandom для простоты)
            return randomBlocks[ThreadLocalRandom.current().nextInt(randomBlocks.length)];
        } else if (o instanceof Character refChar) {
            // Ссылка на другой символ (рекурсивно разрешаем)
            // Это может быть неразрешённая ссылка - пытаемся разрешить
            Object resolved = resolveReferenceRecursive(refChar, 0, 10);
            if (resolved instanceof BlockState state) {
                return state;
            } else if (resolved instanceof BlockState[] randomBlocks) {
                return randomBlocks[ThreadLocalRandom.current().nextInt(randomBlocks.length)];
            }
            // Если не удалось разрешить, возвращаем null
            return null;
        }
        
        return null;
    }
    
    /**
     * Получить BlockState для символа с заданным Random
     */
    public BlockState get(char c, Random rand) {
        Object o = palette.get(c);
        if (o == null) {
            return null;
        }
        
        if (o instanceof BlockState state) {
            return state;
        } else if (o instanceof BlockState[] randomBlocks) {
            return randomBlocks[rand.nextInt(randomBlocks.length)];
        } else if (o instanceof Character refChar) {
            // Ссылка на другой символ (рекурсивно разрешаем)
            Object resolved = resolveReferenceRecursive(refChar, 0, 10);
            if (resolved instanceof BlockState state) {
                return state;
            } else if (resolved instanceof BlockState[] randomBlocks) {
                return randomBlocks[rand.nextInt(randomBlocks.length)];
            }
            return null;
        }
        
        return null;
    }
    
    /**
     * Проверить, определён ли символ в палитре
     */
    public boolean isDefined(char c) {
        return palette.containsKey(c);
    }
    
    /**
     * Проверить, является ли символ простым (один блок, не массив)
     */
    public boolean isSimple(char c) {
        Object o = palette.get(c);
        return o instanceof BlockState && !(o instanceof BlockState[]);
    }
    
    /**
     * Получить все возможные BlockState для символа
     */
    public Set<BlockState> getAll(char c) {
        Object o = palette.get(c);
        if (o == null) {
            return Collections.emptySet();
        }
        
        if (o instanceof BlockState state) {
            return Collections.singleton(state);
        } else if (o instanceof BlockState[] randomBlocks) {
            return Set.of(randomBlocks);
        } else if (o instanceof Character refChar) {
            return getAll(refChar);
        }
        
        return Collections.emptySet();
    }
    
    /**
     * Получить все символы в палитре
     */
    public Set<Character> getCharacters() {
        return palette.keySet();
    }

    public String getLoot(char c) {
        return lootMap.get(c);
    }

    public boolean isTorch(char c) {
        return Boolean.TRUE.equals(torchMap.get(c));
    }
    
    /**
     * Парсинг blockId строки в BlockState с поддержкой свойств
     * Формат: "minecraft:stone_bricks" или "minecraft:stone_bricks[axis=y]" или "minecraft:glass_pane[north=true,south=false]"
     * 
     * Использует упрощённый парсинг свойств, так как BlockStateParser требует RegistryWrapper.
     */
    private BlockState parseBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        
        try {
            // Проверяем, есть ли свойства в квадратных скобках
            if (blockId.contains("[")) {
                // Парсим свойства вручную (упрощённо)
                String[] parts = blockId.split("\\[", 2);
                String blockIdOnly = parts[0].trim();
                String propertiesStr = parts[1].replace("]", "").trim();
                
                Identifier id = Identifier.tryParse(blockIdOnly);
                if (id == null) {
                    return null;
                }
                
                Block block = Registries.BLOCK.get(id);
                if (block == null) {
                    return null;
                }
                
                BlockState state = block.getDefaultState();
                
                // Парсим свойства (упрощённо - только boolean свойства для стекол)
                // Формат: "north=true,south=false" или "facing=north"
                String[] props = propertiesStr.split(",");
                for (String prop : props) {
                    String[] keyValue = prop.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        // Пытаемся найти свойство и установить значение
                        try {
                            // Для boolean свойств (north, south, east, west, up, down)
                            if (value.equals("true") || value.equals("false")) {
                                boolean boolValue = Boolean.parseBoolean(value);
                                // Ищем BooleanProperty с таким именем
                                for (var property : state.getProperties()) {
                                    if (property.getName().equals(key) && property instanceof net.minecraft.state.property.BooleanProperty) {
                                        net.minecraft.state.property.BooleanProperty boolProp = (net.minecraft.state.property.BooleanProperty) property;
                                        state = state.with(boolProp, boolValue);
                                        break;
                                    }
                                }
                            } else {
                                // Для enum свойств (facing, axis и т.д.)
                                for (Property<?> property : state.getProperties()) {
                                    if (property.getName().equals(key)) {
                                        try {
                                            Collection<?> values = property.getValues();
                                            for (Object propValue : values) {
                                                if (propValue.toString().equalsIgnoreCase(value)) {
                                                    state = setPropertyValue(state, property, propValue);
                                                    break;
                                                }
                                            }
                                        } catch (Exception e) {
                                            ModLogger.debug("Failed to set property '{}' to '{}' for block '{}': {}", 
                                                key, value, blockIdOnly, e.getMessage());
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Игнорируем ошибки парсинга свойств
                        }
                    }
                }
                
                return state;
            } else {
                // Простой случай - без свойств
                Identifier id = Identifier.tryParse(blockId.trim());
                if (id == null) {
                    return null;
                }
                
                Block block = Registries.BLOCK.get(id);
                if (block != null) {
                    return block.getDefaultState();
                }
            }
        } catch (Exception e) {
            ModLogger.warn("Failed to parse block '{}': {}", blockId, e.getMessage());
        }
        
        return null;
    }
    
    /** Хелпер: установить enum-свойство без жёсткой типизации. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private BlockState setPropertyValue(BlockState state, Property prop, Object value) {
        return state.with(prop, (Comparable) value);
    }
}
