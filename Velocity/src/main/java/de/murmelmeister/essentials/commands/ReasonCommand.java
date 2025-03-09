package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.TimeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ReasonCommand extends CommandManager {
    private final User user;
    private final PunishmentReason reason;
    private final Permission permission;

    public ReasonCommand(User user, PunishmentReason reason, Permission permission) {
        this.user = user;
        this.reason = reason;
        this.permission = permission;
    }

    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder("reason")
                .requires(source -> source.hasPermission("murmelessentials.command.reason"))
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(getReasonAdd())
                .then(getReasonRemove())
                .then(getReasonList())
                .then(getReasonEdit())
                .build();
        return new BrigadierCommand(rootNode);
    }

    private LiteralArgumentBuilder<CommandSource> getReasonAdd() {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                        .suggests(this::getPunishTypes)
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer())
                                .then(BrigadierCommand.requiredArgumentBuilder("duration", StringArgumentType.word())
                                        .suggests(this::getSuggestionTime)
                                        .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.string())
                                                .executes(context -> {
                                                    int executorId = getExecutorId(context, user);
                                                    CommandSource source = context.getSource();
                                                    String input = StringArgumentType.getString(context, "type");
                                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                                    if (!PunishmentType.exists(type)) {
                                                        sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                                        return -1;
                                                    }

                                                    int typeId = type.getId();
                                                    int reasonId = IntegerArgumentType.getInteger(context, "id");

                                                    if (reason.exists(reasonId, typeId)) {
                                                        sendMessage(source, "<#990000>A reason with the ID %s already exists.", reasonId);
                                                        return -2;
                                                    }

                                                    String time = StringArgumentType.getString(context, "duration");
                                                    long duration = TimeUtil.formatTime(time);

                                                    if (duration == -2) {
                                                        sendMessage(source, "<#990000>No negative value allowed.");
                                                        return -3;
                                                    }

                                                    if (duration == -3) {
                                                        sendMessage(source, "<#990000>Invalid time format.");
                                                        return -4;
                                                    }

                                                    String message = StringArgumentType.getString(context, "reason");
                                                    reason.add(reasonId, typeId, executorId, message, duration, true, true);
                                                    sendMessage(source, "<#009999>Successfully added the reason with the ID <#999900>%s</#999900>.", reasonId);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getReasonRemove() {
        return BrigadierCommand.literalArgumentBuilder("remove")
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                        .suggests(this::getPunishTypes)
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer())
                                .suggests(this::getPunishIds)
                                .executes(context -> {
                                    int executorId = getExecutorId(context, user);
                                    CommandSource source = context.getSource();
                                    String input = StringArgumentType.getString(context, "type");
                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                    if (!PunishmentType.exists(type)) {
                                        sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                        return -1;
                                    }

                                    int typeId = type.getId();
                                    int reasonId = IntegerArgumentType.getInteger(context, "id");

                                    if (!reason.exists(reasonId, typeId)) {
                                        sendMessage(source, "<#990000>A reason with the ID %s does not exist.", reasonId);
                                        return -2;
                                    }

                                    reason.remove(reasonId, typeId);
                                    sendMessage(source, "<#009999>Successfully removed the reason with the ID <#999900>%s</#999900>.", reasonId);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getReasonList() {
        return BrigadierCommand.literalArgumentBuilder("list")
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                        .suggests(this::getPunishTypes)
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            String input = StringArgumentType.getString(context, "type");
                            PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                            if (!PunishmentType.exists(type)) {
                                sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                return -1;
                            }

                            int typeId = type.getId();
                            List<Integer> reasons = reason.getReasons(typeId);

                            if (reasons.isEmpty()) {
                                sendMessage(source, "<#990000>No reasons found.");
                                return -2;
                            }

                            sendMessage(source, "<#009999>%s reasons:", type);
                            sendMessage(source, "<#999900>ID <#999999>|</#999999> Reason <#999999>|</#999999> Duration <#999999>|</#999999> AutoFlagIP <#999999>|</#999999> AutoPunish");
                            reasons.forEach(id -> {
                                String message = reason.getReason(id, typeId);
                                long duration = reason.getDuration(id, typeId);
                                boolean autoFlagIp = reason.getAutoFlagIP(id, typeId);
                                boolean autoPunish = reason.getAutoPunish(id, typeId);
                                sendMessage(source, "<#999999>- <#999900>%s</#999900> - <#999900>%s</#999900> - <#999900>%s</#999900> - <#999900>%s</#999900> - <#999900>%s</#999900>",
                                        id, message, TimeUtil.formatTimeValue(duration), autoFlagIp, autoPunish);
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getReasonEdit() {
        return BrigadierCommand.literalArgumentBuilder("edit")
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                        .suggests(this::getPunishTypes)
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer())
                                .suggests(this::getPunishIds)
                                .then(BrigadierCommand.literalArgumentBuilder("duration")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.word())
                                                .suggests(this::getSuggestionTime)
                                                .executes(context -> {
                                                    int executorId = getExecutorId(context, user);
                                                    CommandSource source = context.getSource();
                                                    String input = StringArgumentType.getString(context, "type");
                                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                                    if (!PunishmentType.exists(type)) {
                                                        sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                                        return -1;
                                                    }

                                                    int typeId = type.getId();
                                                    int reasonId = IntegerArgumentType.getInteger(context, "id");

                                                    if (!reason.exists(reasonId, typeId)) {
                                                        sendMessage(source, "<#990000>A reason with the ID %s does not exist.", reasonId);
                                                        return -2;
                                                    }

                                                    String time = StringArgumentType.getString(context, "value");
                                                    long duration = TimeUtil.formatTime(time);

                                                    if (duration == -2) {
                                                        sendMessage(source, "<#990000>No negative value allowed.");
                                                        return -3;
                                                    }

                                                    if (duration == -3) {
                                                        sendMessage(source, "<#990000>Invalid time format.");
                                                        return -4;
                                                    }

                                                    reason.setDuration(reasonId, typeId, executorId, duration);
                                                    sendMessage(source, "<#009999>Successfully edited the duration of the reason with the ID <#999900>%s</#999900>.", reasonId);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("reason")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                                .executes(context -> {
                                                    int executorId = getExecutorId(context, user);
                                                    CommandSource source = context.getSource();
                                                    String input = StringArgumentType.getString(context, "type");
                                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                                    if (!PunishmentType.exists(type)) {
                                                        sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                                        return -1;
                                                    }

                                                    int typeId = type.getId();
                                                    int reasonId = IntegerArgumentType.getInteger(context, "id");

                                                    if (!reason.exists(reasonId, typeId)) {
                                                        sendMessage(source, "<#990000>A reason with the ID %s does not exist.", reasonId);
                                                        return -2;
                                                    }

                                                    String message = StringArgumentType.getString(context, "value");
                                                    reason.setReason(reasonId, typeId, executorId, message);
                                                    sendMessage(source, "<#009999>Successfully edited the reason with the ID <#999900>%s</#999900>.", reasonId);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("autoFlagIp")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    int executorId = getExecutorId(context, user);
                                                    CommandSource source = context.getSource();
                                                    String input = StringArgumentType.getString(context, "type");
                                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                                    if (!PunishmentType.exists(type)) {
                                                        sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                                        return -1;
                                                    }

                                                    int typeId = type.getId();
                                                    int reasonId = IntegerArgumentType.getInteger(context, "id");

                                                    if (!reason.exists(reasonId, typeId)) {
                                                        sendMessage(source, "<#990000>A reason with the ID %s does not exist.", reasonId);
                                                        return -2;
                                                    }

                                                    boolean autoFlagIp = BoolArgumentType.getBool(context, "value");
                                                    reason.setAutoFlagIP(reasonId, typeId, executorId, autoFlagIp);
                                                    sendMessage(source, "<#009999>Successfully edited the auto flag ip of the reason with the ID <#999900>%s</#999900>.", reasonId);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("autoPunish")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    int executorId = getExecutorId(context, user);
                                                    CommandSource source = context.getSource();
                                                    String input = StringArgumentType.getString(context, "type");
                                                    PunishmentType type = PunishmentType.fromString(input.toUpperCase());

                                                    if (!PunishmentType.exists(type)) {
                                                        sendMessage(source, "<#990000>The type %s does not exist.", type.getName());
                                                        return -1;
                                                    }

                                                    int typeId = type.getId();
                                                    int reasonId = IntegerArgumentType.getInteger(context, "id");

                                                    if (!reason.exists(reasonId, typeId)) {
                                                        sendMessage(source, "<#990000>A reason with the ID %s does not exist.", reasonId);
                                                        return -2;
                                                    }

                                                    boolean autoPunish = BoolArgumentType.getBool(context, "value");
                                                    reason.setAutoPunish(reasonId, typeId, executorId, autoPunish);
                                                    sendMessage(source, "<#009999>Successfully edited the auto punish of the reason with the ID <#999900>%s</#999900>.", reasonId);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                );
    }

    private CompletableFuture<Suggestions> getPunishIds(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        /*int executorId = getExecutorId(context, user);
        String type = punishment.getTypes().getTypeName(typeId);
        punishment.getReason().getReasons(typeId).stream()
                .filter(id -> permission.hasPermission(executorId, MurmelEssentials.PERMISSION_PUNISHMENT_REASON + type + "." + id))
                .toList()
                .forEach(id -> builder.suggest(String.valueOf(id)));*/
        String input = StringArgumentType.getString(context, "type");
        PunishmentType type = PunishmentType.fromString(input.toUpperCase());
        if (!PunishmentType.exists(type)) return builder.buildFuture();
        reason.getReasons(type.getId()).forEach(id -> builder.suggest(String.valueOf(id)));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> getPunishTypes(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        int executorId = getExecutorId(context, user);
        Arrays.stream(PunishmentType.VALUES)
                .filter(type -> permission.hasPermission(executorId, MurmelEssentials.PERMISSION_PUNISHMENT_TYPE + type.getName()))
                .toList()
                .forEach(type -> builder.suggest(type.getName()));
        return builder.buildFuture();
    }

    private void syntax(CommandSource source) {
        sendMessage(source, """
                <#009999>Syntax:
                <#454545>- <#999999>/reason <#999900>add</#999900> <type> <id> <duration> <reason> <reset>- Add a new punish reason
                <#454545>- <#999999>/reason <#999900>remove</#999900> <type> <id> <reset>- Remove a punish reason
                <#454545>- <#999999>/reason <#999900>list</#999900> <type> <reset>- List all punish reasons
                <#454545>- <#999999>/reason <#999900>edit</#999900> <type> <id> duration <value> <reset>- Edit the duration of a punish reason
                <#454545>- <#999999>/reason <#999900>edit</#999900> <type> <id> reason <value> <reset>- Edit the reason of a punish reason
                <#454545>- <#999999>/reason <#999900>edit</#999900> <type> <id> autoFlag <value> <reset>- Edit the auto flag of a punish reason""");
    }
}
