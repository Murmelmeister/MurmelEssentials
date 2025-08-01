package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

public final class RefreshCommand extends CommandManager {
    public RefreshCommand(MurmelEssentials plugin) {
        super(plugin);
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("refresh")
                .requires(source -> source.hasPermission("murmel.command.refresh"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            RefreshUtil.fireAll();
                            sendMessage(source, "<#00cc88>All caches refreshed.");
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.literalArgumentBuilder("languages")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.LANGUAGES);
                                    sendMessage(source, "<#00cc88>Reloaded languages.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("messages")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.MESSAGES);
                                    sendMessage(source, "<#00cc88>Reloaded messages.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_reasons")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.PUNISHMENT_REASONS);
                                    sendMessage(source, "<#00cc88>Reloaded punishment reasons.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_logs")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.PUNISHMENT_LOGS);
                                    sendMessage(source, "<#00cc88>Reloaded punishment logs.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_users")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.PUNISHMENT_USERS);
                                    sendMessage(source, "<#00cc88>Reloaded punishment users.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_ips")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.PUNISHMENT_IPS);
                                    sendMessage(source, "<#00cc88>Reloaded punishment IPs.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("users")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.USERS);
                                    sendMessage(source, "<#00cc88>Reloaded users.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_logins")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.USER_LOGINS);
                                    sendMessage(source, "<#00cc88>Reloaded user logins.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_sessions")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.USER_SESSIONS);
                                    sendMessage(source, "<#00cc88>Reloaded user sessions.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_play_times")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.USER_PLAY_TIMES);
                                    sendMessage(source, "<#00cc88>Reloaded user play times.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("all")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    RefreshUtil.fireCache(RefreshType.ALL);
                                    sendMessage(source, "<#00cc88>Reloaded all caches.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("send")
                        .then(BrigadierCommand.requiredArgumentBuilder("message", StringArgumentType.word())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            String message = StringArgumentType.getString(context, "message");
                                            RefreshUtil.fireCache(message);
                                            sendMessage(source, "<#00cc88>Sent message: " + message);
                                            return CommandResult.of(Command.SINGLE_SUCCESS);
                                        })
                                )
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }
}
