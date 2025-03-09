package de.murmelmeister.essentials;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.essentials.api.PlayTimeUpdater;
import de.murmelmeister.essentials.files.MySQL;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
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

/*

Colors:
RED = #cc0099
GREEN = #00cc88
YELLOW = #e6c200
GRAY = #999999

 */
@Plugin(
        id = "murmelessentials",
        name = "MurmelEssentials",
        version = "0.0.3",
        description = "MurmelEssentials is a plugin that adds a lot of useful commands to your server.",
        authors = {"Murmelmeister"},
        url = "https://www.youtube.com/Murmelmeister"
)
public final class MurmelEssentials {
    private final Logger logger;
    private final ProxyServer server;

    private MySQL mySQL;
    public static final String TEAM_MEMBER_PERMISSION = "murmelessentials.member.team";
    public static final String PERMISSION_PUNISHMENT_TYPE = "murmelessentials.punishment.type.";
    public static final String PERMISSION_PUNISHMENT_REASON = "murmelessentials.punishment.reason.";
    public static final String PERMISSION_IP_ADDRESS_SHOW = "murmelessentials.ipaddress.show";

    @Inject
    public MurmelEssentials(Logger logger, ProxyServer server) {
        this.logger = logger;
        this.server = server;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        mySQL = new MySQL(logger);
        mySQL.connect();
        getGroup().createDefaultGroup("default");
        CustomPermission.updatePermission(this, server);
        ListenerManager.register(this, server);
        CommandManager.register(this, logger, server);
        PlayTimeUpdater.startTimer(this, server);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        mySQL.disconnect();
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

    public LoginHistory getLoginHistory() {
        return MurmelAPI.getLoginHistory();
    }

    public ActiveSession getActiveSession() {
        return MurmelAPI.getActiveSession();
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
}
