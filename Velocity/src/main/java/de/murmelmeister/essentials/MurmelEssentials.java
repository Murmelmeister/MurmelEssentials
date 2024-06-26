package de.murmelmeister.essentials;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.essentials.api.PlayTimeUpdater;
import de.murmelmeister.essentials.files.MySQL;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

@Plugin(id = "murmelessentials", name = "MurmelEssentials", version = "0.0.1", description = "MurmelEssentials is a plugin that adds a lot of useful commands to your server.", authors = {"Murmelmeister"}, url = "https://www.youtube.com/Murmelmeister")
public final class MurmelEssentials {
    private final Logger logger;
    private final ProxyServer proxyServer;

    private MySQL mySQL;
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("permission", "refresh");

    @Inject
    public MurmelEssentials(Logger logger, ProxyServer proxyServer) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        proxyServer.getChannelRegistrar().register(CHANNEL);
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        mySQL = new MySQL(logger);
        mySQL.connect();
        CustomPermission.updatePermission(proxyServer, this);
        ListenerManager.register(proxyServer, this);
        CommandManager.register(proxyServer, this);
        PlayTimeUpdater.startTimer(proxyServer, this);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        mySQL.disconnect();
    }

    public Group getGroup() {
        return MurmelAPI.getGroup();
    }

    public User getUser() {
        return MurmelAPI.getUser();
    }

    public PlayTime getPlayTime() {
        return MurmelAPI.getPlayTime();
    }

    public Permission getPermission() {
        return MurmelAPI.getPermission();
    }

    public static void playerSendRefreshMessage(Player player) {
        player.sendPluginMessage(CHANNEL, "refresh".getBytes());
    }

    public static void serverSendRefreshMessage(ProxyServer server) {
        server.getAllServers().parallelStream().forEach(registeredServer -> registeredServer.sendPluginMessage(CHANNEL, "refresh".getBytes(StandardCharsets.UTF_8)));
    }
}
