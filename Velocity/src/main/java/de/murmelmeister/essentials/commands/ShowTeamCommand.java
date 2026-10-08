package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.excuse.UserExcuse;
import de.murmelmeister.murmelapi.user.excuse.UserExcuseProvider;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.utils.TimeFilterUtil;
import net.kyori.adventure.text.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@CommandConfig(id = "showteam", name = "showteam")
public final class ShowTeamCommand extends CommandManager {
    private final UserProvider userProvider;
    private final UserService userService;
    private final PermissionService permissionService;
    private final UserExcuseProvider userExcuseProvider;
    private final ProxyServer server;

    public ShowTeamCommand(MurmelEssentials plugin) {
        super(plugin);
        this.userProvider = plugin.getUserProvider();
        this.userService = plugin.getUserService();
        this.permissionService = plugin.getPermissionService();
        this.userExcuseProvider = plugin.getUserExcuseProvider();
        this.server = plugin.getServer();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "showteam"))
                .executes(context ->
                        execute(context, 1)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                execute(context, IntegerArgumentType.getInteger(context, "page"))
                        )
                );
    }

    private int execute(CommandContext<CommandSource> context, int page) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            String teamPermission = MurmelEssentials.TEAM_MEMBER_PERMISSION;

            List<User> teamMembers = userProvider.findAll().stream()
                    .filter(user -> permissionService.hasPermission(PermissionTarget.user(user.id()), teamPermission))
                    .toList();
            if (teamMembers.isEmpty())
                throw new CommandException(Message.COMMAND_SHOW_TEAM_LIST_EMPTY);

            Message headerName = teamMembers.size() == 1
                    ? Message.COMMAND_SHOW_TEAM_LIST_SINGULAR
                    : Message.COMMAND_SHOW_TEAM_LIST_PLURAL;
            sendMessage(source, languageId,
                    Message.COMMAND_SHOW_TEAM_LIST_HEADER,
                    tagParsed("header_name", languageId, headerName),
                    tagParsed("members", teamMembers.size())
            );

            Player player = server.getPlayer(executor.mojangId()).
                    orElseThrow(() -> new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagParsed("user", executor.username())));
            String currentServer = player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(null);

            LocalDate today = LocalDate.now();
            List<Component> messages = teamMembers.stream()
                    .map(target -> {
                        boolean online = userService.isOnline(target.id());
                        Component status = online ? component(languageId, Message.USER_ONLINE) : component(languageId, Message.USER_OFFLINE);

                        UserExcuse excuse = userExcuseProvider.findByUserId(target.id()).stream()
                                .filter(entry -> !today.isBefore(entry.startDate())
                                        && !today.isAfter(entry.startDate().plusDays(entry.extraDays())))
                                .max(Comparator.comparing(UserExcuse::startDate)
                                        .thenComparingInt(UserExcuse::id))
                                .orElse(null);
                        Component excuseMessage = excuse != null ?
                                component(languageId, Message.COMMAND_SHOW_TEAM_MESSAGE_EXCUSE,
                                        tagParsed("start_date", excuse.startDate().format(getDateFormatter(languageId))),
                                        tagParsed("end_date", excuse.startDate().plusDays(excuse.extraDays()).format(getDateFormatter(languageId)))
                                ) : Component.empty();

                        if (online) {
                            String serverName = server.getPlayer(target.mojangId())
                                    .flatMap(Player::getCurrentServer)
                                    .map(server -> server.getServerInfo().getName())
                                    .orElse(null);
                            Component clickedServer = (currentServer != null && serverName != null && !currentServer.equals(serverName))
                                    ? component(languageId, Message.COMMAND_SHOW_TEAM_MESSAGE_CLICKED, tagParsed("name", serverName))
                                    : (serverName != null ? component(serverName) : Component.empty());

                            return component(languageId, Message.COMMAND_SHOW_TEAM_MESSAGE_ONLINE,
                                    tagParsed("username", target.username()),
                                    tagComponent("online", status),
                                    tagComponent("server", clickedServer),
                                    tagComponent("excuse", excuseMessage)
                            );
                        } else {
                            UserLogin login = userService.getLastLogin(target.id());
                            Component time = login != null
                                    ? component(formatTimeAgo(languageId, login.logoutTime(), TimeFilterUtil.SECONDS))
                                    : component(languageId, Message.USER_UNKNOWN);

                            Component offline = login != null
                                    ? component(login.logoutTime().format(getDateFormatter(languageId)))
                                    : status;

                            return component(languageId, Message.COMMAND_SHOW_TEAM_MESSAGE_OFFLINE,
                                    tagParsed("username", target.username()),
                                    tagComponent("time", time),
                                    tagComponent("offline", offline),
                                    tagComponent("excuse", excuseMessage)
                            );
                        }
                    })
                    .toList();

            sendPagedMessage(source, messages, "showteam", page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }
}
