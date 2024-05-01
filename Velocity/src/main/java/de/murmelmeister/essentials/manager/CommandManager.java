package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.PermissionCommand;
import net.kyori.adventure.text.Component;

public final class CommandManager {
    public static void register(ProxyServer server, MurmelEssentials instance) {
        addCommand(server, "permission", new PermissionCommand(instance.getPermission(), instance.getGroup(), instance.getUser()));
    }

    private static void addCommand(ProxyServer server, String name, Object clazz) {
        server.getCommandManager().register(name, (Command) clazz);
    }

    public static void sendSourceMessage(CommandSource source, String message, Object... objects) {
        source.sendMessage(Component.text(String.format(message, objects)));
    }
}
