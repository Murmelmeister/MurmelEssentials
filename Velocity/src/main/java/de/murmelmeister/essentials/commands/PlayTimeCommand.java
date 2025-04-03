package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.LoginHistory;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.concurrent.CompletableFuture;

public final class PlayTimeCommand extends CommandManager {
    private final User user;

    public PlayTimeCommand(User user) {
        this.user = user;
    }

    public BrigadierCommand createCommand(PlayTime playTime, ActiveSession session, LoginHistory login) {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("playtime")
                .requires(source -> source.hasPermission("murmelessentials.command.playtime"))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Player player = getPlayer(source);
                    if (!existPlayer(source)) return 2;

                    int uid = user.getId(player.getUniqueId());
                    String time = TimeUtil.formatTimeValue(playTime, uid);
                    sendMessage(source, "<#999999>PlayTime: <#00cc88>%s", time);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests(this::playerSuggestions)
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            String username = context.getArgument("player", String.class);
                            if (isUserNotExist(source, user, username)) return 2;

                            int userId = user.getId(username);
                            String lastQuit = login.getLastQuit(userId) == null ? "unknown" : MurmelAPI.getDateFormat().format(login.getLastQuit(userId));
                            String online = session.isOnline(userId) ? "<#00cc88>online" : "<#cc0099>" + lastQuit;
                            String time = TimeUtil.formatTimeValue(playTime, userId);
                            sendMessage(source, "<#e6c200>%s <#999999>online mode: %s", username, online);
                            sendMessage(source, "<#999999>PlayTime from <#e6c200>%s<#999999>: <#00cc88>%s", username, time);
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }

    private CompletableFuture<Suggestions> playerSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        user.getUsernames().stream()
                .parallel()
                .filter(s -> StringUtil.startsWithIgnoreCase(s, prefix))
                .sorted().toList().forEach(username -> builder.suggest(username,
                        VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + username))));
        return builder.buildFuture();
    }
}
