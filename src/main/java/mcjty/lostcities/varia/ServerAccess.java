package mcjty.lostcities.varia;

import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

/**
 * Замена NeoForge-овскому ServerLifecycleHooks.getCurrentServer().
 * Значение проставляют ServerLifecycleEvents в {@link mcjty.lostcities.setup.ForgeEventHandlers}.
 */
public final class ServerAccess {

    private static volatile MinecraftServer currentServer;

    private ServerAccess() {
    }

    public static void setServer(@Nullable MinecraftServer server) {
        currentServer = server;
    }

    @Nullable
    public static MinecraftServer getServer() {
        return currentServer;
    }
}
