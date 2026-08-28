plugins {
    id("net.fabricmc.fabric-loom-remap")
}

// group НЕ выставляем — Stonecutter/Loom управляют этим сами.
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

/** Свойство из stonecutter.properties.toml — с учётом секции активной версии. */
fun scProp(key: String): String = sc.properties[key]

repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.architectury.dev/")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings("net.fabricmc:yarn:${scProp("deps.yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${scProp("deps.fabric_api")}")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("lostcity") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
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
        filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }

        // В 1.21 каталоги датапаков переименованы в единственное число
        // (data/<ns>/loot_tables -> loot_table и т.д.). Держим исходники в одном
        // виде и переименовываем при сборке, чтобы не дублировать JSON по версиям.
        if (sc.current.parsed >= "1.21") {
            filesMatching("data/*/loot_tables/**") {
                path = path.replace("/loot_tables/", "/loot_table/")
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

