package de.murmelmeister.essentials.api;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProvider;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public final class PlayTimeUpdater {
    private static ScheduledTask task;

    private static void cancelExistingTask() {
        if (task != null && task.status() != TaskStatus.CANCELLED)
            task.cancel();
    }

    private static void updateTimer(Logger logger, ProxyServer server, UserProvider userProvider, UserService userService, UserPlayTimeProvider playTimeProvider) {
        for (Player player : server.getAllPlayers()) {
            User user = userProvider.findByMojangId(player.getUniqueId());
            if (user == null) {
                logger.warn("User not found for player: {}", player.getUsername());
                continue;
            }

            UserPlayTime playTime = playTimeProvider.findByUserId(user.id());
            if (playTime == null) {
                logger.warn("PlayTime not found for user: {}", user.username());
                continue;
            }

            playTimeProvider.incrementPlayTime(playTime);
            userService.checkLoginStreakWhileOnline(user.id(), playTime);
        }
    }

    public static void startTimer(MurmelEssentials plugin, Logger logger, ProxyServer server) {
        cancelExistingTask();
        UserProvider userProvider = plugin.getUserProvider();
        UserService userService = plugin.getUserService();
        UserPlayTimeProvider playTimeProvider = plugin.getUserPlayTimeProvider();
        task = server.getScheduler().buildTask(plugin, () -> updateTimer(logger, server, userProvider, userService, playTimeProvider))
                .repeat(1L, TimeUnit.SECONDS).schedule();
    }
}
