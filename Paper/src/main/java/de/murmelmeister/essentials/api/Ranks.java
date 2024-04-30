package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.HexColor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.settings.GroupColorSettings;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.SQLException;

public class Ranks {
    public static void updatePlayers(MurmelEssentials instance, Server server) {
        server.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            try {
                Group group = instance.getGroup();
                User user = instance.getUser();
                for (Player player : server.getOnlinePlayers()) {
                    setPlayerTeams(group, user, player);
                    setPlayerListName(group, user, player);
                    player.updateCommands(); // Update the player commands
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, 60L, 3 * 20L);
    }

    private void handleChat(AsyncPlayerChatEvent event) {
        event.setFormat("%s : %s");
    }

    @SuppressWarnings("deprecation")
    private static void setPlayerListName(Group group, User user, Player player) throws SQLException {
        int groupId = user.getParent().getParentId(user.getId(player.getUniqueId()));
        GroupColorSettings colorSettings = group.getColorSettings();
        try {
            player.setPlayerListName(HexColor.format(colorSettings.getTabPrefix(groupId) + colorSettings.getTagColor(groupId) + player.getName() + colorSettings.getTabSuffix(groupId)));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setPlayerTeams(Group group, User user, Player player) throws SQLException {
        for (String name : group.getNames()) {
            Scoreboard scoreboard = player.getScoreboard();
            Team team = scoreboard.getTeam(name);
            if (team == null) team = scoreboard.registerNewTeam(name);
            team.setPrefix(HexColor.format(group.getColorSettings().getTagPrefix(group.getUniqueId(name))));
            team.setSuffix(HexColor.format(group.getColorSettings().getTagSuffix(group.getUniqueId(name))));
            for (Player target : player.getServer().getOnlinePlayers())
                team.addEntry(target.getName());
        }
    }
}
