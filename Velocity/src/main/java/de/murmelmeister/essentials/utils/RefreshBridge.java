package de.murmelmeister.essentials.utils;

import com.google.gson.*;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshOrigin;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.slf4j.Logger;

public final class RefreshBridge {
    private final ProxyServer server;
    private final ChannelIdentifier channel;
    private final RefreshProvider refreshProvider;
    private final Logger logger;
    private final Gson gson;
    private final RefreshListener refreshListener;

    public RefreshBridge(ProxyServer server, ChannelIdentifier channel, RefreshProvider refreshProvider, Logger logger, Gson gson) {
        this.server = server;
        this.channel = channel;
        this.refreshProvider = refreshProvider;
        this.logger = logger;
        this.gson = gson;
        this.refreshListener = this::broadcastToBackends;
    }

    public void register() {
        refreshProvider.register(refreshListener);
    }

    public void unregister() {
        refreshProvider.unregister(refreshListener);
    }

    private void broadcastToBackends(RefreshEvent<?> event) {
        if (event.origin() == RefreshOrigin.REMOTE) return;

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", event.type());
        if (event.key() != null)
            jsonObject.add("key", gson.toJsonTree(event.key()));
        String json = gson.toJson(jsonObject);
        logger.info("Broadcasting plugin message: {}", json); // TODO: remove this later
        byte[] data = BufferUtils.encodeUTF(json);
        server.getAllServers()
                .forEach(registeredServer -> registeredServer.sendPluginMessage(channel, data));
    }
}
