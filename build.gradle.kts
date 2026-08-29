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
