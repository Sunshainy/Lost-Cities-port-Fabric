package com.lostcity;

import com.lostcity.util.ModLogger;
import net.fabricmc.api.ClientModInitializer;

/**
 * Клиентская инициализация мода Lost City
 *
 * Здесь будет:
 * - GUI для выбора профилей (Шаг 12)
 * - Клиентские обработчики событий
 * - Рендеринг (если потребуется)
 */
public class LostCityModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModLogger.logClientInitStart();

        // TODO: Шаг 12 - Клиентская часть GUI

        ModLogger.logClientInitComplete();
    }
}
