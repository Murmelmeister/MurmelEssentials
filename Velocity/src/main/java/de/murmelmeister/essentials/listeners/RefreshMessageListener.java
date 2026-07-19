package de.murmelmeister.essentials.listeners;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshOrigin;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.slf4j.Logger;

public final class RefreshMessageListener {
    private final MurmelEssentials plugin;
    private final Logger logger;
    private final ProxyServer server;
    private final RefreshProvider refreshProvider;
    private final Gson gson;

    public RefreshMessageListener(MurmelEssentials plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.server = plugin.getServer();
        this.refreshProvider = plugin.getRefreshProvider();
        this.gson = plugin.getGson();
    }

    @Subscribe
    public void handlePluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(plugin.getChannel())) return;
        if (!(event.getSource() instanceof ServerConnection connection)) return;

        byte[] data = event.getData();
        String json = BufferUtils.decodeUTF(data);

        // Validate JSON format
        JsonObject jsonObject;
        try {
            jsonObject = gson.fromJson(json, JsonObject.class);
        } catch (JsonSyntaxException e) {
            logger.warn("Invalid JSON received in plugin message: {}", e.getMessage());
            return;
        }

        if (jsonObject == null) {
            logger.warn("Received null JSON object from plugin message.");
            return;
        }

        // Check for required fields and their conditions
        int jsonSize = jsonObject.size();
        boolean hasType = jsonObject.has("type");
        boolean hasKey = jsonObject.has("key") && !jsonObject.get("key").isJsonNull();

        if (!(hasType && (jsonSize == 1 || (hasKey && jsonSize == 2)))) {
            logger.warn("Received plugin message with unexpected fields: {}", jsonObject);
            return;
        }

        // Process the refresh event
        String type = jsonObject.get("type").getAsString();
        if (hasKey) {
            String key = gson.toJson(jsonObject.get("key"));
            refreshProvider.fireSingle(type, key, RefreshOrigin.REMOTE);
        } else
            refreshProvider.fireCache(type, RefreshOrigin.REMOTE);

        // Broadcast the message to all other servers (without the sender's server)
        String senderServerName = connection.getServer().getServerInfo().getName();
        server.getAllServers().stream()
                .filter(registeredServer -> !registeredServer.getServerInfo().getName().equals(senderServerName))
                .forEach(registeredServer -> registeredServer.sendPluginMessage(plugin.getChannel(), data));
    }
}
