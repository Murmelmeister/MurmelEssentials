package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.CacheInvalidateListener;
import de.murmelmeister.essentials.listeners.ConnectionListener;
import de.murmelmeister.essentials.listeners.PermissionListener;
import de.murmelmeister.murmelapi.user.User;

public final class ListenerManager {
    public static void register(MurmelEssentials plugin, ProxyServer server) {
        User user = plugin.getUser();
        addListener(plugin, server, new CacheInvalidateListener(plugin));
        addListener(plugin, server, new PermissionListener(user, plugin.getPermission()));
        addListener(plugin, server, new ConnectionListener(user, plugin.getGroup(), plugin.getPlayTime(), plugin.getActiveSession()));
    }

    private static void addListener(MurmelEssentials plugin, ProxyServer server, Object clazz) {
        server.getEventManager().register(plugin, clazz);
    }
}
