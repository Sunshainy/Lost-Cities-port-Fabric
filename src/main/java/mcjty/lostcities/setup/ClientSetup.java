package mcjty.lostcities.setup;

import mcjty.lostcities.network.PacketReturnProfileToClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientEventHandlers.register();
        // Подпись обработчика сменилась в 1.20.5 вместе с переходом Fabric
        // с FabricPacket на CustomPacketPayload.
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(PacketReturnProfileToClient.TYPE, (payload, context) -> payload.handle());
        //?} else
        /*ClientPlayNetworking.registerGlobalReceiver(PacketReturnProfileToClient.TYPE, (packet, player, sender) -> packet.handle());*/
    }
}
