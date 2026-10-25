package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.events.ReceiveRefreshEvent;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class RefreshListener implements Listener {
    private final UserProvider userProvider;
    private final Server server;

    public RefreshListener(UserProvider userProvider, Server server) {
        this.userProvider = userProvider;
        this.server = server;
    }

    @EventHandler
    public void handleRefresh(ReceiveRefreshEvent event) {
        String cache = event.getType();
        String key = event.getKey();

        server.getOnlinePlayers().forEach(player -> {
            User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
            if (user == null) return;
            if (user.debugMode()) {
                player.sendRichMessage("<#8800cc>Cache - Type: <cache_type>, Key: <cache_key>",
                        Placeholder.unparsed("cache_type", cache), Placeholder.unparsed("cache_key", key == null ? "null" : key));
            }

        });
    }
}
