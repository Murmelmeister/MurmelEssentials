package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;

import java.net.InetAddress;

public final class ConnectionListener {
    private final User user;
    private final Group group;
    private final PlayTime playTime;
    private final ActiveSession activeSession;
    private final Permission permission;
    private final PunishmentReason punishmentReason;
    private final PunishmentIP punishmentIp;
    private final PunishmentUser punishmentUser;

    public ConnectionListener(User user, Group group, PlayTime playTime, ActiveSession activeSession, Permission permission, PunishmentReason punishmentReason, PunishmentIP punishmentIp, PunishmentUser punishmentUser) {
        this.user = user;
        this.group = group;
        this.playTime = playTime;
        this.activeSession = activeSession;
        this.permission = permission;
        this.punishmentReason = punishmentReason;
        this.punishmentIp = punishmentIp;
        this.punishmentUser = punishmentUser;
    }

    @Subscribe
    public void handlePostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        int userId = processUserJoin(player);
        processSessionStart(player, userId);
        checkPunishments(player, userId);
    }

    private int processUserJoin(Player player) {
        user.joinUser(player.getUniqueId(), player.getUsername()); // create user if not exists or rename if username changed
        int userId = user.getId(player.getUniqueId());
        int defaultGroupId = group.getUniqueId("default"); // Default → GroupName: default
        if (!user.getParent().existsParent(userId, defaultGroupId))
            user.getParent().addParent(-1, userId, defaultGroupId, -1);
        user.setFirstJoinTime(userId);
        if (!playTime.existsUser(userId))
            playTime.createUser(userId);
        return userId;
    }

    private void processSessionStart(Player player, int userId) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        if (activeSession.existsSession(userId)) activeSession.closeSession(userId); // Or maybe kick the player and send a message with reconnect
        activeSession.startSession(userId, inetAddress, player.getClientBrand(), player.getProtocolVersion() + "");
    }

    private void checkPunishments(Player player, int userId) {
        if (permission.hasPermission(userId, MurmelEssentials.PERMISSION_PUNISH_IMMUNE)) return;
        InetAddress inetAddress = player.getRemoteAddress().getAddress();

        // maybe a type list of punishment types
        int banId = PunishmentType.BAN.getId();
        int ipBanId = PunishmentType.IP_BAN.getId();

        if (punishmentUser.exists(userId, banId)) {
            if (punishmentUser.isPunished(userId, banId)) {
                PunishmentUtil.disconnectPunishMessage(player, userId, banId, false, false);
                return;
            } else {
                punishmentUser.unpunished(userId, banId);
            }
        }

        checkIpPunishment(player, userId, inetAddress, banId);
        checkIpPunishment(player, userId, inetAddress, ipBanId);
    }

    private void checkIpPunishment(Player player, int userId, InetAddress inetAddress, int punishId) {
        if (punishmentIp.isPunished(inetAddress, punishId)) {
            int reasonId = punishmentIp.getReasonId(inetAddress, punishId);
            boolean autoPunish = punishmentReason.getAutoPunish(reasonId, punishId);
            if (autoPunish && !punishmentUser.exists(userId, punishId))
                punishmentUser.punish(userId, punishId, -1, inetAddress, reasonId);
            PunishmentUtil.disconnectPunishMessage(player, userId, punishId, true, autoPunish);
        } else {
            punishmentIp.unpunished(inetAddress, punishId);
        }
    }

    @Subscribe
    public void handleDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        activeSession.closeSession(userId);
    }
}
