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
import de.murmelmeister.essentials.utils.Messages;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

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
                            sendMessage(source, messageService.getMessage(Messages.PLAY_TIME_COMMAND_USE, executor.languageId()).replace("[TIME]", time));
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<String> usernames = userProvider.findUsernames().stream().sorted().toList();
                            usernames.forEach(username ->
                                    builder.suggest(username, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + username)))
                            );
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    String username = StringArgumentType.getString(context, "player");
                                    User user = getUser(executor.languageId(), username);
                                    UserPlayTime playTime = getUserPlayTime(user.id());

                                    String time = TimeUtil.formatDuration(messageService, executor.languageId(), playTime.getPlayTime());
                                    sendMessage(source, messageService.getMessage(Messages.PLAY_TIME_COMMAND_OTHER, executor.languageId())
                                            .replace("[PLAYER]", username)
                                            .replace("[TIME]", time));
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }
}
