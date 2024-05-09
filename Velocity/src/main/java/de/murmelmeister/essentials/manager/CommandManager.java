package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.PermissionCommand;
import de.murmelmeister.essentials.commands.PlayTimeCommand;
import net.kyori.adventure.text.Component;

public abstract class CommandManager implements SimpleCommand {
    public static void register(ProxyServer server, MurmelEssentials instance) {
        var group = instance.getGroup();
        var user = instance.getUser();
        var permission = instance.getPermission();
        var playTime = instance.getPlayTime();
        addCommand(server, "permission", new PermissionCommand(permission, group, user));
        addCommand(server, "playtime", new PlayTimeCommand(user, playTime));
    }

    private static void addCommand(ProxyServer server, String name, Object clazz) {
        server.getCommandManager().register(name, (Command) clazz);
    }

    public void sendSourceMessage(CommandSource source, String message, Object... objects) {
        source.sendMessage(Component.text(String.format(message, objects)));
    }
}
