package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.events.ReceiveRefreshEvent;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class RefreshListener implements Listener {
    private final UserProvider userProvider;

    public RefreshListener(UserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @EventHandler
    public void handleRefresh(ReceiveRefreshEvent event) {
        String cache = event.getType();
        String key = event.getKey();
        Player player = event.getPlayer();
        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) return;
        if (user.debugMode()) {
            player.sendRichMessage("<#8800cc>Cache - Type: <cache_type>, Key: <cache_key>",
                    Placeholder.unparsed("cache_type", cache), Placeholder.unparsed("cache_key", key));
        }
    }
}
