package de.murmelmeister.essentials.manager;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.CustomPermissionListener;
import de.murmelmeister.essentials.listeners.PlayerChatListener;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.event.Listener;

public class ListenerManager implements Listener {
    protected final MurmelEssentials instance;
    protected final User user;
    protected final Permission permission;

    public ListenerManager(MurmelEssentials instance) {
        this.instance = instance;
        this.user = instance.getUser();
        this.permission = instance.getPermission();
    }

    public static void register(MurmelEssentials instance) {
        addListener(instance, new CustomPermissionListener(instance));
        addListener(instance, new PlayerChatListener(instance));
    }

    private static void addListener(MurmelEssentials instance, Listener listener) {
        instance.getServer().getPluginManager().registerEvents(listener, instance);
    }
}
