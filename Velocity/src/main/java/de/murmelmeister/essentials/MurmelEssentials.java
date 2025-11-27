package de.murmelmeister.essentials;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.essentials.api.PlayTimeUpdater;
import de.murmelmeister.essentials.configs.DatabaseConfig;
import de.murmelmeister.essentials.configs.MessageConfig;
import de.murmelmeister.essentials.configurations.Config;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.*;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.language.message.MessageProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
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

    private final DatabaseConfig databaseConfig;
    private final MessageConfig messageConfig;
    private final Config config;
    private final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from("murmel:main");
    private final PunishmentUtil punishmentUtil;
    private RefreshBridge refreshBridge;

    public static final String TEAM_MEMBER_PERMISSION = "murmel.member.team";
    public static final String PUNISHMENT_REASON_PERMISSION = "murmel.punishment.reason.";
    public static final String PUNISHMENT_IMMUNITY_PERMISSION = "murmel.punishment.immunity";
    public static final String PUNISHMENT_NOTIFY_PERMISSION = "murmel.punishment.notify";

    private final TablistUtil tablistUtil;

    @Inject
    public MurmelEssentials(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.databaseConfig = new DatabaseConfig(dataDirectory);
        this.messageConfig = new MessageConfig(dataDirectory);
        this.config = new Config(dataDirectory);
        databaseConfig.connect();
        MurmelAPI.setup();

        final MessageProvider messageProvider = getMessageProvider();
        int messages = 0;
        messages += messageConfig.loadToDatabase(messageProvider,
                List.of("lang/message_en.properties", "lang/message_de.properties")
        ).length;

        MurmelMessageTranslator translator = new MurmelMessageTranslator(getMessageService());
        GlobalTranslator.translator().addSource(translator);
        logger.info("Updated {} messages.", messages);

        this.punishmentUtil = new PunishmentUtil(this);
        this.tablistUtil = new TablistUtil(this, config, logger, server);
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(channel); // Note: Channel registration only works at the ProxyInitializeEvent not in the constructor
        refreshBridge = new RefreshBridge(server, channel);
        refreshBridge.register();

        CustomPermission.updatePermission(this, server);
        ListenerManager.register(this, server);
        CommandManager.register(this);
        PlayTimeUpdater.startTimer(this, logger, server);

        if (config.getAutoRefresh())
            RefreshUtil.fireAll(); // Get all cached data from the database
        tablistUtil.start();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        tablistUtil.stop();
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
        tablistUtil.reload();
    }

    public DateTimeFormatter getDateTimeFormatter(int languageId) {
        return MurmelAPI.getDateTimeFormatter(languageId);
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    public LanguageProvider getLanguageProvider() {
        return MurmelAPI.getLanguageProvider();
    }

    public MessageProvider getMessageProvider() {
        return MurmelAPI.getMessageProvider();
    }

    public MessageService getMessageService() {
        return MurmelAPI.getMessageService();
    }

    public UserProvider getUserProvider() {
        return MurmelAPI.getUserProvider();
    }

    public UserPlayTimeProvider getUserPlayTimeProvider() {
        return MurmelAPI.getUserPlayTimeProvider();
    }

    public UserLoginProvider getUserLoginProvider() {
        return MurmelAPI.getUserLoginProvider();
    }

    public UserSessionProvider getUserSessionProvider() {
        return MurmelAPI.getUserSessionProvider();
    }

    public UserService getUserService() {
        return MurmelAPI.getUserService();
    }

    public GroupProvider getGroupProvider() {
        return MurmelAPI.getGroupProvider();
    }

    public GroupColorProvider getGroupColorProvider() {
        return MurmelAPI.getGroupColorProvider();
    }

    public UserPermissionProvider getUserPermissionProvider() {
        return MurmelAPI.getUserPermissionProvider();
    }

    public UserParentProvider getUserParentProvider() {
        return MurmelAPI.getUserParentProvider();
    }

    public GroupPermissionProvider getGroupPermissionProvider() {
        return MurmelAPI.getGroupPermissionProvider();
    }

    public GroupParentProvider getGroupParentProvider() {
        return MurmelAPI.getGroupParentProvider();
    }

    public Permission getPermission() {
        return MurmelAPI.getPermission();
    }

    public PunishmentReasonProvider getPunishmentReasonProvider() {
        return MurmelAPI.getPunishmentReasonProvider();
    }

    public PunishmentLogProvider getPunishmentLogProvider() {
        return MurmelAPI.getPunishmentLogProvider();
    }

    public PunishmentCurrentIpProvider getPunishmentIpProvider() {
        return MurmelAPI.getPunishmentCurrentIpProvider();
    }

    public PunishmentCurrentUserProvider getPunishmentUserProvider() {
        return MurmelAPI.getPunishmentCurrentUserProvider();
    }

    public PunishmentService getPunishmentService() {
        return MurmelAPI.getPunishmentService();
    }

    public MinecraftChannelIdentifier getChannel() {
        return channel;
    }

    public PunishmentUtil getPunishmentUtil() {
        return punishmentUtil;
    }
}
