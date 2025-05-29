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
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import de.murmelmeister.murmelapi.punishment.reason.Reason;
import de.murmelmeister.murmelapi.punishment.reason.ReasonProvider;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public final class ReasonCommand extends CommandManager {
    private final ReasonProvider reasonProvider;

    public ReasonCommand(MurmelEssentials plugin) {
        super(plugin);
        this.reasonProvider = plugin.getReasonProvider();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("reason")
                .requires(source -> source.hasPermission("murmel.command.reason"))
                .executes(context ->
                        runWithTiming(context, (source, executorId) -> {
                            List<Reason> reasons = reasonProvider.getReasons();

                            if (reasons.isEmpty()) {
                                sendMessage(source, "<#990000>No punishment reasons available.");
                                return CommandResult.of(-2);
                            }

                            sendMessage(source, "<#999999>===- %s:", reasons.size() == 1 ? "Reason" : "Reasons");
                            reasons.forEach(reason -> {
                                int reasonId = reason.getId();
                                PunishmentType type = PunishmentType.fromId(reason.getTypeId());
                                String reasonText = reason.getReason();
                                long duration = reason.getDuration();
                                boolean isAutoIpFlag = reason.isAutoIpFlag();
                                boolean isAutoPunish = reason.isAutoPunish();
                                int createdBy = reason.getCreatedBy();
                                String createdByName = user.getUsername(createdBy);
                                String createdDate = reason.getCreatedDate();
                                int updatedBy = reason.getUpdatedBy();
                                String updatedDate = reason.getUpdatedDate();
                                String updatedByName = user.getUsername(updatedBy);
                                String hoverText = """
                                        ID: %d
                                        Type: %s (%d)
                                        Reason: %s
                                        Duration: %s
                                        Auto IP Flag: %s
                                        Auto Punish: %s
                                        Created By: %s (%d) on %s
                                        Updated By: %s (%d) on %s"""
                                        .formatted(reasonId, type.getName(), type.getId(),
                                                reasonText, duration > 0 ? TimeUtil.formatTimeValue(duration) : "Permanent",
                                                isAutoIpFlag ? "<#00cc88>Yes" : "<#cc0088>No",
                                                isAutoPunish ? "<#00cc88>Yes" : "<#cc0088>No",
                                                createdByName, createdBy, createdDate,
                                                updatedByName, updatedBy, updatedDate);
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
                                                        runWithTiming(context, (source, executorId) -> {
                                                            int id = IntegerArgumentType.getInteger(context, "id");
                                                            if (reasonProvider.get(id) != null) {
                                                                sendMessage(source, "<#990000>Reason with ID %d already exists.", id);
                                                                return CommandResult.of(-2);
                                                            }

                                                            String typeName = StringArgumentType.getString(context, "type");
                                                            PunishmentType type = PunishmentType.fromString(typeName);
                                                            if (type == null) {
                                                                sendMessage(source, "<#990000>Invalid punishment type: %s", typeName);
                                                                return CommandResult.of(-3);
                                                            }

                                                            String time = StringArgumentType.getString(context, "duration");
                                                            long duration = TimeUtil.formatTime(time);

                                                            if (duration == -2) {
                                                                sendMessage(source, "<#990000>No negative value allowed");
                                                                return CommandResult.of(-4);
                                                            }

                                                            if (duration == -3) {
                                                                sendMessage(source, "<#990000>Invalid time format");
                                                                return CommandResult.of(-5);
                                                            }

                                                            String reasonText = context.getArgument("reason", String.class);

                                                            reasonProvider.create(id, type.getId(), reasonText, duration, true, false, executorId);
                                                            sendMessage(source, "<#00cc88>Added new punishment reason: %s (%s)", id, reasonText);
                                                            return CommandResult.of(Command.SINGLE_SUCCESS);
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
                                runWithTiming(context, (source, executorId) -> {
                                    int id = IntegerArgumentType.getInteger(context, "id");
                                    if (reasonProvider.get(id) == null) {
                                        sendMessage(source, "<#990000>Reason with ID %d does not exist.", id);
                                        return CommandResult.of(-2);
                                    }

                                    int row = reasonProvider.delete(id);
                                    sendMessage(source, "<#00cc88>Removed punishment reason with ID %d.", id);
                                    return CommandResult.of(Command.SINGLE_SUCCESS, row);
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getCommandUpdate() {
        return BrigadierCommand.literalArgumentBuilder("update")
                .requires(source -> source.hasPermission("murmel.command.reason.update"))
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
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
                                                runWithTiming(context, (source, executorId) -> {
                                                    int id = IntegerArgumentType.getInteger(context, "id");
                                                    Reason reason = reasonProvider.get(id);
                                                    if (reason == null) {
                                                        sendMessage(source, "<#990000>Reason with ID %d does not exist.", id);
                                                        return CommandResult.of(-2);
                                                    }

                                                    String field = StringArgumentType.getString(context, "field");
                                                    String value = StringArgumentType.getString(context, "value");

                                                    switch (field) {
                                                        case "typeId" -> {
                                                            PunishmentType type = PunishmentType.fromString(value);
                                                            if (type == null) {
                                                                sendMessage(source, "<#990000>Invalid punishment type: %s", value);
                                                                return CommandResult.of(-3);
                                                            }
                                                            reason.setTypeId(type.getId());
                                                        }
                                                        case "reason" -> reason.setReason(value);
                                                        case "duration" -> {
                                                            long duration = TimeUtil.formatTime(value);
                                                            if (duration == -2) {
                                                                sendMessage(source, "<#990000>No negative value allowed");
                                                                return CommandResult.of(-4);
                                                            }
                                                            if (duration == -3) {
                                                                sendMessage(source, "<#990000>Invalid time format");
                                                                return CommandResult.of(-5);
                                                            }
                                                            reason.setDuration(duration);
                                                        }
                                                        case "autoFlagIp" ->
                                                                reason.setAutoIpFlag(Boolean.parseBoolean(value));
                                                        case "autoPunish" ->
                                                                reason.setAutoPunish(Boolean.parseBoolean(value));
                                                        default -> {
                                                            sendMessage(source, "<#990000>Unknown field: %s", field);
                                                            return CommandResult.of(-6);
                                                        }
                                                    }

                                                    reasonProvider.update(id, reason);
                                                    sendMessage(source, "<#00cc88>Updated punishment reason with ID %d.", id);
                                                    return CommandResult.of(Command.SINGLE_SUCCESS);
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
                        runWithTiming(context, (source, executorId) -> {
                                    sendMessage(context.getSource(), syntax());
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                }
                        )
                );
    }

    private SuggestionProvider<CommandSource> getSuggestionReasons() {
        return (context, builder) -> {
            reasonProvider.getReasons().forEach(reason ->
                    builder.suggest(String.valueOf(reason.getId()),
                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + reason.getReason())))
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
