package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.utils.TimeUtil;

import java.time.LocalDateTime;

public final class PlayTimeCommand extends CommandManager {
    private final MessageService messageService;
    private final UserProvider userProvider;

    public PlayTimeCommand(MurmelEssentials plugin) {
        super(plugin);
        this.messageService = plugin.getMessageService();
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("playtime")
                .requires(source -> source.hasPermission("murmel.command.playtime"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            UserPlayTime playTime = getUserPlayTime(executor.id());
                            String time = TimeUtil.formatDuration(messageService, executor.languageId(), playTime.getPlayTime());
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
                                    UserPlayTime playTime = getUserPlayTime(userId);

                                    String online = getOnlineStatus(languageId, userId);
                                    String time = TimeUtil.formatDuration(messageService, languageId, playTime.getPlayTime());
                                    String ago = getOnlineAgo(languageId, userId);
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
                )
                .build();
        return new BrigadierCommand(node);
    }
}
