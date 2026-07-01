package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.configs.settings.Config;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.stats.UserStats;
import de.murmelmeister.murmelapi.utils.TimeFilterUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TablistUtil {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final MurmelEssentials plugin;
    private final Logger logger;
    private final ProxyServer server;
    private final MessageService messageService;

    private ScheduledTask task;
    private final AtomicLong tickCounter = new AtomicLong(0);

    public TablistUtil(MurmelEssentials plugin, Logger logger, ProxyServer server) {
        this.plugin = plugin;
        this.logger = logger;
        this.server = server;
        this.messageService = plugin.getMessageService();
    }

    private final List<String> patternNames = List.of(
            "<#3A2EF4>", "<#432EF4>", "<#4C2EF4>", "<#552EF4>", "<#5E2EF4>",
            "<#672EF4>", "<#702EF4>", "<#792EF4>", "<#822EF4>", "<#8B2EF4>",
            "<#8B2EF4>", "<#822EF4>", "<#792EF4>", "<#702EF4>", "<#672EF4>",
            "<#5E2EF4>", "<#552EF4>", "<#4C2EF4>", "<#432EF4>", "<#3A2EF4>"
    );

    private void setTablist(Player player) {
        //String test = AnimationUtils.animatePerColorCycle(patternNames, "ByteMiner", 3.00f);
        User user = plugin.getUserProvider().findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) return;
        UserStats userStats = plugin.getUserStatsProvider().findByUserId(user.id()).orElse(null);
        if (userStats == null) return;
        long playTime = userStats.currentPlayTime();
        String time = TimeUtil.formatDuration(plugin.getMessageService(), user.languageId(), playTime, TimeFilterUtil.SECONDS);
        player.sendPlayerListHeaderAndFooter(
                //miniMessage.deserialize(test + " » " + config.geTablistHeader()),
                miniMessage.deserialize(messageService.getMessage(Message.CONFIG_TABLIST_HEADER.getTag(), user.languageId())),
                miniMessage.deserialize(messageService.getMessage(Message.CONFIG_TABLIST_FOOTER.getTag(), user.languageId()), Placeholder.parsed("time", time))
        );
    }

    public void stop(Config config) {
        if (!config.tablistEnable()) return;
        if (task != null && task.status() != TaskStatus.CANCELLED) {
            tickCounter.set(0);
            task.cancel();
            logger.info("Proxy tablist stopped.");
        }
    }

    public void start(Config config) {
        if (!config.tablistEnable()) return;
        task = server.getScheduler().buildTask(plugin,
                        () -> {
                            long tick = tickCounter.getAndIncrement();
                            server.getAllPlayers().forEach(this::setTablist);
                        })
                .repeat(config.tablistRefresh(), TimeUnit.MILLISECONDS).schedule();
        logger.info("Proxy tablist started.");
    }

    public void reload(Config config) {
        stop(config);
        start(config);
    }
}
