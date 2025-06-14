package de.murmelmeister.essentials;

import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.configurations.Config;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.PluginMessageRefresh;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.plugin.java.JavaPlugin;

public final class MurmelEssentials extends JavaPlugin {
    public static final String CHANNEL = "murmel:main";
    private static final PluginMessageRefresh PLUGIN_MESSAGE_REFRESH = new PluginMessageRefresh();
    public static final String PLUGIN_PATH = "./plugins/" + MurmelEssentials.class.getSimpleName() + "/";

    private final Config config;

    @Override
    public void onDisable() {
        Ranks.cancelTask();
        config.disconnectFromDatabase();
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, PLUGIN_MESSAGE_REFRESH);
    }

    @Override
    public void onEnable() {
        config.connectToDatabase();
        ListenerManager.register(this);
        Ranks.updatePlayers(this, getServer());
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, PLUGIN_MESSAGE_REFRESH);
    }

    public MurmelEssentials() {
        this.config = new Config();
    }

    public MurmelEssentials getInstance() {
        return this;
    }

    public Group getGroup() {
        return MurmelAPI.getGroup();
    }

    public User getUser() {
        return MurmelAPI.getUser();
    }

    public Permission getPermission() {
        return MurmelAPI.getPermission();
    }
}
