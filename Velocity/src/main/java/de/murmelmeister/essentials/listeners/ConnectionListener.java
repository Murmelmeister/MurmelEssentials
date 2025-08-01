package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIp;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;
import java.util.UUID;

import static de.murmelmeister.murmelapi.group.GroupProviderImpl.DEFAULT_GROUP_ID;

public final class ConnectionListener {
    private final UserProvider userProvider;
    private final UserService userService;
    private final GroupProvider group;
    private final UserParentProvider userParent;
    private final PunishmentService punishmentService;
    private final PunishmentUtil punishmentUtil;
    private final PunishmentCurrentUserProvider punishedUserProvider;
    private final PunishmentCurrentIpProvider punishedIpProvider;
    private final int typeBanId = PunishmentType.BAN.getId();
    private final int typeIpBanId = PunishmentType.IP_BAN.getId();

    public ConnectionListener(MurmelEssentials plugin) {
        this.userProvider = plugin.getUserProvider();
        this.userService = plugin.getUserService();
        this.group = plugin.getGroupProvider();
        this.userParent = plugin.getUserParentProvider();
        this.punishmentService = plugin.getPunishmentService();
        this.punishmentUtil = plugin.getPunishmentUtil();
        this.punishedUserProvider = plugin.getPunishmentUserProvider();
        this.punishedIpProvider = plugin.getPunishmentIpProvider();
    }

    private User processUserJoin(Player player) {
        User user = userService.join(player.getUniqueId(), player.getUsername());
        userService.loginStreak(user.id()); // Update login streak
        int defaultGroupId = group.findById(DEFAULT_GROUP_ID).id(); // Default group ID is 1
        UserParent parent = userParent.getParent(user.id(), defaultGroupId);
        if (parent == null)
            userParent.add(user.id(), defaultGroupId, -1, -1);
        return user;
    }

    private void processSessionStart(Player player, int userId) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        userService.startSession(userId, inetAddress.getHostAddress(), player.getClientBrand(), player.getProtocolVersion().toString());
    }

    @Subscribe
    public void handlePostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        User user = processUserJoin(player);
        processSessionStart(player, user.id());
        checkPunishment(player);
    }

    @Subscribe
    public void handleDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        User user = userProvider.findByMojangId(player.getUniqueId());
        if (user != null)
            userService.closeSession(user.id());
    }

    private void checkPunishment(Player player) {
        User user = userProvider.findByMojangId(player.getUniqueId());
        if (user == null) {
            player.disconnect(MiniMessage.miniMessage().deserialize("<red>Something went wrong, please try again..."));
            return; // Disconnect the player if they are not registered
        }
        int userId = user.id();
        int languageId = user.languageId();

        if (punishmentService.isPunishedUser(userId, typeBanId)) {
            PunishmentCurrentUser punishedUser = punishedUserProvider.getPunishedUser(userId, typeBanId);
            UUID logId = punishedUser.logId();
            if (punishmentService.isExpiredUser(logId))
                punishmentService.autoUnpunishedUser(userId, typeBanId);
            else {
                punishmentUtil.disconnectPunishMessage(player, languageId, logId);
                return; // Disconnect the player if they are banned
            }
        }

        if (punishmentService.isPunishedUser(userId, typeIpBanId)) {
            PunishmentCurrentUser punishedUser = punishedUserProvider.getPunishedUser(userId, typeIpBanId);
            UUID logId = punishedUser.logId();
            if (punishmentService.isExpiredUser(logId))
                punishmentService.autoUnpunishedUser(userId, typeIpBanId);
            else {
                punishmentUtil.disconnectPunishMessage(player, languageId, logId);
                return; // Disconnect the player if they are IP banned
            }
        }

        checkPunishmentIp(player, languageId);
    }

    private void checkPunishmentIp(Player player, int languageId) {
        String ipAddress = player.getRemoteAddress().getAddress().getHostAddress();

        if (punishmentService.isPunishedIp(ipAddress, typeBanId)) {
            PunishmentCurrentIp punishedIp = punishedIpProvider.getPunishedIp(ipAddress, typeBanId);
            UUID logId = punishedIp.logId();
            if (punishmentService.isExpiredIp(logId))
                punishmentService.autoUnpunishedIp(ipAddress, typeBanId);
            else {
                punishmentUtil.disconnectPunishMessage(player, languageId, logId);
                return; // Disconnect the player if their IP is banned
            }
        }

        if (punishmentService.isPunishedIp(ipAddress, typeIpBanId)) {
            PunishmentCurrentIp punishedIp = punishedIpProvider.getPunishedIp(ipAddress, typeIpBanId);
            UUID logId = punishedIp.logId();
            if (punishmentService.isExpiredIp(logId))
                punishmentService.autoUnpunishedIp(ipAddress, typeIpBanId);
            else punishmentUtil.disconnectPunishMessage(player, languageId, logId);
        }
    }
}
