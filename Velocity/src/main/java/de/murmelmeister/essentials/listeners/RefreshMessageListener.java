package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

public final class RefreshMessageListener {
    private final MurmelEssentials plugin;
    private final ProxyServer server;

    public RefreshMessageListener(MurmelEssentials plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Subscribe
    public void handlePluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(plugin.getChannel())) return;
        if (!(event.getSource() instanceof ServerConnection connection)) return;

        byte[] data = event.getData();
        String cacheName = BufferUtils.decodeUTF(data);

        RefreshUtil.markAsRefreshed(cacheName);
        server.getAllServers().stream()
                .filter(registeredServer -> !registeredServer.getServerInfo().getName().equals(connection.getServer().getServerInfo().getName()))
                .forEach(registeredServer -> registeredServer.sendPluginMessage(plugin.getChannel(), data));
    }
}
