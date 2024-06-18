package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.HexColor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.settings.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Ranks {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PERMISSION_CHAT_COLOR = "murmelessentials.chat.color";
    private static final String PERMISSION_CHAT_HEX = "murmelessentials.chat.hex";

    public static void updatePlayers(MurmelEssentials instance, Server server) {
        server.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            var group = instance.getGroup();
            var user = instance.getUser();
            for (var player : server.getOnlinePlayers()) {
                setPlayerTeams(group, user, player);
                setPlayerListName(group, user, player);
                player.updateCommands(); // Update the player commands
            }
        }, 10L, 5 * 20L);
    }

    @SuppressWarnings("deprecation")
    public static void setChatFormat(AsyncChatEvent event, Group group, User user) {
        var player = event.getPlayer();
        var idAndSortId = getUserIdAndSortId(group, user, player);
        var userId = idAndSortId.getKey();
        var highestSortId = idAndSortId.getValue();
        var colorSettings = group.getColorSettings();
        var chat = GroupColorType.CHAT;

        var serializer = LegacyComponentSerializer.builder().hexColors().build();
        var originalMessage = serializer.serialize(event.message());

        if (player.hasPermission(PERMISSION_CHAT_COLOR))
            originalMessage = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', originalMessage);
        if (player.hasPermission(PERMISSION_CHAT_HEX)) originalMessage = HexColor.format(originalMessage);

        final var finalMessage = originalMessage;
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            var groupId = user.getParent().getParentIds(userId).stream()
                    .filter(id -> highestSortId.equals(group.getSettings().getSortId(id)))
                    .findFirst();

            if (groupId.isPresent()) {
                int id = groupId.get();
                var format = colorSettings.getPrefix(chat, id) + player.getName() + colorSettings.getSuffix(chat, id) + " : ";
                var chatMessage = serializer.deserialize(HexColor.format(colorSettings.getColor(chat, id)) + finalMessage);

                return MINI_MESSAGE.deserialize(format).append(chatMessage);
            } else return message;
        });
    }

    private static void setPlayerListName(Group group, User user, Player player) {
        var idAndSortId = getUserIdAndSortId(group, user, player);
        var userId = idAndSortId.getKey();
        var highestSortId = idAndSortId.getValue();
        var colorSettings = group.getColorSettings();
        var tab = GroupColorType.TAB;

        user.getParent().getParentIds(userId).stream()
                .filter(groupId -> highestSortId.equals(group.getSettings().getSortId(groupId)))
                .forEach(groupId -> player.playerListName(MINI_MESSAGE.deserialize(
                        colorSettings.getPrefix(tab, groupId) + colorSettings.getColor(tab, groupId) + player.getName() + colorSettings.getSuffix(tab, groupId))));
    }

    @SuppressWarnings("deprecation")
    private static void setPlayerTeams(Group group, User user, Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        scoreboard.getTeams().forEach(Team::unregister);

        for (var groupName : group.getNames()) {
            var tag = GroupColorType.TAG;
            var groupId = group.getUniqueId(groupName);
            var groupSortId = group.getSettings().getSortId(groupId);
            var name = group.getSettings().getTeamId(groupId);

            Team team = scoreboard.getTeam(name);
            if (team == null) team = scoreboard.registerNewTeam(name);
            var colorSettings = group.getColorSettings();
            var prefix = colorSettings.getPrefix(tag, groupId);
            var suffix = colorSettings.getSuffix(tag, groupId);
            var color = colorSettings.getColor(tag, groupId);
            team.prefix(MINI_MESSAGE.deserialize(prefix));
            team.suffix(MINI_MESSAGE.deserialize(suffix));
            team.setColor(Objects.requireNonNull(ChatColor.getByChar(color.replace("§", "").replace("&", ""))));

            // Create a map of players and their highest SortID
            Map<Player, Integer> playerSortIds = player.getServer().getOnlinePlayers().stream()
                    .collect(Collectors.toMap(Function.identity(), target -> getHighestSortId(group, user, user.getId(target.getUniqueId()))));

            playerSortIds.entrySet().stream()
                    .filter(entry -> groupSortId == entry.getValue())
                    .map(entry -> entry.getKey().getName())
                    .forEach(team::addEntry);
        }
    }

    private static Pair<Integer, Integer> getUserIdAndSortId(Group group, User user, Player player) {
        var userId = user.getId(player.getUniqueId());
        var highestSortId = getHighestSortId(group, user, userId);
        return new ImmutablePair<>(userId, highestSortId);
    }

    private static int getHighestSortId(Group group, User user, int userId) {
        return user.getParent().getParentIds(userId).stream()
                .map(groupId -> group.getSettings().getSortId(groupId))
                .collect(Collectors.summarizingInt(Integer::intValue))
                .getMax();
    }
}
