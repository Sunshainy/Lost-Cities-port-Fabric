plugins {
    id("net.fabricmc.fabric-loom-remap")
}

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

/** Свойство из stonecutter.properties.toml — с учётом секции активной версии. */
fun scProp(key: String): String = sc.properties[key]

/*
 * Массовые переименования ванильных классов между версиями.
 *
 * Исходники хранятся в диалекте верхней версии диапазона (1.21.11) — это диалект ветки
 * апстрима, с которой мы забираем коммиты через cherry-pick. Для более старых версий
 * имена переписываются после препроцессинга Stonecutter, в build/generated/stonecutter/.
 * Рабочее дерево при этом не меняется, поэтому в git не попадает ничего версионного.
 *
 * Условие Stonecutter на каждое обращение тут не годится: одно только
 * ResourceLocation -> Identifier из 1.21.9 задело бы ~460 строк.
 *
 * Механизм `replacements` самого Stonecutter не подошёл: правила регистрируются,
 * но к сгенерированным исходникам не применяются (0.9.7).
 */
val vanillaRenames: List<Pair<Regex, String>> = buildList {
    // Переименования из 1.21.9. Все три — чистые переименования, семантика та же.
    if (sc.current.parsed < "1.21.9") {
        // ResourceLocation -> Identifier
        add(Regex("\\bIdentifierArgument\\b") to "ResourceLocationArgument")
        add(Regex("\\bIdentifier\\b") to "ResourceLocation")
        // ResourceKey#location() -> identifier()
        add(Regex("\\bidentifier\\(\\)") to "location()")
        add(Regex("ResourceKey::identifier\\b") to "ResourceKey::location")
        // NoiseRouter#initialDensityWithoutJaggedness() -> preliminarySurfaceLevel()
        add(Regex("\\bpreliminarySurfaceLevel\\(\\)") to "initialDensityWithoutJaggedness()")
    }
    // Переименования из 1.21.5.
    if (sc.current.parsed < "1.21.5") {
        // WallBlock.NORTH_WALL -> NORTH и так далее по всем четырём сторонам.
        add(Regex("\\bWallBlock\\.(NORTH|EAST|SOUTH|WEST)\\b") to "WallBlock.$1_WALL")
        // Commands.hasPermission(x) появился вместе с PermissionCheck; до него
        // предикат писали вручную.
        add(Regex("Commands\\.hasPermission\\((Commands\\.LEVEL_[A-Z]+)\\)") to "source -> source.hasPermission($1)")
        // NBT: аксессоры со значением по умолчанию появились в 1.21.5.
        add(Regex("\\.getStringOr\\((\"[^\"]*\"), \"\"\\)") to ".getString($1)")
    }
    // Переименования из 1.21.
    if (sc.current.parsed < "1.21") {
        // ResourceLocation: фабричные методы появились в 1.21, до них — конструктор.
        // Оба варианта бросают исключение на неверном идентификаторе, семантика та же.
        // tryParse под правило не попадает: в нём другое имя метода.
        add(Regex("\\bResourceLocation\\.fromNamespaceAndPath\\(") to "new ResourceLocation(")
        add(Regex("\\bResourceLocation\\.parse\\(") to "new ResourceLocation(")
        // ChunkAccess.getStatus() -> getPersistedStatus() в 1.21.
        add(Regex("\\.getPersistedStatus\\(\\)") to ".getStatus()")
    }
    // Переименования из 1.21.2.
    if (sc.current.parsed < "1.21.2") {
        // RegistryAccess: lookupOrThrow/lookup -> registryOrThrow/registry.
        // Возвращают Registry вместо HolderLookup, а нужные тут getResourceKey,
        // getOrThrow и getKey есть у обоих.
        add(Regex("\\.lookupOrThrow\\(") to ".registryOrThrow(")
        add(Regex("registryAccess\\(\\)\\.lookup\\(") to "registryAccess().registry(")
        // Registry: get() стал возвращать Optional<Holder.Reference>, а прямое
        // значение переехало в getValue(). Порядок правил важен — сперва get,
        // потом getValue, иначе второе правило переписало бы результат первого.
        add(Regex("BuiltInRegistries\\.([A-Z_]+)\\.get\\(") to "BuiltInRegistries.$1.getHolder(")
        add(Regex("BuiltInRegistries\\.([A-Z_]+)\\.getValue\\(") to "BuiltInRegistries.$1.get(")
        // Тот же сдвиг get/getValue у обычного Registry, полученного из RegistryAccess.
        add(Regex("registryOrThrow\\((Registries\\.[A-Z_]+)\\)\\.getValue\\(") to "registryOrThrow($1).get(")
        add(Regex("\\.getValueOrThrow\\(") to ".getOrThrow(")
        // getOrThrow у Registry стал возвращать Holder.Reference; прежнее поведение —
        // getHolderOrThrow. Правила привязаны к именам получателей намеренно: у наших
        // собственных AssetRegistries есть свой getOrThrow, и его трогать нельзя.
        // Если апстрим переименует переменную, правило просто перестанет срабатывать
        // и сборка упадёт — то есть отказ будет заметным, а не тихим.
        add(Regex("\\bstructures\\.getOrThrow\\(") to "structures.getHolderOrThrow(")
        add(Regex("\\bbiomeRegistry\\.getOrThrow\\(") to "biomeRegistry.getHolderOrThrow(")
        // Границы мира. getMinY() совпадает по смыслу с getMinBuildHeight(), а вот
        // getMaxY() включающий, тогда как getMaxBuildHeight() исключающий, поэтому
        // здесь нужен сдвиг на единицу, а не просто переименование.
        //
        // Правила требуют получателя перед точкой намеренно: в varia/GeometryTools
        // есть свои getMinY()/getMaxY() у вспомогательного бокса, и их трогать нельзя.
        val receiver = "((?:[A-Za-z_][A-Za-z0-9_]*\\.)*[A-Za-z_][A-Za-z0-9_]*(?:\\(\\))?)"
        add(Regex("$receiver\\.getMinY\\(\\)") to "$1.getMinBuildHeight()")
        add(Regex("$receiver\\.getMaxY\\(\\)") to "($1.getMaxBuildHeight() - 1)")
    }
    // Переименования из 1.20.5. Группа идёт последней намеренно: три её правила
    // работают по тексту, который получается уже после правил 1.21 и 1.21.2
    // (ResourceLocation вместо Identifier, getHolder вместо get).
    if (sc.current.parsed < "1.20.5") {
        // ChunkStatus переехал в подпакет chunk.status.
        add(Regex("net\\.minecraft\\.world\\.level\\.chunk\\.status\\.ChunkStatus")
                to "net.minecraft.world.level.chunk.ChunkStatus")
        // SpawnData получил третий параметр (снаряжение) в 1.20.5.
        add(Regex("new SpawnData\\(sd, Optional\\.empty\\(\\), Optional\\.empty\\(\\)\\)")
                to "new SpawnData(sd, Optional.empty())")
        // Реестр лут-таблиц появился в 1.20.5: до него setLootTable принимает
        // сам ResourceLocation, а не ключ реестра.
        add(Regex("setLootTable\\(ResourceKey\\.create\\(Registries\\.LOOT_TABLE, ([^;]*)\\)\\)")
                to "setLootTable($1)")
        // Registry.getHolder(ResourceLocation) появился в 1.20.5; до него есть
        // только перегрузка по ключу реестра. Возвращаемый тип тот же —
        // Optional<Holder.Reference<Block>>, поэтому вызывающий код не меняется.
        // Имена классов полные: так правило не требует новых импортов и файлы
        // апстрима остаются нетронутыми, что важно для cherry-pick.
        add(Regex("BuiltInRegistries\\.BLOCK\\.getHolder\\(new ResourceLocation\\(([^;()]*)\\)\\)")
                to "BuiltInRegistries.BLOCK.getHolder(net.minecraft.resources.ResourceKey.create("
                        + "net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation($1)))")
    }
    // Переименования из 1.20.2. Группа идёт после 1.20.5 намеренно: два её правила
    // разбирают текст, который получается уже после правил той группы (setLootTable
    // без ключа реестра, Factory вместо Codec).
    if (sc.current.parsed < "1.20.2") {
        /*
         * Конфиги. 1.20.1 — единственная версия диапазона на форджевой эпохе
         * Forge Config API Port (8.0.3): там, где выше лежит neoforge/v4 или v5,
         * здесь api/config/v2, а сам ModConfigSpec ещё зовётся ForgeConfigSpec.
         * NeoForge переименовал класс, не меняя ни вложенных типов (Builder,
         * ConfigValue, IntValue, BooleanValue, EnumValue, Range), ни подписей
         * register(), поэтому переименования достаточно и ветка Stonecutter не нужна.
         */
        add(Regex("fuzs\\.forgeconfigapiport\\.fabric\\.api\\.neoforge\\.v4\\.NeoForgeConfigRegistry")
                to "fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry")
        add(Regex("\\bNeoForgeConfigRegistry\\b") to "ForgeConfigRegistry")
        add(Regex("net\\.neoforged\\.fml\\.config\\.ModConfig") to "net.minecraftforge.fml.config.ModConfig")
        add(Regex("net\\.neoforged\\.neoforge\\.common\\.ModConfigSpec") to "net.minecraftforge.common.ForgeConfigSpec")
        add(Regex("\\bModConfigSpec\\b") to "ForgeConfigSpec")
        // SavedData.Factory и датафиксеры сохранений появились в 1.20.2: до них
        // computeIfAbsent принимает десериализатор и конструктор прямо, причём в
        // обратном порядке — Function перед Supplier. Отсюда $2 перед $1: текстом
        // обе ссылки одинаковы, но порядок аргументов должен быть верным по смыслу.
        add(Regex("computeIfAbsent\\(new Factory<>\\((\\w+)::new, (\\w+)::new, DataFixTypes\\.[A-Z_]+\\), NAME\\)")
                to "computeIfAbsent($2::new, $1::new, NAME)")
        // Перегрузка setLootTable без сида появилась в 1.20.2. До неё сид обязателен,
        // и ноль — то же самое, что делает односложная перегрузка: она сид не трогает,
        // а по умолчанию он и так нулевой (сундук берёт случайный сид при вскрытии).
        add(Regex("\\.setLootTable\\((new ResourceLocation\\([^;()]*\\))\\)") to ".setLootTable($1, 0L)")
    }
}

if (vanillaRenames.isNotEmpty()) {
    tasks.named("stonecutterGenerate") {
        doLast {
            val root = layout.buildDirectory.dir("generated/stonecutter").get().asFile
            var touched = 0
            root.walkTopDown().filter { it.isFile && it.extension == "java" }.forEach { file ->
                val before = file.readText()
                var after = before
                for ((pattern, replacement) in vanillaRenames) {
                    after = pattern.replace(after, replacement)
                }
                if (after != before) {
                    file.writeText(after)
                    touched++
                }
            }
            logger.lifecycle("Переименования под ${sc.current.version}: затронуто файлов — $touched")
        }
    }
}

/** Версии Minecraft, которые покрывает этот джарник. */
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

/**
 * Диапазон для имени файла: "1.20-1.20.1" вместо "1.20.1".
 * Так по названию джарника сразу видно, на какие версии он встанет, и не надо
 * сверяться с описанием на странице мода.
 */
val versionRange: String = when {
    compatibleVersions.isEmpty() -> sc.current.version
    compatibleVersions.size == 1 -> compatibleVersions.first()
    else -> "${compatibleVersions.first()}-${compatibleVersions.last()}"
}

// group НЕ выставляем — Stonecutter/Loom управляют этим сами.
// Версия мода в fabric.mod.json берётся отдельно из mod.version и остаётся чистой,
// здесь диапазон нужен только для имени файла.
version = "${property("mod.version")}+$versionRange"
base.archivesName = property("mod.id") as String

repositories {
    mavenCentral()
    // Forge Config API Port (fuzs) — даёт ModConfigSpec, благодаря чему
    // setup/Config.java и config/LostCityProfile.java берутся из оригинала без правок.
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
        name = "Fuzs Mod Resources"
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Официальные маппинги Mojang, а не Yarn: исходники оригинала написаны под них,
    // поэтому апстрим применяется через cherry-pick без перевода имён.
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${scProp("deps.fabric_api")}")
    modImplementation("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${scProp("deps.forge_config_api_port")}")
    // javax.annotation.Nonnull/Nullable — оригинал использует их повсеместно.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

sourceSets {
    main {
        // Шесть JSON тегов блоков, ранее генерировавшихся датагеном оригинала.
        resources.srcDir(rootProject.file("src/generated/resources"))
    }
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/META-INF/lostcities.accesswidener")

    mods {
        create("lostcities") {
            sourceSet(sourceSets["main"])
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
}

tasks.withType<JavaCompile>().configureEach {
    options.release = requiredJava.majorVersion.toInt()
}

tasks {
    processResources {
        val props = mapOf(
            "id" to scProp("mod.id"),
            "name" to scProp("mod.name"),
            "version" to scProp("mod.version"),
            "minecraft" to scProp("mod.mc_compat"),
            // Джарник компилируется под Java этой версии MC, значит и требовать
            // надо её же: с 1.20.5 это 21, до неё 17.
            "java" to ">=${requiredJava.majorVersion}",
        )
        props.forEach { (k, v) -> inputs.property(k, v) }

        filesMatching("fabric.mod.json") { expand(props) }

        // Исходники ресурсов лежат в раскладке 1.21.11 (верх диапазона). Для более
        // старых версий каталоги датапака переименовываются обратно при сборке,
        // чтобы не держать по копии JSON на версию.
        if (sc.current.parsed < "1.21") {
            filesMatching("data/*/loot_table/**") {
                path = path.replace("/loot_table/", "/loot_tables/")
            }
        }
        if (sc.current.parsed < "1.21.9") {
            filesMatching("data/*/tags/block/**") {
                path = path.replace("/tags/block/", "/tags/blocks/")
            }
        }
    }

    jar {
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${base.archivesName.get()}" }
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Собирает джарники и кладёт их в build/libs/{версия мода}/"

        from(named<AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
        from(named<AbstractArchiveTask>("remapSourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${scProp("mod.version")}"))
    }
}
