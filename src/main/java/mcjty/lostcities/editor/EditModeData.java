package mcjty.lostcities.editor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.varia.WorldTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.*;

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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
*///?}

/**
 * In a world created in editmode this structure will contain information about all generated parts
 */
public class EditModeData extends SavedData {

    public static final String NAME = "lostcity_editdata";

    public record PartData(String partName, int y) { }
    private record CoordWithPart(ChunkCoord coord, PartData part) {
        public static final Codec<CoordWithPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("level").forGetter(s -> s.coord.dimension()),
                Codec.INT.fieldOf("x").forGetter(s -> s.coord.chunkX()),
                Codec.INT.fieldOf("z").forGetter(s -> s.coord.chunkZ()),
                Codec.STRING.fieldOf("part").forGetter(s -> s.part.partName()),
                Codec.INT.fieldOf("y").forGetter(s -> s.part.y())
        ).apply(instance, (level, x, z, part, y) -> new CoordWithPart(new ChunkCoord(level, x, z), new PartData(part, y))));
    }
    private final Map<ChunkCoord, List<PartData>> partData = new HashMap<>();

    //? if >=1.21.5 {
    private static final Codec<EditModeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CoordWithPart.CODEC.listOf().fieldOf("data").forGetter(d -> {
                List<CoordWithPart> result = new ArrayList<>();
                d.partData.forEach((coord, list) -> list.forEach(part -> result.add(new CoordWithPart(coord, part))));
                return result;
            })
    ).apply(instance, EditModeData::new));

    private static final SavedDataType<EditModeData> TYPE = new SavedDataType<>(
            NAME,
            EditModeData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
    //?}

    @Nonnull
    public static EditModeData getData() {
        ServerLevel overworld = WorldTools.getOverworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        //? if >=1.21.5 {
        return storage.computeIfAbsent(TYPE);
        //?} elif >=1.20.5 {
        /*return storage.computeIfAbsent(new Factory<>(EditModeData::new, (compoundTag, provider) -> new EditModeData(compoundTag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);
        *///?} else {
        /*return storage.computeIfAbsent(new Factory<>(EditModeData::new, EditModeData::new, DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);
        *///?}
    }

    private EditModeData() {
    }

    //? if >=1.21.5 {
    private EditModeData(List<CoordWithPart> partData) {
        for (CoordWithPart d : partData) {
            addPartData(d.coord, d.part.y, d.part.partName);
        }
    }
    //?} else {
    /*public EditModeData(CompoundTag nbt) {
        ListTag data = nbt.getList("data", Tag.TAG_COMPOUND);
        for (Tag t : data) {
            CompoundTag pdTag = (CompoundTag) t;
            ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(pdTag.getString("level")));
            int chunkX = pdTag.getInt("x");
            int chunkZ = pdTag.getInt("z");
            ChunkCoord pos = new ChunkCoord(level, chunkX, chunkZ);
            String part = pdTag.getString("part");
            int y = pdTag.getInt("y");
            addPartData(pos, y, part);
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
        ListTag data = new ListTag();
        partData.forEach((pos, list) -> {
            for (PartData pd : list) {
                CompoundTag pdTag = new CompoundTag();
                pdTag.putString("level", pos.dimension().location().toString());
                pdTag.putInt("x", pos.chunkX());
                pdTag.putInt("z", pos.chunkZ());
                pdTag.putString("part", pd.partName());
                pdTag.putInt("y", pd.y());
                data.add(pdTag);
            }
        });
        tag.put("data", data);
        return tag;
    }
    *///?}

    public void addPartData(ChunkCoord pos, int y, String partName) {
        partData.computeIfAbsent(pos, p -> new ArrayList<>()).add(new PartData(partName, y));
        setDirty();
    }

    public List<PartData> getPartData(ChunkCoord pos) {
        return partData.getOrDefault(pos, Collections.emptyList());
    }
}
