package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.manager.ListenerManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;

import java.sql.SQLException;

public final class PlayerChatListener extends ListenerManager {
    public PlayerChatListener(MurmelEssentials instance) {
        super(instance);
    }

    @EventHandler
    public void handlePlayerChat(AsyncChatEvent event) throws SQLException {
        Ranks.setChatFormat(event, this.instance.getGroup(), this.instance.getUser());
    }
}
