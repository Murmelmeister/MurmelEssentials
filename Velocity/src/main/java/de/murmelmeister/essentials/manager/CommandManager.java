package de.murmelmeister.essentials.manager;

import com.velocitypowered.api.command.*;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.PlayTimeCommand;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.UUID;

public abstract class CommandManager implements CommandBrigadier {
    protected final Logger logger;
    protected final ProxyServer server;
    protected final User user;
    protected final Group group;
    protected final PlayTime playTime;
    protected final Permission permission;

    public CommandManager(MurmelEssentials plugin) {
        this.logger = plugin.getLogger();
        this.server = plugin.getServer();
        this.user = plugin.getUser();
        this.group = plugin.getGroup();
        this.playTime = plugin.getPlayTime();
        this.permission = plugin.getPermission();
    }

    public static void register(MurmelEssentials plugin) {
        ProxyServer server = plugin.getServer();
        addCommand(server, new PlayTimeCommand(plugin));
        //addCommand(server, "permission", new PermissionCommand(server, permission, group, user));
        //addCommand(server, "playtime", new PlayTimeCommand(user, playTime));
        //server.getCommandManager().register(PlayTimeCommand.createBrigadierCommand(user, playTime));
    }

    private static void addCommand(ProxyServer server, CommandManager manager) {
        BrigadierCommand command = manager.createCommand();
        addCommand(server, command);
    }

    private static void addCommand(ProxyServer server, CommandManager manager, String... aliases) {
        BrigadierCommand command = manager.createCommand();
        addCommand(server, command, aliases);
    }

    private static void addCommand(ProxyServer server, BrigadierCommand command) {
        CommandMeta meta = server.getCommandManager().metaBuilder(command).build();
        server.getCommandManager().register(meta, command);
    }

    private static void addCommand(ProxyServer server, BrigadierCommand command, String... aliases) {
        CommandMeta meta = server.getCommandManager().metaBuilder(command).aliases(aliases).build();
        server.getCommandManager().register(meta, command);
    }

    protected void sendMessage(CommandSource source, String message, Object... args) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(String.format(message, args)));
    }

    protected Player getPlayer(CommandSource source) {
        return source instanceof Player ? (Player) source : null;
    }

    protected boolean existsPlayer(CommandSource source) {
        Player player = getPlayer(source);
        if (player == null) {
            sendMessage(source, "<red>This command does not work in the console.");
            return false;
        } else return true;
    }

    protected boolean existsUser(CommandSource source, UUID uuid) {
        if (!user.existsUser(uuid)) {
            sendMessage(source, "<red>User %s does not exist.", uuid);
            return false;
        }
        return true;
    }

    protected boolean existsUser(CommandSource source, String username) {
        if (!user.existsUser(username)) {
            sendMessage(source, "<red>User %s does not exist.", username);
            return false;
        }
        return true;
    }

    protected boolean existsGroup(CommandSource source, String groupName) {
        if (!group.existsGroup(groupName)) {
            sendMessage(source, "<red>Group %s does not exist.", groupName);
            return false;
        }
        return true;
    }
}
