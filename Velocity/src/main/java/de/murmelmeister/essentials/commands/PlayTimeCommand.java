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
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDateTime;

public final class PlayTimeCommand extends CommandManager {
    private final MessageService messageService;
    private final UserProvider userProvider;
    private final UserSessionProvider sessionProvider;
    private final UserLoginProvider loginProvider;

    public PlayTimeCommand(MurmelEssentials plugin) {
        super(plugin);
        this.messageService = plugin.getMessageService();
        this.userProvider = plugin.getUserProvider();
        this.sessionProvider = plugin.getUserSessionProvider();
        this.loginProvider = plugin.getUserLoginProvider();
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
                            String prefix = builder.getRemaining();
                            userProvider.findUsernames().stream()
                                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                                    .sorted()
                                    .forEach(username ->
                                            builder.suggest(username, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + username)))
                                    );
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String username = StringArgumentType.getString(context, "player");
                                    User user = getUser(languageId, username);
                                    int userId = user.id();
                                    UserPlayTime playTime = getUserPlayTime(userId);

                                    UserLogin lastLogin = loginProvider.getLastLogin(userId);
                                    String online = sessionProvider.isOnline(userId) ? "<#00cc88>online" :
                                            (lastLogin != null ? "<#cc0099>" + lastLogin.logoutTime().format(getDateTimeFormatter(languageId)) : "<#cc0099>unknown");
                                    String time = TimeUtil.formatDuration(messageService, languageId, playTime.getPlayTime());

                                    sendMessage(source, "<#e6c200>%s <#999999>online mode: %s", username, online);
                                    sendMessage(source, messageService.getMessage(Messages.PLAY_TIME_COMMAND_OTHER, languageId)
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
