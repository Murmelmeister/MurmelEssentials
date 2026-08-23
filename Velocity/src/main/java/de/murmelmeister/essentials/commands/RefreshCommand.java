package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.language.message.MessageProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;

import java.util.List;

@CommandConfig(id = "refresh", name = "refresh", bypass = true)
public final class RefreshCommand extends CommandManager {
    private final MurmelEssentials plugin;
    private final RefreshProvider refreshProvider;

    public RefreshCommand(MurmelEssentials plugin) {
        super(plugin);
        this.plugin = plugin;
        this.refreshProvider = plugin.getRefreshProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission("murmel.command.refresh"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            refreshProvider.fireAll();
                            plugin.getPluginConfig().reload();
                            reload(plugin);
                            plugin.reloadTablist();
                            sendRawMessage(source, executor.languageId(), "<#00cc88>All caches refreshed.");
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.literalArgumentBuilder("languages")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.LANGUAGES);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded languages.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("messages")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    final MessageProvider messageProvider = plugin.getMessageProvider();
                                    int result = 0;
                                    result += plugin.getMessageConfig().loadToDatabase(messageProvider,
                                            List.of("lang/message_en.properties", "lang/message_de.properties")
                                    ).length;

                                    if (result == 0)
                                        refreshProvider.fireCache(RefreshType.MESSAGES);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded messages. Updated messages: <messages>", tagParsed("messages", result));
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_reasons")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_REASONS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded punishment reasons.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_audits")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_AUDITS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded punishment audits.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_users")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_USERS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded punishment users.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_ips")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_IPS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded punishment IPs.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("users")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USERS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded users.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_logins")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_LOGINS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded user logins.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_sessions")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_SESSIONS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded user sessions.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("parents")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PARENTS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded parents.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("permissions")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PERMISSIONS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded permissions.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("groups")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.GROUPS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded groups.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("group_colors")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.GROUP_COLORS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded group colors.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("all")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.ALL);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded all caches.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("send")
                        .then(BrigadierCommand.requiredArgumentBuilder("message", StringArgumentType.word())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            String message = StringArgumentType.getString(context, "message");
                                            refreshProvider.fireCache(message);
                                            sendRawMessage(source, executor.languageId(), "<#00cc88>Sent message: " + message);
                                            return CommandResult.of(Command.SINGLE_SUCCESS);
                                        })
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("tablist")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.SETTINGS);
                                    plugin.reloadTablist();
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded tablist.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("settings")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.SETTINGS);
                                    sendRawMessage(source, executor.languageId(), "<#00cc88>Reloaded settings.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                );
    }
}
