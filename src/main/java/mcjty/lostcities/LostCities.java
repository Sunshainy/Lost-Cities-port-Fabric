package mcjty.lostcities;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import mcjty.lostcities.network.PacketRequestProfile;
import mcjty.lostcities.network.PacketReturnProfileToClient;
import mcjty.lostcities.setup.*;
import mcjty.lostcities.varia.ServerAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class LostCities implements ModInitializer {
    public static final String MODID = "lostcities";

    public static final Logger LOGGER = LogManager.getLogger(LostCities.MODID);

    public static final ModSetup setup = new ModSetup();
    public static LostCities instance;
    public static final LostCitiesImp lostCitiesImp = new LostCitiesImp();

    @Override
    public void onInitialize() {
        instance = this;

        Registration.init();
        CustomRegistries.init();

        Path configPath = FabricLoader.getInstance().getConfigDir();
        File dir = new File(configPath + File.separator + "lostcities");
        dir.mkdirs();

        // Forge Config API Port keeps the NeoForge ModConfigSpec API intact on Fabric
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.CLIENT, Config.CLIENT_CONFIG, "lostcities/client.toml");
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.COMMON, Config.COMMON_CONFIG, "lostcities/common.toml");
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.SERVER, Config.SERVER_CONFIG);

        setup.preInit();
        setup.init();

        registerNetworking();

        // Track the current server (replaces NeoForge's ServerLifecycleHooks)
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerAccess.setServer(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerAccess.setServer(null));

        // Feature injection (replaces the NeoForge biome modifier JSONs)
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.RAW_GENERATION,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(MODID, "lostcities")));
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
                ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(MODID, "spheres")));

        // Note: NeoForge IMC (ILostCities/ILostCitiesPre) has no Fabric equivalent. Mods that
        // want the Lost Cities API can access LostCities.lostCitiesImp directly.
    }

    private void registerNetworking() {
        PayloadTypeRegistry.playS2C().register(PacketReturnProfileToClient.TYPE, PacketReturnProfileToClient.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketRequestProfile.TYPE, PacketRequestProfile.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PacketRequestProfile.TYPE, (payload, context) -> payload.handle());
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
