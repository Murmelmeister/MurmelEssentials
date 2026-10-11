package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.language.message.MessageProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;

import java.util.List;

@CommandConfig(id = "refresh", name = "refresh", bypass = true)
public final class RefreshCommand extends CommandManager {
    private static final List<String> MESSAGE_FILES = List.of(
            "lang/message_en.properties",
            "lang/message_de.properties"
    );

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
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "refresh"))
                .executes(this::executeAll)
                .then(BrigadierCommand.literalArgumentBuilder("languages")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.LANGUAGES);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("messages")
                        .executes(this::executeMessages)
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_reasons")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_REASONS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_audits")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_AUDITS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_users")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_USERS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("punishment_ips")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PUNISHMENT_IPS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("users")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USERS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_logins")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_LOGINS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_sessions")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_SESSIONS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("parents")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PARENTS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("permissions")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PERMISSIONS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("groups")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.GROUPS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("group_colors")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.GROUP_COLORS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("all")
                        .executes(this::executeAll)
                )
                .then(BrigadierCommand.literalArgumentBuilder("send")
                        .then(BrigadierCommand.requiredArgumentBuilder("message", StringArgumentType.word())
                                .requires(source -> {
                                    User user = getExecutor(source);
                                    return user.debugMode();
                                })
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
                                    plugin.getPluginConfig().reload();
                                    plugin.reloadTablist();
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("settings")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.SETTINGS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("clans")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.CLANS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("clan_members")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.CLAN_MEMBERS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("clan_groups")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.CLAN_GROUPS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("prefix_colors")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.PREFIX_COLORS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_prefix_colors")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_PREFIX_COLORS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("inventory_types")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.INVENTORY_TYPES);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_inventories")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_INVENTORIES);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_stats")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_STATS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("maintenances")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.MAINTENANCES);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("maintenance_whitelists")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.MAINTENANCE_WHITELISTS);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("user_excuses")
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    refreshProvider.fireCache(RefreshType.USER_EXCUSES);
                                    sendMessage(source, executor.languageId(), Message.COMMAND_REFRESH_ONE);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                ;
    }

    private int executeMessages(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            MessageProvider messageProvider = plugin.getMessageProvider();
            refreshProvider.fireCache(RefreshType.MESSAGES);
            int updatedMessages = plugin.getMessageConfig()
                    .loadToDatabase(messageProvider, MESSAGE_FILES)
                    .length;

            sendMessage(source, executor.languageId(),
                    Message.COMMAND_REFRESH_MESSAGES,
                    tagParsed("messages", updatedMessages));
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private int executeAll(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            plugin.getPluginConfig().reload();
            refreshProvider.fireAll();
            int updatedMessages = plugin.getMessageConfig()
                    .loadToDatabase(plugin.getMessageProvider(), MESSAGE_FILES)
                    .length;
            reload(plugin);
            plugin.reloadTablist();

            sendMessage(source, executor.languageId(),
                    Message.COMMAND_REFRESH_ALL,
                    tagParsed("messages", updatedMessages));
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }
}
