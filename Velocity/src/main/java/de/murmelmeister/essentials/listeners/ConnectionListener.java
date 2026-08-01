package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.configs.ConfigProvider;
import de.murmelmeister.essentials.configs.settings.Maintenance;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.murmelapi.language.LanguageType;
import de.murmelmeister.murmelapi.language.LanguageTypeProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.permission.parent.Parent;
import de.murmelmeister.murmelapi.permission.parent.ParentProvider;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static de.murmelmeister.murmelapi.MurmelAPI.DEFAULT_GROUP_ID;
import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;

public final class ConnectionListener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final int TYPE_BAN_ID = PunishmentType.BAN.getId();
    private static final int TYPE_IP_BAN_ID = PunishmentType.IP_BAN.getId();

    private final Logger logger;
    private final Executor databaseExecutor;
    private final UserProvider userProvider;
    private final UserService userService;
    private final LanguageTypeProvider languageProvider;
    private final ParentProvider parentProvider;
    private final PunishmentService punishmentService;
    private final PunishmentUtil punishmentUtil;

    private final SettingsService settingsService;
    private final MessageService messageService;
    private final UserStatsProvider userStatsProvider;

    public ConnectionListener(@NotNull MurmelEssentials plugin) {
        this.logger = plugin.getLogger();
        this.databaseExecutor = plugin.getDatabaseExecutor();
        this.userProvider = plugin.getUserProvider();
        this.userService = plugin.getUserService();
        this.languageProvider = plugin.getLanguageProvider();
        this.parentProvider = plugin.getParentProvider();
        this.punishmentService = plugin.getPunishmentService();
        this.punishmentUtil = plugin.getPunishmentUtil();
        this.settingsService = plugin.getSettingsService();
        this.messageService = plugin.getMessageService();
        this.userStatsProvider = plugin.getUserStatsProvider();
    }

    private @NotNull User processUserJoin(@NotNull Player player) {
        User user = userService.join(player.getUniqueId(), player.getUsername());
        PermissionTarget target = PermissionTarget.user(user.id());

        Optional<Parent> parent = parentProvider.findParent(target, DEFAULT_GROUP_ID);
        if (parent.isEmpty())
            parentProvider.upsert(target, DEFAULT_GROUP_ID, -1, -1);

        String code = player.getPlayerSettings().getLocale().toLanguageTag();
        LanguageType language = languageProvider.findByCode(code).orElse(
                languageProvider.findByCode(ENGLISH_CODE).orElseThrow(() -> new IllegalStateException("Default language not found"))
        );

        return userProvider.update(user.id(), user.username(), user.firstLogin(),
                user.debugUser(), user.debugEnabled(), language.id()).orElse(user);
    }

    private void processSessionStart(@NotNull Player player, int userId) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        // TODO: Add ModInfo and ResourcenPack
        userService.startSession(userId, inetAddress, player.getClientBrand(), player.getProtocolVersion().getProtocol()); // It checks when a session is started
        userStatsProvider.refreshSingle(userId);
    }

    @Subscribe
    public void handleLogin(@NotNull LoginEvent event, @NotNull Continuation continuation) {
        Player player = event.getPlayer();
        runDatabaseTask(() -> {
            try {
                User user = userProvider.findByMojangId(player.getUniqueId())
                        .orElseGet(() -> userService.join(player.getUniqueId(), player.getUsername()));

                Maintenance maintenance = ConfigProvider.loadMaintenance(settingsService);
                if (maintenance.mode() && !maintenance.whitelist().contains(user.id())) {
                    event.setResult(ResultedEvent.ComponentResult.denied(
                            MINI_MESSAGE.deserialize(
                                    messageService.getMessage(
                                            Message.MAINTENANCE_KICK_MESSAGE.getTag(),
                                            user.languageId()
                                    )
                            )
                    ));
                    return;
                }

                checkPunishment(event, user);
            } catch (Exception e) {
                logger.error("Error during login event processing for player {}", player.getUsername(), e);
                event.setResult(ResultedEvent.ComponentResult.denied(
                        MINI_MESSAGE.deserialize("<red>Internal server error occurred. Please try again later.")
                ));
            }
        }).whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                logger.error("Could not schedule login processing for player {}", player.getUsername(), throwable);
                event.setResult(ResultedEvent.ComponentResult.denied(
                        MINI_MESSAGE.deserialize("<red>Internal server error occurred. Please try again later.")
                ));
            }
            continuation.resume();
        });
    }

    @Subscribe
    public void handlePostLogin(@NotNull PostLoginEvent event, @NotNull Continuation continuation) {
        Player player = event.getPlayer();
        runDatabaseTask(() -> {
            User user = processUserJoin(player);
            processSessionStart(player, user.id());
        }).whenComplete((ignored, throwable) -> {
            if (throwable != null)
                logger.error("Failed to start session for player {}", player.getUsername(), throwable);
            continuation.resume();
        });
    }

    @Subscribe
    public void handleDisconnect(@NotNull DisconnectEvent event) {
        Player player = event.getPlayer();
        runDatabaseTask(() -> closeSession(player)).exceptionally(throwable -> {
            logger.error("Failed to close session for player {}", player.getUsername(), throwable);
            return null;
        });
    }

    @Subscribe
    public void handlePing(@NotNull ProxyPingEvent event) {
        Maintenance maintenance = ConfigProvider.loadMaintenance(settingsService);
        if (!maintenance.mode())
            return;

        ServerPing ping = event.getPing();
        ServerPing.Builder builder = ping.asBuilder()
                .description(MINI_MESSAGE.deserialize(maintenance.motd()))
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
        User user = userProvider.findByMojangId(mojangId).orElse(null);
        if (user != null) {
            userService.closeSession(user.id());
            userStatsProvider.refreshSingle(user.id());
            return;
        }

        logger.warn("Skipping closeSession because user is unknown for player {}", player.getUsername());
    }

    private void disconnectPunishment(@NotNull LoginEvent event, int userId, int languageId, @NotNull PunishmentAudit audit) {
        punishmentUtil.disconnectPunishMessage(event, userId, languageId, audit);
    }

    private void checkPunishment(@NotNull LoginEvent event, @NotNull User user) {
        UUID mojangId = user.mojangId();
        int userId = user.id();
        int languageId = user.languageId();

        if (punishmentService.checkUserPunishment(mojangId, TYPE_BAN_ID, audit ->
                disconnectPunishment(event, userId, languageId, audit)
        )) return;

        if (punishmentService.checkUserPunishment(mojangId, TYPE_IP_BAN_ID, audit ->
                disconnectPunishment(event, userId, languageId, audit)
        )) return;

        checkPunishmentIp(event, userId, languageId);
    }

    private void checkPunishmentIp(@NotNull LoginEvent event, int userId, int languageId) {
        InetAddress ipAddress = event.getPlayer().getRemoteAddress().getAddress();

        if (punishmentService.checkIpPunishment(ipAddress, TYPE_BAN_ID, audit ->
                disconnectPunishment(event, userId, languageId, audit)
        )) return;

        punishmentService.checkIpPunishment(ipAddress, TYPE_IP_BAN_ID, audit ->
                disconnectPunishment(event, userId, languageId, audit)
        );
    }

    private @NotNull CompletableFuture<Void> runDatabaseTask(@NotNull Runnable task) {
        try {
            return CompletableFuture.runAsync(task, databaseExecutor);
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
