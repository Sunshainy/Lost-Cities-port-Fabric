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
}
