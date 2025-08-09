package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
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
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIp;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.library.utils.StringUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;

public final class UnpunishCommand extends CommandManager {
    private final ProxyServer server;
    private final UserProvider userProvider;
    private final UserLoginProvider userLoginProvider;
    private final Permission permission;
    private final PunishmentLogProvider punishmentLogProvider;
    private final PunishmentCurrentUserProvider punishmentUserProvider;
    private final PunishmentCurrentIpProvider punishmentIpProvider;
    private final PunishmentService punishmentService;

    public UnpunishCommand(MurmelEssentials plugin) {
        super(plugin);
        this.server = plugin.getServer();
        this.userProvider = plugin.getUserProvider();
        this.userLoginProvider = plugin.getUserLoginProvider();
        this.permission = plugin.getPermission();
        this.punishmentLogProvider = plugin.getPunishmentLogProvider();
        this.punishmentUserProvider = plugin.getPunishmentUserProvider();
        this.punishmentIpProvider = plugin.getPunishmentIpProvider();
        this.punishmentService = plugin.getPunishmentService();

    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("unpunish")
                .requires(source -> source.hasPermission("murmel.command.unpunish"))
                .executes(context ->
                        runWithTiming(context, (source, executorId) -> {
                            sendMessage(source, syntax());
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(getUnpunishUserCommand())
                .then(getUnpunishIpCommand())
                .build();
        return new BrigadierCommand(node);
    }

    private LiteralArgumentBuilder<CommandSource> getUnpunishUserCommand() {
        return BrigadierCommand.literalArgumentBuilder("user")
                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                        .suggests(getSuggestionTypes())
                        .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                                .suggests(getSuggestionUsernames())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            int executorId = executor.id();
                                            String typeName = StringArgumentType.getString(context, "type");
                                            PunishmentType type = PunishmentType.fromName(typeName);
                                            if (type == null) {
                                                sendMessage(source, "<#990000>Invalid punishment type: %s", typeName);
                                                return CommandResult.of(-2);
                                            }

                                            String username = StringArgumentType.getString(context, "username");
                                            User user = getUser(executor.languageId(), username);

                                            int userId = user.id();
                                            PunishmentCurrentUser currentPunished = punishmentUserProvider.getPunishedUser(userId, type.getId());
                                            if (currentPunished == null) {
                                                sendMessage(source, "<#990000>User %s is not punished with type %s.", username, type.getName());
                                                return CommandResult.of(-4);
                                            }

                                            PunishmentLog log = punishmentLogProvider.getLog(currentPunished.logId());
                                            if (log == null) {
                                                sendMessage(source, "<#990000>Log for user %s not found.", username);
                                                return CommandResult.of(-5);
                                            }

                                            String ipAddress = userLoginProvider.getLastLogin(userId).ipAddress(); // Methode getIpAddress() has a null check
                                            InetAddress inetAddress;
                                            try {
                                                inetAddress = InetAddress.getByName(ipAddress);
                                            } catch (Exception e) {
                                                sendMessage(source, "<#990000>Invalid IP address for user %s: %s", username, ipAddress);
                                                return CommandResult.of(-6);
                                            }

                                            int rowsAffected = 0;
                                            if (log.reasonAutoFlagIp()) {
                                                PunishmentCurrentIp currentPunishedIp = punishmentIpProvider.getPunishedIp(inetAddress.getHostAddress(), type.getId());

                                                if (currentPunishedIp != null) {
                                                    rowsAffected += punishmentService.unpunishedIp(inetAddress.getHostAddress(), currentPunishedIp.typeId(), currentPunishedIp.logId(), executorId);
                                                    sendMessage(source, "<#00cc88>Unpunished ip %s.", inetAddress.getHostAddress());

                                                    server.getAllPlayers().forEach(player -> {
                                                        User target = getUser(executor.languageId(), player.getUniqueId());
                                                        if (target.id() != executorId
                                                            && permission.hasPermission(player.getUniqueId(), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION))
                                                            sendMessage(player, "<#00cc88>IP %s has been unpunished.", inetAddress.getHostAddress()); // TODO: Add unpunished message
                                                    });
                                                }
                                            }

                                            rowsAffected += punishmentService.unpunishedUser(userId, currentPunished.typeId(), currentPunished.logId(), executorId);
                                            sendMessage(source, "<#00cc88>Unpunished user %s.", username);

                                            server.getAllPlayers().forEach(player -> {
                                                User target = getUser(executor.languageId(), player.getUniqueId());
                                                if (target.id() != executorId
                                                    && permission.hasPermission(player.getUniqueId(), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION))
                                                    sendMessage(player, "<#00cc88>User %s has been unpunished.", username); // TODO: Add unpunished message
                                            });
                                            return CommandResult.of(Command.SINGLE_SUCCESS, rowsAffected);
                                        })
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUnpunishIpCommand() {
        return BrigadierCommand.literalArgumentBuilder("ip")
                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                        .suggests(getSuggestionTypes())
                        .then(BrigadierCommand.requiredArgumentBuilder("ipAddress", StringArgumentType.word())
                                .suggests(getSuggestionIps())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            int executorId = executor.id();
                                            String typeName = StringArgumentType.getString(context, "type");
                                            PunishmentType type = PunishmentType.fromName(typeName);
                                            if (type == null) {
                                                sendMessage(source, "<#990000>Invalid punishment type: %s", typeName);
                                                return CommandResult.of(-2);
                                            }

                                            String ipAddress = StringArgumentType.getString(context, "ipAddress");
                                            InetAddress inetAddress;
                                            try {
                                                inetAddress = InetAddress.getByName(ipAddress);
                                            } catch (Exception e) {
                                                sendMessage(source, "<#990000>Invalid IP address: %s", ipAddress);
                                                return CommandResult.of(-3);
                                            }

                                            PunishmentCurrentIp currentPunishedIp = punishmentIpProvider.getPunishedIp(inetAddress.getHostAddress(), type.getId());
                                            if (currentPunishedIp == null) {
                                                sendMessage(source, "<#990000>IP %s is not punished with type %s.", inetAddress.getHostAddress(), type.getName());
                                                return CommandResult.of(-4);
                                            }

                                            PunishmentLog log = punishmentLogProvider.getLog(currentPunishedIp.logId());
                                            if (log == null) {
                                                sendMessage(source, "<#990000>Log for IP %s not found.", inetAddress.getHostAddress());
                                                return CommandResult.of(-5);
                                            }

                                            int rowsAffected = punishmentService.unpunishedIp(inetAddress.getHostAddress(), currentPunishedIp.typeId(), currentPunishedIp.logId(), executorId);
                                            sendMessage(source, "<#00cc88>Unpunished IP %s.", inetAddress.getHostAddress());

                                            server.getAllPlayers().forEach(player -> {
                                                User target = getUser(executor.languageId(), player.getUniqueId());

                                                if (target.id() != executorId
                                                    && permission.hasPermission(player.getUniqueId(), MurmelEssentials.PUNISHMENT_NOTIFY_PERMISSION))
                                                    sendMessage(player, "<#00cc88>IP %s has been unpunished.", inetAddress.getHostAddress()); // TODO: Add unpunished message
                                            });
                                            return CommandResult.of(Command.SINGLE_SUCCESS, rowsAffected);
                                        })
                                )
                        )
                );
    }

    private SuggestionProvider<CommandSource> getSuggestionTypes() {
        return (context, builder) -> {
            for (PunishmentType type : PunishmentType.VALUES) {
                builder.suggest(type.getName().toLowerCase(),
                        VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + type.getName())));
            }
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSource> getSuggestionUsernames() {
        return (context, builder) -> {
            String typeName = StringArgumentType.getString(context, "type");
            PunishmentType punishmentType = PunishmentType.fromName(typeName);
            if (punishmentType == null) {
                sendMessage(context.getSource(), "<#990000>Invalid punishment type: %s", typeName);
                return builder.buildFuture();
            }

            String prefix = builder.getRemaining();
            punishmentUserProvider.getAllPunishedUserIds(punishmentType.getId()).stream()
                    .map(userProvider::findById)
                    .filter(user -> user != null && user.username() != null)
                    .map(User::username)
                    .filter(username -> StringUtil.startsWithIgnoreCase(username, prefix))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSource> getSuggestionIps() {
        return (context, builder) -> {
            String typeName = StringArgumentType.getString(context, "type");
            PunishmentType punishmentType = PunishmentType.fromName(typeName);
            if (punishmentType == null) {
                sendMessage(context.getSource(), "<#990000>Invalid punishment type: %s", typeName);
                return builder.buildFuture();
            }

            String prefix = builder.getRemaining();
            punishmentIpProvider.getAllPunishedIps(punishmentType.getId()).stream()
                    .filter(ip -> StringUtil.startsWithIgnoreCase(ip, prefix))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private String syntax() {
        return """
                <#009999>Syntax:
                <#454545>- <#999999>/unpunish user <type> <username> <reset>- Unpunished the player.
                <#454545>- <#999999>/unpunish ip <type> <ipAddress> <reset>- Unpunished the ip address.""";
    }
}
