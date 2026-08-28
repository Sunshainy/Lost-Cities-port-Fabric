plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.1"

// См. https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    // Позволяет писать предикаты вида `//? if fapi >=0.100` для версий Fabric API.
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}
