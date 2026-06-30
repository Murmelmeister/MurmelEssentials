package de.murmelmeister.essentials.manager;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.ConnectionListener;
import de.murmelmeister.essentials.listeners.CustomPermissionListener;
import de.murmelmeister.essentials.listeners.PlayerChatListener;
import de.murmelmeister.essentials.listeners.RefreshListener;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ListenerManager implements Listener {
    public static void register(@NotNull MurmelEssentials instance) {
        addListener(instance, new CustomPermissionListener(instance.getSLF4JLogger(), instance.getUserProvider(), instance.getPermissionService()));
        addListener(instance, new PlayerChatListener(instance));
        addListener(instance, new RefreshListener(instance.getUserProvider()));
        addListener(instance, new ConnectionListener(instance.getRanks()));
    }

    private static void addListener(@NotNull MurmelEssentials instance, @NotNull Listener listener) {
        instance.getServer().getPluginManager().registerEvents(listener, instance);
    }
}
