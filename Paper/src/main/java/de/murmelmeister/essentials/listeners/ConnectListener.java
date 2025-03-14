package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.ListenerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectListener extends ListenerManager {
    public ConnectListener(MurmelEssentials instance) {
        super(instance);
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.updateCommands();
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.updateCommands();
    }
}
