package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.LoginHistory;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ShowTeamCommand extends CommandManager {
    public BrigadierCommand createCommand(Permission permission, User user, ProxyServer proxyServer, ActiveSession session, LoginHistory login) {
        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder("showteam")
                .requires(source -> source.hasPermission("murmelessentials.command.showteam"))
                .executes(context -> {
                    List<UUID> userIds = user.getUniqueIds();
                    String teamPermission = MurmelEssentials.TEAM_MEMBER_PERMISSION;

                    List<Integer> teamUsers = new ArrayList<>();
                    for (int i = userIds.size() - 1; i >= 0; i--) {
                        int userId = user.getId(userIds.get(i));
                        if (userId == -1) continue;
                        if (permission.hasPermission(userId, teamPermission))
                            teamUsers.add(userId);
                    }

                    CommandSource source = context.getSource();
                    if (teamUsers.isEmpty()) {
                        sendMessage(source, "<#990000>No team members found.");
                        return Command.SINGLE_SUCCESS;
                    }

                    Player player = getPlayer(source);
                    int userId = user.getId(player.getUniqueId());

                    sendMessage(source, "<#009999>Team members:");
                    for (int targetId : teamUsers.reversed()) {
                        String username = user.getUsername(targetId);

                        boolean isOnline = session.isOnline(targetId);
                        String online = isOnline ? "<#00cc88>online" : "<#cc0088>offline";
                        String lastQuit = login.getLastQuit(targetId) == null ? "unknown" : MurmelAPI.getDateFormat().format(login.getLastQuit(targetId));
                        String lastLogin = "<#999999>-</#999999> " + lastQuit;

                        Optional<Player> target = proxyServer.getPlayer(username);
                        String serverName = target.flatMap(Player::getCurrentServer).map(server -> server.getServerInfo().getName()).orElse("unknown");
                        String clickedServer = serverName.equals("unknown") ? serverName : (userId == targetId ? serverName :
                                "<hover:show_text:'<#999900>Send you to the server'><click:run_command:/server " + serverName + ">" + serverName + "</click></hover>");

                        sendMessage(source, "<#454545>- <#999999>User <#999900>%s</#999900> is %s %s", username, online,
                                (isOnline ? "<#999999>- Server: </#999999> " + clickedServer : lastLogin));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .build();
        return new BrigadierCommand(rootNode);
    }
}
