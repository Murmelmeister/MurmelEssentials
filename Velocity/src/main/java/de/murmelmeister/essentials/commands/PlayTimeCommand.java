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
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.stats.UserStats;
import de.murmelmeister.murmelapi.utils.TimeFilterUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;

import java.time.LocalDateTime;

@CommandConfig(id = "playtime", name = "playtime")
public final class PlayTimeCommand extends CommandManager {
    private final MessageService messageService;
    private final UserProvider userProvider;

    public PlayTimeCommand(MurmelEssentials plugin) {
        super(plugin);
        this.messageService = plugin.getMessageService();
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "playtime"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            UserStats userStats = getUserStats(executor.id());
                            long playTime = userStats.currentPlayTime();
                            String time = TimeUtil.formatDuration(messageService, executor.languageId(), playTime);
                            sendMessage(source, executor.languageId(), Message.COMMAND_PLAY_TIME_USE, tagParsed("time", time));
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String prefix = builder.getRemaining();
                            userProvider.findUsernames().stream()
                                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                                    .sorted()
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String inputUser = StringArgumentType.getString(context, "player");
                                    User user = getUser(inputUser);
                                    int userId = user.id();
                                    UserStats userStats = getUserStats(userId);

                                    String online = getOnlineStatus(languageId, userId);
                                    long playTime = userStats.currentPlayTime();
                                    String time = TimeUtil.formatDuration(messageService, languageId, playTime);
                                    String ago = getOnlineAgo(languageId, userId, TimeFilterUtil.SECONDS);
                                    String now = LocalDateTime.now().format(getDateTimeFormatter(languageId));

                                    sendMessage(source, languageId, Message.COMMAND_PLAY_TIME_OTHER,
                                            tagParsed("username", user.username()),
                                            tagParsed("online", online),
                                            tagParsed("time", time),
                                            tagParsed("ago", ago),
                                            tagParsed("now", now)
                                    );
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                );
    }
}
