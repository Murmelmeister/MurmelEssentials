package de.murmelmeister.essentials;

import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.configurations.Config;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.PluginMessageRefresh;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.format.DateTimeFormatter;

public final class MurmelEssentials extends JavaPlugin {
    public static final String CHANNEL = "murmel:main";
    private static final PluginMessageRefresh PLUGIN_MESSAGE_REFRESH = new PluginMessageRefresh();
    public static final String PLUGIN_PATH = "./plugins/" + MurmelEssentials.class.getSimpleName() + "/";

    private final Config config;
    private final Ranks ranks;

    public MurmelEssentials() {
        this.config = new Config();
        this.ranks = new Ranks(this);
    }

    @Override
    public void onDisable() {
        ranks.cancelTask();
        ranks.close();
        config.disconnectFromDatabase();
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, PLUGIN_MESSAGE_REFRESH);
    }

    @Override
    public void onEnable() {
        config.connectToDatabase();
        ListenerManager.register(this);
        ranks.updatePlayers(this, getServer());
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, PLUGIN_MESSAGE_REFRESH);
    }

    public MurmelEssentials getInstance() {
        return getPlugin(MurmelEssentials.class);
    }

    public GroupProvider getGroupProvider() {
        return MurmelAPI.getGroupProvider();
    }

    public GroupColorProvider getGroupColorProvider() {
        return MurmelAPI.getGroupColorProvider();
    }

    public UserProvider getUserProvider() {
        return MurmelAPI.getUserProvider();
    }

    public UserParentProvider getUserParentProvider() {
        return MurmelAPI.getUserParentProvider();
    }

    public Permission getPermission() {
        return MurmelAPI.getPermission();
    }

    public Ranks getRanks() {
        return ranks;
    }

    public DateTimeFormatter getDateTimeFormatter(int languageId) {
        return MurmelAPI.getDateTimeFormatter(languageId);
    }

    public PunishmentLogProvider getPunishmentLogProvider() {
        return MurmelAPI.getPunishmentLogProvider();
    }

    public PunishmentCurrentUserProvider getPunishmentUserProvider() {
        return MurmelAPI.getPunishmentCurrentUserProvider();
    }

    public PunishmentCurrentIpProvider getPunishmentIpProvider() {
        return MurmelAPI.getPunishmentCurrentIpProvider();
    }

    public PunishmentService getPunishmentService() {
        return MurmelAPI.getPunishmentService();
    }
}
