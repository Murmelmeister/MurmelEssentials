package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.language.Language;
import de.murmelmeister.murmelapi.language.LanguageProvider;
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
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;
import static de.murmelmeister.murmelapi.MurmelAPI.DEFAULT_GROUP_ID;

public final class ConnectionListener {
    private final Logger logger;
    private final UserProvider userProvider;
    private final UserService userService;
    private final LanguageProvider languageProvider;
    private final GroupProvider group;
    private final UserParentProvider userParent;
    private final PunishmentService punishmentService;
    private final PunishmentUtil punishmentUtil;
    private final PunishmentCurrentUserProvider punishedUserProvider;
    private final PunishmentCurrentIpProvider punishedIpProvider;
    private final int typeBanId = PunishmentType.BAN.getId();
    private final int typeIpBanId = PunishmentType.IP_BAN.getId();
    private final Map<UUID, Integer> sessionUserIds = new ConcurrentHashMap<>();

    public ConnectionListener(MurmelEssentials plugin) {
        this.logger = plugin.getLogger();
        this.userProvider = plugin.getUserProvider();
        this.userService = plugin.getUserService();
        this.languageProvider = plugin.getLanguageProvider();
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
        String code = player.getPlayerSettings().getLocale().toLanguageTag();
        Language language = languageProvider.get(code);
        if (language == null)
            language = languageProvider.get(ENGLISH_CODE);
        userProvider.update(user.id(), user.username(), user.firstLogin(),
                user.debugUser(), user.debugEnabled(), language.id());
        return user;
    }

    private void processSessionStart(Player player, int userId) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        // TODO: Add ModInfo and RessourcenPack
        userService.startSession(userId, inetAddress.getHostAddress(), player.getClientBrand(), player.getProtocolVersion().getProtocol());
    }

    @Subscribe
    public void handleLogin(LoginEvent event) {
        User user = userProvider.findByMojangId(event.getPlayer().getUniqueId());
        if (user != null)
            checkPunishment(event, user);
    }

    @Subscribe
    public void handlePostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        User user = processUserJoin(player);
        /*if (!checkPunishment(player, user))
            return;*/
        processSessionStart(player, user.id());
        sessionUserIds.put(player.getUniqueId(), user.id());
    }

    @Subscribe
    public void handleKickedFromServer(KickedFromServerEvent event) {
        // If the player is kicked while connecting (e.g., backend whitelist), ensure we close their session
        if (event.kickedDuringServerConnect())
            closeSession(event.getPlayer());
    }

    @Subscribe
    public void handleDisconnect(DisconnectEvent event) {
        closeSession(event.getPlayer());
    }

    private void closeSession(Player player) {
        UUID mojangId = player.getUniqueId();
        Integer userId = sessionUserIds.remove(mojangId);
        if (userId != null) {
            userService.closeSession(userId);
            return;
        }

        User user = userProvider.findByMojangId(mojangId);
        if (user != null) {
            userService.closeSession(user.id());
            return;
        }

        logger.warn("Skipping closeSession because user is unknown for player {}", player.getUsername());
    }

    private void checkPunishment(LoginEvent event, User user) {
        if (user == null) {
            event.setResult(LoginEvent.ComponentResult.denied(MiniMessage.miniMessage().deserialize("<red>Something went wrong, please try again...")));
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
                punishmentUtil.disconnectPunishMessage(event, languageId, logId);
                return; // Disconnect the player if they are banned
            }
        }

        if (punishmentService.isPunishedUser(userId, typeIpBanId)) {
            PunishmentCurrentUser punishedUser = punishedUserProvider.getPunishedUser(userId, typeIpBanId);
            UUID logId = punishedUser.logId();
            if (punishmentService.isExpiredUser(logId))
                punishmentService.autoUnpunishedUser(userId, typeIpBanId);
            else {
                punishmentUtil.disconnectPunishMessage(event, languageId, logId);
                return; // Disconnect the player if they are IP banned
            }
        }

        checkPunishmentIp(event, languageId);
    }

    private void checkPunishmentIp(LoginEvent event, int languageId) {
        String ipAddress = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();

        if (punishmentService.isPunishedIp(ipAddress, typeBanId)) {
            PunishmentCurrentIp punishedIp = punishedIpProvider.getPunishedIp(ipAddress, typeBanId);
            UUID logId = punishedIp.logId();
            if (punishmentService.isExpiredIp(logId))
                punishmentService.autoUnpunishedIp(ipAddress, typeBanId);
            else {
                punishmentUtil.disconnectPunishMessage(event, languageId, logId);
                return; // Disconnect the player if their IP is banned
            }
        }

        if (punishmentService.isPunishedIp(ipAddress, typeIpBanId)) {
            PunishmentCurrentIp punishedIp = punishedIpProvider.getPunishedIp(ipAddress, typeIpBanId);
            UUID logId = punishedIp.logId();
            if (punishmentService.isExpiredIp(logId))
                punishmentService.autoUnpunishedIp(ipAddress, typeIpBanId);
            else {
                punishmentUtil.disconnectPunishMessage(event, languageId, logId);
            }
        }
    }
}
