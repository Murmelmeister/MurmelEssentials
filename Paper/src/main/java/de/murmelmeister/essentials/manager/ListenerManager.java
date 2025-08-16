package de.murmelmeister.essentials.manager;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.ConnectionListener;
import de.murmelmeister.essentials.listeners.CustomPermissionListener;
import de.murmelmeister.essentials.listeners.PlayerChatListener;
import de.murmelmeister.essentials.listeners.RefreshListener;
import org.bukkit.event.Listener;

public class ListenerManager implements Listener {
    public static void register(MurmelEssentials instance) {
        addListener(instance, new CustomPermissionListener(instance.getPermission()));
        addListener(instance, new PlayerChatListener(instance));
        addListener(instance, new RefreshListener(instance.getSLF4JLogger(), instance.getUserProvider()));
        addListener(instance, new ConnectionListener(instance.getRanks()));
    }

    private static void addListener(MurmelEssentials instance, Listener listener) {
        instance.getServer().getPluginManager().registerEvents(listener, instance);
    }
}
