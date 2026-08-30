package mcjty.lostcities.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lostcities.LostCities;
import mcjty.lostcities.config.HighwayGenerationMode;
import mcjty.lostcities.config.StreetGenerationMode;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/*
 * Сохраняемые данные в 1.21.5 перевели с ручного NBT на Codec + SavedDataType.
 * Ниже обе формы: новая активна с 1.21.5, старая — до неё.
 */
import net.minecraft.util.datafix.DataFixTypes;

//? if >=1.21.5 {
import net.minecraft.world.level.saveddata.SavedDataType;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
*///?}

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-world generation choices stored in the overworld data storage.
 *
 * Absence of this SavedData means the world predates this version and therefore
 * resolves to LEGACY. Only CreateSpawnPosition marks a genuinely new world as
 * eligible to select profile-requested versioned modes.
 */

public class LostCityWorldGenData extends SavedData {

    public static final String NAME = "LostCityWorldGenData";
    private static final String NEW_WORLD_KEY = "newWorldStreetModes";
    private static final String NEW_WORLD_HIGHWAY_KEY = "newWorldHighwayModes";
    private static final String STREET_MODES_KEY = "streetModes";
    private static final String HIGHWAY_MODES_KEY = "highwayModes";

    //? if >=1.21.5 {
    private static final Codec<StreetGenerationMode> STREET_MODE_CODEC = Codec.STRING.xmap(
            StreetGenerationMode::byName, StreetGenerationMode::name);
    private static final Codec<HighwayGenerationMode> HIGHWAY_MODE_CODEC = Codec.STRING.xmap(
            HighwayGenerationMode::byName, HighwayGenerationMode::name);
    private static final Codec<LostCityWorldGenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf(NEW_WORLD_KEY).forGetter(data -> data.newWorldStreetModes),
            Codec.BOOL.fieldOf(NEW_WORLD_HIGHWAY_KEY).forGetter(data -> data.newWorldHighwayModes),
            Codec.unboundedMap(Codec.STRING, STREET_MODE_CODEC).fieldOf(STREET_MODES_KEY)
                    .forGetter(LostCityWorldGenData::streetModesSnapshot),
            Codec.unboundedMap(Codec.STRING, HIGHWAY_MODE_CODEC).fieldOf(HIGHWAY_MODES_KEY)
                    .forGetter(LostCityWorldGenData::highwayModesSnapshot)
    ).apply(instance, LostCityWorldGenData::new));
    private static final SavedDataType<LostCityWorldGenData> TYPE = new SavedDataType<>(
            NAME,
            LostCityWorldGenData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
    //?}

    private boolean newWorldStreetModes;
    private boolean newWorldHighwayModes;
    private final Map<String, StreetGenerationMode> streetModes = new HashMap<>();
    private final Map<String, HighwayGenerationMode> highwayModes = new HashMap<>();

    public LostCityWorldGenData() {
        // A default-constructed instance means no SavedData existed on disk. It
        // must behave as an old world until the new-world lifecycle event marks it.
        newWorldStreetModes = false;
        newWorldHighwayModes = false;
    }

    private LostCityWorldGenData(boolean newWorldStreetModes, boolean newWorldHighwayModes,
                                 Map<String, StreetGenerationMode> streetModes,
                                 Map<String, HighwayGenerationMode> highwayModes) {
        this.newWorldStreetModes = newWorldStreetModes;
        this.newWorldHighwayModes = newWorldHighwayModes;
        this.streetModes.putAll(streetModes);
        this.highwayModes.putAll(highwayModes);
    }

    @Nonnull
    public static LostCityWorldGenData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Cannot access Lost Cities world generation data without an overworld");
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        //? if >=1.21.5 {
        return storage.computeIfAbsent(TYPE);
        //?} elif >=1.20.5 {
        /*return storage.computeIfAbsent(new Factory<>(LostCityWorldGenData::new, (tag, provider) -> new LostCityWorldGenData(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);
        *///?} else {
        /*return storage.computeIfAbsent(new Factory<>(LostCityWorldGenData::new, LostCityWorldGenData::new, DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);
        *///?}
    }

    public static void initializeNewWorld(ServerLevel level) {
        LostCityWorldGenData data = get(level);
        data.markNewWorld();
    }

    void markNewWorld() {
        if (!newWorldStreetModes || !newWorldHighwayModes) {
            newWorldStreetModes = true;
            newWorldHighwayModes = true;
            setDirty();
        }
    }

    public synchronized StreetGenerationMode getStreetMode(ResourceKey<Level> dimension,
                                                             StreetGenerationMode requestedMode) {
        return getStreetMode(dimension.identifier().toString(), requestedMode);
    }

    synchronized StreetGenerationMode getStreetMode(String dimensionId, StreetGenerationMode requestedMode) {
        StreetGenerationMode persisted = streetModes.get(dimensionId);
        if (persisted != null) {
            return persisted;
        }
        StreetGenerationMode selected = resolveUnpersistedMode(newWorldStreetModes, requestedMode);
        if (newWorldStreetModes) {
            streetModes.put(dimensionId, selected);
            setDirty();
        }
        return selected;
    }

    /** Pure compatibility rule, exposed for focused tests. */
    public static StreetGenerationMode resolveUnpersistedMode(boolean initializedAsNewWorld,
                                                               StreetGenerationMode requestedMode) {
        return initializedAsNewWorld ? requestedMode : StreetGenerationMode.LEGACY;
    }

    public synchronized HighwayGenerationMode getHighwayMode(ResourceKey<Level> dimension,
                                                               HighwayGenerationMode requestedMode) {
        return getHighwayMode(dimension.identifier().toString(), requestedMode);
    }

    synchronized HighwayGenerationMode getHighwayMode(String dimensionId, HighwayGenerationMode requestedMode) {
        HighwayGenerationMode persisted = highwayModes.get(dimensionId);
        if (persisted != null) {
            return persisted;
        }
        HighwayGenerationMode selected = resolveUnpersistedHighwayMode(newWorldHighwayModes, requestedMode);
        if (newWorldHighwayModes) {
            highwayModes.put(dimensionId, selected);
            setDirty();
        }
        return selected;
    }

    /** Pure highway compatibility rule, exposed for focused tests. */
    public static HighwayGenerationMode resolveUnpersistedHighwayMode(boolean initializedAsNewWorld,
                                                                       HighwayGenerationMode requestedMode) {
        return initializedAsNewWorld ? requestedMode : HighwayGenerationMode.LEGACY;
    }

    private synchronized Map<String, StreetGenerationMode> streetModesSnapshot() {
        return new TreeMap<>(streetModes);
    }

    private synchronized Map<String, HighwayGenerationMode> highwayModesSnapshot() {
        return new TreeMap<>(highwayModes);
    }


    //? if <1.21.5 {
    /*    public LostCityWorldGenData(CompoundTag tag) {
        newWorldStreetModes = tag.getBoolean(NEW_WORLD_KEY);
        newWorldHighwayModes = tag.getBoolean(NEW_WORLD_HIGHWAY_KEY);
        CompoundTag modes = tag.getCompound(STREET_MODES_KEY);
        for (String dimension : modes.getAllKeys()) {
            if (modes.contains(dimension, Tag.TAG_STRING)) {
                String value = modes.getString(dimension);
                try {
                    streetModes.put(dimension, StreetGenerationMode.byName(value));
                } catch (IllegalArgumentException e) {
                    LostCities.getLogger().error("Unknown persisted street mode '{}' for {}; using LEGACY", value, dimension);
                    streetModes.put(dimension, StreetGenerationMode.LEGACY);
                }
            }
        }
        CompoundTag highwayModeTag = tag.getCompound(HIGHWAY_MODES_KEY);
        for (String dimension : highwayModeTag.getAllKeys()) {
            if (highwayModeTag.contains(dimension, Tag.TAG_STRING)) {
                String value = highwayModeTag.getString(dimension);
                try {
                    highwayModes.put(dimension, HighwayGenerationMode.byName(value));
                } catch (IllegalArgumentException e) {
                    LostCities.getLogger().error("Unknown persisted highway mode '{}' for {}; using LEGACY", value, dimension);
                    highwayModes.put(dimension, HighwayGenerationMode.LEGACY);
                }
            }
        }
    }

    // save потерял параметр HolderLookup.Provider в 1.20.4 и получил его в 1.20.5.
    // Он не нужен ни одному из наших сохранений (чистый NBT, без обращений к
    // реестрам), поэтому объявлены обе перегрузки без @Override: в каждой версии
    // одна закрывает абстрактный метод, вторая просто не используется.
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return save(tag);
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean(NEW_WORLD_KEY, newWorldStreetModes);
        tag.putBoolean(NEW_WORLD_HIGHWAY_KEY, newWorldHighwayModes);
        CompoundTag modes = new CompoundTag();
        streetModes.forEach((dimension, mode) -> modes.putString(dimension, mode.name()));
        tag.put(STREET_MODES_KEY, modes);
        CompoundTag highwayModeTag = new CompoundTag();
        highwayModes.forEach((dimension, mode) -> highwayModeTag.putString(dimension, mode.name()));
        tag.put(HIGHWAY_MODES_KEY, highwayModeTag);
        return tag;
    }
    *///?}
}
