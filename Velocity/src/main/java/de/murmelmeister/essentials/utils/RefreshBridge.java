package de.murmelmeister.essentials.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

public final class RefreshBridge {
    private final ProxyServer server;
    private final ChannelIdentifier channel;
    private final Gson gson = new Gson();

    public RefreshBridge(ProxyServer server, ChannelIdentifier channel) {
        this.server = server;
        this.channel = channel;
    }

    public void register() {
        RefreshUtil.register(this::broadcastToBackends);
    }

    public void unregister() {
        RefreshUtil.unregister(this::broadcastToBackends);
    }

    private void broadcastToBackends(RefreshEvent<?> event) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", event.getType());
        if (event.getKey() != null)
            jsonObject.addProperty("key", event.getKey().toString());
        String json = gson.toJson(jsonObject);
        byte[] data = BufferUtils.encodeUTF(json);
        server.getAllServers()
                .forEach(registeredServer -> registeredServer.sendPluginMessage(channel, data));
    }
}
