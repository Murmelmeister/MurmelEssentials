package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.user.UserProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;

public final class CustomPermissionListener implements Listener {
    private final Logger logger;
    private final UserProvider userProvider;
    private final PermissionService permissionService;

    public CustomPermissionListener(Logger logger, UserProvider userProvider, PermissionService permissionService) {
        this.logger = logger;
        this.userProvider = userProvider;
        this.permissionService = permissionService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handlePlayerLogin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            Field field = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity").getDeclaredField("perm");
            field.setAccessible(true);
            field.set(player, new CustomPermission(player, userProvider, permissionService));
            field.setAccessible(false);
        } catch (NoSuchFieldException | SecurityException | ClassNotFoundException | IllegalArgumentException |
                 IllegalAccessException e) {
            logger.error("Could not set custom permission for player {}", player.getName(), e);
        }
        // Maybe could work? -> player.getServer().getPluginManager().subscribeToDefaultPerms(false, new CustomPermission(player, permission));
    }
}
