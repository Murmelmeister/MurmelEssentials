package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@CommandConfig(id = "reason", name = "reason")
public final class ReasonCommand extends CommandManager {
    private final PunishmentReasonProvider reasonProvider;
    private final MessageService messageService;

    public ReasonCommand(MurmelEssentials plugin) {
        super(plugin);
        this.reasonProvider = plugin.getPunishmentReasonProvider();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason"))
                .executes(context ->
                        executeList(context, 1)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                executeList(context, IntegerArgumentType.getInteger(context, "page"))
                        )
                )
                .then(getCommandAdd())
                .then(getCommandRemove())
                .then(getCommandUpdate())
                .then(BrigadierCommand.literalArgumentBuilder("help")
                        .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason.help"))
                        .executes(this::executeHelp)
                )
                ;
    }

    private int executeList(CommandContext<CommandSource> context, int page) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            List<PunishmentReason> reasons = reasonProvider.findAll();

            if (reasons.isEmpty())
                throw new CommandException(Message.COMMAND_REASON_LIST_EMPTY);

            Message headerName = reasons.size() == 1
                    ? Message.COMMAND_REASON_LIST_SINGULAR
                    : Message.COMMAND_REASON_LIST_PLURAL;
            sendMessage(source, languageId,
                    Message.COMMAND_REASON_LIST_HEADER,
                    tagParsed("header_name", languageId, headerName),
                    tagParsed("register", reasons.size())
            );

            List<Component> messages = reasons.stream()
                    .map(entry -> {
                        Integer changerId = entry.changedBy();
                        LocalDateTime changedAt = entry.changedAt();
                        Component changedText;
                        if (changerId != null && changedAt != null) {
                            User changer = getUser(changerId);
                            changedText = component(languageId, Message.COMMAND_REASON_USE_HOVER_CHANGED,
                                    tagParsed("changer_name", changer.username()),
                                    tagParsed("changer_id", changer.id()),
                                    tagParsed("changed_at", changedAt.format(getDateTimeFormatter(languageId)))
                            );
                        } else changedText = Component.empty();

                        PunishmentType type = PunishmentType.fromId(entry.typeId())
                                .orElseThrow(() -> new CommandException(Message.PUNISHMENT_TYPE_NOT_FOUND, tagParsed("type_id", entry.typeId())));
                        User creator = getUser(entry.createdBy());
                        LocalDateTime createdAt = entry.createdAt();
                        Long duration = entry.durationSecs();
                        Component durationText = duration != null
                                ? component(TimeUtil.formatDuration(messageService, languageId, duration))
                                : component(languageId, Message.COMMAND_REASON_DURATION);

                        Component hoverText = component(languageId, Message.COMMAND_REASON_USE_HOVER_TEXT,
                                tagParsed("reason_id", entry.id()),
                                tagParsed("type_name", type.getName()),
                                tagParsed("type_id", type.getId()),
                                tagParsed("text", entry.reasonText()),
                                tagComponent("duration", durationText),
                                tagParsed("auto_ip_flag", languageId, entry.autoFlagIp() ? Message.MESSAGE_YES : Message.MESSAGE_NO),
                                tagParsed("creator_name", creator.username()),
                                tagParsed("creator_id", creator.id()),
                                tagParsed("created_at", createdAt.format(getDateTimeFormatter(languageId))),
                                tagComponent("changed", changedText)
                        );

                        return component(languageId, Message.COMMAND_REASON_USE_MESSAGE,
                                tagComponent("hover_text", hoverText),
                                tagParsed("reason_id", entry.id()),
                                tagParsed("reason_text", entry.reasonText())
                        );
                    })
                    .toList();

            sendPagedMessage(source, messages, "reason", page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private LiteralArgumentBuilder<CommandSource> getCommandAdd() {
        return BrigadierCommand.literalArgumentBuilder("add")
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason.add"))
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                        .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                                .suggests(getSuggestionTypes())
                                .then(BrigadierCommand.requiredArgumentBuilder("duration", StringArgumentType.word())
                                        .suggests(getSuggestionTime())
                                        .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.string())
                                                .executes(context ->
                                                        runWithTiming(context, (source, executor) -> {
                                                            int languageId = executor.languageId();
                                                            int id = IntegerArgumentType.getInteger(context, "id");
                                                            if (reasonProvider.findReason(id).isPresent()) {
                                                                sendRawMessage(source, languageId, "<#990000>Reason with ID <reason_id> already exists.", tagUnparsed("reason_id", String.valueOf(id)));
                                                                return CommandResult.of(Command.SINGLE_SUCCESS);
                                                            }

                                                            String typeName = StringArgumentType.getString(context, "type");
                                                            PunishmentType type = getType(typeName);

                                                            String time = StringArgumentType.getString(context, "duration");
                                                            long duration = parseTime(time);
                                                            String reasonText = StringArgumentType.getString(context, "reason");

                                                            Optional<PunishmentReason> success = reasonProvider.upsert(id, type.getId(), reasonText, duration == -1 ? null : duration, true, executor.id());
                                                            sendRawMessage(source, languageId, "<#00cc88>Added new punishment reason: <reason_id> (<reason_text>)",
                                                                    tagUnparsed("reason_id", String.valueOf(id)),
                                                                    tagUnparsed("reason_text", reasonText)
                                                            );
                                                            return CommandResult.of(Command.SINGLE_SUCCESS, success.isPresent() ? 1 : null);
                                                        })
                                                )
                                        )
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getCommandRemove() {
        return BrigadierCommand.literalArgumentBuilder("remove")
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason.remove"))
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                        .suggests(getSuggestionReasons())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    int id = IntegerArgumentType.getInteger(context, "id");
                                    PunishmentReason reason = getReason(id);

                                    int result = reasonProvider.delete(reason.id());
                                    sendRawMessage(source, languageId, "<#00cc88>Removed punishment reason with ID <reason_id>.", tagUnparsed("reason_id", String.valueOf(reason.id())));
                                    return CommandResult.of(Command.SINGLE_SUCCESS, result < 1 ? null : 1);
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getCommandUpdate() {
        return BrigadierCommand.literalArgumentBuilder("update")
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason.update"))
                .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                        .suggests(getSuggestionReasons())
                        .then(BrigadierCommand.requiredArgumentBuilder("field", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("typeId", tooltip("<#00cc88>Type ID"));
                                    builder.suggest("reason", tooltip("<#00cc88>Reason"));
                                    builder.suggest("duration", tooltip("<#00cc88>Duration"));
                                    builder.suggest("autoFlagIp", tooltip("<#00cc88>Auto IP Flag"));
                                    builder.suggest("autoPunish", tooltip("<#00cc88>Auto Punish"));
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
                                                    int languageId = executor.languageId();
                                                    int id = IntegerArgumentType.getInteger(context, "id");
                                                    PunishmentReason reason = getReason(id);
                                                    String field = StringArgumentType.getString(context, "field");
                                                    String value = StringArgumentType.getString(context, "value");

                                                    int typeId = reason.typeId();
                                                    String reasonText = reason.reasonText();
                                                    Long durationSecs = reason.durationSecs();
                                                    boolean autoFlagIp = reason.autoFlagIp();
                                                    switch (field) {
                                                        case "typeId" -> {
                                                            PunishmentType type = getType(value);
                                                            typeId = type.getId();
                                                        }
                                                        case "reason" -> reasonText = value;
                                                        case "duration" -> {
                                                            long duration = parseTime(value);
                                                            durationSecs = (duration == -1) ? null : duration;
                                                        }
                                                        case "autoFlagIp" -> autoFlagIp = Boolean.parseBoolean(value);
                                                        default -> {
                                                            sendRawMessage(source, languageId, "<#990000>Unknown field: <field>", tagUnparsed("field", field));
                                                            return CommandResult.of(Command.SINGLE_SUCCESS);
                                                        }
                                                    }

                                                    Optional<PunishmentReason> success = reasonProvider.upsert(reason.id(), typeId, reasonText,
                                                            durationSecs, autoFlagIp, executor.id());
                                                    sendRawMessage(source, languageId,
                                                            "<#00cc88>Updated punishment reason with ID <reason_id>.",
                                                            tagUnparsed("reason_id", String.valueOf(id))
                                                    );
                                                    return CommandResult.of(Command.SINGLE_SUCCESS, success.isPresent() ? 1 : null);
                                                })
                                        )
                                )
                        )
                );
    }

    private int executeHelp(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            sendRawMessage(source, executor.languageId(),
                    """
                            <#999999>Syntax:
                            - <#999999>/reason</#999999> - List all available punishment reasons.
                            - <#999999>/reason add<#00cc88> <id> <type> <duration> <reason> </#00cc88></#999999>- Add a new punishment reason.
                            - <#999999>/reason remove<#00cc88> <id> </#00cc88></#999999> - Remove a punishment reason by ID.
                            - <#999999>/reason update<#00cc88> <id> <typeId|reason|duration|autoFlagIp|autoPunish> <value> </#00cc88></#999999> - Update an existing punishment reason.
                            - <#999999>/reason help</#999999> - Show this help message."""
            );
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private @NotNull SuggestionProvider<CommandSource> getSuggestionReasons() {
        return (context, builder) -> {
            reasonProvider.findAll().forEach(reason ->
                    builder.suggest(String.valueOf(reason.id()),
                            tooltip(
                                    "<#00cc88><reason>",
                                    tagParsed("reason", reason.reasonText())
                            )
                    )
            );
            return builder.buildFuture();
        };
    }

    private @NotNull SuggestionProvider<CommandSource> getSuggestionTypes() {
        return (context, builder) -> {
            for (PunishmentType type : PunishmentType.values()) {
                builder.suggest(type.getName().toLowerCase(),
                        tooltip(
                                "<#00cc88><type>",
                                tagParsed("type", type.getName())
                        )
                );
            }
            return builder.buildFuture();
        };
    }

    private @NotNull PunishmentReason getReason(int id) {
        return reasonProvider.findReason(id).orElseThrow(() -> new CommandException("No punishment reason found with id: " + id));
    }

    private @NotNull PunishmentType getType(@NotNull String name) {
        return PunishmentType.fromName(name).orElseThrow(() -> new CommandException("Invalid punishment type: " + name));
    }
}
