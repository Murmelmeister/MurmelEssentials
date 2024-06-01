package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.PermissionCommand;
import de.murmelmeister.essentials.commands.PlayTimeCommand;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.SQLException;

public abstract class CommandManager implements SimpleCommand {
    public static void register(ProxyServer server, MurmelEssentials instance) {
        var group = instance.getGroup();
        var user = instance.getUser();
        var permission = instance.getPermission();
        var playTime = instance.getPlayTime();
        addCommand(server, "permission", new PermissionCommand(permission, group, user));
        //addCommand(server, "playtime", new PlayTimeCommand(user, playTime));
        server.getCommandManager().register(PlayTimeCommand.createBrigadierCommand(user, playTime));
    }

    private static void addCommand(ProxyServer server, String name, Object clazz) {
        server.getCommandManager().register(name, (Command) clazz);
    }

    public static void sendSourceMessage(CommandSource source, String message, Object... objects) {
        source.sendMessage(Component.text(String.format(message, objects)));
    }

    public static void sendHexColorMessage(CommandSource source, String message, Object... objects) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(String.format(message, objects)));
    }

    public static boolean isUserNotExist(CommandSource source, User user, String username) throws SQLException {
        if (!user.existsUser(username)) {
            sendSourceMessage(source, "§cUser does not exist.");
            return true;
        } else return false;
    }

    public void sendCreatorMessage(CommandSource source, User user, int creatorId) throws SQLException {
        sendSourceMessage(source, "§3Creator: ");
        sendSourceMessage(source, "§7- §3ID: §e%s", creatorId);
        sendSourceMessage(source, "§7- §3UUID: §e%s", user.getUniqueId(creatorId));
        sendSourceMessage(source, "§7- §3Name: §e%s", user.getUsername(creatorId));
    }
}
