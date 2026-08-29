package mcjty.lostcities.mixin;

import mcjty.lostcities.setup.ForgeEventHandlers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.9 {
import net.minecraft.server.level.progress.LevelLoadListener;
//?}

/**
 * Замена NeoForge-овскому событию LevelEvent.CreateSpawnPosition — на Fabric аналога нет.
 * Даёт Lost Cities выбрать начальную точку спавна мира: в городе, вне города, в сфере
 * и так далее, по настройкам профиля.
 * <p>
 * Подпись {@code setInitialSpawn} менялась: в 1.21.9 добавился параметр слушателя загрузки
 * уровня, поэтому цель миксина версионная.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    //? if >=1.21.9 {
    @Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void lostcities$onSetInitialSpawn(ServerLevel level, ServerLevelData levelData, boolean spawnBonusChest,
                                                     boolean isDebug, LevelLoadListener levelLoadListener, CallbackInfo ci) {
        if (!isDebug && ForgeEventHandlers.INSTANCE.onCreateSpawnPoint(level, levelData)) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void lostcities$onSetInitialSpawn(ServerLevel level, ServerLevelData levelData, boolean spawnBonusChest,
                                                     boolean isDebug, CallbackInfo ci) {
        if (!isDebug && ForgeEventHandlers.INSTANCE.onCreateSpawnPoint(level, levelData)) {
            ci.cancel();
        }
    }
    *///?}
}
