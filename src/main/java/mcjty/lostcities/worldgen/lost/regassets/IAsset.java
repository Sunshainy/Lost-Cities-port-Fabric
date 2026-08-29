package mcjty.lostcities.worldgen.lost.regassets;

import net.minecraft.resources.Identifier;

public interface IAsset<T extends IAsset> {
    T setRegistryName(Identifier name);
}
