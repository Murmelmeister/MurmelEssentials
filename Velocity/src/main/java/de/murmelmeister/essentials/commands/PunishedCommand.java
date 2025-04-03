package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.utils.PunishmentUtil;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.LoginHistory;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.StringUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public final class PunishedCommand extends CommandManager {
    private final ProxyServer server;
    private final User user;
    private final Permission permission;
    private final ActiveSession session;
    private final LoginHistory login;
    private final PunishmentReason punishmentReason;
    private final PunishmentIP punishmentIP;
    private final PunishmentUser punishmentUser;

    public PunishedCommand(ProxyServer server, User user, Permission permission, ActiveSession session, LoginHistory login, PunishmentReason punishmentReason, PunishmentIP punishmentIP, PunishmentUser punishmentUser) {
        this.server = server;
        this.user = user;
        this.permission = permission;
        this.session = session;
        this.login = login;
        this.punishmentReason = punishmentReason;
        this.punishmentIP = punishmentIP;
        this.punishmentUser = punishmentUser;
    }

    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder("punished")
                .requires(source -> source.hasPermission("murmelessentials.command.punished"))
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(getPunishmentId())
                .build();
        return new BrigadierCommand(rootNode);
    }

    private RequiredArgumentBuilder<CommandSource, String> getPunishmentId() {
        return BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                .suggests(this::getPunishTypes)
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer())
                        .suggests((this::getPunishIds))
                        .then(BrigadierCommand.requiredArgumentBuilder("target", StringArgumentType.word()) // TODO: user and ip argument, check if type a ip type or user type
                                .suggests(this::getUsernames)
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    String input = StringArgumentType.getString(context, "type");
                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                    if (!PunishmentType.exists(type)) {
                                        sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                        return -1;
                                    }

                                    int typeId = type.getId();
                                    int reasonId = IntegerArgumentType.getInteger(context, "id");

                                    if (!punishmentReason.exists(reasonId, typeId)) {
                                        sendMessage(source, "<#990000>A reason with the ID %s does not exist.", reasonId);
                                        return -2;
                                    }

                                    int executorId = getExecutorId(context, user);

                                    if (!permission.hasPermission(executorId, MurmelEssentials.PERMISSION_PUNISHMENT_REASON + type.getName() + "." + reasonId)) {
                                        sendMessage(source, "<#990000>You do not have permission to use this reason.");
                                        return -3;
                                    }

                                    String target = StringArgumentType.getString(context, "target");
                                    if (type.isTypeIp()) {
                                        // IP-Punishment
                                        InetAddress inetAddress;
                                        try {
                                            inetAddress = InetAddress.getByName(target);
                                        } catch (UnknownHostException e) {
                                            sendMessage(source, "<#cc0088>Invalid ip address. Input: <#009999>%s", target);
                                            return -5;
                                        }

                                        if (punishmentIP.exists(inetAddress, typeId)) {
                                            sendMessage(source, "<#990000>The user is already punished with this type.");
                                            return -6;
                                        }

                                        punishmentIP.punish(inetAddress, typeId, executorId, reasonId);
                                        server.getAllPlayers().forEach(player -> {
                                            if (player.getRemoteAddress().getAddress().equals(inetAddress) && !permission.hasPermission(player.getUniqueId(), MurmelEssentials.PERMISSION_PUNISH_IMMUNE)) {
                                                player.disconnect(MiniMessage.miniMessage().deserialize("<#990000>You have been punished.")); // TODO: Change the message
                                            }
                                        });
                                        sendMessage(source, "<#009999>IP-Address <#999900>%s <#009999>has been punished with the reason <#999900>%s<#009999>.",
                                                target, punishmentReason.getReason(reasonId, typeId));
                                    } else {
                                        // User-Punishment
                                        if (isUserNotExist(source, user, target)) return -4;

                                        int userId = user.getId(target);

                                        if (permission.hasPermission(userId, MurmelEssentials.PERMISSION_PUNISH_IMMUNE)) {
                                            sendMessage(source, "<#990000>The user is immune to punishment.");
                                            return -5;
                                        }

                                        String ip = session.isOnline(userId) ? session.getIpAddress(userId) : login.getIPAddress(login.getLastLoginId(userId));
                                        InetAddress inetAddress;

                                        try {
                                            inetAddress = InetAddress.getByName(ip);
                                        } catch (UnknownHostException e) {
                                            sendMessage(source, "<#cc0088>Invalid ip address. Input: <#009999>%s", ip);
                                            return -6;
                                        }

                                        if (punishmentUser.exists(userId, typeId)) {
                                            sendMessage(source, "<#990000>The user is already punished with this type.");
                                            return -7;
                                        }

                                        boolean autoFlag = punishmentReason.getAutoFlagIP(reasonId, typeId);
                                        boolean autoPunish = punishmentReason.getAutoPunish(reasonId, typeId);
                                        punishmentUser.punish(userId, typeId, executorId, inetAddress, reasonId);
                                        if (autoFlag && !punishmentIP.exists(inetAddress, typeId)) punishmentIP.punish(inetAddress, typeId, executorId, reasonId);

                                        // TODO: Add loggers

                                        // Punish the user if he is online
                                        server.getPlayer(target).ifPresent(player ->
                                                PunishmentUtil.disconnectPunishMessage(player, userId, typeId, autoFlag, autoPunish));

                                        server.getAllPlayers().forEach(player -> {
                                            // Punish double account of the user if they are online
                                            if (autoFlag && player.getRemoteAddress().getAddress().equals(inetAddress))
                                                PunishmentUtil.disconnectPunishMessage(player, userId, typeId, true, autoPunish); // TODO: Maybe another message

                                            // Send a message to all players if they have the permission to see the punishment message
                                            if (permission.hasPermission(player.getUniqueId(), MurmelEssentials.PERMISSION_SHOW_PUNISHMENT_MESSAGE))
                                                sendMessage(player, PunishmentUtil.punishMessage(player, userId, typeId, true, autoPunish).
                                                        replace("You are punished from the network.", "User <#999900>%s</#999900> is punished from the network."));
                                        });
                                        sendMessage(source, "<#009999>User <#999900>%s <#009999>has been punished with the reason <#999900>%s<#009999>.",
                                                target, punishmentReason.getReason(reasonId, typeId));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private CompletableFuture<Suggestions> getUsernames(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        user.getUsernames().stream().parallel()
                .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                .sorted()
                .toList()
                .forEach(name -> builder.suggest(name, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + name))));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> getPunishIds(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        int executorId = getExecutorId(context, user);
        String input = StringArgumentType.getString(context, "type");
        PunishmentType type = PunishmentType.fromString(input.toUpperCase());
        if (!PunishmentType.exists(type)) return builder.buildFuture();
        punishmentReason.getReasons(type.getId()).stream()
                .filter(id -> permission.hasPermission(executorId, MurmelEssentials.PERMISSION_PUNISHMENT_REASON + type.getName() + "." + id))
                .toList()
                .forEach(id -> builder.suggest(String.valueOf(id)));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> getPunishTypes(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        int executorId = getExecutorId(context, user);
        Arrays.stream(PunishmentType.VALUES)
                .filter(type -> permission.hasPermission(executorId, MurmelEssentials.PERMISSION_PUNISHMENT_TYPE + type.getName()))
                .toList()
                .forEach(type -> builder.suggest(type.getName()));
        return builder.buildFuture();
    }

    private void syntax(CommandSource source) {
        sendMessage(source, """
                <#009999>Syntax:
                <#454545>- <#999999>/punished <type> <id> <user|ip> <reset>- Punish a user or ip address with a punishment id""");
    }
}
