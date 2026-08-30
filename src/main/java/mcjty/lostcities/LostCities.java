package mcjty.lostcities;

/*
 * Forge Config API Port переехал с neoforge/v4/NeoForgeConfigRegistry на v5/ConfigRegistry
 * в версии для 1.21.5. Подпись register() при этом не изменилась.
 *
 * На 1.20.1 действует ещё более раннее, форджевое поколение того же API
 * (api/config/v2/ForgeConfigRegistry). Ветки Stonecutter под него нет: имена
 * переписываются переименованиями из build.gradle.kts, потому что там меняются
 * только названия классов и пакетов, а подписи те же.
 */
//? if >=1.21.5 {
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
//?} else {
/*import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
*///?}
import mcjty.lostcities.network.PacketRequestProfile;
import mcjty.lostcities.network.PacketReturnProfileToClient;
import mcjty.lostcities.setup.*;
import mcjty.lostcities.varia.ServerAccess;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}
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
        //? if >=1.21.5 {
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.CLIENT, Config.CLIENT_CONFIG, "lostcities/client.toml");
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.COMMON, Config.COMMON_CONFIG, "lostcities/common.toml");
        ConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.SERVER, Config.SERVER_CONFIG);
        //?} else {
        /*NeoForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.CLIENT, Config.CLIENT_CONFIG, "lostcities/client.toml");
        NeoForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.COMMON, Config.COMMON_CONFIG, "lostcities/common.toml");
        NeoForgeConfigRegistry.INSTANCE.register(MODID, ModConfig.Type.SERVER, Config.SERVER_CONFIG);
        *///?}

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

    // До 1.20.5 в Fabric не было PayloadTypeRegistry: PacketType.create сам несёт
    // и идентификатор, и читающую функцию, так что регистрировать кодеки не нужно.
    private void registerNetworking() {
        //? if >=1.20.5 {
        PayloadTypeRegistry.playS2C().register(PacketReturnProfileToClient.TYPE, PacketReturnProfileToClient.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketRequestProfile.TYPE, PacketRequestProfile.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PacketRequestProfile.TYPE, (payload, context) -> payload.handle());
        //?} else
        /*ServerPlayNetworking.registerGlobalReceiver(PacketRequestProfile.TYPE, (packet, player, sender) -> packet.handle());*/
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
