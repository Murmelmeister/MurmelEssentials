package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.configurations.Config;
import de.murmelmeister.library.utils.AnimationUtils;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.utils.TimeFilterUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static de.murmelmeister.murmelapi.MurmelAPI.DEFAULT_GROUP_ID;

public class TablistUtil {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final MurmelEssentials plugin;
    private final Config config;
    private final Logger logger;
    private final ProxyServer server;

    private ScheduledTask task;
    private final AtomicLong tickCounter = new AtomicLong(0);

    public TablistUtil(MurmelEssentials plugin, Config config, Logger logger, ProxyServer server) {
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
        this.server = server;
    }

    private final List<String> patternNames = List.of(
            "<#3A2EF4>", "<#432EF4>", "<#4C2EF4>", "<#552EF4>", "<#5E2EF4>",
            "<#672EF4>", "<#702EF4>", "<#792EF4>", "<#822EF4>", "<#8B2EF4>",
            "<#8B2EF4>", "<#822EF4>", "<#792EF4>", "<#702EF4>", "<#672EF4>",
            "<#5E2EF4>", "<#552EF4>", "<#4C2EF4>", "<#432EF4>", "<#3A2EF4>"
    );

    private void setTablist(Player player) {
        String test = AnimationUtils.animatePerColorCycle(patternNames, "ByteMiner", 3.00f);
        User user = plugin.getUserProvider().findByMojangId(player.getUniqueId());
        if (user == null) return;
        UserPlayTime playTime = plugin.getUserPlayTimeProvider().findByUserId(user.id());
        if (playTime == null) return;
        String time = TimeUtil.formatDuration(plugin.getMessageService(), user.languageId(), playTime.getPlayTime(), TimeFilterUtil.SECONDS);
        player.sendPlayerListHeaderAndFooter(
                //miniMessage.deserialize(test + " » " + config.geTablistHeader()),
                miniMessage.deserialize(config.geTablistHeader()),
                miniMessage.deserialize(config.getTablistFooter().replace("[TIME]", time))
        );
    }

    private void setPlayerList(Player player) {
        GroupColorProvider groupColorProvider = plugin.getGroupColorProvider();
        if (player.getCurrentServer().isEmpty()) return;
        ServerConnection serverConnection = player.getCurrentServer().get();
        server.getAllPlayers().forEach(other -> {
            if (other.getCurrentServer().isEmpty()) return;
            ServerConnection otherServerConnection = other.getCurrentServer().get();

            if (serverConnection.getServerInfo().getName().equals(otherServerConnection.getServerInfo().getName())) {
                User user = plugin.getUserProvider().findByMojangId(other.getUniqueId());
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
                String formattedColor = color != null ? color.value() : "<gray>";

                //String serverName = "<#454545>[<#0088cc>" + otherServerConnection.getServerInfo().getName() + "</#0088cc>]</#454545> ";
                //Component baseComponent = miniMessage.deserialize(serverName + formattedColor + formattedPrefix + other.getUsername() + formattedSuffix);
                Component baseComponent = miniMessage.deserialize(formattedColor + formattedPrefix + other.getUsername() + formattedSuffix);
                player.getTabList().addEntry(
                        TabListEntry.builder()
                                .displayName(baseComponent)
                                .listOrder(group.priority())
                                .latency((int) other.getPing())
                                .profile(other.getGameProfile())
                                .showHat(true)
                                .gameMode(0)
                                .tabList(player.getTabList())
                                .build()
                );
            }
        });
    }

    private Group getHighestPriority(int userId) {
        UserParentProvider userParentProvider = plugin.getUserParentProvider();
        GroupProvider groupProvider = plugin.getGroupProvider();

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

    public void stop() {
        if (!config.getPlayerListEnable() && !config.getTablistEnable()) return;
        if (task != null && task.status() != TaskStatus.CANCELLED) {
            tickCounter.set(0);
            task.cancel();
            logger.info("Proxy tablist stopped.");
        }
    }

    public void start() {
        if (!config.getPlayerListEnable() && !config.getTablistEnable()) return;
        task = server.getScheduler().buildTask(plugin,
                        () -> {
                            long tick = tickCounter.getAndIncrement();
                            server.getAllPlayers().forEach(player -> {
                                if (config.getTablistEnable())
                                    setTablist(player);
                                if (config.getPlayerListEnable())
                                    setPlayerList(player);
                            });
                        })
                .repeat(config.getTablistRefresh(), TimeUnit.MILLISECONDS).schedule();
        logger.info("Proxy tablist started.");
    }

    public void reload() {
        stop();
        start();
    }
}
