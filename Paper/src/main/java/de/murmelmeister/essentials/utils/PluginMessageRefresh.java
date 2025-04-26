package de.murmelmeister.essentials.utils;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class PluginMessageRefresh implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(MurmelEssentials.CHANNEL)) return;
        String cacheName = BufferUtils.decodeUTF(message);
        RefreshUtil.markAsRefreshed(cacheName);
    }
}
