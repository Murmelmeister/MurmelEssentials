package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;

public final class CustomPermissionListener extends ListenerManager{
    public CustomPermissionListener(MurmelEssentials instance) {
        super(instance);
    }

    @EventHandler
    public void handlePlayerLogin(PlayerLoginEvent event) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Player player = event.getPlayer();
        Field field = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity").getDeclaredField("perm");
        field.setAccessible(true);
        field.set(player, new CustomPermission(player, this.permission));
        field.setAccessible(false);
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        RefreshUtil.markAsRefreshed();
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        RefreshUtil.markAsRefreshed();
    }
}
