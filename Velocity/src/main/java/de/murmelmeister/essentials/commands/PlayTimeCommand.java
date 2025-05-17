package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public final class PlayTimeCommand extends CommandManager {
    public PlayTimeCommand(MurmelEssentials plugin) {
        super(plugin);
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("playtime")
                .requires(source -> source.hasPermission("murmel.command.playtime"))
                .executes(context ->
                        runWithTiming(context, (source, executorId) -> {
                            if (!playTime.existsUser(executorId)) {
                                sendLoggerErrorPlayTime(user.getUsername(executorId));
                                return CommandResult.of(-2);
                            }

                            String time = TimeUtil.formatTimeValue(playTime, executorId);
                            sendMessage(source, "<#999999>PlayTime: <#00cc88>%s", time);
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<String> usernames = user.getUsernames().stream().sorted().toList();
                            usernames.forEach(username ->
                                    builder.suggest(username, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + username)))
                            );
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executorId) -> {
                                    String username = context.getArgument("player", String.class);
                                    if (!user.existsUser(username)) {
                                        sendLoggerErrorUser(username);
                                        sendMessage(source, "<#990000>User %s does not exist.", username);
                                        return CommandResult.of(-2);
                                    }

                                    int userId = user.getId(username);
                                    if (!playTime.existsUser(userId)) {
                                        sendLoggerErrorPlayTime(username);
                                        sendMessage(source, "<#990000>User %s does not exist.", username);
                                        return CommandResult.of(-3);
                                    }

                                    String time = TimeUtil.formatTimeValue(playTime, userId);
                                    sendMessage(source, "<#999999>PlayTime from %s: <#00cc88>%s", username, time);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }

    private void sendLoggerErrorUser(String username) {
        logger.error("User {} does not exist in users table.", username);
    }

    private void sendLoggerErrorPlayTime(String username) {
        logger.error("User {} does not exist in playtime table.", username);
    }
}
