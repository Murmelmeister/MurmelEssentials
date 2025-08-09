package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.events.ReceiveRefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;

public class RefreshListener implements Listener {
    private final Logger logger;

    public RefreshListener(Logger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void handleRefresh(ReceiveRefreshEvent event) {
        String cache = event.getType();
        if (cache.equalsIgnoreCase(RefreshType.SINGLE_USER_PLAY_TIME.getName())) return;
        String key = event.getKey();
        Player player = event.getPlayer();
        logger.info("Cache: {}, Key: {}", cache, key);
        player.sendMessage(MiniMessage.miniMessage().deserialize("<#8800cc>CacheName: " + cache + ", Key: " + key));
    }
}
