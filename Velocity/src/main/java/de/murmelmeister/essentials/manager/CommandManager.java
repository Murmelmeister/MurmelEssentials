package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;

public final class CommandManager {
    public static void register(ProxyServer server, MurmelEssentials instance) {

    }

    private static void addCommand(ProxyServer server, String name, Object clazz) {
        server.getCommandManager().register(name, (Command) clazz);
    }
}
