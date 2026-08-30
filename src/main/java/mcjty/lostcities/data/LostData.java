package mcjty.lostcities.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;

/*
 * Сохраняемые данные в 1.21.5 перевели с ручного NBT на Codec + SavedDataType.
 * Ниже обе формы: новая активна с 1.21.5, старая — до неё. Внутри старой формы
 * есть ещё одна граница: в 1.20.5 у save и у десериализатора Factory появился
 * параметр HolderLookup.Provider.
 */
import net.minecraft.util.datafix.DataFixTypes;

//? if >=1.21.5 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.saveddata.SavedDataType;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
*///?}

public class LostData extends SavedData {

    public static final String NAME = "lostcities_data";

    //? if >=1.21.5 {
    private static final Codec<LostData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("profile").forGetter(d -> d.selectedProfile),
            Codec.STRING.fieldOf("json").forGetter(d -> d.selectedJson)
            ).apply(instance, LostData::new));

    private static final SavedDataType<LostData> TYPE = new SavedDataType<>(
            NAME,
            LostData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );
    //?}

    private String selectedProfile = "";
    private String selectedJson = "";

    @Nonnull
    public static LostData getData(Level level) {
        if (level.isClientSide()) {
            throw new RuntimeException("Don't access this client-side!");
        }
        MinecraftServer server = level.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        DimensionDataStorage storage = overworld.getDataStorage();
        //? if >=1.21.5 {
        return storage.computeIfAbsent(TYPE);
        //?} elif >=1.20.5 {
        /*return storage.computeIfAbsent(new Factory<>(LostData::new, (tag, provider) -> new LostData(tag), DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);
        *///?} else {
        /*return storage.computeIfAbsent(new Factory<>(LostData::new, LostData::new, DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);
        *///?}
    }

    public LostData() {
    }

    //? if >=1.21.5 {
    public LostData(String profile, String json) {
        selectedProfile = profile;
        selectedJson = json;
    }
    //?} else {
    /*public LostData(CompoundTag tag) {
        selectedProfile = tag.getString("profile");
        selectedJson = tag.getString("json");
    }

    // save потерял параметр HolderLookup.Provider в 1.20.4 и получил его в 1.20.5.
    // Ни одному из наших сохранений он не нужен (чистый NBT, без обращений к
    // реестрам), поэтому объявлены обе перегрузки без @Override: в каждой версии
    // одна из них закрывает абстрактный метод, вторая просто не используется.
    // Так тело сохранения не приходится держать в двух копиях.
    public CompoundTag save(CompoundTag tag) {
        tag.putString("profile", selectedProfile);
        tag.putString("json", selectedJson);
        return tag;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        return save(tag);
    }
    *///?}

    public void setProfile(String profile, String json) {
        selectedProfile = profile;
        selectedJson = json;
        setDirty();
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public String getSelectedJson() {
        return selectedJson;
    }
}
