package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIp;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.library.utils.StringUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class PunishCommand extends CommandManager {
    private final ProxyServer server;
    private final UserProvider userProvider;
    private final UserSessionProvider userSessionProvider;
    private final UserLoginProvider userLoginProvider;
    private final Permission permission;
    private final PunishmentReasonProvider reasonProvider;
    private final PunishmentLogProvider punishmentLogProvider;
    private final PunishmentCurrentUserProvider punishmentUserProvider;
    private final PunishmentCurrentIpProvider punishmentIpProvider;
    private final PunishmentService punishmentService;
    private final PunishmentUtil punishmentUtil;

    public PunishCommand(MurmelEssentials plugin) {
        super(plugin);
        this.server = plugin.getServer();
        this.userProvider = plugin.getUserProvider();
        this.userSessionProvider = plugin.getUserSessionProvider();
        this.userLoginProvider = plugin.getUserLoginProvider();
        this.permission = plugin.getPermission();
        this.reasonProvider = plugin.getPunishmentReasonProvider();
        this.punishmentLogProvider = plugin.getPunishmentLogProvider();
        this.punishmentUserProvider = plugin.getPunishmentUserProvider();
        this.punishmentIpProvider = plugin.getPunishmentIpProvider();
        this.punishmentService = plugin.getPunishmentService();
        this.punishmentUtil = plugin.getPunishmentUtil();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("punish")
                .requires(source -> source.hasPermission("murmel.command.punish"))
                .executes(context ->
                        runWithTiming(context, (source, executorId) -> {
                            sendMessage(source, syntax());
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(getPunishUserCommand())
                .then(getPunishIpCommand())
                .build();
        return new BrigadierCommand(node);
    }

    private LiteralArgumentBuilder<CommandSource> getPunishUserCommand() {
        return BrigadierCommand.literalArgumentBuilder("user")
                .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                        .suggests(getSuggestionUsernames())
                        .then(BrigadierCommand.requiredArgumentBuilder("reasonId", IntegerArgumentType.integer(1))
                                .suggests(getSuggestionReasons())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            int executorId = executor.id();
                                            String username = StringArgumentType.getString(context, "username");
                                            User user = getUser(executor.languageId(), username);

                                            int userId = user.id();
                                            int languageId = user.languageId();
                                            int reasonId = IntegerArgumentType.getInteger(context, "reasonId");
                                            PunishmentReason reason = reasonProvider.getReason(reasonId);
                                            if (reason == null) {
                                                sendMessage(source, "<#990000>Reason with ID %d does not exist.", reasonId);
                                                return CommandResult.of(-3);
                                            }

                                            if (!permission.hasPermission(executor, MurmelEssentials.PUNISHMENT_REASON_PERMISSION + reasonId)) {
                                                sendMessage(source, "<#990000>You do not have permission to use this reason.");
                                                return CommandResult.of(-4);
                                            }

                                            if (permission.hasPermission(user, MurmelEssentials.PUNISHMENT_IMMUNITY_PERMISSION)) {
                                                sendMessage(source, "<#990000>User %s is immune to punishment.", username);
                                                return CommandResult.of(-5);
                                            }

                                            String ipAddress = userSessionProvider.isOnline(userId) ? userSessionProvider.findByUserId(userId).ipAddress()
                                                    : userLoginProvider.getLastLogin(userId).ipAddress(); // Methode getIpAddress() has a null check
                                            InetAddress inetAddress;
                                            try {
                                                inetAddress = InetAddress.getByName(ipAddress);
                                            } catch (Exception e) {
                                                sendMessage(source, "<#990000>Invalid IP address for user %s: %s", username, ipAddress);
                                                return CommandResult.of(-6);
                                            }

                                            PunishmentCurrentUser punishedUser = punishmentUserProvider.getPunishedUser(userId, reason.typeId());
                                            if (punishedUser != null) {
                                                PunishmentLog punishedLog = punishmentLogProvider.getLog(punishedUser.logId());

                                                if (punishedLog == null) {
                                                    sendMessage(source, "<#990000>Failed to retrieve punishment log for user %s.", username);
                                                    return CommandResult.of(-7);
                                                }

                                                UUID logId = punishedLog.id();
                                                Integer logReasonId = punishedLog.reasonId();
                                                int logTypeId = punishedLog.reasonTypeId();
                                                if (punishmentService.isExpiredUser(logId)) {
                                                    punishmentService.autoUnpunishedUser(userId, logTypeId);
                                                    punishedUser = null; // Reset punishedUser to allow re-punishment
                                                }

                                                if (punishedLog.reasonAutoFlagIp()) {
                                                    PunishmentCurrentIp punishedIp = punishmentIpProvider.getPunishedIp(inetAddress.getHostAddress(), logTypeId);
                                                    if (punishedIp != null && punishmentService.isExpiredIp(logId))
                                                        punishmentService.autoUnpunishedIp(inetAddress.getHostAddress(), logTypeId);
                                                }

                                                if (punishedUser != null && logReasonId != null && reasonId == logReasonId) {
                                                    sendMessage(source, "<#990000>User %s is already punished with reason %s.", username, punishedLog.reasonText());
                                                    return CommandResult.of(-8);
                                                }
                                            }

                                            PunishmentCurrentIp punishedIp = punishmentIpProvider.getPunishedIp(inetAddress.getHostAddress(), reason.typeId());
                                            AtomicInteger rowsAffected = new AtomicInteger(punishedUser == null ? punishmentService.punishedUser(userId, reasonId, executorId)
                                                    : punishmentService.updatedPunishedUser(userId, reasonId, executorId));
                                            if (rowsAffected.get() <= 0) {
                                                sendMessage(source, "<#990000>Failed to punish user %s.", username);
                                                return CommandResult.of(-9);
                                            }

                                            if (reason.autoFlagIp())
                                                rowsAffected.addAndGet(punishedIp == null ? punishmentService.punishedIp(inetAddress.getHostAddress(), reasonId, executorId)
                                                        : punishmentService.updatedPunishedIp(inetAddress.getHostAddress(), reasonId, executorId));

                                            PunishmentCurrentUser finalPunishedUser = punishedUser != null ? punishedUser : punishmentUserProvider.getPunishedUser(userId, reason.typeId());
                                            server.getPlayer(username).ifPresent(player -> punishmentUtil.disconnectPunishMessage(player, languageId, finalPunishedUser.logId()));
                                            sendMessage(source, "<#00cc88>Successfully punished user <#999999>%s</#999999> with reason <#00cc88>%s</#00cc88>."
                                                    .formatted(username, reason.reasonText()));

                                            server.getAllPlayers().forEach(player -> {
                                                User target = getUser(executor.languageId(), player.getUniqueId());
                                                int targetId = target.id();
                                                int targetLanguageId = target.languageId();

                                                if (reason.autoFlagIp() && player.getRemoteAddress().getAddress().equals(inetAddress)) {
                                                    if (reason.autoPunish()) {
                                                        if (permission.hasPermission(target, MurmelEssentials.PUNISHMENT_IMMUNITY_PERMISSION)) {
                                                            sendMessage(source, "<#990000>Auto-punishment failed: User %s is immune to punishment.", player.getUsername());
                                                            return;
                                                        }

                                                        rowsAffected.addAndGet(punishmentService.punishedUser(targetId, reasonId, executorId));
                                                        sendMessage(source, "<#00cc88>Successfully auto-punished user <#999999>%s</#999999> with reason <#00cc88>%s</#00cc88>."
                                                                .formatted(player.getUsername(), reason.reasonText()));
                                                    }
                                                    punishmentUtil.disconnectPunishMessage(player, targetLanguageId, finalPunishedUser.logId());
                                                }

                                                if (targetId != executorId
                                                    && permission.hasPermission(player.getUniqueId(), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION))
                                                    sendMessage(player, "<#00cc88>User <#999999>%s</#999999> has been punished with reason <#00cc88>%s</#00cc88>."
                                                            .formatted(username, reason.reasonText())); // TODO: Add punishment message notification
                                            });
                                            return CommandResult.of(Command.SINGLE_SUCCESS, rowsAffected.get());
                                        })
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getPunishIpCommand() {
        return BrigadierCommand.literalArgumentBuilder("ip")
                .then(BrigadierCommand.requiredArgumentBuilder("ipAddress", StringArgumentType.word())
                        .then(BrigadierCommand.requiredArgumentBuilder("reasonId", IntegerArgumentType.integer(1))
                                .suggests(getSuggestionReasons())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            int executorId = executor.id();
                                            String ipAddress = StringArgumentType.getString(context, "ipAddress");
                                            InetAddress inetAddress;
                                            try {
                                                inetAddress = InetAddress.getByName(ipAddress);
                                            } catch (Exception e) {
                                                sendMessage(source, "<#990000>Invalid IP address: %s", ipAddress);
                                                return CommandResult.of(-2);
                                            }

                                            int reasonId = IntegerArgumentType.getInteger(context, "reasonId");
                                            PunishmentReason reason = reasonProvider.getReason(reasonId);
                                            if (reason == null) {
                                                sendMessage(source, "<#990000>Reason with ID %d does not exist.", reasonId);
                                                return CommandResult.of(-3);
                                            }

                                            if (!permission.hasPermission(executor, MurmelEssentials.PUNISHMENT_REASON_PERMISSION + reasonId)) {
                                                sendMessage(source, "<#990000>You do not have permission to use this reason.");
                                                return CommandResult.of(-4);
                                            }

                                            PunishmentCurrentIp punishedIp = punishmentIpProvider.getPunishedIp(inetAddress.getHostAddress(), reason.typeId());
                                            if (punishedIp != null) {
                                                PunishmentLog punishedLog = punishmentLogProvider.getLog(punishedIp.logId());

                                                if (punishedLog == null) {
                                                    sendMessage(source, "<#990000>Failed to retrieve punishment log for IP %s.", ipAddress);
                                                    return CommandResult.of(-5);
                                                }

                                                UUID logId = punishedLog.id();
                                                Integer logReasonId = punishedLog.reasonId();
                                                int logTypeId = punishedLog.reasonTypeId();
                                                if (punishmentService.isExpiredIp(logId)) {
                                                    punishmentService.autoUnpunishedIp(inetAddress.getHostAddress(), logTypeId);
                                                    punishedIp = null; // Reset punishedIp to allow re-punishment
                                                }

                                                if (punishedIp != null && logReasonId != null && reasonId == logReasonId) {
                                                    sendMessage(source, "<#990000>IP %s is already punished with reason %s.", ipAddress, punishedLog.reasonText());
                                                    return CommandResult.of(-6);
                                                }
                                            }

                                            AtomicInteger rowsAffected = new AtomicInteger(punishedIp == null
                                                    ? punishmentService.punishedIp(inetAddress.getHostAddress(), reasonId, executorId)
                                                    : punishmentService.updatedPunishedIp(inetAddress.getHostAddress(), reasonId, executorId));
                                            if (rowsAffected.get() <= 0) {
                                                sendMessage(source, "<#990000>Failed to punish IP %s.", ipAddress);
                                                return CommandResult.of(-7);
                                            }

                                            sendMessage(source, "<#00cc88>Successfully punished IP <#999999>%s</#999999> with reason <#00cc88>%s</#00cc88>."
                                                    .formatted(ipAddress, reason.reasonText()));

                                            PunishmentCurrentIp finalPunishedIp = punishedIp != null ? punishedIp
                                                    : punishmentIpProvider.getPunishedIp(inetAddress.getHostAddress(), reason.typeId());
                                            server.getAllPlayers().forEach(player -> {
                                                User target = getUser(executor.languageId(), player.getUniqueId());
                                                int targetId = target.id();
                                                int targetLanguageId = target.languageId();

                                                if (targetId != executorId
                                                    && permission.hasPermission(player.getUniqueId(), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION)) {
                                                    sendMessage(player, "<#00cc88>IP <#999999>%s</#999999> has been punished with reason <#00cc88>%s</#00cc88>."
                                                            .formatted(ipAddress, reason.reasonText())); // TODO: Add punishment message notification
                                                }

                                                if (player.getRemoteAddress().getAddress().equals(inetAddress)) {
                                                    if (reason.autoPunish()) {
                                                        if (permission.hasPermission(target, MurmelEssentials.PUNISHMENT_IMMUNITY_PERMISSION)) {
                                                            sendMessage(source, "<#990000>Auto-punishment failed: User %s is immune to punishment.", player.getUsername());
                                                            return;
                                                        }

                                                        rowsAffected.addAndGet(punishmentService.punishedUser(targetId, reasonId, executorId));
                                                        sendMessage(source, "<#00cc88>Successfully auto-punished user <#999999>%s</#999999> with reason <#00cc88>%s</#00cc88>."
                                                                .formatted(player.getUsername(), reason.reasonText()));
                                                    }
                                                    punishmentUtil.disconnectPunishMessage(player, targetLanguageId, finalPunishedIp.logId());
                                                }

                                                if (targetId != executorId
                                                    && permission.hasPermission(player.getUniqueId(), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION))
                                                    sendMessage(player, "<#00cc88>User <#999999>%s</#999999> has been punished with reason <#00cc88>%s</#00cc88>."
                                                            .formatted(player.getUsername(), reason.reasonText())); // TODO: Add punishment message notification
                                            });
                                            return CommandResult.of(Command.SINGLE_SUCCESS, rowsAffected.get());
                                        })
                                )
                        )
                );
    }

    private SuggestionProvider<CommandSource> getSuggestionReasons() {
        return (context, builder) -> {
            reasonProvider.getAllReasons().forEach(reason ->
                    builder.suggest(String.valueOf(reason.id()),
                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + reason.reasonText())))
            );
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSource> getSuggestionUsernames() {
        return (context, builder) -> {
            String prefix = builder.getRemaining();
            userProvider.findUsernames().stream()
                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private String syntax() {
        return """
                <#009999>Syntax:
                <#454545>- <#999999>/punish user <username> <reasonId> <reset>- Punish a user or change their punishment.
                <#454545>- <#999999>/punish ip <ipAddress> <reasonId> <reset>- Punish a ip address or change its punishment.""";
    }
}
