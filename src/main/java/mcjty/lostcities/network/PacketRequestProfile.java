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
 *
 * Поле TYPE в обеих ветках названо одинаково, поэтому места регистрации в
 * LostCities и ClientSetup различаются только строкой самой регистрации.
 */
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
*///?}

//? if >=1.20.5 {
public record PacketRequestProfile(ResourceKey<Level> dimension) implements CustomPacketPayload {
//?} else
/*public record PacketRequestProfile(ResourceKey<Level> dimension) implements FabricPacket {*/

    public static final Identifier ID = Identifier.fromNamespaceAndPath(LostCities.MODID, "requestproofile");

    //? if >=1.20.5 {
    public static final CustomPacketPayload.Type<PacketRequestProfile> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRequestProfile> CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), PacketRequestProfile::dimension,
            PacketRequestProfile::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?} else {
    /*public static final PacketType<PacketRequestProfile> TYPE = PacketType.create(ID, PacketRequestProfile::new);

    public PacketRequestProfile(FriendlyByteBuf buf) {
        this(buf.readResourceKey(Registries.DIMENSION));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceKey(dimension);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
    *///?}

    public void handle() {
        // @todo 1.14
//            ServerPlayerEntity player = ctx.get().getSender();
//            LostCityProfile profile = WorldTypeTools.getProfile(WorldTools.getWorld(dimension));
//            PacketHandler.INSTANCE.sendTo(new PacketRequestProfile(dimension, profile.getName()), player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }
}
