package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.events.ReceiveRefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RefreshListener implements Listener {
    @EventHandler
    public void handleRefresh(ReceiveRefreshEvent event) {
        String cache = event.getType();
        if (cache.equalsIgnoreCase(RefreshType.SINGLE_USER_PLAY_TIME.getName())) return;
        String key = event.getKey();
        Player player = event.getPlayer();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<#8800cc>CacheName: " + cache + ", Key: " + key));
    }
}
