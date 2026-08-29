package mcjty.lostcities.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.CommonLevelAccessor;

public interface ILostCityAsset {

    // Called after the asset is fetched from the registry
    default void init(CommonLevelAccessor level) {}

    String getName();

    Identifier getId();
}
