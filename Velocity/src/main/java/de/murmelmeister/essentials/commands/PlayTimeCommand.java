package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.settings.UserSettings;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class PlayTimeCommand extends CommandManager {
    public static BrigadierCommand createBrigadierCommand(User user, PlayTime playTime) {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("playtime")
                .requires(source -> source.hasPermission("murmelessentials.command.playtime"))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Player player = source instanceof Player ? (Player) source : null;

                    if (player == null) {
                        sendSourceMessage(source, "<#cc0099>This command does not work in the console.");
                        return 2;
                    }

                    int uid = user.getId(player.getUniqueId());
                    String time = TimeUtil.formatTimeValue(playTime, uid);
                    sendHexColorMessage(source, "<#999999>PlayTime: <#00cc88>%s", time);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String prefix = builder.getRemaining();
                            user.getUsernames().stream()
                                    .parallel()
                                    .filter(s -> StringUtil.startsWithIgnoreCase(s, prefix))
                                    .sorted().toList().forEach(username -> builder.suggest(username,
                                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + username))));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            String username = context.getArgument("player", String.class);
                            try {
                                if (isUserNotExist(source, user, username)) return 2;

                                int userId = user.getId(username);
                                UserSettings settings = user.getSettings();
                                String online = settings.getOnline(userId) == 1 ? "<#00cc88>Online" : "<#cc0099>" + settings.getLastQuitDate(userId);
                                String time = TimeUtil.formatTimeValue(playTime, userId);
                                sendHexColorMessage(source, "<#e6c200>%s <#999999>online mode: %s", username, online);
                                sendHexColorMessage(source, "<#999999>PlayTime from <#e6c200>%s<#999999>: <#00cc88>%s", username, time);
                                return Command.SINGLE_SUCCESS;
                            } catch (IllegalArgumentException e) {
                                sendSourceMessage(source, "<#cc0099>Error: " + e.getMessage());
                                return 2;
                            }
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
