package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAuditProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddress;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddressProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUserProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

@CommandConfig(id = "punish", name = "punish")
public final class PunishCommand extends CommandManager {
    private final ProxyServer server;
    private final UserProvider userProvider;
    private final UserSessionProvider userSessionProvider;
    private final UserLoginProvider userLoginProvider;
    private final PunishmentReasonProvider reasonProvider;
    private final PunishmentAuditProvider punishmentAuditProvider;
    private final PunishmentUserProvider punishmentUserProvider;
    private final PunishmentIpAddressProvider punishmentIpProvider;
    private final PermissionService permissionService;
    private final PunishmentService punishmentService;
    private final PunishmentUtil punishmentUtil;
    private final UserService userService;

    public PunishCommand(MurmelEssentials plugin) {
        super(plugin);
        this.server = plugin.getServer();
        this.userProvider = plugin.getUserProvider();
        this.userSessionProvider = plugin.getUserSessionProvider();
        this.userLoginProvider = plugin.getUserLoginProvider();
        this.reasonProvider = plugin.getPunishmentReasonProvider();
        this.punishmentAuditProvider = plugin.getPunishmentAuditProvider();
        this.punishmentUserProvider = plugin.getPunishmentUserProvider();
        this.punishmentIpProvider = plugin.getPunishmentIpProvider();
        this.permissionService = plugin.getPermissionService();
        this.punishmentService = plugin.getPunishmentService();
        this.punishmentUtil = plugin.getPunishmentUtil();
        this.userService = plugin.getUserService();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "punish"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            sendRawMessage(source, executor.languageId(), syntax());
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.literalArgumentBuilder("user")
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .suggests(getSuggestionUsernames())
                                .then(BrigadierCommand.requiredArgumentBuilder("reasonId", IntegerArgumentType.integer(1))
                                        .suggests(getSuggestionReasons())
                                        .executes(this::executePunishUser)
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("ip")
                        .then(BrigadierCommand.requiredArgumentBuilder("ipAddress", StringArgumentType.word())
                                .then(BrigadierCommand.requiredArgumentBuilder("reasonId", IntegerArgumentType.integer(1))
                                        .suggests(getSuggestionReasons())
                                        .executes(this::executePunishIp)
                                )
                        )
                )
                ;
    }

    private int executePunishUser(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            int executorId = executor.id();
            int languageId = executor.languageId();
            String username = StringArgumentType.getString(context, "username");
            int reasonId = IntegerArgumentType.getInteger(context, "reasonId");

            PunishmentReason reason = getReason(reasonId);
            if (!permissionService.hasPermission(PermissionTarget.user(executorId), MurmelEssentials.PUNISHMENT_REASON_PERMISSION + reason.id()))
                throw new CommandException("You do not have permission to use this reason.");

            User user = userProvider.findByUsername(username).orElse(null);
            if (user == null) {
                // Punish user by mojangId if they are not found in the database
                UUID mojangId = getMojangId(username);
                int result = punishmentService.punishedUser(mojangId, reasonId, executorId);

                if (result < 1)
                    throw new CommandException("Failed to punish user " + username);

                sendRawMessage(source, languageId,
                        "<#00cc88>Successfully punished user <#999999><username></#999999> with reason <#00cc88><reason_text></#00cc88>.",
                        tagUnparsed("username", username),
                        tagParsed("reason_text", reason.reasonText())
                );
                // TODO: Add notify
                return CommandResult.of(Command.SINGLE_SUCCESS, result);
            }

            int userId = user.id();
            UUID mojangId = user.mojangId();

            if (permissionService.hasPermission(PermissionTarget.user(userId), MurmelEssentials.PUNISHMENT_IMMUNITY_PERMISSION))
                throw new CommandException("User is immune to punishment.");

            int resultIp = 0;
            if (reason.autoFlagIp()) {
                // Retrieve the user's last known IP address from their session or login history
                // And punish that IP address as well
                UserSession userSession = userSessionProvider.findByUserId(userId).orElse(null);
                UserLogin userLogin = userService.getLastLogin(userId);
                InetAddress inetAddress = userSession != null ? userSession.inetAddress()
                        : (userLogin != null ? userLogin.inetAddress() : null);
                if (inetAddress == null)
                    throw new CommandException("Failed to retrieve IP address for user " + username);

                resultIp = punishmentService.punishedIp(inetAddress, reasonId, executorId);
            }

            int resultUser = punishmentService.punishedUser(mojangId, reasonId, executorId);
            int result = resultUser + resultIp;
            if (result < 1)
                throw new CommandException("Failed to punish user " + username);

            // Disconnect the player if they are online
            PunishmentUser punishedUser = punishmentUserProvider.findPunishedUser(mojangId, reason.typeId())
                    .orElseThrow(() -> new CommandException("Failed to retrieve Punishment User for user " + username));

            Optional<PunishmentAudit> audit = punishmentAuditProvider.findAudit(punishedUser.auditId());
            server.getPlayer(mojangId).ifPresent(player -> {
                if (audit.isPresent() && isBanType(reason.typeId()))
                    punishmentUtil.disconnectPunishMessage(player, userId, languageId, audit.get());
            });

            // Notify the executor about the successful punishment
            sendRawMessage(source, languageId,
                    "<#00cc88>Successfully punished user <#999999><username></#999999> with reason <#00cc88><reason_text></#00cc88>.",
                    tagUnparsed("username", username),
                    tagParsed("reason_text", reason.reasonText())
            );

            // Notify all players with permission about the punishment
            server.getAllPlayers().forEach(player -> {
                User target = getUser(player.getUniqueId());
                int targetId = target.id();
                int targetLanguageId = target.languageId();

                if (targetId != executorId
                        && permissionService.hasPermission(PermissionTarget.user(targetId), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION))
                    sendRawMessage(player, targetLanguageId,
                            "<#00cc88>User <#999999><username></#999999> has been punished with reason <#00cc88><reason_text></#00cc88>.",
                            tagUnparsed("username", username),
                            tagParsed("reason_text", reason.reasonText())
                    );
            });

            return CommandResult.of(Command.SINGLE_SUCCESS, result);
        });
    }

    private int executePunishIp(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            int executorId = executor.id();
            int languageId = executor.languageId();
            String ipAddress = StringArgumentType.getString(context, "ipAddress");
            InetAddress inetAddress = getInetAddress(ipAddress);

            int reasonId = IntegerArgumentType.getInteger(context, "reasonId");
            PunishmentReason reason = getReason(reasonId);

            if (!permissionService.hasPermission(PermissionTarget.user(executorId), MurmelEssentials.PUNISHMENT_REASON_PERMISSION + reason.id()))
                throw new CommandException("You do not have permission to use this reason.");

            int result = punishmentService.punishedIp(inetAddress, reasonId, executorId);
            if (result < 1)
                throw new CommandException("Failed to punish IP " + ipAddress);

            // Disconnect all players with this IP address if they are online
            PunishmentIpAddress punishedIp = punishmentIpProvider.findPunishedIpAddress(inetAddress, reason.typeId())
                    .orElseThrow(() -> new CommandException("Failed to retrieve Punishment IP for IP " + ipAddress));

            Optional<PunishmentAudit> audit = punishmentAuditProvider.findAudit(punishedIp.auditId());
            userLoginProvider.findByIpAddress(inetAddress).stream()
                    .map(UserLogin::userId)
                    .distinct()
                    .forEach(userId -> {
                        User target = getUser(userId);
                        server.getPlayer(target.mojangId()).ifPresent(player -> {
                            if (audit.isPresent() && isBanType(reason.typeId()))
                                punishmentUtil.disconnectPunishMessage(player, target.id(), target.languageId(), audit.get());
                        });
                    });

            // Notify the executor about the successful punishment
            sendRawMessage(source, languageId, "<#00cc88>Successfully punished IP <#999999><ip_address></#999999> with reason <#00cc88><reason_text></#00cc88>.",
                    Placeholder.unparsed("ip_address", ipAddress), Placeholder.unparsed("reason_text", reason.reasonText()));

            // Notify all players with permission about the punishment
            server.getAllPlayers().forEach(player -> {
                User target = getUser(player.getUniqueId());
                int targetId = target.id();
                int targetLanguageId = target.languageId();

                if (targetId != executorId
                        && permissionService.hasPermission(PermissionTarget.user(targetId), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION))
                    sendRawMessage(player, targetLanguageId, "<#00cc88>IP <#999999><ip_address></#999999> has been punished with reason <#00cc88><reason_text></#00cc88>.",
                            Placeholder.unparsed("ip_address", ipAddress), Placeholder.unparsed("reason_text", reason.reasonText()));
            });
            return CommandResult.of(Command.SINGLE_SUCCESS, result);
        });
    }

    private SuggestionProvider<CommandSource> getSuggestionReasons() {
        return (context, builder) -> {
            reasonProvider.findAll().forEach(reason ->
                    builder.suggest(
                            String.valueOf(reason.id()),
                            tooltip(
                                    "<#00cc88><reason>",
                                    tagParsed("reason", reason.reasonText())
                            )
                    )
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

    private boolean isBanType(int typeId) {
        return typeId == PunishmentType.BAN.getId() || typeId == PunishmentType.IP_BAN.getId();
    }

    private @NotNull PunishmentReason getReason(int reasonId) {
        return reasonProvider.findReason(reasonId).orElseThrow(() -> new CommandException("<#990000>Reason with ID " + reasonId + " does not exist."));
    }
}
