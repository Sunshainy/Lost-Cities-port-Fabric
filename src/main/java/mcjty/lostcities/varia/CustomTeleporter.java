package mcjty.lostcities.varia;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/*
 * Смена измерения в 1.21.2 переехала с changeDimension(DimensionTransition)
 * на teleport(TeleportTransition) — класс переименован, поля те же.
 */
//? if >=1.21.2 {
import net.minecraft.world.level.portal.TeleportTransition;
//?} else {
/*import net.minecraft.world.level.portal.DimensionTransition;
*///?}

public class CustomTeleporter {

    public static void teleportToDimension(Player player, ServerLevel dimension, BlockPos pos){
        teleportToDimension(player, dimension, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    public static void teleportToDimension(Player player, ServerLevel dimension, double x, double y, double z) {
        //? if >=1.21.2 {
        player.teleport(new TeleportTransition(dimension, new Vec3(x, y, z), Vec3.ZERO, 0.0f, 0.0f, TeleportTransition.PLAY_PORTAL_SOUND));
        //?} else
        /*player.changeDimension(new DimensionTransition(dimension, new Vec3(x, y, z), Vec3.ZERO, 0.0f, 0.0f, DimensionTransition.PLAY_PORTAL_SOUND));*/
    }

}
