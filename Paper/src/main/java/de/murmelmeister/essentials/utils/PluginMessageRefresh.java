package de.murmelmeister.essentials.utils;

import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class PluginMessageRefresh implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (channel.equals("permission:refresh")) {
            var msg = new String(message);
            if (msg.equals("refresh")) RefreshUtil.markAsRefreshed();
        }
    }
}
