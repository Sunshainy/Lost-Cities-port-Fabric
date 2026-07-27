package com.lostcity.commands;

import com.lostcity.LostCityMod;
import com.lostcity.assets.CityStyle;
import com.lostcity.config.LostCityConfig;
import com.lostcity.util.ModLogger;
import com.lostcity.worldgen.BuildingInfo;
import com.lostcity.worldgen.City;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
            CommandManager.literal("lostcities")
                .requires(source -> source.hasPermissionLevel(0)) // Доступно всем игрокам
                .then(CommandManager.literal("info")
                    .executes(ModCommands::executeInfo))
                .then(CommandManager.literal("chunk")
                    .executes(ModCommands::executeInfo))
        );

        // Алиас /lc info
        dispatcher.register(
            CommandManager.literal("lc")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.literal("info")
                    .executes(ModCommands::executeInfo))
        );
    }

    private static int executeInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        StructureWorldAccess world = source.getWorld();
        BlockPos playerPos = source.getPlayer() != null ? source.getPlayer().getBlockPos() : new BlockPos((int)source.getPosition().x, (int)source.getPosition().y, (int)source.getPosition().z);
        ChunkPos chunkPos = new ChunkPos(playerPos);

        LostCityConfig config = LostCityMod.getConfig();
        if (config == null) {
            source.sendFeedback(() -> Text.literal("LostCities config is not loaded!").formatted(Formatting.RED), false);
            return 0;
        }

        BuildingInfo info = BuildingInfo.get(chunkPos, config, world);
        float factor = City.getCityFactor(chunkPos, config, world);
        CityStyle cityStyle = City.getCityStyle(chunkPos, config, world);

        StringBuilder sb = new StringBuilder();
        sb.append("=== LOST CITIES CHUNK INFO ===\n");
        sb.append(String.format("Chunk Pos: (%d, %d) | Block Pos: (%d, %d, %d)\n", 
            chunkPos.x, chunkPos.z, playerPos.getX(), playerPos.getY(), playerPos.getZ()));
        sb.append(String.format("Is City: %b (City Factor: %.3f)\n", info.isCity, factor));
        sb.append(String.format("Has Building: %b\n", info.hasBuilding));

        if (info.hasBuilding) {
            sb.append(String.format("Building Type: %s\n", info.buildingType != null ? info.buildingType : "none"));
            sb.append(String.format("Floors: %d | Cellars: %d\n", info.floors, info.cellars));
            sb.append(String.format("Front Type: %s | Door Block: %s\n", 
                info.frontType != null ? info.frontType : "none",
                info.doorBlock != null ? info.doorBlock.getName().getString() : "none"));
            
            sb.append("Palettes:\n");
            sb.append(String.format("  Bricks: %s\n", info.selectedBricksPalette != null ? info.selectedBricksPalette : "default"));
            sb.append(String.format("  Glass: %s\n", info.selectedGlassPalette != null ? info.selectedGlassPalette : "default"));
            sb.append(String.format("  GlassSide: %s\n", info.selectedGlassSidePalette != null ? info.selectedGlassSidePalette : "default"));

            if (info.multiBuildingPos != null && info.multiBuildingPos.isMulti()) {
                sb.append(String.format("MultiBuilding: %s (Part [%d,%d] of %dx%d, TopLeft: %b)\n",
                    info.multiBuilding != null ? info.multiBuilding.getName() : "none",
                    info.multiBuildingPos.x(), info.multiBuildingPos.z(), info.multiBuildingPos.w(), info.multiBuildingPos.h(),
                    info.multiBuildingPos.isTopLeft()));
            }
        }

        if (cityStyle != null) {
            sb.append(String.format("CityStyle: %s (Style Name: %s)\n", cityStyle.getId(), cityStyle.getStyle()));
        } else {
            sb.append("CityStyle: none\n");
        }

        if (info.streetType != null) {
            sb.append(String.format("Street Type: %s\n", info.streetType));
        }

        if (info.highwayXLevel >= 0 || info.highwayZLevel >= 0) {
            sb.append(String.format("Highway Levels -> X: %d, Z: %d\n", info.highwayXLevel, info.highwayZLevel));
        }

        sb.append("==============================");

        String resultText = sb.toString();
        // Логируем в консоль для удобного копирования
        ModLogger.info("\n" + resultText);

        // Отправляем в чат игроку
        source.sendFeedback(() -> Text.literal(resultText).formatted(Formatting.YELLOW), false);

        return 1;
    }
}
