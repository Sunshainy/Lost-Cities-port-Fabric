package mcjty.lostcities.varia;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/*
 * Смена измерения переписывалась дважды.
 *
 * До 1.21 ванильный changeDimension(ServerLevel) не умел задавать точку прибытия —
 * оригинал добирал это NeoForge-овским ITeleporter. На Fabric ту же роль играет
 * FabricDimensions.teleport с PortalInfo: измерение, позиция, нулевая скорость,
 * повороты сохраняются. Поведение совпадает с оригиналом.
 *
 * В 1.21 появился DimensionTransition, в 1.21.2 его переименовали в
 * TeleportTransition, а метод стал teleport(). Поля те же.
 */
//? if >=1.21.2 {
import net.minecraft.world.level.portal.TeleportTransition;
//?} elif >=1.21 {
/*import net.minecraft.world.level.portal.DimensionTransition;
*///?} else {
/*import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.world.level.portal.PortalInfo;
*///?}

public class CustomTeleporter {

    public static void teleportToDimension(Player player, ServerLevel dimension, BlockPos pos){
        teleportToDimension(player, dimension, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    public static void teleportToDimension(Player player, ServerLevel dimension, double x, double y, double z) {
        //? if >=1.21.2 {
        player.teleport(new TeleportTransition(dimension, new Vec3(x, y, z), Vec3.ZERO, 0.0f, 0.0f, TeleportTransition.PLAY_PORTAL_SOUND));
        //?} elif >=1.21 {
        /*player.changeDimension(new DimensionTransition(dimension, new Vec3(x, y, z), Vec3.ZERO, 0.0f, 0.0f, DimensionTransition.PLAY_PORTAL_SOUND));
        *///?} else {
        /*FabricDimensions.teleport(player, dimension, new PortalInfo(new Vec3(x, y, z), Vec3.ZERO, player.getYRot(), player.getXRot()));
        *///?}
    }

}
