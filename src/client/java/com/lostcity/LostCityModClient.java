package com.lostcity;

import com.lostcity.gui.ProfileSelectionScreen;
import com.lostcity.util.ModLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Клиентская инициализация мода Lost City.
 */
public class LostCityModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModLogger.logClientInitStart();

        addCitiesButtonToWorldCreation();

        ModLogger.logClientInitComplete();
    }

    /**
     * Кнопка "Cities" на экране создания мира — вход в выбор профиля.
     *
     * Раньше это делал миксин на CreateWorldScreen.init с @Shadow на
     * addDrawableChild. В собранном джарнике он не работал ни на одной версии:
     * refmap не генерировался, поэтому @Shadow не находил цель, а имя метода
     * в @Inject оставалось в yarn-виде. В логе это выглядело как
     * "@Shadow method method_37063 ... was not located in the target class
     * net.minecraft.class_525" и экран создания мира просто не открывался.
     *
     * ScreenEvents — публичное API Fabric с одинаковой подписью на всех версиях
     * от 1.20 до 1.21.11. Ни миксина, ни refmap, ни версионных веток.
     */
    private static void addCitiesButtonToWorldCreation() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof CreateWorldScreen)) {
                return;
            }
            ButtonWidget citiesButton = ButtonWidget.builder(
                    Text.literal("Cities"),
                    button -> client.setScreen(new ProfileSelectionScreen(screen)))
                .dimensions(screen.width - 100, 40, 70, 20)
                .build();
            Screens.getButtons(screen).add(citiesButton);
        });
    }
}
