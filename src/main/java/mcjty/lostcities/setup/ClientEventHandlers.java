package mcjty.lostcities.setup;

import mcjty.lostcities.gui.GuiLCConfig;
import mcjty.lostcities.gui.LostCitySetup;
import mcjty.lostcities.varia.ComponentFactory;
import mcjty.lostcities.worldgen.LostCityFeature;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

public class ClientEventHandlers {

    public static void register() {
        // Inject the "Cities" button into the world creation screen.
        // (The decorative config icon that was blitted in ScreenEvent.Render.Post on NeoForge
        // has been dropped; Fabric's screen API in 26.2 has no direct post-render hook.)
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof CreateWorldScreen createWorldScreen) {
                Button lostCitiesButton = Button.builder(ComponentFactory.literal("Cities"), b ->
                        Minecraft.getInstance().setScreen(new GuiLCConfig(createWorldScreen))
                ).bounds(screen.width - 100, 40, 70, 20).build();
                lostCitiesButton.visible = false;
                Screens.getButtons(screen).add(lostCitiesButton);
                // Only show the button while the "More" tab is active
                ScreenEvents.afterTick(screen).register(s ->
                        lostCitiesButton.visible = createWorldScreen.tabManager.getCurrentTab() instanceof CreateWorldScreen.MoreTab);
            }
        });

        // Clean up client-side state when leaving a world/server
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LostCitySetup.CLIENT_SETUP.reset();
            Config.reset();
            LostCityFeature.globalDimensionInfoDirtyCounter++;
        });
    }
}
