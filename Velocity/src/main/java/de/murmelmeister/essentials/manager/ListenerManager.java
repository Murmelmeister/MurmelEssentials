package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.ConnectionListener;
import de.murmelmeister.essentials.listeners.PermissionListener;

public final class ListenerManager {
    public static void register(MurmelEssentials plugin, ProxyServer server) {
        addListener(plugin, server, new PermissionListener(plugin.getPermission()));
        addListener(plugin, server, new ConnectionListener(plugin.getUser(), plugin.getGroup(), plugin.getPlayTime(), plugin.getActiveSession()));
    }

    private static void addListener(MurmelEssentials plugin, ProxyServer server, Object clazz) {
        server.getEventManager().register(plugin, clazz);
    }
}
