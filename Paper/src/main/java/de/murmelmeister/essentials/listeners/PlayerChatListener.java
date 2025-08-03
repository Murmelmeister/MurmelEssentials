package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.Ranks;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PlayerChatListener implements Listener {
    private final Ranks ranks;

    public PlayerChatListener(MurmelEssentials instance) {
        this.ranks = instance.getRanks();
    }

    @EventHandler
    public void handlePlayerChat(AsyncChatEvent event) {
        // TODO: CheckPunishment
        ranks.setChatFormat(event);
    }
}
