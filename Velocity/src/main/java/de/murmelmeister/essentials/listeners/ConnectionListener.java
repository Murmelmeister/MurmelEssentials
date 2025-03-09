package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;

public final class ConnectionListener {
    private final User user;
    private final Group group;
    private final PlayTime playTime;
    private final ActiveSession activeSession;
    private final PunishmentIP punishmentIp;
    private final PunishmentUser punishmentUser;

    public ConnectionListener(User user, Group group, PlayTime playTime, ActiveSession activeSession, PunishmentIP punishmentIp, PunishmentUser punishmentUser) {
        this.user = user;
        this.group = group;
        this.playTime = playTime;
        this.activeSession = activeSession;
        this.punishmentIp = punishmentIp;
        this.punishmentUser = punishmentUser;
    }

    @Subscribe
    public void handlePostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        user.joinUser(player.getUniqueId(), player.getUsername()); // create user if not exists or rename if username changed
        int userId = user.getId(player.getUniqueId());
        int groupId = group.getUniqueId("default"); // Default → GroupName: default

        if (!user.getParent().existsParent(userId, groupId)) user.getParent().addParent(-1, userId, groupId, -1);
        user.setFirstJoinTime(userId);
        if (!playTime.existsUser(userId)) playTime.createUser(userId);

        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        String ipAddress = inetAddress.getHostAddress();
        activeSession.startSession(userId, inetAddress, player.getClientBrand(), player.getProtocolVersion() + "");

        /*int banTypeId = punishment.getTypes().getTypeId(PunishmentType.BAN.getName());
        if (punishment.exists(ipAddress, banTypeId) && !punishment.exists(userId, banTypeId)) {
            punishment.punishedUser(-1, userId, ipAddress, banTypeId, 99);
            punishedMessage(player, userId, banTypeId);
        } else if (punishment.exists(ipAddress, banTypeId) && punishment.isPunished(userId, banTypeId)) {
            punishedMessage(player, userId, banTypeId);
        } else if (punishment.exists(ipAddress, banTypeId) && !punishment.isPunished(userId, banTypeId)) {
            punishment.unpunishedUser(userId, banTypeId);
        }*/
    }

    /*private void punishedMessage(Player player, int userId, int banTypeId, boolean isIp) {
        UUID logId = isIp ? punishmentIp.getLogId(banTypeId, player.getRemoteAddress().getAddress()) : punishmentUser.getLogId(banTypeId, userId);
        String reason = isIp ? punishmentUser.getLog().getReason(logId, banTypeId);
        String expireDate = punishment.getLog().getExpiredDate(logId, banTypeId);
        String startDate = punishment.getLog().getCreatedDate(logId, banTypeId);
        String punisher = user.getUsername(punishment.getLog().getCreatedBy(logId, banTypeId));
        player.disconnect(MiniMessage.miniMessage().deserialize(String.format("""
                <#990000>You are banned from this server.
                <#999999>Reason: <#009999>%s
                <#999999>Punisher: <#009999>%s
                <#999999>Start: <#009999>%s
                <#999999>Expires: <#009999>%s
                <#999999>BanID: <#009999>%s
                """, reason, punisher, startDate, expireDate, logId.toString())));
    }*/

    @Subscribe
    public void handleDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        activeSession.closeSession(userId);
    }
}
