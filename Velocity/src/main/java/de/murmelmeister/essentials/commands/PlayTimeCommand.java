package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class PlayTimeCommand extends CommandManager {
    private final User user;
    private final PlayTime playTime;

    public PlayTimeCommand(User user, PlayTime playTime) {
        this.user = user;
        this.playTime = playTime;
    }

    public static BrigadierCommand createBrigadierCommand(User user, PlayTime playTime) {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("playtime")
                .requires(source -> source.hasPermission("murmelessentials.command.playtime"))
                .executes(context -> {
                    var source = context.getSource();
                    var player = source instanceof Player ? (Player) source : null;

                    if (player == null) {
                        sendSourceMessage(source, "§cThis command does not work in the console.");
                        return 0;
                    }

                    var uid = user.getId(player.getUniqueId());
                    var time = TimeUtil.formatTimeValue(playTime, uid);
                    sendHexColorMessage(source, "<rainbow>PlayTime: %s", time);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            user.getUsernames().stream().sorted().toList().forEach(username -> builder.suggest(username,
                                    VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + username))));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            var source = context.getSource();
                            var username = context.getArgument("player", String.class);
                            try {
                                if (isUserNotExist(source, user, username)) return 0;

                                var userId = user.getId(username);
                                var settings = user.getSettings();
                                var online = settings.getOnline(userId) == 1 ? "<green>Online" : "<red>" + settings.getLastQuitDate(userId);
                                var time = TimeUtil.formatTimeValue(playTime, userId);
                                sendHexColorMessage(source, "<yellow>%s <gray>online mode: %s", username, online);
                                sendHexColorMessage(source, "<rainbow>PlayTime from %s: %s", username, time);
                                return 1;
                            } catch (IllegalArgumentException e) {
                                sendSourceMessage(source, "§cError: " + e.getMessage());
                                return 0;
                            }
                        }))
                .build();
        return new BrigadierCommand(node);
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
        } catch (IllegalArgumentException e) {
            sendSourceMessage(source, "&c" + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            var args = invocation.arguments();
            if (args.length == 1)
                return user.getUsernames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            return Collections.emptyList();
        });
    }
}
