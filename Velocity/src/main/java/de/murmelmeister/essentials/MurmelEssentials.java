package de.murmelmeister.essentials;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.essentials.configs.ConfigProvider;
import de.murmelmeister.essentials.configs.DatabaseConfig;
import de.murmelmeister.essentials.configs.MessageConfig;
import de.murmelmeister.essentials.configs.settings.Config;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.*;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.clan.ClanProvider;
import de.murmelmeister.murmelapi.clan.group.ClanGroupProvider;
import de.murmelmeister.murmelapi.clan.member.ClanMemberProvider;
import de.murmelmeister.murmelapi.color.PrefixColorProvider;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.language.LanguageTypeProvider;
import de.murmelmeister.murmelapi.language.message.MessageProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.maintenance.MaintenanceProvider;
import de.murmelmeister.murmelapi.maintenance.whitelist.MaintenanceWhitelistProvider;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.parent.ParentProvider;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAuditProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddressProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUserProvider;
import de.murmelmeister.murmelapi.settings.SettingsService;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProvider;
import de.murmelmeister.murmelapi.user.excuse.UserExcuseProvider;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.user.stats.UserStatsProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import net.kyori.adventure.translation.GlobalTranslator;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Plugin(
        id = "murmelessentials",
        name = "MurmelEssentials",
        version = "0.0.3",
        description = "MurmelEssentials is a plugin that adds a lot of useful commands to your server.",
        url = "https://www.youtube.com/Murmelmeister",
        authors = {"Murmelmeister"}
)
public final class MurmelEssentials {
    private final Logger logger;
    private final ProxyServer server;

    private final MurmelAPI murmelAPI;
    private final DatabaseConfig databaseConfig;
    private final MessageConfig messageConfig;
    private Config config;
    private final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from("murmel:main");
    private final PunishmentUtil punishmentUtil;
    private RefreshBridge refreshBridge;

    public static final String BASE_PERMISSION_COMMAND = "murmel.command.";
    public static final String TEAM_MEMBER_PERMISSION = "murmel.member.team";
    public static final String PUNISHMENT_REASON_PERMISSION = "murmel.punishment.reason.";
    public static final String PUNISHMENT_IMMUNITY_PERMISSION = "murmel.punishment.immunity";
    public static final String PUNISHMENT_NOTIFY_PERMISSION = "murmel.punishment.notify";

    private final TablistUtil tablistUtil;

    @Inject
    public MurmelEssentials(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.murmelAPI = new MurmelAPI();
        this.databaseConfig = new DatabaseConfig(dataDirectory, murmelAPI);
        this.messageConfig = new MessageConfig(dataDirectory);
        databaseConfig.connect();
        murmelAPI.setupTables();

        final SettingsService settingsService = getSettingsService();
        this.config = ConfigProvider.load(settingsService);

        murmelAPI.loadMessages();
        final MessageProvider messageProvider = getMessageProvider();
        int messages = 0;
        messages += messageConfig.loadToDatabase(messageProvider,
                List.of("lang/message_en.properties", "lang/message_de.properties")
        ).length;

        MurmelMessageTranslator translator = new MurmelMessageTranslator(getMessageService());
        GlobalTranslator.translator().addSource(translator);
        logger.info("Updated {} messages.", messages);

        this.punishmentUtil = new PunishmentUtil(this);
        this.tablistUtil = new TablistUtil(this, logger, server);
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(channel); // Note: Channel registration only works at the ProxyInitializeEvent not in the constructor
        refreshBridge = new RefreshBridge(server, channel, getRefreshProvider(), logger, getGson());
        refreshBridge.register();

        CustomPermission.updatePermission(this, server);
        ListenerManager.register(this, server);
        CommandManager.register(this);
        //PlayTimeUpdater.startTimer(this, logger, server);

        if (config.autoUpdate())
            getRefreshProvider().fireAll(); // Get all cached data from the database
        tablistUtil.start(config);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        tablistUtil.stop(config);
        refreshBridge.unregister();
        server.getChannelRegistrar().unregister(channel);
        databaseConfig.disconnect();
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public void reloadTablist() {
        this.config = ConfigProvider.load(getSettingsService());
        tablistUtil.reload(config);
    }

    public DateTimeFormatter getDateTimeFormatter(int languageId) {
        return murmelAPI.getDateTimeFormatter(languageId);
    }

    public Gson getGson() {
        return murmelAPI.getGson();
    }

    public SettingsService getSettingsService() {
        return murmelAPI.getSettingsService();
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public LanguageTypeProvider getLanguageProvider() {
        return murmelAPI.getLanguageTypeProvider();
    }

    public MessageProvider getMessageProvider() {
        return murmelAPI.getMessageProvider();
    }

    public MessageService getMessageService() {
        return murmelAPI.getMessageService();
    }

    public UserProvider getUserProvider() {
        return murmelAPI.getUserProvider();
    }

    public UserStatsProvider getUserStatsProvider() {
        return murmelAPI.getUserStatsProvider();
    }

    public UserLoginProvider getUserLoginProvider() {
        return murmelAPI.getUserLoginProvider();
    }

    public UserSessionProvider getUserSessionProvider() {
        return murmelAPI.getUserSessionProvider();
    }

    public UserExcuseProvider getUserExcuseProvider() {
        return murmelAPI.getUserExcuseProvider();
    }

    public UserService getUserService() {
        return murmelAPI.getUserService();
    }

    public GroupProvider getGroupProvider() {
        return murmelAPI.getGroupProvider();
    }

    public GroupColorProvider getGroupColorProvider() {
        return murmelAPI.getGroupColorProvider();
    }

    public ParentProvider getParentProvider() {
        return murmelAPI.getParentProvider();
    }

    public PermissionProvider getPermissionProvider() {
        return murmelAPI.getPermissionProvider();
    }

    public PermissionService getPermissionService() {
        return murmelAPI.getPermissionService();
    }

    public PunishmentReasonProvider getPunishmentReasonProvider() {
        return murmelAPI.getPunishReasonProvider();
    }

    public PunishmentAuditProvider getPunishmentAuditProvider() {
        return murmelAPI.getPunishAuditProvider();
    }

    public PunishmentIpAddressProvider getPunishmentIpProvider() {
        return murmelAPI.getPunishIpAddressProvider();
    }

    public PunishmentUserProvider getPunishmentUserProvider() {
        return murmelAPI.getPunishUserProvider();
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

    public ClanGroupProvider getClanGroupProvider() {
        return murmelAPI.getClanGroupProvider();
    }

    public ClanMemberProvider getClanMemberProvider() {
        return murmelAPI.getClanMemberProvider();
    }

    /*public ClanParentProvider getClanParentProvider() {
        return murmelAPI.getClanParentProvider();
    }

    public ClanPermissionProvider getClanPermissionProvider() {
        return murmelAPI.getClanPermissionProvider();
    }*/

    public MaintenanceProvider getMaintenanceProvider() {
        return murmelAPI.getMaintenanceProvider();
    }

    public MaintenanceWhitelistProvider getMaintenanceWhitelistProvider() {
        return murmelAPI.getMaintenanceWhitelistProvider();
    }

    public PunishmentService getPunishmentService() {
        return murmelAPI.getPunishmentService();
    }

    public RefreshProvider getRefreshProvider() {
        return murmelAPI.getRefreshProvider();
    }

    public MinecraftChannelIdentifier getChannel() {
        return channel;
    }

    public PunishmentUtil getPunishmentUtil() {
        return punishmentUtil;
    }
}
