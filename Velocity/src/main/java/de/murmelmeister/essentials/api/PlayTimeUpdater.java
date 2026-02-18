package de.murmelmeister.essentials.api;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.stats.UserStats;
import de.murmelmeister.murmelapi.user.stats.UserStatsProvider;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public final class PlayTimeUpdater {
    private static ScheduledTask task;

    private static void cancelExistingTask() {
        if (task != null && task.status() != TaskStatus.CANCELLED)
            task.cancel();
    }

    private static void updateTimer(Logger logger, ProxyServer server, UserProvider userProvider, UserService userService, UserStatsProvider userStatsProvider) {
        for (Player player : server.getAllPlayers()) {
            User user = userProvider.findByMojangId(player.getUniqueId());
            if (user == null) {
                logger.warn("User not found for player: {}", player.getUsername());
                continue;
            }

            UserStats userStats = userStatsProvider.findByUserId(user.id());
            if (userStats == null) {
                logger.warn("UserStats not found for user: {}", user.username());
                continue;
            }

            int playTime = userStats.playTime() + 300; // 5 minutes
            UserStats updatedStats = userStatsProvider.update(user.id(), playTime, userStats.dailyStreak(), userStats.dailyStreakLastDay(), userStats.lastSeenAt());
            if (updatedStats == null) {
                logger.warn("Failed to update playtime for user: {}", user.username());
                continue;
            }

            userService.checkLoginStreakWhileOnline(user.id(), updatedStats);
        }
    }

    public static void startTimer(MurmelEssentials plugin, Logger logger, ProxyServer server) {
        cancelExistingTask();
        UserProvider userProvider = plugin.getUserProvider();
        UserService userService = plugin.getUserService();
        UserStatsProvider userStatsProvider = plugin.getUserStatsProvider();
        task = server.getScheduler().buildTask(plugin, () -> updateTimer(logger, server, userProvider, userService, userStatsProvider))
                .repeat(5L, TimeUnit.MINUTES).schedule();
    }
}
