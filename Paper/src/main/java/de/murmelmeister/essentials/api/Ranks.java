package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.HexColor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Ranks {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String PERMISSION_CHAT_COLOR = "murmelessentials.chat.color";
    private static final String PERMISSION_CHAT_HEX = "murmelessentials.chat.hex";

    public static void updatePlayers(MurmelEssentials instance, Server server) {
        var hasUpdateOccurred = new AtomicBoolean(false);

        RefreshUtil.setRefreshListener(() -> hasUpdateOccurred.set(true));

        server.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            if (hasUpdateOccurred.get()) {
                var group = instance.getGroup();
                var user = instance.getUser();
                for (var player : server.getOnlinePlayers()) {
                    setPlayerTeams(group, user, player);
                    setPlayerListName(group, user, player);
                    player.updateCommands(); // Update the player commands
                }
                hasUpdateOccurred.set(false);
            }
        }, 10L, 2 * 20L);
    }

    @SuppressWarnings("deprecation")
    public static void setChatFormat(AsyncChatEvent event, Group group, User user) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        int highestSortId = user.getParent().getHighestPriority(group, userId);
        GroupColor groupColor = group.getColor();
        GroupColorType groupType = GroupColorType.CHAT;

        LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().hexColors().build();
        String originalMessage = serializer.serialize(event.message());

        if (player.hasPermission(PERMISSION_CHAT_COLOR))
            originalMessage = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', originalMessage);
        if (player.hasPermission(PERMISSION_CHAT_HEX)) originalMessage = HexColor.format(originalMessage);

        final String finalMessage = originalMessage;
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Optional<Integer> groupId = user.getParent().getParentIds(userId).stream()
                    .filter(id -> highestSortId == group.getPriority(id))
                    .findFirst();

            if (groupId.isPresent()) {
                int id = groupId.get();
                String format = groupColor.getPrefix(id, groupType) + player.getName() + groupColor.getSuffix(id, groupType) + " : ";
                TextComponent chatMessage = serializer.deserialize(HexColor.format(groupColor.getColor(id, groupType)) + finalMessage);

                return MINI_MESSAGE.deserialize(format).append(chatMessage);
            } else return message;
        });
    }

    private static void setPlayerListName(Group group, User user, Player player) {
        int userId = user.getId(player.getUniqueId());
        int highestSortId = user.getParent().getHighestPriority(group, userId);
        GroupColor groupColor = group.getColor();
        GroupColorType groupType = GroupColorType.TAB;

        Optional<Integer> groupId = user.getParent().getParentIds(userId).stream()
                .filter(id -> highestSortId == group.getPriority(id))
                .findFirst();

        if (groupId.isPresent()) {
            int id = groupId.get();
            String prefix = groupColor.getPrefix(id, groupType);
            String suffix = groupColor.getSuffix(id, groupType);
            String color = groupColor.getColor(id, groupType);

            Component baseComponent = MINI_MESSAGE.deserialize(prefix + color + player.getName() + suffix);
            player.playerListName(baseComponent);
        }
    }

    @SuppressWarnings("deprecation")
    private static void setPlayerTeams(Group group, User user, Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        GroupColorType groupType = GroupColorType.TEAM;
        GroupColor groupColor = group.getColor();

        Map<String, Team> existingTeams = new HashMap<>();
        for (Team team : scoreboard.getTeams())
            existingTeams.put(team.getName(), team);

        Map<Integer, List<String>> playersBySortId = new HashMap<>();
        for (Player target : player.getServer().getOnlinePlayers()) {
            int userId = user.getId(target.getUniqueId());
            int highestSortId = user.getParent().getHighestPriority(group, userId);
            playersBySortId.computeIfAbsent(highestSortId, k -> new ArrayList<>()).add(target.getName());
        }

        for (String groupName : group.getGroupNames()) {
            int groupId = group.getId(groupName);
            int groupSortId = group.getPriority(groupId);
            String name = group.getTeamSort(groupId);

            Team team = existingTeams.get(name);
            if (team == null) team = scoreboard.registerNewTeam(name);

            String prefix = groupColor.getPrefix(groupId, groupType);
            String suffix = groupColor.getSuffix(groupId, groupType);
            String color = groupColor.getColor(groupId, groupType);

            if (prefix == null) prefix = "";
            if (suffix == null) suffix = "";
            if (color == null) color = "7";

            if (!prefix.equals(team.getPrefix())) team.prefix(MINI_MESSAGE.deserialize(prefix));
            if (!suffix.equals(team.getSuffix())) team.suffix(MINI_MESSAGE.deserialize(suffix));

            ChatColor chatColor = ChatColor.getByChar(color.replace("§", "").replace("&", ""));
            if (chatColor != null && !chatColor.equals(team.getColor()))
                team.setColor(Objects.requireNonNull(chatColor));

            List<String> playerNames = playersBySortId.get(groupSortId);
            if (playerNames != null)
                for (String playerName : playerNames)
                    team.addEntry(playerName);
        }
    }
}
