package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public final class ReasonCommand extends CommandManager {
    private final PunishmentReasonProvider reasonProvider;
    private final MessageService messageService;

    public ReasonCommand(MurmelEssentials plugin) {
        super(plugin);
        this.reasonProvider = plugin.getPunishmentReasonProvider();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("reason")
                .requires(source -> source.hasPermission("murmel.command.reason"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            int languageId = executor.languageId();
                            List<PunishmentReason> reasons = reasonProvider.getAllReasons();

                            if (reasons.isEmpty()) {
                                sendMessage(source, "<#990000>No punishment reasons available.");
                                return CommandResult.of(-2);
                            }

                            sendMessage(source, "<#999999>===- %s:", reasons.size() == 1 ? "Reason" : "Reasons");
                            reasons.forEach(reason -> {
                                int reasonId = reason.id();
                                PunishmentType type = PunishmentType.fromId(reason.typeId());
                                String reasonText = reason.reasonText();
                                Long duration = reason.durationSecs();
                                boolean isAutoIpFlag = reason.autoFlagIp();
                                boolean isAutoPunish = reason.autoPunish();
                                User creator = getUser(languageId, reason.createdBy());
                                String createdDate = reason.createdAt().format(getDateTimeFormatter(languageId));
                                User changer = reason.changedBy() == null ? null : getUser(languageId, reason.changedBy());
                                String changedDate = reason.changedAt() == null ? null : reason.changedAt().format(getDateTimeFormatter(languageId));
                                String changedText = (changer == null || changedDate == null) ? null :
                                        String.format("<#999999>Changed by <#999900>%s (%d)</#999900> on <#999900>%s</#999900>",
                                                changer.username(), changer.id(), changedDate);

                                String hoverText = """
                                        <#999999>ID: <#999900>%d
                                        <#999999>Type: <#999900>%s (%d)
                                        <#999999>Reason: <#999900>%s
                                        <#999999>Duration: <#999900>%s
                                        <#999999>Auto IP Flag: <#999900>%s
                                        <#999999>Auto Punish: <#999900>%s
                                        <#999999>Created by <#999900>%s (%d)</#999900> on <#999900>%s %s"""
                                        .formatted(reasonId, type.getName(), type.getId(),
                                                reasonText, duration != null ? TimeUtil.formatDuration(messageService, languageId, duration) : "Permanent",
                                                isAutoIpFlag ? "<#00cc88>Yes" : "<#cc0088>No",
                                                isAutoPunish ? "<#00cc88>Yes" : "<#cc0088>No",
                                                creator.username(), creator.id(), createdDate,
                                                changedText == null ? "" : "\n" + changedText);
                                sendMessage(source, "<#999999>- <#00cc88><hover:show_text:'%s'>%s (%s)</hover>", hoverText, reasonId, reasonText);
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(getCommandAdd())
                .then(getCommandRemove())
                .then(getCommandUpdate())
                .then(getCommandHelp())
                .build();
        return new BrigadierCommand(node);
    }

    private LiteralArgumentBuilder<CommandSource> getCommandAdd() {
        return BrigadierCommand.literalArgumentBuilder("add")
                .requires(source -> source.hasPermission("murmel.command.reason.add"))
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                        .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                                .suggests(getSuggestionTypes())
                                .then(BrigadierCommand.requiredArgumentBuilder("duration", StringArgumentType.word())
                                        .suggests(getSuggestionTime())
                                        .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.string())
                                                .executes(context ->
                                                        runWithTiming(context, (source, executor) -> {
                                                            int id = IntegerArgumentType.getInteger(context, "id");
                                                            if (reasonProvider.getReason(id) != null) {
                                                                sendMessage(source, "<#990000>Reason with ID %d already exists.", id);
                                                                return CommandResult.of(-2);
                                                            }

                                                            String typeName = StringArgumentType.getString(context, "type");
                                                            PunishmentType type = getType(typeName);

                                                            String time = StringArgumentType.getString(context, "duration");
                                                            long duration = parseTime(executor.languageId(), time);
                                                            String reasonText = StringArgumentType.getString(context, "reason");

                                                            PunishmentReason success = reasonProvider.create(id, type.getId(), reasonText, duration == -1 ? null : duration, true, false, executor.id());
                                                            sendMessage(source, "<#00cc88>Added new punishment reason: %s (%s)", id, reasonText);
                                                            return CommandResult.of(Command.SINGLE_SUCCESS, success != null ? 1 : null);
                                                        })
                                                )
                                        )
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getCommandRemove() {
        return BrigadierCommand.literalArgumentBuilder("remove")
                .requires(source -> source.hasPermission("murmel.command.reason.remove"))
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                        .suggests(getSuggestionReasons())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int id = IntegerArgumentType.getInteger(context, "id");
                                    PunishmentReason reason = getReason(id);

                                    int result = reasonProvider.delete(reason.id());
                                    sendMessage(source, "<#00cc88>Removed punishment reason with ID %d.", reason.id());
                                    return CommandResult.of(Command.SINGLE_SUCCESS, result < 1 ? null : 1);
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getCommandUpdate() {
        return BrigadierCommand.literalArgumentBuilder("update")
                .requires(source -> source.hasPermission("murmel.command.reason.update"))
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                        .suggests(getSuggestionReasons())
                        .then(BrigadierCommand.requiredArgumentBuilder("field", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("typeId", VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>Type ID")));
                                    builder.suggest("reason", VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>Reason")));
                                    builder.suggest("duration", VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>Duration")));
                                    builder.suggest("autoFlagIp", VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>Auto IP Flag")));
                                    builder.suggest("autoPunish", VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>Auto Punish")));
                                    return builder.buildFuture();
                                })
                                .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String field = StringArgumentType.getString(context, "field");
                                            if ("typeId".equals(field))
                                                return getSuggestionTypes().getSuggestions(context, builder);
                                            else if ("duration".equals(field))
                                                return getSuggestionTime().getSuggestions(context, builder);
                                            else
                                                return builder.buildFuture();
                                        })
                                        .executes(context ->
                                                runWithTiming(context, (source, executor) -> {
                                                    int id = IntegerArgumentType.getInteger(context, "id");
                                                    PunishmentReason reason = getReason(id);
                                                    String field = StringArgumentType.getString(context, "field");
                                                    String value = StringArgumentType.getString(context, "value");

                                                    int typeId = reason.typeId();
                                                    String reasonText = reason.reasonText();
                                                    Long durationSecs = reason.durationSecs();
                                                    boolean autoFlagIp = reason.autoFlagIp();
                                                    boolean autoPunish = reason.autoPunish();
                                                    switch (field) {
                                                        case "typeId" -> {
                                                            PunishmentType type = getType(value);
                                                            typeId = type.getId();
                                                        }
                                                        case "reason" -> reasonText = value;
                                                        case "duration" -> {
                                                            long duration = parseTime(executor.languageId(), value);
                                                            durationSecs = (duration == -1) ? null : duration;
                                                        }
                                                        case "autoFlagIp" -> autoFlagIp = Boolean.parseBoolean(value);
                                                        case "autoPunish" -> autoPunish = Boolean.parseBoolean(value);
                                                        default -> {
                                                            sendMessage(source, "<#990000>Unknown field: %s", field);
                                                            return CommandResult.of(-2);
                                                        }
                                                    }

                                                    PunishmentReason success = reasonProvider.update(reason.id(), typeId, reasonText,
                                                            durationSecs, autoFlagIp, autoPunish, executor.id());
                                                    sendMessage(source, "<#00cc88>Updated punishment reason with ID %d.", id);
                                                    return CommandResult.of(Command.SINGLE_SUCCESS, success != null ? 1 : null);
                                                })
                                        )
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getCommandHelp() {
        return BrigadierCommand.literalArgumentBuilder("help")
                .requires(source -> source.hasPermission("murmel.command.reason.help"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                                    sendMessage(context.getSource(), syntax());
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                }
                        )
                );
    }

    private SuggestionProvider<CommandSource> getSuggestionReasons() {
        return (context, builder) -> {
            reasonProvider.getAllReasons().forEach(reason ->
                    builder.suggest(String.valueOf(reason.id()),
                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + reason.reasonText())))
            );
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSource> getSuggestionTypes() {
        return (context, builder) -> {
            for (PunishmentType type : PunishmentType.VALUES) {
                builder.suggest(type.getName().toLowerCase(),
                        VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + type.getName())));
            }
            return builder.buildFuture();
        };
    }

    private PunishmentReason getReason(int id) {
        PunishmentReason reason = reasonProvider.getReason(id);
        if (reason == null)
            throw new CommandException("Reason with ID " + id + " does not exist.");
        return reason;
    }

    private PunishmentType getType(String name) {
        PunishmentType type = PunishmentType.fromName(name);
        if (type == null)
            throw new CommandException("Invalid punishment type: " + name);
        return type;
    }

    private String syntax() {
        return """
                <#999999>Syntax:
                - <#999999>/reason</#999999> - List all available punishment reasons.
                - <#999999>/reason add<#00cc88> <id> <type> <duration> <reason> </#00cc88></#999999>- Add a new punishment reason.
                - <#999999>/reason remove<#00cc88> <id> </#00cc88></#999999> - Remove a punishment reason by ID.
                - <#999999>/reason update<#00cc88> <id> <typeId|reason|duration|autoFlagIp|autoPunish> <value> </#00cc88></#999999> - Update an existing punishment reason.
                - <#999999>/reason help</#999999> - Show this help message.""";
    }
}
