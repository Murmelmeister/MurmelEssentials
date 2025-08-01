package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.manager.ListenerManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;

public final class PlayerChatListener extends ListenerManager {
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
