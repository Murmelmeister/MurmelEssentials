package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.nio.ByteBuffer;

public final class CacheInvalidateListener {
    private final MurmelEssentials plugin;

    public CacheInvalidateListener(MurmelEssentials plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void handlePluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(plugin.getChannel())) return;

        ByteBuffer buffer = ByteBuffer.wrap(event.getData());
        String cacheName = BufferUtils.readUTF(buffer);

        RefreshUtil.markAsRefreshed(cacheName);
        plugin.broadcastToBackends(event.getData());
    }
}
