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
                .then(BrigadierCommand.literalArgumentBuilder("add")
                        .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason.add"))
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                .then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.word())
                                        .suggests(getSuggestionTypes())
                                        .then(BrigadierCommand.requiredArgumentBuilder("duration", StringArgumentType.word())
                                                .suggests(getSuggestionTime())
                                                .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.string())
                                                        .executes(this::executeAdd)
                                                )
                                        )
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("remove")
                        .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason.remove"))
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                .suggests(getSuggestionReasons())
                                .executes(this::executeRemove)
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("update")
                        .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "reason.update"))
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                .suggests(getSuggestionReasons())
                                .then(BrigadierCommand.literalArgumentBuilder("type")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .suggests(getSuggestionTypes())
                                                .executes(context -> executeUpdate(context, 0))
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("reason")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .executes(context -> executeUpdate(context, 1))
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("duration")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .suggests(getSuggestionTime())
                                                .executes(context -> executeUpdate(context, 2))
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("autoFlagIp")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .executes(context -> executeUpdate(context, 3))
                                        )
                                )
                        )
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

    private int executeAdd(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            int id = IntegerArgumentType.getInteger(context, "id");
            if (reasonProvider.findReason(id).isPresent())
                throw new CommandException(Message.PUNISHMENT_REASON_EXISTS, tagParsed("reason_id", id));

            String typeName = StringArgumentType.getString(context, "type");
            PunishmentType type = getType(typeName);

            String time = StringArgumentType.getString(context, "duration");
            long duration = parseTime(time);
            String reasonText = StringArgumentType.getString(context, "reason");

            PunishmentReason success = reasonProvider.upsert(id, type.getId(), reasonText, duration == -1 ? null : duration, true, executor.id())
                    .orElseThrow(() -> new CommandException(Message.COMMAND_REASON_ADD_FAILED, tagParsed("reason_id", id)));
            sendMessage(source, languageId,
                    Message.COMMAND_REASON_ADD_SUCCESS,
                    tagParsed("reason_id", success.id()),
                    tagParsed("reason_text", success.reasonText())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeRemove(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            int id = IntegerArgumentType.getInteger(context, "id");
            PunishmentReason reason = getReason(id);

            int result = reasonProvider.delete(reason.id());
            if (result != 1)
                throw new CommandException(Message.COMMAND_REASON_REMOVE_FAILED, tagParsed("reason_id", id));

            sendMessage(source, languageId,
                    Message.COMMAND_REASON_REMOVE_SUCCESS,
                    tagParsed("reason_id", id)
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeUpdate(CommandContext<CommandSource> context, int action) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            int id = IntegerArgumentType.getInteger(context, "id");
            PunishmentReason reason = getReason(id);
            String value = StringArgumentType.getString(context, "value");

            int typeId = reason.typeId();
            String reasonText = reason.reasonText();
            Long durationSecs = reason.durationSecs();
            boolean autoFlagIp = reason.autoFlagIp();

            switch (action) {
                case 0 -> {
                    PunishmentType type = getType(value);
                    typeId = type.getId();
                }
                case 1 -> reasonText = value;
                case 2 -> {
                    long duration = parseTime(value);
                    durationSecs = (duration == -1) ? null : duration;
                }
                case 3 -> autoFlagIp = Boolean.parseBoolean(value);
                default ->
                        throw new CommandException(Message.COMMAND_REASON_UPDATE_INVALID, tagUnparsed("action", value));
            }

            PunishmentReason success = reasonProvider.upsert(reason.id(), typeId, reasonText, durationSecs, autoFlagIp, executor.id())
                    .orElseThrow(() -> new CommandException(Message.COMMAND_REASON_UPDATE_FAILED, tagParsed("reason_id", reason.id())));

            sendMessage(source, languageId,
                    Message.COMMAND_REASON_UPDATE_SUCCESS,
                    tagUnparsed("reason_id", success.id())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
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
        return reasonProvider.findReason(id).orElseThrow(() -> new CommandException(Message.PUNISHMENT_REASON_NOT_FOUND, tagParsed("reason_id", id)));
    }

    private @NotNull PunishmentType getType(@NotNull String name) {
        return PunishmentType.fromName(name).orElseThrow(() -> new CommandException(Message.PUNISHMENT_TYPE_NOT_FOUND, tagUnparsed("type", name)));
    }
}
