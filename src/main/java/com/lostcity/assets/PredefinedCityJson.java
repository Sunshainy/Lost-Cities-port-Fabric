package com.lostcity.assets;

import java.util.List;

/**
 * JSON структура для PredefinedCity.
 * Этап 1.3: Базовая поддержка Predefined Assets.
 */
public class PredefinedCityJson {
    public String dimension;
    public Integer chunkx;
    public Integer chunkz;
    public Integer radius;
    public String citystyle;
    public List<PredefinedBuildingJson> buildings;
    public List<PredefinedStreetJson> streets;
}
