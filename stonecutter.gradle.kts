plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

// Собирает все версии одной командой: ./gradlew buildAndCollect
// Без этого задача существует только в подпроектах (:1.20.1:buildAndCollect).
// В Stonecutter 0.9 вместо `chiseled` используется stonecutter.tasks.named(...),
// который отдаёт карту «версия -> задача» по всем подпроектам.
tasks.register("buildAndCollect") {
    group = "build"
    description = "Собирает джарники всех версий в build/libs/{версия мода}/"
    dependsOn(stonecutter.tasks.named("buildAndCollect").map { it.values })
}

// См. https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    // Позволяет писать предикаты вида `//? if fapi >=0.100` для версий Fabric API.
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    // Массовые переименования ванильных классов между версиями.
    //
    // Исходники хранятся в диалекте верхней версии диапазона (1.21.11), потому что
    // это диалект ветки апстрима, с которой мы забираем коммиты через cherry-pick.
    // Для более старых версий имена переписываются обратно при препроцессинге.
    //
    // Без этого пришлось бы ставить условие Stonecutter на каждое обращение:
    // только ResourceLocation -> Identifier в 1.21.9 задел ~460 строк кода.
    replacements {
        // Mojang переименовал ResourceLocation -> Identifier в 1.21.9.
        // Одно правило покрывает и импорт, и все обращения.
        // direction = true — применять прямое правило (Identifier -> ResourceLocation),
        // то есть на версиях старше 1.21.9.
        regex("identifier") {
            direction.set(current.parsed < "1.21.9")
            replace("\\bIdentifier\\b", "ResourceLocation", "\\bResourceLocation\\b", "Identifier")
        }
    }
}
