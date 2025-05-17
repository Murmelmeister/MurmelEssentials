package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.LoginHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class ShowTeamCommand extends CommandManager {
    private final ActiveSession session;
    private final LoginHistory login;
    private final ProxyServer server;

    public ShowTeamCommand(MurmelEssentials plugin) {
        super(plugin);
        this.session = plugin.getActiveSession();
        this.login = plugin.getLoginHistory();
        this.server = plugin.getServer();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("showteam")
                .requires(source -> source.hasPermission("murmel.command.showteam"))
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    int executorId = getExecutorId(source);
                    if (executorId == -2) return -1;

                    String teamPermission = MurmelEssentials.TEAM_MEMBER_PERMISSION;
                    List<UUID> userIds = user.getUniqueIds();
                    List<Integer> teamUsers = new ArrayList<>();
                    for (int i = userIds.size() - 1; i >= 0; i--) {
                        int userId = user.getId(userIds.get(i));
                        if (userId == -1) continue;
                        if (permission.hasPermission(userId, teamPermission))
                            teamUsers.add(userId);
                    }

                    if (teamUsers.isEmpty()) {
                        sendMessage(source, "<#990000>No team members found.");
                        return -2;
                    }

                    sendMessage(source, "<#999999>%s:", teamUsers.size() == 1 ? "Team member" : "Team members");
                    teamUsers.reversed().forEach(targetId -> {
                        String targetName = user.getUsername(targetId);
                        boolean isOnline = session.isOnline(targetId);
                        String onlineMessage = isOnline ? "<#00cc88>online" : "<#cc0088>offline";
                        String lastQuitDate = login.getLastQuitDate(targetId);
                        String lastSeenMessage = formatTimeAgo(login.getLastQuit(targetId));
                        String hoverLastSeenMessage = "<#999999>-</#999999> " + (lastQuitDate == null ? "unknown"
                                : "<hover:show_text:'<#999999>Last seen: <#00cc88>%s'>%s</hover>".formatted(lastSeenMessage, lastQuitDate));

                        Optional<Player> target = server.getPlayer(targetName);
                        String serverName = target.flatMap(Player::getCurrentServer)
                                .map(server -> server.getServerInfo().getName()).orElse("unknown");
                        String clickedServer = serverName.equals("unknown") ? serverName : (executorId == targetId ? serverName :
                                "<hover:show_text:'<#999900>Click to send you to the server'><click:run_command:'/server " + serverName + "'>" + serverName + "</click></hover>");

                        String message = "<#454545>- <#999999>User <#999900>%s</#999900> is %s %s"
                                .formatted(targetName, onlineMessage, (isOnline ? "<#999999>- Server: </#999999>" + clickedServer : hoverLastSeenMessage));
                        sendMessage(source, message);
                    });

                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>ShowTeam command executed in %s ms", durationMs);
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .build();
        return new BrigadierCommand(node);
    }
}
