# Аудит порта: что реально сделано, а что нет

Дата: 2026-08-30.
Сверка велась с оригиналом на коммите `14cfb810` (ветка `1.20`, обновлена в этот же день
с `5aced936`, +19 коммитов).

Метод: сравнение списка классов, полей `BuildingInfo`, полей профиля, реестров ассетов
и файлов датапака один к одному. Все пункты ниже проверены по коду, а не по памяти.

## Сводка

| Слой | Готовность |
|---|---|
| Датапак (JSON) | ~97% — не хватает только новых ассетов из свежих коммитов |
| Ядро расстановки блоков (ChunkDriver, части, палитры, трансформы) | ~90% |
| Разметка города (City, BuildingInfo, cityLevel, multibuilding) | ~75% |
| Здания, улицы, двери, мосты, коридоры, stuff | ~80% |
| Повреждения (взрывы, руины, щебень, обломки) | 0% |
| Железные дороги, монорельсы, сферы, scattered | 0% |
| Иерархические улицы + межгородские шоссе (новое в оригинале) | 0% |
| Избегание структур | 0% |
| API / события / команды / GUI / сеть / редактор | ~5% |

Итого по всему моду: **примерно 45–50%**. По «базовой генерации города, которую видно
глазом» — около 70%. Заявленные в `TODO_CHECKLIST.md` «60–65%» относятся только к генерации
и не учитывают, что оригинал за это время ушёл вперёд.

Оригинальный код: 27 096 строк Java (221 файл). Порт: 10 888 строк (64 файла).

---

## 1. Обновления оригинала, которых в порте нет вообще

Это самая свежая часть мода, и она большая. Автор за последние коммиты добавил целую
подсистему планирования дорог и шоссе.

- [ ] `worldgen/street/` — 9 классов: `HierarchicalStreetPlanner`, `EffectiveStreetResolver`,
      `HierarchicalBridgePlanner`, `PlannedStreetInfo`, `PlannedBridgeInfo`, `PlannedRoadType`,
      `RoadDirection`, `StreetPlannerSettings`, `TertiaryRoadSegment`.
      Иерархия «первичная / вторичная / третичная дорога», наклонные первичные дороги,
      полноширинные лестничные улицы.
- [ ] `worldgen/highway/` — 15 классов: `IntercityHighwayPlanner` (633 строки),
      `HighwayHub`, `HighwayRoute`, `HighwaySegment`, `HighwaySector`, `HighwayAxis`,
      `CityPotential`, `ApproximateCityPotential`, `HighwayHubPersistence`, `HighwayInfo`,
      `HighwayLevelSource`, `HighwayPlannerSettings`, `HighwayConnectionKey`, `HubKey`.
      Шоссе между городами с хабами, поворотами, T-развязками и сохранением хабов в мир.
- [ ] Повороты и T-развязки шоссе (`getJunction`, `bendTransform`, `tTransform`,
      `clearRailing`, `generateSupport` на изгибах) — в порте шоссе только прямые.
- [ ] Придорожные заправки и рестораны (`highway_gas_station`, `highway_restaurant`)
      + флаг `nearhighway` у scattered.
- [ ] `clearhighwayrailing` в метаданных ассетов.
- [ ] `forcedair` — метка «жёсткого воздуха» в ячейках.
- [ ] `openLotParkChance` и переопределения профиля на уровне города
      (`CityProfileOverrides`).
- [ ] `tertiaryparts` — семейство ассетов третичных дорог.
- [ ] Переработанный выбор точки спавна (по пригодности чанка, а не точки).
- [ ] Фиксы: краш при переключении на профиль space/spheres в настройке мира;
      `testfill` больше не требует OP.

Отсюда же отсутствующие файлы датапака (21 part, 3 building, 1 multibuilding,
2 scattered, 1 palette):
`street_large_*` (10 файлов), `street_stair`, `bridge_large_open`,
`highway_bridge_bend`, `highway_bridge_t`, `highway_open_bend`, `highway_open_t`,
`highway_tunnel_bend`, `highway_tunnel_t`, `roadside_supports`,
`highway_gas_station`, `highway_restaurant`, `highway_restaurant_parking`,
`palettes/street_large`.

Остальные 300+ JSON-файлов совпадают побайтово. Расхождения только в
`citystyles/citystyle_common.json`, `worldstyles/standard.json`,
`worldstyles/standard_everywhere.json` — и все они из-за перечисленного выше.
В `palettes/common.json` у порта есть лишний символ `w` (cobblestone_wall) —
нужно проверить, не подменяет ли он что-то из оригинала.

---

## 2. Подсистемы генерации, отсутствующие целиком

Проверено грепом: в порте эти слова встречаются только в полях конфига, кода нет.

- [ ] **Повреждения и разрушения** — `DamageArea`, `Explosion`, `breakBlocksForDamageNew`,
      `fixAfterExplosion`, `generateRubble`, `generateRuins`, `generateDebris`,
      `generateDebrisFromChunk`, `getInterpolatedHeight`, `bipolate`.
      Профили `explosionChance`, `ruinChance`, `rubbleLayer`, `debrisToNearbyChunkFactor`
      в порте есть, но ни на что не влияют. Это самое заметное визуальное расхождение:
      город в порте выглядит целым, а в оригинале — разрушенным.
- [ ] **Случайная растительность** — `generateRandomVegetation` (78 строк в оригинале).
- [ ] **Железные дороги** — `gen/Railways.java`, `lost/Railway.java`,
      станции, подземные станции, вагонетки, `railwayDungeonChance`.
      Поля `railwaysEnabled` и т.д. в профиле есть, кода нет.
- [ ] **Монорельсы** — `gen/Monorails.java`, `horizontalMonorail`/`verticalMonorail`.
- [ ] **Сферы городов** — `gen/Spheres.java`, `lost/CitySphere.java` (509 строк),
      `LostCitySphereFeature`, `PredefinedSphere`. Профили `biosphere`, `biosphere_caves`,
      `space` запускаются, но сфер не строят.
- [ ] **Scattered-структуры** — `gen/Scattered.java` (537 строк), `ScatteredBuilding`.
      Файлы `scattered/*.json` в ресурсах лежат, но не загружаются.
- [ ] **Избегание структур** — `lost/StructureAvoidance.java`, настройка `avoidStructures`,
      `avoidVillages`. Города наезжают на деревни и другие структуры.
- [ ] **Наклонные улицы** — `generateStreetSlopeSection`, `generateMinorStreetConnector(s)`,
      `hasStreetPartConnection`.
- [ ] **Починка факелов** — `fixTorches`, `torchTodo`.
- [ ] **Профили** `bio_wasteland` и `void_outside` (остальные 15 из 17 активных есть).

---

## 3. Реестры ассетов: чего не хватает

Порт грузит ассеты через `SimpleSynchronousResourceReloadListener` по тем же путям
`data/<ns>/lostcities/...`, что и оригинальные датапак-реестры — совместимость датапаков
сохранена, это сделано правильно. Но реестров меньше.

Есть: variants, palettes, parts, buildings, multibuildings, stuff,
predefinedcities, styles, citystyles.

- [ ] **conditions** — не грузятся вообще. Из-за этого лут и мобы в спавнерах
      выбираются не по условию (уровень, этаж, подвал, тип здания), а напрямую.
      Нужны `Condition`, `ConditionContext`, `ConditionPart`, `ConditionTest`.
- [ ] **worldstyles** — не грузятся. Отсюда в `City.java` четыре места с
      «упрощённой версией»: множители города по биомам (`cityBiomeMultiplier`) всегда
      возвращают `1.0f`, выбор `CityStyle` захардкожен на `citystyle_standard`,
      настройки `MultiSettings`/`WorldSettings`/`ScatteredSettings`/`CitySphereSettings`
      берутся не из мира. Это ломает 1:1 сильнее, чем кажется: плотность и стиль
      городов по биомам расходятся с оригиналом.
- [ ] **scattered** — реестра нет.
- [ ] **predefinedspheres** — реестра нет.

---

## 4. `BuildingInfo`: отсутствующие поля

Присутствует 1713 строк из 2286. Не хватает (проверено по именам полей):

`damageArea`, `ruinHeight`, `structureAvoidance`, `condition`, `effectiveCitySettings`,
`waterLevel`, `torchTodo`, `railDungeon`, `horizontalMonorail`, `verticalMonorail`,
`largeBridgeType`, `hierarchicalOpen`, `plannedRoadType`, `rawPlannedRoadType`,
`parkType`, `fountainType`, `floorTypes2`, `outsideChunk`, `predefinedStreet`,
`memoizationLock`.

`fountainType`/`parkType` важны: фонтаны и парки в порте выбираются иначе, чем в оригинале,
хотя `fountainChance`/`parkChance` в профиле есть.

---

## 5. Профиль: отсутствующие настройки

Есть ~155 из ~200. Не хватает (сгруппировано):

- [ ] Иерархические дороги: `STREET_GENERATION_MODE`, `PRIMARY_ROAD_SPACING_X/Z`,
      `PRIMARY_ROAD_FORCE_EVERY`, `PRIMARY_ROAD_OPTIONAL_CHANCE`,
      `SECONDARY_ROAD_MIN/MAX_COUNT_X/Z`, `TERTIARY_ROAD_CHANCE`,
      `TERTIARY_ROAD_MIN/MAX_LENGTH`, `MINIMUM_ROAD_SEPARATION`,
      `MINIMUM_ROAD_EDGE_DISTANCE`, `PLANNED_PRIMARY_BRIDGE_CHANCE`,
      `PLANNED_PRIMARY_BRIDGE_MAX_LENGTH`, `MULTI_BUILDING_STREET_CONFLICT`,
      `OPEN_LOT_PARK_CHANCE`.
- [ ] Межгородские шоссе: `HIGHWAY_GENERATION_MODE`, `HIGHWAY_NETWORK_LEVEL`,
      `HIGHWAY_PLANNING_CELL_SIZE`, `HIGHWAY_HUB_MINIMUM_POTENTIAL`,
      `HIGHWAY_HUB_SAMPLE_SPACING`, `HIGHWAY_HUB_SEARCH_RADIUS_CELLS`,
      `HIGHWAY_MINIMUM/MAXIMUM_HUB_DISTANCE`, `HIGHWAY_MAXIMUM_CONNECTIONS_PER_HUB`,
      `HIGHWAY_MINIMUM_ROUTE_LENGTH`, `HIGHWAY_ROUTE_CITY_PENALTY`.
- [ ] Спавн игрока: `SPAWN_CITY`, `SPAWN_SPHERE`, `SPAWN_BIOME`, `SPAWN_RADIUS_INCREASE`,
      `SPAWN_CHECK_ATTEMPTS`, `SPAWN_CHECK_RADIUS`, `SPAWN_NOT_IN_BUILDING`,
      `FORCE_SPAWN_IN_BUILDING`, `FORCE_SPAWN_BUILDINGS`, `FORCE_SPAWN_PARTS`.
- [ ] Прочее: `BEDROCK_LAYER`, `GENERATE_NETHER`, `EXPLOSIONS_IN_CITIES_ONLY`,
      `EDITMODE`, `HORIZON`, `FOG_RED/GREEN/BLUE/DENSITY`,
      `CITYSPHERE_OUTSIDE_GROUNDLEVEL`, `CITYSPHERE_OUTSIDE_SURFACE_VARIATION`,
      `AVOID_FOLIAGE`/`AVOID_WATER` (поля есть — проверить, что используются).

---

## 6. Обвязка вокруг генерации

- [ ] **Команды**: 2 из 14. Есть `info` и `chunk` (свои, не из оригинала).
      Нет `createbuilding`, `createpart`, `debug`, `stats`, `map`, `saveprofile`,
      `locate`, `locatepart`, `editpart`, `resumeedit`, `listparts`, `exportpart`,
      `testfill`. Без `exportpart`/`editpart` нельзя делать ассеты — это блокирует
      контент-мейкеров.
- [ ] **API для других модов**: 20 интерфейсов `api/*`, `LostCitiesImp`,
      `LostCitiesPreImp`. Есть только `MultiPos`. Моды-интеграции с портом не заработают.
- [ ] **События**: `LostCityEvent`, `PreExplosionEvent`, `PostGenOutsideChunkEvent`.
- [ ] **GUI настройки мира**: `GuiLCConfig` (515 строк), `LostCitySetup`,
      `NullDimensionInfo`, 8 классов элементов. В порте есть свой
      `ProfileSelectionScreen` — это не то же самое, там нет предпросмотра и правки
      настроек.
- [ ] **Сеть**: `PacketRequestProfile`, `PacketReturnProfileToClient`.
- [ ] **Данные игрока**: `PlayerProperties`, `PlayerSpawnSet`, `PropertiesDispatcher`.
- [ ] **Редактор**: `Editor`, `EditorInfo`, `EditModeData`.
- [ ] **Утилиты**: `NoiseGeneratorPerlin`, `NoiseGeneratorSimplex`, `GeometryTools`,
      `Statistics`, `TodoQueue`, `GlobalTodo`, `Counter`, `WorldTools`, `Tools`,
      `CustomTeleporter`, `ChunkFixer`, `ErrorLogger`, `LostTags`, `LCBlockTags`,
      `NoiseChunkOpt` (962 строки), `HeightGenOpt`.
      `postTodos` в порте — локальный список в `BuildingInfo`, а в оригинале `GlobalTodo`
      переносит задачи между чанками. Из-за этого блоки на границах чанков
      (сундуки, спавнеры) могут теряться.
- [ ] **Языковой файл**: `assets/*/lang/*.json` нет вообще.

---

## 7. Найденные баги, не связанные с полнотой порта

- [ ] `fabric.mod.json` указывает `"icon": "assets/lostcity/icon.png"`, а файл лежит
      в `assets/thelostcity/icon.png`. Иконка мода не грузится.
      Заодно: пространство имён ресурсов `thelostcity` не совпадает ни с `mod.id`
      (`lostcities`), ни с путём в датапаке — остаток от старого имени, стоит привести
      к одному.
- [ ] `MULTIVERSION.md` обещает `./gradlew buildAndCollect` в корне — такой задачи нет.
      `buildAndCollect` зарегистрирован только в подпроектах (`:1.20.1:buildAndCollect`
      и т.д.). Нужно в `stonecutter.gradle.kts` добавить
      `stonecutter registerChiseled tasks.register("buildAndCollect", stonecutter.chiseled)`.
- [ ] `MULTIVERSION.md` ссылается на `tools/check-mixin-targets.sh`, который удалён
      коммитом `1be7622`. Раздел «Проверка версии» надо переписать (миксинов в порте
      сейчас нет вообще, так что скрипт и не нужен).
- [ ] `TODO_CHECKLIST.md` внизу обрезан («альности генерации»), `Corridors` в нём
      указан дважды и помечен как не сделанный, хотя `Corridors.java` есть.
      Этот файл стоит заменить на текущий.

---

## Порядок работ

Отсортировано по тому, насколько это влияет на «похожесть на оригинал» за единицу труда.

**Шаг 1 — то, что видно сразу и не требует новых подсистем**
1. `DamageArea` + `Explosion` + `breakBlocksForDamageNew` + `fixAfterExplosion`.
2. `generateRuins`, `generateRubble`, `generateDebris`.
3. `generateRandomVegetation`, `fixTorches`.

После этого город перестанет выглядеть новостройкой. Профильные поля под это уже есть.

**Шаг 2 — фундамент, на котором стоит всё остальное**
4. Реестр `worldstyles` + `WorldStyle` + `MultiSettings`/`WorldSettings`/
   `ScatteredSettings`/`CitySphereSettings`. Убирает 4 «упрощённых» места в `City.java`.
5. Реестр `conditions` + `ConditionContext`. Правильный лут и мобы.
6. `GlobalTodo` вместо локальных `postTodos`.
7. `StructureAvoidance` + `avoidStructures`.

**Шаг 3 — отсутствующие генераторы**
8. `Railways` + `Railway` + `Monorails`.
9. `Scattered` + реестр `scattered`.
10. `Spheres` + `CitySphere` + `PredefinedSphere`.

**Шаг 4 — догнать свежий оригинал**
11. Пакет `worldgen/street/` целиком + профильные настройки + JSON `street_large_*`.
12. Пакет `worldgen/highway/` целиком + повороты/T-развязки + заправки/рестораны.

**Шаг 5 — обвязка**
13. Команды (сначала `exportpart`, `editpart`, `listparts`, `debug`, `map`).
14. API + события.
15. GUI настройки мира.

**Шаг 0 — мелочи, которые можно закрыть сегодня**
Путь иконки, `registerChiseled` для `buildAndCollect`, правка `MULTIVERSION.md`,
языковой файл, профили `bio_wasteland` и `void_outside`, лишний символ `w` в палитре.
