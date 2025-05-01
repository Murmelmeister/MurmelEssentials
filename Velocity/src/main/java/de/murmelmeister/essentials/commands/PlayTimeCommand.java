package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.UUID;

public final class PlayTimeCommand extends CommandManager {
    public PlayTimeCommand(MurmelEssentials plugin) {
        super(plugin);
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("playtime")
                .requires(source -> source.hasPermission("murmel.command.playtime"))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Player player = getPlayer(source);

                    if (!existsPlayer(player)) return -1;

                    UUID uuid = player.getUniqueId();
                    if (!user.existsUser(uuid)) {
                        sendLoggerErrorUser(player.getUsername());
                        return -2;
                    }

                    int userId = user.getId(uuid);
                    if (!playTime.existsUser(userId)) {
                        sendLoggerErrorPlayTime(player.getUsername());
                        return -3;
                    }

                    String time = TimeUtil.formatTimeValue(playTime, userId);
                    sendMessage(source, "<#999999>PlayTime: <#00cc88>%s", time);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            List<String> usernames = user.getUsernames().stream().sorted().toList();
                            for (String username : usernames)
                                builder.suggest(username, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + username)));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            String username = context.getArgument("player", String.class);
                            if (!user.existsUser(username)) {
                                sendLoggerErrorUser(username);
                                sendMessage(source, "<red>User %s does not exist.", username);
                                return -1;
                            }

                            int userId = user.getId(username);
                            if (!playTime.existsUser(userId)) {
                                sendLoggerErrorPlayTime(username);
                                sendMessage(source, "<red>User %s does not exist.", username);
                                return -2;
                            }

                            String time = TimeUtil.formatTimeValue(playTime, userId);
                            sendMessage(source, "<#999999>PlayTime from %s: <#00cc88>%s", username, time);
                            return Command.SINGLE_SUCCESS;
                        }))
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
