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
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.utils.Messages;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class UserInfoCommand extends CommandManager {
    private static final Logger log = LoggerFactory.getLogger(UserInfoCommand.class);
    private final UserProvider userProvider;
    private final LanguageProvider languageProvider;
    private final UserSessionProvider userSessionProvider;
    private final UserLoginProvider userLoginProvider;
    private final UserPlayTimeProvider userPlayTimeProvider;
    private final Permission permission;
    private final PunishmentLogProvider punishmentLogProvider;
    private final PunishmentCurrentUserProvider punishedUserProvider;
    private final MessageService messageService;

    public UserInfoCommand(MurmelEssentials plugin) {
        super(plugin);
        this.userProvider = plugin.getUserProvider();
        this.languageProvider = plugin.getLanguageProvider();
        this.userSessionProvider = plugin.getUserSessionProvider();
        this.userLoginProvider = plugin.getUserLoginProvider();
        this.userPlayTimeProvider = plugin.getUserPlayTimeProvider();
        this.permission = plugin.getPermission();
        this.punishmentLogProvider = plugin.getPunishmentLogProvider();
        this.punishedUserProvider = plugin.getPunishmentUserProvider();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("userinfo")
                .requires(source -> source.hasPermission("murmel.command.userinfo.use"))
                .executes(context -> {
                    sendMessage(context.getSource(), syntax());
                    return Command.SINGLE_SUCCESS;
                })
                .then(getInfo())
                .then(getIpAddress())
                .build();
        return new BrigadierCommand(node);
    }

    private LiteralArgumentBuilder<CommandSource> getInfo() {
        return BrigadierCommand.literalArgumentBuilder("info")
                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                        .requires(source -> source.hasPermission("murmel.command.userinfo.info"))
                        .suggests(this::getUsernames)
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String username = StringArgumentType.getString(context, "user");
                                    User user = getUser(languageId, username);

                                    int userId = user.id();
                                    boolean isOnline = userSessionProvider.isOnline(user.id());
                                    String onlineStatus = isOnline ? "<#00cc88>online</#00cc88>" : "<#cc0088>offline</#cc0088>";
                                    String firstJoin = user.firstLogin() != null ? user.firstLogin().format(getDateTimeFormatter(languageId)) : "<#009999>unknown</#009999>";
                                    UserLogin lastLogin = userLoginProvider.getLastLogin(userId);
                                    String lastQuit = lastLogin == null ? "<#009999>unknown</#009999>" : lastLogin.logoutTime().format(getDateTimeFormatter(languageId));
                                    String debugUser = user.debugUser() ? messageService.getMessage(Messages.MESSAGE_YES, languageId) : messageService.getMessage(Messages.MESSAGE_NO, languageId);
                                    String debugMode = user.debugEnabled() ? messageService.getMessage(Messages.MESSAGE_YES, languageId) : messageService.getMessage(Messages.MESSAGE_NO, languageId);
                                    String ipAddress = isOnline ? userSessionProvider.findByUserId(userId).ipAddress() // UserSession should always exist because the user is online
                                            : (lastLogin != null ? lastLogin.ipAddress() : null);
                                    String teamMember = permission.hasPermission(user, MurmelEssentials.TEAM_MEMBER_PERMISSION)
                                            ? messageService.getMessage(Messages.MESSAGE_YES, languageId) : messageService.getMessage(Messages.MESSAGE_NO, languageId);

                                    String maybeSameUser = ipAddress == null ? null : userLoginProvider.findByIpAddress(ipAddress).stream()
                                            .filter(login -> Objects.equals(login.ipAddress(), ipAddress))
                                            .map(UserLogin::userId)
                                            .filter(id -> id != userId)
                                            .distinct()
                                            .map(userProvider::findById)
                                            .filter(Objects::nonNull)
                                            .map(User::username)
                                            .sorted(String.CASE_INSENSITIVE_ORDER)
                                            .collect(Collectors.joining("<#999999>,</#999999> "));
                                    String showMoreUser = (maybeSameUser == null || maybeSameUser.isBlank()) ? ""
                                            : "\n<#999999>Maybe the same:</#999999> \n<#00cc88>" + maybeSameUser + "</#00cc88>";

                                    // Ban
                                    PunishmentCurrentUser bannedUser = punishedUserProvider.getPunishedUser(userId, PunishmentType.BAN.getId()); // Maybe check IP_Ban
                                    PunishmentLog bannedLog = bannedUser != null ? punishmentLogProvider.getLog(bannedUser.logId()) : null;
                                    String isBanned = bannedUser != null // BannedLog should always exist
                                            ? messageService.getMessage(Messages.MESSAGE_YES, languageId) + " (" + bannedLog.expiresAt().format(getDateTimeFormatter(languageId)) + ")" // Only show logId or give logId hover?
                                            : messageService.getMessage(Messages.MESSAGE_NO, languageId);
                                    int banCount = punishmentLogProvider.getLogsByUserId(userId).stream()
                                            .filter(log -> log.reasonTypeId() == PunishmentType.BAN.getId())
                                            .toList().size();

                                    // Mute
                                    PunishmentCurrentUser mutedUser = punishedUserProvider.getPunishedUser(userId, PunishmentType.MUTE.getId()); // Maybe check IP_Mute
                                    PunishmentLog mutedLog = mutedUser != null ? punishmentLogProvider.getLog(mutedUser.logId()) : null;
                                    String isMuted = mutedUser != null
                                            ? messageService.getMessage(Messages.MESSAGE_YES, languageId) + " (" + mutedLog.expiresAt().format(getDateTimeFormatter(languageId)) + ")" // Only show logId or give logId hover?
                                            : messageService.getMessage(Messages.MESSAGE_NO, languageId);
                                    int muteCount = punishmentLogProvider.getLogsByUserId(userId).stream()
                                            .filter(log -> log.reasonTypeId() == PunishmentType.MUTE.getId())
                                            .toList().size();

                                    String message = """
                                            <#999999>===- User information
                                            <#999999>User: <#00cc88>%s</#00cc88> <#555555>(ID: <#00cc88>%s</#00cc88>)</#555555>
                                            <#999999>Mojang ID: <#00cc88>%s</#00cc88>
                                            <#999999>Language: <#009999>%s</#009999>
                                            <#999999>Online status: %s
                                            <#999999>First join: <#009999>%s</#009999>
                                            <#999999>Last quit: <#009999>%s</#009999>
                                            <#999999>Login streak: <#009999>%s</#009999>
                                            <#999999>Debug user: %s
                                            <#999999>Debug mode active: %s
                                            <#999999>Last IP: <#009999>%s</#009999> %s
                                            <#999999>Team member: %s
                                            <#999999>Ban count: <#00cc88>%s</#00cc88>
                                            <#999999>Currently banned: %s
                                            <#999999>Mute count: <#00cc88>%s</#00cc88>
                                            <#999999>Currently muted: %s"""
                                            .formatted(
                                                    username, userId,
                                                    user.mojangId(),
                                                    languageProvider.get(user.languageId()).name(), // Language should always exist
                                                    onlineStatus,
                                                    firstJoin,
                                                    lastQuit,
                                                    userPlayTimeProvider.findByUserId(userId).getLoginCount(), // UserPlayTime should always exist
                                                    debugUser,
                                                    debugMode,
                                                    ipAddress, showMoreUser,
                                                    teamMember,
                                                    banCount,
                                                    isBanned,
                                                    muteCount,
                                                    isMuted
                                            );

                                    sendMessage(source, message);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                        .then(BrigadierCommand.requiredArgumentBuilder("loginId", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    User executor = getExecutor(context.getSource());
                                    String username = StringArgumentType.getString(context, "user");
                                    User user = getUser(executor.languageId(), username);
                                    String prefix = builder.getRemaining();

                                    userLoginProvider.findByUserId(user.id()).stream()
                                            .filter(login -> StringUtil.startsWithIgnoreCase(login.id().toString(), prefix))
                                            .forEach(login -> builder.suggest(login.id().toString(),
                                                    VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>Login date: " + login.loginTime().format(getDateTimeFormatter(executor.languageId()))))
                                            ));
                                    return builder.buildFuture();
                                })
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            int languageId = executor.languageId();
                                            String username = StringArgumentType.getString(context, "user");
                                            User user = getUser(languageId, username);
                                            String input = StringArgumentType.getString(context, "loginId");
                                            UUID loginId;
                                            try {
                                                loginId = UUID.fromString(input);
                                            } catch (CommandException e) {
                                                sendMessage(source, "<#990000>Invalid login ID: %s", input);
                                                return CommandResult.of(-2);
                                            }

                                            UserLogin userLogin = userLoginProvider.findById(loginId);

                                            if (userLogin == null) {
                                                sendMessage(source, "<#990000>Login with ID %s does not exist.", loginId);
                                                return CommandResult.of(-3);
                                            }

                                            if (userLogin.userId() != user.id()) {
                                                sendMessage(source, "<#990000>Login with ID %s does not belong to user %s.", loginId, username);
                                                return CommandResult.of(-4);
                                            }

                                            String message = """
                                                    <#999999>===- Login information
                                                    <#999999>Login <#009999><hover:show_text:'<#999900>Click to copy the loginId'><click:copy_to_clipboard:%s>%s</click></hover> <#999999>information:
                                                    <#999999>Username: <#009999><hover:show_text:'<#999900>Click to copy the username'><click:copy_to_clipboard:%s>%s</click></hover> <#555555>(ID: <#009999>%d</#009999>)</#555555>
                                                    <#999999>IP-Address: <#009999><hover:show_text:'<#999900>Click to copy the ip-address'><click:copy_to_clipboard:'%s'>%s</click></hover>
                                                    <#999999>Login time: <#009999>%s
                                                    <#999999>Logout time: <#009999>%s
                                                    <#999999>Client version: <#009999>%s
                                                    <#999999>Protocol version: <#009999>%s"""
                                                    .formatted(
                                                            userLogin.id(),
                                                            userLogin.id(),
                                                            user.username(),
                                                            user.username(),
                                                            user.id(),
                                                            userLogin.ipAddress(),
                                                            userLogin.ipAddress(),
                                                            userLogin.loginTime().format(getDateTimeFormatter(languageId)),
                                                            userLogin.logoutTime() != null ? userLogin.logoutTime().format(getDateTimeFormatter(languageId)) : "<#009999>unknown</#009999>",
                                                            userLogin.clientVersion(),
                                                            userLogin.protocolVersion()
                                                    );

                                            sendMessage(source, message);
                                            return CommandResult.of(Command.SINGLE_SUCCESS);
                                        })
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getIpAddress() {
        return BrigadierCommand.literalArgumentBuilder("ip")
                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                        .requires(source -> source.hasPermission("murmel.command.userinfo.ip"))
                        .suggests(this::getUsernames)
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String username = StringArgumentType.getString(context, "user");
                                    User user = getUser(languageId, username);

                                    List<UserLogin> logins = userLoginProvider.findByUserId(user.id());

                                    if (logins.isEmpty()) {
                                        sendMessage(source, "<#990000>User %s has no login information.", username);
                                        return CommandResult.of(-2);
                                    }
                                    // Maybe make it so that the command only works once you have accepted this information and saved it in the database.
                                    sendMessage(source, """
                                    <#FF0000>--- <bold>WARNING</bold> ---
                                    <#FF0000>Do <bold>NOT</bold> share this information with anyone!
                                    <#FF0000>If you share this information, it may have following <bold>consequences</bold>!!!""");

                                    logins.forEach(login -> {
                                        // TODO: Finish this
                                    });
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                );
    }

    private CompletableFuture<Suggestions> getUsernames(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        userProvider.findUsernames().stream()
                .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                .sorted()
                .toList()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private String syntax() {
        return """
                <#009999>Syntax:
                <#454545>- <#999999>/userinfo <#009999>info</#009999> <user> [loginId] <reset>- Shows the user/login information.
                <#454545>- <#999999>/userinfo <#009999>ip</#009999> <user> <reset>- Shows the IP address of the user.""";
    }
}
