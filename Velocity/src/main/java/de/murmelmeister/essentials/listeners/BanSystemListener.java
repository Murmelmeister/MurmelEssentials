package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.murmelapi.bansystem.ban.Ban;
import de.murmelmeister.murmelapi.bansystem.mute.Mute;
import de.murmelmeister.murmelapi.user.User;

public final class BanSystemListener {
    private final Mute mute;
    private final Ban ban;
    private final User user;

    public BanSystemListener(Mute mute, Ban ban, User user) {
        this.mute = mute;
        this.ban = ban;
        this.user = user;
    }

    @Subscribe
    public void handleConnection(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        int uid = user.getId(player.getUniqueId());
        mute.isMuted(uid);
        ban.isBanned(uid);
    }

    @Subscribe
    public void handleMuteChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        int uid = user.getId(player.getUniqueId());
        if (mute.isMuted(uid)) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }
}
