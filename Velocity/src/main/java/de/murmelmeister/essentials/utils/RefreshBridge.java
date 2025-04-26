package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

public final class RefreshBridge {
    private final MurmelEssentials plugin;
    private final ProxyServer server;

    public RefreshBridge(MurmelEssentials plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
        RefreshUtil.register(this::broadcastToBackends);
    }

    private void broadcastToBackends(String cacheName) {
        byte[] data = BufferUtils.encodeUTF(cacheName);
        server.getAllServers()
                .forEach(registeredServer -> registeredServer.sendPluginMessage(plugin.getChannel(), data));
    }
}
