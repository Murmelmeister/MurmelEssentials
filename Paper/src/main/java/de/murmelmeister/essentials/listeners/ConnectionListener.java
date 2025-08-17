package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.api.Ranks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectionListener implements Listener {
    private final Ranks ranks;

    public ConnectionListener(Ranks ranks) {
        this.ranks = ranks;
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        ranks.getHasUpdated().set(true);
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent event) {
        ranks.getHasUpdated().set(true);
    }
}
