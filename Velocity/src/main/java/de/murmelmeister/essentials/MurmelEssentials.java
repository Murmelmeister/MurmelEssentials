package de.murmelmeister.essentials;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.essentials.api.PlayTimeUpdater;
import de.murmelmeister.essentials.files.MySQL;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.essentials.utils.MessagesService;
import de.murmelmeister.essentials.utils.RefreshBridge;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.language.MessageProvider;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.LoginHistory;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import org.slf4j.Logger;

public final class MurmelEssentials {
    private final Logger logger;
    private final ProxyServer server;

    private MySQL mySQL;
    private MessagesService messagesService;
    private final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from("murmel:main");

    public static final String TEAM_MEMBER_PERMISSION = "murmel.member.team";

    @Inject
    public MurmelEssentials(Logger logger, ProxyServer server) {
        this.logger = logger;
        this.server = server;
        server.getChannelRegistrar().register(channel);
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        mySQL = new MySQL(logger);
        mySQL.connect();
        getLanguageProvider().loadData();
        messagesService = new MessagesService(logger, getMessageProvider());
        messagesService.checkAndLoad();
        getGroup().createDefaultGroup("default");
        CustomPermission.updatePermission(this, server);
        new RefreshBridge(this, server);
        ListenerManager.register(this, server);
        CommandManager.register(this);
        PlayTimeUpdater.startTimer(this, logger, server);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        mySQL.disconnect();
        server.getChannelRegistrar().unregister(channel);
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public LoginHistory getLoginHistory() {
        return MurmelAPI.getLoginHistory();
    }

    public ActiveSession getActiveSession() {
        return MurmelAPI.getActiveSession();
    }

    public User getUser() {
        return MurmelAPI.getUser();
    }

    public Group getGroup() {
        return MurmelAPI.getGroup();
    }

    public PlayTime getPlayTime() {
        return MurmelAPI.getPlayTime();
    }

    public Permission getPermission() {
        return MurmelAPI.getPermission();
    }

    public PunishmentReason getPunishmentReason() {
        return MurmelAPI.getPunishmentReason();
    }

    public PunishmentLog getPunishmentLog() {
        return MurmelAPI.getPunishmentLog();
    }

    public PunishmentIP getPunishmentIP() {
        return MurmelAPI.getPunishmentIP();
    }

    public PunishmentUser getPunishmentUser() {
        return MurmelAPI.getPunishmentUser();
    }

    public LanguageProvider getLanguageProvider() {
        return MurmelAPI.getLanguage();
    }

    public MessageProvider getMessageProvider() {
        return MurmelAPI.getMessage();
    }

    public MessagesService getMessagesService() {
        return messagesService;
    }

    public MinecraftChannelIdentifier getChannel() {
        return channel;
    }
}
