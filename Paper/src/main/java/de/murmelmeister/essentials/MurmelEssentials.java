package de.murmelmeister.essentials;

import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.redis.RedisConfig;
import de.murmelmeister.essentials.redis.RedisRefreshBridge;
import de.murmelmeister.essentials.events.ReceiveRefreshEvent;
import de.murmelmeister.essentials.api.menu.MenuProvider;
import de.murmelmeister.essentials.commands.command.TestCommand;
import de.murmelmeister.essentials.configs.DatabaseConfig;
import de.murmelmeister.essentials.configs.PluginConfig;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.ConfigValue;
import de.murmelmeister.essentials.utils.PluginMessageRefresh;
import de.murmelmeister.essentials.utils.RefreshBridge;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.clan.ClanProvider;
import de.murmelmeister.murmelapi.clan.member.ClanMemberProvider;
import de.murmelmeister.murmelapi.color.PrefixColorProvider;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAuditProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddressProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUserProvider;
import de.murmelmeister.murmelapi.settings.SettingsService;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.format.DateTimeFormatter;

public final class MurmelEssentials extends JavaPlugin {
    public static final String CHANNEL = "murmel:main";
    public static final String PLUGIN_PATH = "./plugins/" + MurmelEssentials.class.getSimpleName() + "/";

    private final PluginConfig pluginConfig;
    private final MurmelAPI murmelAPI;
    private final PluginMessageRefresh pluginMessageRefresh;
    private final RefreshBridge refreshBridge;
    private final DatabaseConfig databaseConfig;
    private final Ranks ranks;
    private RedisRefreshBridge redisBridge;
    private boolean redisEnabled;

    public MurmelEssentials() {
        this.pluginConfig = new PluginConfig(getDataFolder().toPath());
        this.murmelAPI = new MurmelAPI();
        this.pluginMessageRefresh = new PluginMessageRefresh(murmelAPI.getGson(), getRefreshProvider());
        this.refreshBridge = new RefreshBridge(this, getServer(), getRefreshProvider(), getSLF4JLogger(), murmelAPI.getGson());
        this.databaseConfig = new DatabaseConfig(MurmelEssentials.class.getSimpleName(), murmelAPI);
        this.ranks = new Ranks(this);
    }

    @Override
    public void onDisable() {
        if (redisBridge != null) redisBridge.close();
        MenuProvider.unregisterAll();
        ranks.cancelTask();
        ranks.close();
        databaseConfig.disconnect();

        if (!redisEnabled && !pluginConfig.getBoolean(ConfigValue.VELOCITY_SUPPORT))
            refreshBridge.unregister();

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, pluginMessageRefresh);
    }

    @Override
    public void onEnable() {
        databaseConfig.connect();
        ListenerManager.register(this);
        if (isFolia()) ranks.updatePlayersFolia(this, getServer());
        else ranks.updatePlayers(this, getServer());
        RedisConfig redisConfig = RedisConfig.load(getDataFolder().toPath());
        redisEnabled = redisConfig.enabled();
        if (redisEnabled) {
            redisBridge = new RedisRefreshBridge(redisConfig, getRefreshProvider(), getSLF4JLogger(),
                    murmelAPI.getGson(), message -> runServerTask(() -> {
                    if (isEnabled()) getServer().getPluginManager().callEvent(
                            new ReceiveRefreshEvent(message.type(), message.keyJson(murmelAPI.getGson())));
                }));
            redisBridge.register();
        } else {
            getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, pluginMessageRefresh);
            getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
            if (!pluginConfig.getBoolean(ConfigValue.VELOCITY_SUPPORT)) refreshBridge.register();
        }

        //MurmelCommand.register(this, getSLF4JLogger());
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(new TestCommand(this).command().build());
        });
    }

    private void runServerTask(Runnable task) {
        if (!isEnabled()) return;
        if (isFolia()) getServer().getGlobalRegionScheduler().execute(this, task);
        else getServer().getScheduler().runTask(this, task);
    }

    public MurmelEssentials getInstance() {
        return getPlugin(MurmelEssentials.class);
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public GroupProvider getGroupProvider() {
        return murmelAPI.getGroupProvider();
    }

    public GroupColorProvider getGroupColorProvider() {
        return murmelAPI.getGroupColorProvider();
    }

    public UserProvider getUserProvider() {
        return murmelAPI.getUserProvider();
    }

    public PermissionService getPermissionService() {
        return murmelAPI.getPermissionService();
    }

    public Ranks getRanks() {
        return ranks;
    }

    public DateTimeFormatter getDateTimeFormatter(int languageId) {
        return murmelAPI.getDateTimeFormatter(languageId);
    }

    public PunishmentAuditProvider getPunishmentAuditProvider() {
        return murmelAPI.getPunishAuditProvider();
    }

    public PunishmentUserProvider getPunishmentUserProvider() {
        return murmelAPI.getPunishUserProvider();
    }

    public PunishmentIpAddressProvider getPunishmentIpProvider() {
        return murmelAPI.getPunishIpAddressProvider();
    }

    public PunishmentService getPunishmentService() {
        return murmelAPI.getPunishmentService();
    }

    public SettingsService getSettingsService() {
        return murmelAPI.getSettingsService();
    }

    public RefreshProvider getRefreshProvider() {
        return murmelAPI.getRefreshProvider();
    }

    public PrefixColorProvider getPrefixColorProvider() {
        return murmelAPI.getPrefixColorProvider();
    }

    public UserPrefixColorProvider getUserPrefixColorProvider() {
        return murmelAPI.getUserPrefixColorProvider();
    }

    public ClanProvider getClanProvider() {
        return murmelAPI.getClanProvider();
    }

    public ClanMemberProvider getClanMemberProvider() {
        return murmelAPI.getClanMemberProvider();
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
