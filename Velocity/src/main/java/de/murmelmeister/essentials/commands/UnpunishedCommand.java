package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.user.User;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public final class UnpunishedCommand extends CommandManager {
    private final User user;
    private final PunishmentIP punishIP;
    private final PunishmentUser punishUser;

    public UnpunishedCommand(User user, PunishmentIP punishIP, PunishmentUser punishUser) {
        this.user = user;
        this.punishIP = punishIP;
        this.punishUser = punishUser;
    }

    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder("unpunished")
                .requires(source -> source.hasPermission("murmelessentials.command.unpunished"))
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(getUnpunishedUser())
                .then(getUnpunishedIp())
                .build();
        return new BrigadierCommand(rootNode);
    }

    private LiteralArgumentBuilder<CommandSource> getUnpunishedUser() {
        return BrigadierCommand.literalArgumentBuilder("user")
                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                        .suggests(this::getTypeSuggestions)
                        .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                .suggests(this::getUserSuggestions)
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    String input = StringArgumentType.getString(context, "type");
                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                    if (!PunishmentType.exists(type)) {
                                        sendMessage(source, "<#990000>The type %s does not exist.", type);
                                        return -1;
                                    }

                                    int typeId = type.getId();

                                    String username = StringArgumentType.getString(context, "user");
                                    if (isUserNotExist(source, user, username)) return -2;

                                    int userId = user.getId(username);

                                    if (!punishUser.isPunished(userId, typeId)) {
                                        sendMessage(source, "<#990000>%s is not punished for %s.", username, type);
                                        return -3;
                                    }

                                    punishUser.unpunished(userId, typeId);
                                    sendMessage(source, "<#009999>%s has been unpunished for %s.", username, type);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUnpunishedIp() {
        return BrigadierCommand.literalArgumentBuilder("ip")
                .then(BrigadierCommand.requiredArgumentBuilder("ip", StringArgumentType.word())
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            /*String input = StringArgumentType.getString(context, "type");
                            PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                            if (!PunishmentType.exists(type)) {
                                sendMessage(source, "<#990000>The type %s does not exist.", type);
                                return -1;
                            }

                            int typeId = type.getId();*/
                            int typeId = PunishmentType.IP_BAN.getId();
                            String inputIp = StringArgumentType.getString(context, "ip");
                            InetAddress inetAddress;
                            try {
                                inetAddress = InetAddress.getByName(inputIp);
                            } catch (UnknownHostException e) {
                                sendMessage(source, "<#cc0088>Invalid ip address. Input: <#009999>%s", inputIp);
                                return -2;
                            }

                            if (!punishIP.isPunished(inetAddress, typeId)) {
                                sendMessage(source, "<#990000>%s is not punished.", inputIp);
                                return -2;
                            }

                            punishIP.unpunished(inetAddress, typeId);
                            sendMessage(source, "<#009999>%s has been unpunished.", inputIp);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private CompletableFuture<Suggestions> getUserSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String input = StringArgumentType.getString(context, "type");
        PunishmentType type = PunishmentType.fromString(input.toUpperCase());
        if (!PunishmentType.exists(type)) return builder.buildFuture();
        punishUser.getUsers(type.getId())
                .forEach(id -> builder.suggest(user.getUsername(id)));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> getTypeSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        Arrays.stream(PunishmentType.VALUES)
                .filter(type -> !type.isTypeIp())
                .toList()
                .forEach(type -> builder.suggest(type.getName()));
        return builder.buildFuture();
    }

    private void syntax(CommandSource source) {
        sendMessage(source, """
                <#009999>Syntax:
                <#454545>- <#999999>/unpunished <#999900>user</#999900> <type> <user> <reset>- Unpunished a user
                <#454545>- <#999999>/unpunished <#999900>ip</#999900> <ip> <reset>- Unpunished a ip address""");
    }
}
