package de.murmelmeister.essentials.api;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public final class PlayTimeUpdater {
    private static ScheduledTask task;

    private static void cancelExistingTask() {
        if (task != null && task.status() != TaskStatus.CANCELLED)
            task.cancel();
    }

    private static void updateTimer(ProxyServer server, Logger logger, User user, PlayTime playTime) {
        for (Player player : server.getAllPlayers()) {
            if (!user.existsUser(player.getUniqueId())) {
                logger.warn("Player {} doesn't exist in users table.", player.getUsername());
                continue;
            }

            int userId = user.getId(player.getUniqueId());
            playTime.addTime(userId);
        }
    }

    public static void startTimer(MurmelEssentials plugin, ProxyServer server, Logger logger) {
        cancelExistingTask();
        User user = plugin.getUser();
        PlayTime playTime = plugin.getPlayTime();
        task = server.getScheduler().buildTask(playTime, () -> updateTimer(server, logger, user, playTime))
                .repeat(1L, TimeUnit.SECONDS).schedule();
    }
}
