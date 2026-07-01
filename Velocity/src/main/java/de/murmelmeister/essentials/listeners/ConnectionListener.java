package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.configs.ConfigProvider;
import de.murmelmeister.essentials.configs.settings.Maintenance;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.language.LanguageType;
import de.murmelmeister.murmelapi.language.LanguageTypeProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.permission.parent.Parent;
import de.murmelmeister.murmelapi.permission.parent.ParentProvider;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.settings.SettingsService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.stats.UserStatsProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;
import static de.murmelmeister.murmelapi.MurmelAPI.DEFAULT_GROUP_ID;

public final class ConnectionListener {
    private final Logger logger;
    private final UserProvider userProvider;
    private final UserService userService;
    private final LanguageTypeProvider languageProvider;
    private final GroupProvider group;
    private final ParentProvider parentProvider;
    private final PunishmentService punishmentService;
    private final PunishmentUtil punishmentUtil;

    private final SettingsService settingsService;
    private final MessageService messageService;
    private final UserStatsProvider userStatsProvider;

    private final int typeBanId = PunishmentType.BAN.getId();
    private final int typeIpBanId = PunishmentType.IP_BAN.getId();
    private final Map<UUID, Integer> sessionUserIds = new ConcurrentHashMap<>();

    public ConnectionListener(@NotNull MurmelEssentials plugin) {
        this.logger = plugin.getLogger();
        this.userProvider = plugin.getUserProvider();
        this.userService = plugin.getUserService();
        this.languageProvider = plugin.getLanguageProvider();
        this.group = plugin.getGroupProvider();
        this.parentProvider = plugin.getParentProvider();
        this.punishmentService = plugin.getPunishmentService();
        this.punishmentUtil = plugin.getPunishmentUtil();
        this.settingsService = plugin.getSettingsService();
        this.messageService = plugin.getMessageService();
        this.userStatsProvider = plugin.getUserStatsProvider();
    }

    private @NotNull User processUserJoin(@NotNull Player player) {
        User user = userService.join(player.getUniqueId(), player.getUsername());
        int defaultGroupId = group.findById(DEFAULT_GROUP_ID).orElseThrow().id(); // Default group ID is 1

        Optional<Parent> parent = parentProvider.findParent(PermissionTarget.user(user.id()), defaultGroupId);
        if (parent.isEmpty())
            parentProvider.upsert(PermissionTarget.user(user.id()), defaultGroupId, -1, -1);

        String code = player.getPlayerSettings().getLocale().toLanguageTag();
        LanguageType language = languageProvider.findByCode(code).orElse(
                languageProvider.findByCode(ENGLISH_CODE).orElseThrow(() -> new IllegalStateException("Default language not found"))
        );

        userProvider.update(user.id(), user.username(), user.firstLogin(),
                user.debugUser(), user.debugEnabled(), language.id());
        return user;
    }

    private void processSessionStart(@NotNull Player player, int userId) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        // TODO: Add ModInfo and ResourcenPack
        userService.startSession(userId, inetAddress, player.getClientBrand(), player.getProtocolVersion().getProtocol()); // It checks when a session is started
        userStatsProvider.refreshSingle(userId);
    }

    @Subscribe
    public void handleLogin(@NotNull LoginEvent event) {
        Player player = event.getPlayer();
        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null)
            user = userService.join(player.getUniqueId(), player.getUsername());

        Maintenance maintenance = ConfigProvider.loadMaintenance(settingsService);
        if (maintenance.mode())
            if (!maintenance.whitelist().contains(user.id()))
                event.setResult(ResultedEvent.ComponentResult.denied(
                        MiniMessage.miniMessage().deserialize(messageService.getMessage(Message.MAINTENANCE_KICK_MESSAGE.getTag(), user.languageId()))
                ));

        checkPunishment(event, user);
    }

    @Subscribe
    public void handlePostLogin(@NotNull PostLoginEvent event) {
        Player player = event.getPlayer();
        User user = processUserJoin(player);
        /*if (!checkPunishment(player, user))
            return;*/
        processSessionStart(player, user.id());
        sessionUserIds.put(player.getUniqueId(), user.id());
    }

    @Subscribe
    public void handleKickedFromServer(@NotNull KickedFromServerEvent event) {
        // If the player is kicked while connecting (e.g., backend whitelist), ensure we close their session
        if (event.kickedDuringServerConnect())
            closeSession(event.getPlayer());
    }

    @Subscribe
    public void handleDisconnect(@NotNull DisconnectEvent event) {
        closeSession(event.getPlayer());
    }

    @Subscribe
    public void handlePing(@NotNull ProxyPingEvent event) {
        Maintenance maintenance = ConfigProvider.loadMaintenance(settingsService);
        if (!maintenance.mode())
            return;

        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder()
                .description(MiniMessage.miniMessage().deserialize(maintenance.motd()))
                .version(
                        new ServerPing.Version(
                                maintenance.protocolVersion(),
                                maintenance.protocolName()
                        )
                );

        event.setPing(builder.build());
    }

    private void closeSession(@NotNull Player player) {
        UUID mojangId = player.getUniqueId();
        Integer userId = sessionUserIds.remove(mojangId);
        if (userId != null) {
            userStatsProvider.refreshSingle(userId);
            userService.closeSession(userId);
            return;
        }

        User user = userProvider.findByMojangId(mojangId).orElse(null);
        if (user != null) {
            userStatsProvider.refreshSingle(user.id());
            userService.closeSession(user.id());
            return;
        }

        logger.warn("Skipping closeSession because user is unknown for player {}", player.getUsername());
    }

    private void checkPunishment(@NotNull LoginEvent event, @NotNull User user) {
        UUID mojangId = user.mojangId();
        int userId = user.id();
        int languageId = user.languageId();

        if (punishmentService.checkUserPunishment(mojangId, typeBanId, audit -> {
            punishmentUtil.disconnectPunishMessage(event, userId, languageId, audit);
        })) return;

        if (punishmentService.checkUserPunishment(mojangId, typeIpBanId, audit -> {
            punishmentUtil.disconnectPunishMessage(event, userId, languageId, audit);
        })) return;

        // User is not banned

        checkPunishmentIp(event, userId, languageId);
    }

    private void checkPunishmentIp(@NotNull LoginEvent event, int userId, int languageId) {
        InetAddress ipAddress = event.getPlayer().getRemoteAddress().getAddress();

        if (punishmentService.checkIpPunishment(ipAddress, typeBanId, audit -> {
            punishmentUtil.disconnectPunishMessage(event, userId, languageId, audit);
        })) return;

        if (punishmentService.checkIpPunishment(ipAddress, typeIpBanId, audit -> {
            punishmentUtil.disconnectPunishMessage(event, userId, languageId, audit);
        })) return;

        // IP is not banned
    }
}
