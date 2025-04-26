package de.murmelmeister.essentials.utils;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public final class PluginMessageRefresh implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(MurmelEssentials.CHANNEL)) return;
        ByteBuffer buffer = ByteBuffer.wrap(message);
        String cacheName = BufferUtils.readUTF(buffer);
        RefreshUtil.markAsRefreshed(cacheName);
    }
}
