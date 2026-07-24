package mcjty.lostcities.setup;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.worldgen.lost.regassets.*;
import mcjty.lostcities.worldgen.lost.regassets.StuffSettingsRE;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public class CustomRegistries {

    public static final ResourceKey<Registry<BuildingRE>> BUILDING_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "buildings"));

    public static final ResourceKey<Registry<PaletteRE>> PALETTE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "palettes"));

    public static final ResourceKey<Registry<BuildingPartRE>> PART_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "parts"));

    public static final ResourceKey<Registry<StyleRE>> STYLE_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "styles"));

    public static final ResourceKey<Registry<ConditionRE>> CONDITIONS_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "conditions"));

    public static final ResourceKey<Registry<CityStyleRE>> CITYSTYLES_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "citystyles"));

    public static final ResourceKey<Registry<MultiBuildingRE>> MULTIBUILDINGS_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "multibuildings"));

    public static final ResourceKey<Registry<VariantRE>> VARIANTS_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "variants"));

    public static final ResourceKey<Registry<WorldStyleRE>> WORLDSTYLES_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "worldstyles"));

    public static final ResourceKey<Registry<PredefinedCityRE>> PREDEFINEDCITIES_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "predefinedcities"));

    public static final ResourceKey<Registry<PredefinedSphereRE>> PREDEFINEDSPHERES_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "predefinedspheres"));

    public static final ResourceKey<Registry<ScatteredRE>> SCATTERED_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "scattered"));

    public static final ResourceKey<Registry<StuffSettingsRE>> STUFF_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(LostCities.MODID, "stuff"));

    public static void init() {
        // Fabric: register the datapack ("dynamic") registries. These are server-side only
        // (not synced to clients), matching the NeoForge DataPackRegistryEvent behavior
        // (no network codec was provided there either).
        DynamicRegistries.register(BUILDING_REGISTRY_KEY, BuildingRE.CODEC);
        DynamicRegistries.register(PALETTE_REGISTRY_KEY, PaletteRE.CODEC);
        DynamicRegistries.register(PART_REGISTRY_KEY, BuildingPartRE.CODEC);
        DynamicRegistries.register(STYLE_REGISTRY_KEY, StyleRE.CODEC);
        DynamicRegistries.register(CONDITIONS_REGISTRY_KEY, ConditionRE.CODEC);
        DynamicRegistries.register(CITYSTYLES_REGISTRY_KEY, CityStyleRE.CODEC);
        DynamicRegistries.register(MULTIBUILDINGS_REGISTRY_KEY, MultiBuildingRE.CODEC);
        DynamicRegistries.register(VARIANTS_REGISTRY_KEY, VariantRE.CODEC);
        DynamicRegistries.register(WORLDSTYLES_REGISTRY_KEY, WorldStyleRE.CODEC);
        DynamicRegistries.register(PREDEFINEDCITIES_REGISTRY_KEY, PredefinedCityRE.CODEC);
        DynamicRegistries.register(PREDEFINEDSPHERES_REGISTRY_KEY, PredefinedSphereRE.CODEC);
        DynamicRegistries.register(SCATTERED_REGISTRY_KEY, ScatteredRE.CODEC);
        DynamicRegistries.register(STUFF_REGISTRY_KEY, StuffSettingsRE.CODEC);
    }
}
