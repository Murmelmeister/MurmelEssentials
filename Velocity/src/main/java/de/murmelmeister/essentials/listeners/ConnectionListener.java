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
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;
import java.util.UUID;

public final class ConnectionListener {
    private final User user;
    private final Group group;
    private final PlayTime playTime;
    private final ActiveSession activeSession;
    private final PunishmentReason punishmentReason;
    private final PunishmentIP punishmentIp;
    private final PunishmentUser punishmentUser;

    public ConnectionListener(User user, Group group, PlayTime playTime, ActiveSession activeSession, PunishmentReason punishmentReason, PunishmentIP punishmentIp, PunishmentUser punishmentUser) {
        this.user = user;
        this.group = group;
        this.playTime = playTime;
        this.activeSession = activeSession;
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
        activeSession.startSession(userId, inetAddress, player.getClientBrand(), player.getProtocolVersion() + "");
    }

    private void checkPunishments(Player player, int userId) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();

        // maybe a type list of punishment types
        int banId = PunishmentType.BAN.getId();
        int ipBanId = PunishmentType.IP_BAN.getId();

        if (punishmentUser.exists(userId, banId)) {
            if (punishmentUser.isPunished(userId, banId)) {
                punishedMessage(player, userId, banId, false, false);
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
            punishedMessage(player, userId, punishId, true, autoPunish);
        } else {
            punishmentIp.unpunished(inetAddress, punishId);
        }
    }

    private void punishedMessage(Player player, int userId, int punishId, boolean isIp, boolean autoPunish) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        UUID logId;
        String reasonText;
        String expireDate;
        String startDate;
        String punisher;

        if (isIp) {
            logId = punishmentIp.getLogId(inetAddress, punishId);
            reasonText = punishmentIp.getReason(inetAddress, punishId);
            expireDate = punishmentIp.getExpiredDate(inetAddress, punishId);
            startDate = punishmentIp.getCreatedAt(inetAddress, punishId).toString();
            punisher = user.getUsername(punishmentIp.getCreatedBy(inetAddress, punishId));
        } else {
            logId = punishmentUser.getLogId(userId, punishId);
            reasonText = punishmentUser.getReason(userId, punishId);
            expireDate = punishmentUser.getExpiredDate(userId, punishId);
            startDate = punishmentUser.getCreatedAt(userId, punishId).toString();
            punisher = user.getUsername(punishmentUser.getCreatedBy(userId, punishId));
        }

        boolean status = !autoPunish;
        String message = String.format("""
                <#990000>You are banned from the network.
                <#999999>Reason: <#009999>%s
                <#999999>Punisher: <#009999>%s
                <#999999>Start: <#009999>%s
                <#999999>Expires: <#009999>%s
                <#999999>PunishID: <#009999>%s
                <#999999>Mod-Punish: <#009999>%s
                """, reasonText, punisher, startDate, expireDate, logId.toString(), status);
        player.disconnect(MiniMessage.miniMessage().deserialize(message));
    }

    @Subscribe
    public void handleDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        activeSession.closeSession(userId);
    }
}
