package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class Ranks {
    public static void updatePLayerTeams(MurmelEssentials instance, Server server) {
        server.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            try {
                Group group = instance.getGroup();
                User user = instance.getUser();
                for (Player player : server.getOnlinePlayers()) {
                    setPlayerTeams(group, user, player);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, 3 * 60L, 3 * 20L);
    }

    private static void setPlayerTeams(Group group, User user, Player player) throws SQLException {
        for (String name : group.getNames()) {
            Scoreboard scoreboard = player.getScoreboard();
            Team team = scoreboard.getTeam(name);
            if (team == null) team = scoreboard.registerNewTeam(name);
            team.setPrefix(group.getColorSettings().getTabPrefix(group.getUniqueId(name)));
            team.setSuffix(group.getColorSettings().getTabSuffix(group.getUniqueId(name)));
            List<? extends Player> sortedPlayers = player.getServer().getOnlinePlayers().stream().sorted(Comparator.comparing(target -> {
                try {
                    return group.getSettings().getSortId(user.getParent().getParentId(user.getId(player.getUniqueId())));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            })).toList();
            for (Player target : sortedPlayers) {
                team.addEntry(target.getName());
            }
        }
    }
}
