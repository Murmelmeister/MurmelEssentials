package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.HexColor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.settings.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.sql.SQLException;
import java.util.*;

public final class Ranks {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
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
        }, 10L, 3 * 20L);
    }

    @SuppressWarnings("deprecation")
    public static void setChatFormat(AsyncChatEvent event, Group group, User user) throws SQLException {
        var player = event.getPlayer();
        var userId = user.getId(player.getUniqueId());
        var highestSortId = getSortId(group, user, userId);
        for (var groupId : user.getParent().getParentIds(userId)) {
            var sortId = group.getSettings().getSortId(groupId);
            var colorSettings = group.getColorSettings();
            var chat = GroupColorType.CHAT;
            if (highestSortId == sortId) {
                var serializer = LegacyComponentSerializer.builder().hexColors().build();
                var originalMessage = serializer.serialize(event.message());

                if (player.hasPermission("murmelessentials.chat.color")) originalMessage = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', originalMessage);
                if (player.hasPermission("murmelessentials.chat.hex")) originalMessage = HexColor.format(originalMessage);

                var format = colorSettings.getPrefix(chat, groupId) + player.getName() + colorSettings.getSuffix(chat, groupId) + " : ";
                var chatMessage = serializer.deserialize(HexColor.format(colorSettings.getColor(chat, groupId)) + originalMessage);
                var parsedMessage = MINI_MESSAGE.deserialize(format).append(chatMessage);

                event.viewers().forEach(audience -> audience.sendMessage(parsedMessage));
                event.setCancelled(true);
            }
        }
    }

    private static void setPlayerListName(Group group, User user, Player player) throws SQLException {
        var userId = user.getId(player.getUniqueId());
        var highestSortId = getSortId(group, user, userId);
        for (var groupId : user.getParent().getParentIds(userId)) {
            var sortId = group.getSettings().getSortId(groupId);
            var colorSettings = group.getColorSettings();
            var tab = GroupColorType.TAB;
            if (highestSortId == sortId) {
                player.playerListName(MINI_MESSAGE.deserialize(colorSettings.getPrefix(tab, groupId) + colorSettings.getColor(tab, groupId) + player.getName() + colorSettings.getSuffix(tab, groupId)));
            }
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
            var prefix = group.getColorSettings().getPrefix(tag, groupId);
            var suffix = group.getColorSettings().getSuffix(tag, groupId);
            var color = group.getColorSettings().getColor(tag, groupId);
            team.setPrefix(HexColor.format(prefix));
            team.setSuffix(HexColor.format(suffix));
            team.setColor(Objects.requireNonNull(ChatColor.getByChar(color.replace("&", "").replace("§", ""))));

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
