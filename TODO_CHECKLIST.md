# 📋 ЧЕКЛИСТ НЕРЕАЛИЗОВАННЫХ ЗАДАЧ

**Цель:** Достичь 100% соответствия оригинальному моду Lost Cities (Forge)

---

## 🔴 КРИТИЧЕСКИЕ ЗАДАЧИ

### 1. Генераторы структур
- [ ] **Corridors** - коридоры между зданиями (`gen/Corridors.java`)
- [ ] **Monorails** - монорельсы (`gen/Monorails.java`)
- [ ] **Railways** - железные дороги (`gen/Railways.java`)
- [ ] **Scattered** - разбросанные структуры (`gen/Scattered.java`)
- [ ] **Spheres** - сферы/биосферы (`gen/Spheres.java`)
- [x] **Stuff & BlockEntities** - обработка сундуков с лутом, спавнеров мобов и отложенных задач (`postTodos`, `noLoot`)
- [ ] **Corridors** - коридоры между зданиями (`gen/Corridors.java`)
- [ ] Интегрировать все генераторы в `LostCityFeature.doGenerateCityChunk()` и `doNormalChunk()`

### 2. Система избегания структур
- [ ] Добавить настройку `avoidStructures` в `LostCityConfig`
- [ ] Реализовать метод `hasBlacklistedStructure()` в `LostCityFeature`
- [ ] Добавить проверку структур перед генерацией города
- [ ] Добавить настройки `avoidVillages`, `avoidStructuresAdjacent` в конфигурацию
- [ ] Протестировать избегание структур (особенно деревни)


---

## 🟡 ВАЖНЫЕ ЗАДАЧИ

### 3. API для совместимости с другими модами
- [ ] **ILostCities** - основной интерфейс API
- [ ] **ILostCitiesPre** - предварительный интерфейс
- [ ] **ILostCityInformation** - информация о городе
- [ ] **ILostChunkInfo** - информация о чанке (реализовать в `BuildingInfo`)
- [ ] **ILostCityBuilding** - интерфейс здания
- [ ] **ILostCityMultiBuilding** - интерфейс мультиздания (реализовать в `MultiBuilding`)
- [ ] **ILostCityCityStyle** - интерфейс стиля города
- [ ] **ILostCityProfile** - интерфейс профиля
- [ ] **ILostCityAsset** - интерфейс ассета
- [ ] **ILostCityAssetRegistry** - интерфейс реестра ассетов
- [ ] **ILostSphere** - интерфейс сферы
- [ ] **ILostExplosion** - интерфейс взрыва
- [ ] **IChunkPrimerFactory** - фабрика чанков
- [ ] **ILostWorldsChunkGenerator** - генератор миров
- [ ] **LostCitiesImp** - реализация API
- [ ] **LostCitiesPreImp** - предварительная реализация API

### 4. Система событий
- [ ] Создать систему событий для Fabric (используя Fabric API Events)
- [ ] **LostCityEvent** - базовое событие
- [ ] **PreExplosionEvent** - событие перед взрывом
- [ ] **PostGenOutsideChunkEvent** - событие после генерации вне чанка
- [ ] Другие события из оригинала
- [ ] Интегрировать события в точки генерации
- [ ] Добавить документацию для разработчиков других модов

### 5. Команды
- [ ] **ModCommands** - регистрация команд
- [ ] **CommandCreateBuilding** - создание здания
- [ ] **CommandCreatePart** - создание части здания
- [ ] **CommandDebug** - отладочная команда
- [ ] **CommandStats** - статистика
- [ ] **CommandMap** - карта городов
- [ ] **CommandSaveProfile** - сохранение профиля
- [ ] **CommandLocate** - поиск города
- [ ] **CommandLocatePart** - поиск части здания
- [ ] **CommandEditPart** - редактирование части
- [ ] **CommandResumeEdit** - продолжение редактирования
- [ ] **CommandListParts** - список частей
- [ ] **CommandExportPart** - экспорт части
- [ ] **CommandTestFill** - тестовое заполнение

### 6. GUI конфигурации
- [ ] **GuiLCConfig** - главный GUI конфигурации
- [ ] **LostCitySetup** - настройка города через GUI
- [ ] **BooleanElement** - элемент булевого значения
- [ ] **IntElement** - элемент целого числа
- [ ] **FloatElement** - элемент числа с плавающей точкой
- [ ] **DoubleElement** - элемент двойной точности
- [ ] **TextExt** - расширенный текст
- [ ] **ButtonExt** - расширенная кнопка
- [ ] **WidgetElement** - элемент виджета
- [ ] Предпросмотр генерации
- [ ] Настройка профилей через GUI

---

## 🟢 ДОПОЛНИТЕЛЬНЫЕ ЗАДАЧИ

### 7. Сетевые пакеты
- [ ] **PacketHandler** - обработчик пакетов
- [ ] **PacketRequestProfile** - запрос профиля
- [ ] **PacketReturnProfileToClient** - возврат профиля клиенту

### 8. Данные игрока
- [ ] **PlayerProperties** - свойства игрока
- [ ] **PlayerSpawnSet** - настройка спавна игрока
- [ ] **PropertiesDispatcher** - диспетчер свойств

### 9. Редактор
- [ ] **Editor** - редактор частей зданий
- [ ] **EditorInfo** - информация о редактировании
- [ ] **EditModeData** - данные режима редактирования

### 10. Утилиты
- [ ] **ComponentFactory** - фабрика компонентов
- [ ] **Counter** - счетчик
- [ ] **CustomTeleporter** - кастомный телепортер
- [ ] **GeometryTools** - геометрические инструменты
- [ ] **NoiseGeneratorPerlin** - генератор шума Perlin
- [ ] **NoiseGeneratorSimplex** - генератор шума Simplex
- [x] **QualityRandom** - качественный рандом
- [ ] **Statistics** - статистика
- [ ] **TodoQueue** - очередь задач
- [ ] **Tools** - инструменты
- [ ] **WorldTools** - инструменты мира

### 11. Setup и регистрация
- [ ] **Registration** - регистрация компонентов
- [ ] **CustomRegistries** - кастомные реестры
- [ ] **ForgeEventHandlers** - обработчики событий Forge (адаптировать для Fabric)
- [ ] **ClientEventHandlers** - клиентские обработчики событий
- [ ] **TerrainEventHandlers** - обработчики событий террейна

### 12. Worldgen утилиты
- [ ] **NoiseChunkOpt** - оптимизация шума чанков
- [ ] **HeightGenOpt** - оптимизация генерации высот
- [ ] **GlobalTodo** - глобальная очередь задач
- [ ] **ErrorLogger** - логгер ошибок
- [ ] **ChunkFixer** - исправление чанков
- [ ] **IDimensionInfo** - информация о измерении
- [ ] **DefaultDimensionInfo** - информация о измерении по умолчанию
- [ ] **LostTags** - теги мода

### 13. Дополнительные классы
- [ ] **BiomeInfo** - информация о биоме
- [ ] **DamageArea** - область повреждения
- [ ] **Direction** - направление
- [ ] **Explosion** - взрыв
- [ ] **Orientation** - ориентация
- [ ] **Railway** - железная дорога
- [ ] **CitySphere** - сфера города
- [ ] **DataGenerators** - генераторы данных
- [ ] **LCBlockTags** - теги блоков

### 14. Система ассетов (дополнения)
- [ ] **Registry Asset Registry** - реестр реестров ассетов
- [ ] Data классы (20+ классов из оригинала)

### 15. Проверки и тестирование
- [ ] Проверить идентичность API `ChunkDriver` с оригиналом
- [ ] Проверить идентичность методов `BuildingInfo` с оригиналом
- [ ] Протестировать генерацию на границах чанков
- [ ] Протестировать избегание структур
- [ ] Проверить совместимость с существующими датапаками

### 16. Интеграция JSON профилей
- [ ] Интегрировать загрузку профилей из JSON как дополнительный источник
- [ ] Обеспечить приоритет стандартных профилей над JSON

---

## 📊 ПРИОРИТЕТЫ

**Приоритет 1 (Критично):**
- Генераторы структур (Corridors, Monorails, Railways, Scattered, Spheres, Stuff)
- Система избегания структур
- Переименование ресурсов

**Приоритет 2 (Важно):**
- API для совместимости
- Система событий
- Команды
- GUI конфигурации

**Приоритет 3 (Дополнительно):**
- Сетевые пакеты
- Данные игрока
- Редактор
- Утилиты
- Дополнительные классы

---

**Текущая готовность:** ~60-65% по функциональности генерации
альности генерации
