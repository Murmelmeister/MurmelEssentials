package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.HexColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.SQLException;

public final class PlayerChatListener extends ListenerManager {
    public PlayerChatListener(MurmelEssentials instance) {
        super(instance);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void handlePlayerChat(AsyncPlayerChatEvent event) throws SQLException {
        Ranks.setChatFormat(event, this.instance.getGroup(), this.instance.getUser());
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void handlePlayerColor(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        if (player.hasPermission("murmelessentials.chat.color")) event.setMessage(ChatColor.translateAlternateColorCodes('&', event.getMessage()));
        if (player.hasPermission("murmelessentials.chat.hex")) event.setMessage(HexColor.format(event.getMessage()));
    }
}
