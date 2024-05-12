package de.murmelmeister.essentials.commands;

import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class PlayTimeCommand extends CommandManager {
    private final User user;
    private final PlayTime playTime;

    public PlayTimeCommand(User user, PlayTime playTime) {
        this.user = user;
        this.playTime = playTime;
    }

    @Override
    public void execute(Invocation invocation) {
        var args = invocation.arguments();
        var source = invocation.source();

        if (!source.hasPermission("murmelessentials.command.playtime")) {
            sendSourceMessage(source, "§cYou do not have permission to use this command.");
            return;
        }

        try {
            if (args.length == 0) {
                var player = source instanceof Player ? (Player) source : null;

                if (player == null) {
                    sendSourceMessage(source, "§cThis command does not work in the console.");
                    return;
                }

                var time = TimeUtil.formatTimeValue(playTime, user.getId(player.getUniqueId()));
                sendSourceMessage(source, "§3PlayTime: §e%s", time);
            } else if (args.length == 1) {
                var username = args[0];
                if (isUserNotExist(source, user, username)) return;

                var userId = user.getId(username);
                var time = TimeUtil.formatTimeValue(playTime, userId);
                sendSourceMessage(source, "§3PlayTime from §a%s§3: §e%s", username, time);
            } else sendSourceMessage(source, "§7Syntax: §c/playtime [PLAYER]");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        var args = invocation.arguments();
        if (args.length == 1) {
            try {
                return user.getUsernames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.emptyList();
    }
}
