package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public final class ShowTeamCommand extends CommandManager {
    private final UserProvider userProvider;
    private final UserSessionProvider userSessionProvider;
    private final UserLoginProvider userLoginProvider;
    private final Permission permission;
    private final ProxyServer server;

    public ShowTeamCommand(MurmelEssentials plugin) {
        super(plugin);
        this.userProvider = plugin.getUserProvider();
        this.userSessionProvider = plugin.getUserSessionProvider();
        this.userLoginProvider = plugin.getUserLoginProvider();
        this.permission = plugin.getPermission();
        this.server = plugin.getServer();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("showteam")
                .requires(source -> source.hasPermission("murmel.command.showteam"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            String teamPermission = MurmelEssentials.TEAM_MEMBER_PERMISSION;
                            /*List<UUID> userIds = userProvider.findMojangIds();
                            List<User> teamUsers = new ArrayList<>();
                            for (int i = userIds.size() - 1; i >= 0; i--) {
                                User user = userProvider.findByMojangId(userIds.get(i));
                                //if (user == -1) continue;
                                if (permission.hasPermission(user.getId(), teamPermission))
                                    teamUsers.add(user);
                            }*/
                            List<User> teamMembers = userProvider.findAll().stream()
                                    .filter(user -> permission.hasPermission(user, teamPermission))
                                    .toList();

                            if (teamMembers.isEmpty()) {
                                sendMessage(source, "<#990000>No team members found.");
                                return CommandResult.of(-2);
                            }

                            sendMessage(source, "<#999999>%s:", teamMembers.size() == 1 ? "Team member" : "Team members");
                            teamMembers.forEach(user -> {
                                String targetName = user.username();
                                int targetId = user.id();
                                boolean isOnline = userSessionProvider.isOnline(targetId);
                                String onlineMessage = isOnline ? "<#00cc88>online" : "<#cc0088>offline";
                                LocalDateTime lastQuitDate = userLoginProvider.getLastLoginTime(targetId);
                                String lastSeenMessage = formatTimeAgo(executor.languageId(), userLoginProvider.getLastLoginTime(targetId));
                                String hoverLastSeenMessage = "<#999999>-</#999999> " + (lastQuitDate == null ? "unknown"
                                        : "<hover:show_text:'<#999999>Last seen: <#00cc88>%s'>%s</hover>"
                                        .formatted(lastSeenMessage, lastQuitDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))));

                                Optional<Player> target = server.getPlayer(targetName);
                                String serverName = target.flatMap(Player::getCurrentServer)
                                        .map(server -> server.getServerInfo().getName()).orElse("unknown");
                                String clickedServer = serverName.equals("unknown") ? serverName : (executor.id() == targetId ? serverName :
                                        "<hover:show_text:'<#999900>Click to send you to the server'><click:run_command:'/server " + serverName + "'>" + serverName + "</click></hover>");

                                String message = "<#454545>- <#999999>User <#999900>%s</#999900> is %s %s"
                                        .formatted(targetName, onlineMessage, (isOnline ? "<#999999>- Server: </#999999>" + clickedServer : hoverLastSeenMessage));
                                sendMessage(source, message);
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .build();
        return new BrigadierCommand(node);
    }
}
