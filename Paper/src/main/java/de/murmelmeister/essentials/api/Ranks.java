package de.murmelmeister.essentials.api;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.ChatFormatter;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.murmelmeister.murmelapi.group.GroupProviderImpl.DEFAULT_GROUP_ID;

public final class Ranks implements AutoCloseable {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final AtomicBoolean hasUpdated = new AtomicBoolean(false);
    private BukkitTask task;

    private final GroupProvider groupProvider;
    private final GroupColorProvider groupColorProvider;
    private final UserProvider userProvider;
    private final UserParentProvider userParentProvider;

    public Ranks(MurmelEssentials instance) {
        this.groupProvider = instance.getGroupProvider();
        this.groupColorProvider = instance.getGroupColorProvider();
        this.userProvider = instance.getUserProvider();
        this.userParentProvider = instance.getUserParentProvider();
        RefreshUtil.register(cacheName -> hasUpdated.set(true));
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled())
            task.cancel();
    }

    public void updatePlayers(MurmelEssentials instance, Server server) {
        task = server.getScheduler().runTaskTimerAsynchronously(instance, () -> {
            if (hasUpdated.get()) {
                for (Player player : server.getOnlinePlayers()) {
                    setPlayerTeams(player);
                    setPlayerListName(player);
                    player.updateCommands(); // Update the player commands
                }
                hasUpdated.set(false);
            }
        }, 10L, 20L);
    }

    public void setChatFormat(AsyncChatEvent event) {
        // Get the user
        Player player = event.getPlayer();
        User user = userProvider.findByMojangId(player.getUniqueId());
        if (user == null) return;
        int userId = user.id();

        // Get the highest priority group for the user
        Group group = getHighestPriority(userId);
        if (group == null) return;
        int groupId = group.id();

        // Get the group colors
        GroupColor prefix = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_PREFIX.getId());
        GroupColor suffix = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_SUFFIX.getId());
        GroupColor color = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_COLOR.getId());
        GroupColor chatMessage = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_MESSAGE.getId());

        // Format the chat message
        String formattedPrefix = prefix != null ? prefix.value() : "";
        String formattedSuffix = suffix != null ? suffix.value() : "";
        String formattedColor = color != null ? "<" + color.value()
                .replace("<", "")
                .replace(">", "") + ">" : "<gray>";
        String formattedColorMessage = chatMessage != null ? chatMessage.value() : " » ";

        String format = formattedColor + formattedPrefix + player.getName() + formattedSuffix + "<reset>";
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component component = miniMessage.deserialize(format);
            String finalMessage = PlainTextComponentSerializer.plainText().serialize(message);
            Component messageComponent = ChatFormatter.format(player, finalMessage, formattedColorMessage);
            return component.append(messageComponent);
        });
    }

    private void setPlayerListName(Player player) {
        // Get the user
        User user = userProvider.findByMojangId(player.getUniqueId());
        if (user == null) return;
        int userId = user.id();

        // Get the highest priority group for the user
        Group group = getHighestPriority(userId);
        if (group == null) return;
        int groupId = group.id();

        // Get the group colors
        GroupColor prefix = groupColorProvider.getGroupColor(groupId, GroupColorType.TAB_PREFIX.getId());
        GroupColor suffix = groupColorProvider.getGroupColor(groupId, GroupColorType.TAB_SUFFIX.getId());
        GroupColor color = groupColorProvider.getGroupColor(groupId, GroupColorType.TAB_COLOR.getId());

        // Format the player list name
        String formattedPrefix = prefix != null ? prefix.value() : "";
        String formattedSuffix = suffix != null ? suffix.value() : "";
        String formattedColor = color != null ? "<" + color.value()
                .replace("<", "")
                .replace(">", "") + ">" : "<gray>";

        Component baseComponent = miniMessage.deserialize(formattedColor + formattedPrefix + player.getName() + formattedSuffix);
        player.playerListName(baseComponent);

    }

    private void setPlayerTeams(Player player) {
        User user = userProvider.findByMojangId(player.getUniqueId());
        if (user == null) return;
        Group userGroup = getHighestPriority(user.id());
        if (userGroup == null) return;
        Scoreboard scoreboard = player.getScoreboard();

        Map<String, Team> existingTeams = new HashMap<>();
        for (Team team : scoreboard.getTeams())
            existingTeams.put(team.getName(), team);

        Map<Integer, List<String>> playersBySortId = new HashMap<>();
        for (Player target : player.getServer().getOnlinePlayers())
            playersBySortId.computeIfAbsent(userGroup.id(), k -> new ArrayList<>()).add(target.getName());


        for (Group group : groupProvider.findAll()) {
            int groupId = group.id();
            int priority = group.priority();
            String teamTagId = group.teamTagId();

            Team team = existingTeams.get(teamTagId);
            if (team == null) team = scoreboard.registerNewTeam(teamTagId);

            GroupColor prefix = groupColorProvider.getGroupColor(groupId, GroupColorType.TEAM_PREFIX.getId());
            GroupColor suffix = groupColorProvider.getGroupColor(groupId, GroupColorType.TEAM_SUFFIX.getId());
            GroupColor color = groupColorProvider.getGroupColor(groupId, GroupColorType.TEAM_COLOR.getId());

            String formattedPrefix = prefix != null ? prefix.value() : "";
            String formattedSuffix = suffix != null ? suffix.value() : "";
            String formattedColor = color != null ? color.value() : "gray";
            NamedTextColor textColor = NamedTextColor.NAMES.value(formattedColor.toLowerCase());

            if (!formattedPrefix.equals(miniMessage.serialize(team.prefix())))
                team.prefix(miniMessage.deserialize(formattedPrefix));
            if (!formattedSuffix.equals(miniMessage.serialize(team.suffix())))
                team.suffix(miniMessage.deserialize(formattedSuffix));
            if (textColor != null)
                team.color(textColor);

            List<String> playerNames = playersBySortId.get(priority);
            if (playerNames != null)
                for (String playerName : playerNames)
                    team.addEntry(playerName);
        }
    }

    private Group getHighestPriority(int userId) {
        List<Integer> parentIds = userParentProvider.getParents(userId).stream()
                .map(UserParent::parentId)
                .toList();

        if (parentIds.isEmpty())
            return groupProvider.findById(DEFAULT_GROUP_ID);

        return parentIds.stream()
                .map(groupProvider::findById)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(Group::priority))
                .map(Group::id)
                .map(groupProvider::findById)
                .orElse(groupProvider.findById(DEFAULT_GROUP_ID));
    }

    @Override
    public void close() {
        RefreshUtil.unregister(cacheName -> hasUpdated.set(true));
    }
}
