package mcjty.lostcities.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;

/*
 * Сохраняемые данные в 1.21.5 перевели с ручного NBT на Codec + SavedDataType.
 * Ниже обе формы: новая активна с 1.21.5, старая — до неё.
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
        //?} else
        /*return storage.computeIfAbsent(new Factory<>(LostData::new, LostData::new, DataFixTypes.SAVED_DATA_COMMAND_STORAGE), NAME);*/
    }

    public LostData() {
    }

    //? if >=1.21.5 {
    public LostData(String profile, String json) {
        selectedProfile = profile;
        selectedJson = json;
    }
    //?} else {
    /*public LostData(CompoundTag tag, HolderLookup.Provider provider) {
        selectedProfile = tag.getString("profile");
        selectedJson = tag.getString("json");
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("profile", selectedProfile);
        tag.putString("json", selectedJson);
        return tag;
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
