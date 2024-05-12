package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.HexColor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.settings.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.SQLException;
import java.util.*;

public final class Ranks {
    public static void updatePlayers(MurmelEssentials instance, Server server) {
        server.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            try {
                var group = instance.getGroup();
                var user = instance.getUser();
                for (var player : server.getOnlinePlayers()) {
                    setPlayerTeams(group, user, player);
                    setPlayerListName(group, user, player);
                    player.updateCommands(); // Update the player commands
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, 10L, 2 * 20L);
    }

    @SuppressWarnings("deprecation")
    public static void setChatFormat(AsyncPlayerChatEvent event, Group group, User user) throws SQLException {
        var userId = user.getId(event.getPlayer().getUniqueId());
        var highestSortId = getSortId(group, user, userId);
        for (var groupId : user.getParent().getParentIds(userId)) {
            var sortId = group.getSettings().getSortId(groupId);
            var colorSettings = group.getColorSettings();
            var chat = GroupColorType.CHAT;
            if (highestSortId == sortId)
                event.setFormat(HexColor.format(colorSettings.getPrefix(chat, groupId) + "%s" + colorSettings.getSuffix(chat, groupId) + " : " + colorSettings.getColor(chat, groupId) + "%s"));
        }
    }

    @SuppressWarnings("deprecation")
    private static void setPlayerListName(Group group, User user, Player player) throws SQLException {
        var userId = user.getId(player.getUniqueId());
        var highestSortId = getSortId(group, user, userId);
        for (var groupId : user.getParent().getParentIds(userId)) {
            var sortId = group.getSettings().getSortId(groupId);
            var colorSettings = group.getColorSettings();
            var tab = GroupColorType.TAB;
            if (highestSortId == sortId)
                player.setPlayerListName(HexColor.format(colorSettings.getPrefix(tab, groupId) + colorSettings.getColor(tab, groupId) + player.getName() + colorSettings.getSuffix(tab, groupId)));
        }
    }

    @SuppressWarnings("deprecation")
    private static void setPlayerTeams(Group group, User user, Player player) throws SQLException {
        Scoreboard scoreboard = player.getScoreboard();

        for (var team : scoreboard.getTeams()) team.unregister();

        for (var groupName : group.getNames()) {
            var tag = GroupColorType.TAG;
            var groupId = group.getUniqueId(groupName);
            var groupSortId = group.getSettings().getSortId(groupId);
            var name = group.getSettings().getTeamId(groupId);

            Team team = scoreboard.getTeam(name);
            if (team == null) team = scoreboard.registerNewTeam(name);
            team.setPrefix(HexColor.format(group.getColorSettings().getPrefix(tag, groupId)));
            team.setSuffix(HexColor.format(group.getColorSettings().getSuffix(tag, groupId)));
            team.setColor(Objects.requireNonNull(ChatColor.getByChar(group.getColorSettings().getColor(tag, groupId).replace("&", "").replace("§", ""))));

            // Create a map of players and their highest SortID
            Map<Player, Integer> playerSortIds = new HashMap<>();
            for (var target : player.getServer().getOnlinePlayers()) {
                var userId = user.getId(target.getUniqueId());
                var highestSortId = getSortId(group, user, userId);
                playerSortIds.put(target, highestSortId);
            }

            List<Map.Entry<Player, Integer>> sortedEntries = new ArrayList<>(playerSortIds.entrySet());
            sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            for (var playerEntry : team.getEntries()) team.removeEntry(playerEntry);
            for (var playerEntry : sortedEntries) {
                if (groupSortId == playerEntry.getValue())
                    team.addEntry(playerEntry.getKey().getName());
            }
        }
    }

    private static int getSortId(Group group, User user, int userId) throws SQLException {
        List<Integer> sortIdList = new ArrayList<>();
        for (var groupId : user.getParent().getParentIds(userId)) {
            var sortId = group.getSettings().getSortId(groupId);
            sortIdList.add(sortId);
        }
        return Collections.max(sortIdList);
    }
}
