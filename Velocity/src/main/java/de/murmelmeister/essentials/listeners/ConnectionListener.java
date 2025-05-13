package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;

import java.net.InetAddress;
import java.sql.Timestamp;

public final class ConnectionListener {
    private final User user;
    private final Group group;
    private final PlayTime playTime;
    private final ActiveSession activeSession;

    public ConnectionListener(User user, Group group, PlayTime playTime, ActiveSession activeSession) {
        this.user = user;
        this.group = group;
        this.playTime = playTime;
        this.activeSession = activeSession;
    }

    private int processUserJoin(Player player) {
        user.joinUser(player.getUniqueId(), player.getUsername()); // Create a user if not exists or rename if the username is changed
        int userId = user.getId(player.getUniqueId());
        int defaultGroupId = group.getId("default"); // Default -> GroupName: default
        if (!user.getParent().existsParent(userId, defaultGroupId))
            user.getParent().addParent(userId, defaultGroupId, -1, -1);
        Timestamp firstLogin = new Timestamp(System.currentTimeMillis());
        if (user.getFirstJoin(userId) == null)
            user.setFirstJoin(userId, firstLogin);
        if (!playTime.existsUser(userId))
            playTime.createUser(userId);
        return userId;
    }

    private void processSessionStart(Player player, int userId) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        if (activeSession.existsSession(userId))
            activeSession.closeSession(userId); // Or kick the player and send a message with reconnecting
        activeSession.startSession(userId, inetAddress, player.getClientBrand(), player.getProtocolVersion().toString());
    }

    @Subscribe
    public void handlePostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        int userId = processUserJoin(player);
        processSessionStart(player, userId);
        // TODO: checkPunishment
    }

    @Subscribe
    public void handleDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        activeSession.closeSession(userId);
    }
}
