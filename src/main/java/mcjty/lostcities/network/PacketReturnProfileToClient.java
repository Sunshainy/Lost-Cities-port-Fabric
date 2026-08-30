package mcjty.lostcities.network;

import mcjty.lostcities.LostCities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/*
 * Пользовательские пакеты в 1.20.5 переписали: CustomPacketPayload получил
 * типизированный Type и StreamCodec, до этого у него были id() и
 * write(FriendlyByteBuf). Fabric в том же релизе сменил свой API с
 * FabricPacket/PacketType на PayloadTypeRegistry.
 */
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
*///?}

//? if >=1.20.5 {
public record PacketReturnProfileToClient(ResourceKey<Level> dimension, String profile) implements CustomPacketPayload {
//?} else
/*public record PacketReturnProfileToClient(ResourceKey<Level> dimension, String profile) implements FabricPacket {*/

    public static final Identifier ID = Identifier.fromNamespaceAndPath(LostCities.MODID, "returnprofile");

    //? if >=1.20.5 {
    public static final CustomPacketPayload.Type<PacketReturnProfileToClient> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketReturnProfileToClient> CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), PacketReturnProfileToClient::dimension,
            ByteBufCodecs.STRING_UTF8, PacketReturnProfileToClient::profile,
            PacketReturnProfileToClient::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?} else {
    /*public static final PacketType<PacketReturnProfileToClient> TYPE = PacketType.create(ID, PacketReturnProfileToClient::new);

    public PacketReturnProfileToClient(FriendlyByteBuf buf) {
        this(buf.readResourceKey(Registries.DIMENSION), buf.readUtf());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceKey(dimension);
        buf.writeUtf(profile);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
    *///?}

    public void handle() {
        // @todo 1.14
//            WorldTypeTools.setProfileFromServer(dimension, profile);
    }
}
