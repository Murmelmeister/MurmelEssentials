package de.murmelmeister.essentials;

import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.files.MySQL;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.PluginMessageRefresh;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.plugin.java.JavaPlugin;

public final class MurmelEssentials extends JavaPlugin {
    private final MySQL mySQL;
    private static final PluginMessageRefresh PLUGIN_MESSAGE_REFRESH = new PluginMessageRefresh();

    @Override
    public void onDisable() {
        mySQL.disconnect();
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "permission:refresh", PLUGIN_MESSAGE_REFRESH);
    }

    @Override
    public void onEnable() {
        mySQL.connect();
        ListenerManager.register(this);
        Ranks.updatePlayers(this, getServer());
        getServer().getMessenger().registerIncomingPluginChannel(this, "permission:refresh", PLUGIN_MESSAGE_REFRESH);
    }

    public MurmelEssentials() {
        this.mySQL = new MySQL(getSLF4JLogger());
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
