package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.PermissionListener;

public final class ListenerManager {
    public static void register(ProxyServer server, MurmelEssentials instance) {
        addListener(server, instance, new PermissionListener(instance.getPermission(), instance.getGroup(), instance.getUser()));
    }

    private static void addListener(ProxyServer server, MurmelEssentials instance, Object clazz) {
        server.getEventManager().register(instance, clazz);
    }
}
