package de.murmelmeister.essentials.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.events.ReceiveRefreshEvent;
import de.murmelmeister.murmelapi.utils.BufferUtils;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class PluginMessageRefresh implements PluginMessageListener {
    private final Gson gson;
    private final RefreshProvider refreshProvider;

    public PluginMessageRefresh(Gson gson, RefreshProvider refreshProvider) {
        this.gson = gson;
        this.refreshProvider = refreshProvider;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(MurmelEssentials.CHANNEL)) return;

        Server server = player.getServer();
        String json = BufferUtils.decodeUTF(message);

        // Validate JSON format
        JsonObject jsonObject;
        try {
            jsonObject = gson.fromJson(json, JsonObject.class);
        } catch (JsonSyntaxException e) {
            server.sendMessage(Component.text("Invalid JSON received in plugin message: " + e.getMessage()));
            return;
        }

        if (jsonObject == null) {
            server.sendMessage(Component.text("Received null JSON object from plugin message."));
            return;
        }

        // Check for required fields and their conditions
        int jsonSize = jsonObject.size();
        boolean hasType = jsonObject.has("type");
        boolean hasKey = jsonObject.has("key") && !jsonObject.get("key").isJsonNull();

        if (!(hasType && (jsonSize == 1 || (hasKey && jsonSize == 2)))) {
            server.sendMessage(Component.text("Received plugin message with unexpected fields: " + jsonObject));
            return;
        }

        // Process the refresh event
        String type = jsonObject.get("type").getAsString();
        if (hasKey) {
            String key = gson.toJson(jsonObject.get("key"));
            refreshProvider.fireSingle(type, key);
            server.getPluginManager().callEvent(new ReceiveRefreshEvent(player, type, key));
        } else {
            refreshProvider.fireCache(type);
            server.getPluginManager().callEvent(new ReceiveRefreshEvent(player, type));
        }
    }
}
