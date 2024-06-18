package de.murmelmeister.essentials.api;

import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;

import java.util.concurrent.TimeUnit;

public final class PlayTimeUpdater {
    public static void startTimer(ProxyServer server, MurmelEssentials instance) {
        var user = instance.getUser();
        var playTime = instance.getPlayTime();
        server.getScheduler().buildTask(instance, () -> updateTimer(server, user, playTime)).repeat(1L, TimeUnit.SECONDS).schedule();
    }

    private static void updateTimer(ProxyServer server, User user, PlayTime playTime) {
        for (var player : server.getAllPlayers()) {
            var userId = user.getId(player.getUniqueId());
            playTime.timer(userId);
        }
    }
}
