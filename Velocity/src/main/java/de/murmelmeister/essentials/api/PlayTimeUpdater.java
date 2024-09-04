package de.murmelmeister.essentials.api;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;

import java.util.concurrent.TimeUnit;

public final class PlayTimeUpdater {
    private static ScheduledTask task;

    public static void startTimer(ProxyServer server, MurmelEssentials instance) {
        if (task != null && task.status() != TaskStatus.CANCELLED) task.cancel();
        User user = instance.getUser();
        PlayTime playTime = instance.getPlayTime();
        task = server.getScheduler().buildTask(instance, () -> updateTimer(server, user, playTime)).repeat(1L, TimeUnit.SECONDS).schedule();
    }

    private static void updateTimer(ProxyServer server, User user, PlayTime playTime) {
        for (Player player : server.getAllPlayers()) {
            int userId = user.getId(player.getUniqueId());
            playTime.timer(userId);
        }
    }
}
