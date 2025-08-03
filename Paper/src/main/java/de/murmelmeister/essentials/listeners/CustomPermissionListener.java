package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.murmelapi.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.lang.reflect.Field;

public final class CustomPermissionListener implements Listener {
    private final Permission permission;

    public CustomPermissionListener(Permission permission) {
        this.permission = permission;
    }

    @EventHandler
    public void handlePlayerLogin(PlayerLoginEvent event) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Player player = event.getPlayer();
        Field field = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity").getDeclaredField("perm");
        field.setAccessible(true);
        field.set(player, new CustomPermission(player, permission));
        field.setAccessible(false);
        // Maybe could work? -> player.getServer().getPluginManager().subscribeToDefaultPerms(false, new CustomPermission(player, permission));
    }
}
