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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

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
                        runWithTiming(context, (source, executor) -> {
                            int languageId = executor.languageId();
                            List<PunishmentReason> reasons = reasonProvider.findAll();

                            if (reasons.isEmpty()) {
                                sendRawMessage(source, languageId, "<#990000>No punishment reasons available.");
                                return CommandResult.of(Command.SINGLE_SUCCESS);
                            }

                            sendRawMessage(source, languageId, "<#999999>===- <header_name>:", tagUnparsed("header_name", reasons.size() == 1 ? "Reason" : "Reasons"));
                            reasons.forEach(reason -> {
                                int reasonId = reason.id();
                                PunishmentType type = PunishmentType.fromId(reason.typeId()).orElse(null);
                                if (type == null) return;
                                String reasonText = reason.reasonText();
                                Long duration = reason.durationSecs();
                                boolean isAutoIpFlag = reason.autoFlagIp();
                                User creator = getUser(reason.createdBy());
                                String createdDate = reason.createdAt().format(getDateTimeFormatter(languageId));
                                User changer = reason.changedBy() == null ? null : getUser(reason.changedBy());
                                String changedDate = reason.changedAt() == null ? null : reason.changedAt().format(getDateTimeFormatter(languageId));

                                Component changedText = (changer == null || changedDate == null) ? Component.empty() :
                                        MiniMessage.miniMessage().deserialize(messageService.getMessage(Message.PERMISSION_INFO_CHANGE_STUFF.getTag(), languageId),
                                                tagUnparsed("changed_name", changer.username()),
                                                tagUnparsed("changed_id", String.valueOf(changer.id())),
                                                tagUnparsed("changed_at", changedDate));

                                Component hoverText = MiniMessage.miniMessage().deserialize("""
                                                <#999999>ID: <#999900><reason_id>
                                                <#999999>Type: <#999900><type_name> (<type_id>)
                                                <#999999>Reason: <#999900><text>
                                                <#999999>Duration: <#999900><duration>
                                                <#999999>Auto IP Flag: <#999900><auto_ip_flag>
                                                <#999999>Created by <#999900><created_name> (<created_id>)</#999900> on <#999900><created_at>
                                                <changed>""",
                                        tagUnparsed("reason_id", String.valueOf(reasonId)),
                                        tagUnparsed("type_name", type.getName()), tagUnparsed("type_id", String.valueOf(type.getId())),
                                        tagUnparsed("text", reasonText),
                                        tagUnparsed("duration", duration != null ? TimeUtil.formatDuration(messageService, languageId, duration) : "Permanent"),
                                        tagParsed("auto_ip_flag", isAutoIpFlag ? "<#00cc88>Yes" : "<#cc0088>No"),
                                        tagUnparsed("created_name", creator.username()), Placeholder.unparsed("created_id", String.valueOf(creator.id())),
                                        tagUnparsed("created_at", createdDate),
                                        Placeholder.component("changed", changedText)
                                );
                                sendRawMessage(source, languageId, "<#999999>- <#00cc88><hover:show_text:'<hover_text>'><reason_id> (<reason_text>)</hover>",
                                        Placeholder.component("hover_text", hoverText),
                                        tagUnparsed("reason_id", String.valueOf(reasonId)),
                                        tagUnparsed("reason_text", reasonText));
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
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
