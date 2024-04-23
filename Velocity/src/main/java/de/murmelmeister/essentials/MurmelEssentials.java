package de.murmelmeister.essentials;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.files.MySQL;
import de.murmelmeister.essentials.listeners.PermissionListener;
import org.slf4j.Logger;

import java.sql.SQLException;

@Plugin(id = "murmelessentials", name = "MurmelEssentials", version = "0.0.1", description = "MurmelEssentials is a plugin that adds a lot of useful commands to your server.", authors = {"Murmelmeister"}, url = "https://www.youtube.com/Murmelmeister")
public final class MurmelEssentials {
    @Inject
    private Logger logger;
    @Inject
    private ProxyServer proxyServer;

    private MySQL mySQL;

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) throws SQLException {
        mySQL = new MySQL(logger);
        mySQL.connect();
        mySQL.load();
        proxyServer.getEventManager().register(this, new PermissionListener(mySQL.getPermission(), mySQL.getUser()));
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        mySQL.disconnect();
    }
}
