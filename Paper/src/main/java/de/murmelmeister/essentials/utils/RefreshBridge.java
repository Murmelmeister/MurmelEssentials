package de.murmelmeister.essentials.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshOrigin;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

public final class RefreshBridge {
    private final Plugin plugin;
    private final Server server;
    private final RefreshProvider refreshProvider;
    private final Logger logger;
    private final Gson gson;
    private final RefreshListener refreshListener;

    public RefreshBridge(Plugin plugin, Server server, RefreshProvider refreshProvider, Logger logger, Gson gson) {
        this.plugin = plugin;
        this.server = server;
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

        server.sendPluginMessage(plugin, MurmelEssentials.CHANNEL, data);
    }
}
