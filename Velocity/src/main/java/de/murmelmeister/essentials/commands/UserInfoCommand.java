package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.LoginHistory;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class UserInfoCommand extends CommandManager {
    private final User user;
    private final ActiveSession session;
    private final LoginHistory login;
    private final Permission permission;
    private final PlayTime playTime;
    private final PunishmentReason reason;
    private final PunishmentLog log;
    private final PunishmentUser punishment;

    public UserInfoCommand(User user, ActiveSession activeSession, LoginHistory loginHistory, Permission permission, PlayTime playTime, PunishmentReason reason, PunishmentLog log, PunishmentUser punishment) {
        this.user = user;
        this.session = activeSession;
        this.login = loginHistory;
        this.permission = permission;
        this.playTime = playTime;
        this.reason = reason;
        this.log = log;
        this.punishment = punishment;
    }

    /*
        /userinfo info <user> [loginId] - Shows information about a user/login.
        /userinfo ip <user> - Shows the IP-Addresses of a user.
        /userinfo punish <type> <user> - Shows alle the punishment of a user.
     */

    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder("userinfo")
                .requires(source -> source.hasPermission("murmelessentials.command.userinfo.use"))
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(getInfo())
                .then(getIpAddressesUser())
                .build();
        return new BrigadierCommand(rootNode);
    }

    private LiteralArgumentBuilder<CommandSource> getInfo() {
        return BrigadierCommand.literalArgumentBuilder("info")
                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                        .requires(source -> source.hasPermission("murmelessentials.command.userinfo.info.user"))
                        .suggests(this::getUsernames)
                        .executes(context -> {
                            String username = StringArgumentType.getString(context, "user");
                            CommandSource source = context.getSource();
                            if (isUserNotExist(source, user, username)) return -1;
                            int userId = user.getId(username);
                            UUID uuid = user.getUniqueId(userId);
                            boolean isOnline = session.isOnline(userId);
                            UUID loginId = isOnline ? session.getSessionId(userId) : login.getLastLoginId(userId); // TODO: Debug which id is used.
                            String ipAddress = isOnline ? session.getIpAddress(userId) : login.getIPAddress(loginId); // TODO: if executor does not have the permission to see the ip-address, it will be null or something else.
                            String maybeSameUser = login.getUserIdsByIP(ipAddress).stream()
                                    .filter(id -> id != userId)
                                    .map(user::getUsername)
                                    .collect(Collectors.joining("<#999999>,</#999999> "));
                            String online = isOnline ? "<#00cc88>online</#00cc88>" : "<#cc0088>offline</#cc0088>";
                            String time = TimeUtil.formatTimeValue(playTime, userId);
                            String teamMember = permission.hasPermission(userId, MurmelEssentials.TEAM_MEMBER_PERMISSION) ? "<#00cc88>yes</#00cc88>" : "<#cc0088>no</#cc0088>";

                            int banTypeId = PunishmentType.BAN.getId();
                            int muteTypeId = PunishmentType.MUTE.getId();
                            UUID banLogId = punishment.exists(userId, banTypeId) ? punishment.getLogId(userId, banTypeId) : null;
                            UUID muteLogId = punishment.exists(userId, muteTypeId) ? punishment.getLogId(userId, muteTypeId) : null;
                            String isBanned = punishment.isPunished(userId, banTypeId) ?
                                    "<#cc0088>yes (" + log.getCreatedAt(banLogId, banTypeId) + " - " + log.getExpiredDate(banLogId, banTypeId) + ")</#cc0088>"
                                    : "<#00cc88>no</#00cc88>";
                            String isMuted = punishment.isPunished(userId, muteTypeId) ?
                                    "<#cc0088>yes (" + log.getCreatedAt(muteLogId, muteTypeId) + " - " + log.getExpiredDate(muteLogId, muteTypeId) + ")</#cc0088>"
                                    : "<#00cc88>no</#00cc88>";
                            int banCount = log.getLogs(userId, banTypeId).size();
                            int muteCount = log.getLogs(userId, muteTypeId).size();

                            String showMoreUser = maybeSameUser.isBlank() ? "" : "\n<#999999>Maybe the same:\n<#009999>" + maybeSameUser;
                            // TODO: Show the reason of the ban/mute.
                            sendMessage(source, """
                                            <#999999>User <#009999>%s</#009999> information:
                                            <#999999>UserId: <#009999>%d</#009999>
                                            <#999999>UUID: <#009999>%s</#009999>
                                            <#999999>IP-Address: <#009999>%s</#009999>%s
                                            <#999999>First join date: <#009999>%s</#009999>
                                            <#999999>Last quit: <#009999>%s</#009999>
                                            <#999999>Online mode: %s
                                            <#999999>Playtime: <#009999>%s</#009999>
                                            <#999999>Team member: %s
                                            <#999999>Ban count: <#009999>%d</#009999>
                                            <#999999>Banned: %s
                                            <#999999>Mute count: <#009999>%d</#009999>
                                            <#999999>Muted: %s""",
                                    username, userId, uuid.toString(), ipAddress, showMoreUser, user.getFirstJoinDate(userId), login.getLastQuit(userId),
                                    online, time, teamMember, banCount, isBanned, muteCount, isMuted);
                            login.getSortedLogins(userId)
                                    .forEach(login -> sendMessage(source, "LoginID: " + login.toString() + "; <rainbow>Login date: " + this.login.getLoginTime(login).toString()));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("loginId", StringArgumentType.word())
                                .requires(source -> source.hasPermission("murmelessentials.command.userinfo.info.login"))
                                .suggests((context, builder) -> {
                                    String prefix = builder.getRemaining();
                                    String username = StringArgumentType.getString(context, "user");
                                    if (!user.existsUser(username)) return builder.buildFuture();
                                    int userId = user.getId(username);
                                    login.getSortedLogins(userId).stream()
                                            .filter(login -> StringUtil.startsWithIgnoreCase(login.toString(), prefix))
                                            .toList()
                                            .forEach(login -> builder.suggest(login.toString(), VelocityBrigadierMessage.tooltip(
                                                            MiniMessage.miniMessage().deserialize("<rainbow>Login date: " + this.login.getLoginTime(login).toString()))
                                                    )
                                            );
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    String inputName = StringArgumentType.getString(context, "user");
                                    if (isUserNotExist(source, user, inputName)) return -1;

                                    String inputLogin = StringArgumentType.getString(context, "loginId");
                                    UUID loginId;
                                    try {
                                        loginId = UUID.fromString(inputLogin);
                                    } catch (IllegalArgumentException e) {
                                        sendMessage(source, "<#cc0088>Invalid loginId. Input: <#009999>%s", inputLogin);
                                        return -2;
                                    }

                                    if (!login.existsLogin(loginId)) {
                                        sendMessage(source, "<#cc0088>Login <#009999>%s <#cc0088>does not exist.", inputLogin);
                                        return -3;
                                    }

                                    int userId = login.getUserId(loginId);
                                    int inputId = user.getId(inputName);

                                    if (userId != inputId) {
                                        sendMessage(source, "<#cc0088>User <#009999>%s <#cc0088>does not match the login.", inputName);
                                        return -4;
                                    }

                                    String username = user.getUsername(userId);
                                    String ipAddress = login.getIPAddress(loginId);
                                    String loginTime = login.getLoginTime(loginId).toString();
                                    String logoutTime = login.getLogoutDate(loginId);
                                    String clientVersion = login.getClientVersion(loginId);
                                    String protocolVersion = login.getProtocolVersion(loginId);
                                    sendMessage(source, """
                                                    <#999999>Login <#009999><hover:show_text:'<#999900>Click to copy the loginId'><click:copy_to_clipboard:%s>%s</click></hover> <#999999>information:
                                                    <#999999>UserId: <#009999>%d
                                                    <#999999>Username: <#009999><hover:show_text:'<#999900>Click to copy the username'><click:copy_to_clipboard:%s>%s</click></hover>
                                                    <#999999>IP-Address: <#009999><hover:show_text:'<#999900>Click to copy the ip-address'><click:copy_to_clipboard:'%s'>%s</click></hover>
                                                    <#999999>Login time: <#009999>%s
                                                    <#999999>Logout time: <#009999>%s
                                                    <#999999>Client version: <#009999>%s
                                                    <#999999>Protocol version: <#009999>%s""",
                                            loginId, loginId, userId, username, username, ipAddress, ipAddress, loginTime, logoutTime, clientVersion, protocolVersion);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getIpAddressesUser() {
        return BrigadierCommand.literalArgumentBuilder("ip")
                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                        .requires(source -> source.hasPermission("murmelessentials.command.userinfo.ip"))
                        .suggests(this::getUsernames)
                        .executes(context -> {
                            String username = StringArgumentType.getString(context, "user");
                            CommandSource source = context.getSource();
                            if (isUserNotExist(source, user, username)) return -1;
                            int userId = user.getId(username);
                            List<String> ips = login.getLogins(userId).stream()
                                    .map(login::getIPAddress)
                                    .distinct().toList();

                            if (ips.isEmpty()) {
                                sendMessage(source, "<#cc0088>User <#009999>%s <#cc0088>has no ip-addresses.", username);
                                return -2;
                            }

                            // Maybe make it so that the command only works once you have accepted this information and saved it in the database.
                            sendMessage(source, """
                                    <#FF0000>--- <bold>WARNING</bold> ---
                                    <#FF0000>Do <bold>NOT</bold> share this information with anyone!
                                    <#FF0000>If you share this information, it may have following <bold>consequences</bold>!!!""");
                            sendMessage(source, "<#999999>IP-Addresses of <#999900>%s</#999900>:", username);
                            ips.forEach(ip -> {
                                String firstTime = login.getFirstLoginTimeByUser(userId, ip).toString();
                                String lastTime = login.getLastLoginTimeByUser(userId, ip).toString();
                                sendMessage(source,
                                        "<#999999>- <#009999><hover:show_text:'<#999900>Click to copy the ip-address'><click:copy_to_clipboard:%s>%s</click></hover> <#990099>(%s - %s)",
                                        ip, ip, firstTime, lastTime);
                            });
                            return Command.SINGLE_SUCCESS;
                        })
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

    private void syntax(CommandSource source) {
        sendMessage(source, """
                <#009999>Syntax:
                <#454545>- <#999999>/userinfo <#009999>info</#009999> <user> [loginId] <reset>- Shows information about a user/login.
                <#454545>- <#999999>/userinfo <#009999>ip</#009999> <user> <reset>- Shows the IP-Addresses of a user.""");
    }
}
