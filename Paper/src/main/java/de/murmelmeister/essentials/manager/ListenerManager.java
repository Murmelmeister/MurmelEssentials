package de.murmelmeister.essentials.manager;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.CustomPermissionListener;
import de.murmelmeister.murmelapi.permission.Permission;
import org.bukkit.event.Listener;

public class ListenerManager implements Listener {
    public final MurmelEssentials instance;
    public final Permission permission;

    public ListenerManager(MurmelEssentials instance) {
        this.instance = instance;
        this.permission = instance.getPermission();
    }

    public static void register(MurmelEssentials instance) {
        addListener(instance, new CustomPermissionListener(instance));
    }

    private static void addListener(MurmelEssentials instance, Listener listener) {
        instance.getServer().getPluginManager().registerEvents(listener, instance);
    }
}
