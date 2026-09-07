package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.excuse.UserExcuse;
import de.murmelmeister.murmelapi.user.excuse.UserExcuseProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@CommandConfig(id = "excuse", name = "excuse")
public final class ExcuseCommand extends CommandManager {
    private final UserExcuseProvider excuseProvider;
    private final UserProvider userProvider;

    /*
     * Excuse Command: (Admin Command)
     * /excuse create <user> <start> [amount] [reason]
     * /excuse edit <id> <start|amount|reason> <value>
     * /excuse show [user] [page]
     */

    public ExcuseCommand(@NotNull MurmelEssentials plugin) {
        super(plugin);
        this.excuseProvider = plugin.getUserExcuseProvider();
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "excuse"))
                .then(BrigadierCommand.literalArgumentBuilder("create")
                        .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                .suggests(this::suggestUsername)
                                .then(BrigadierCommand.requiredArgumentBuilder("start", StringArgumentType.string())
                                        .suggests(this::suggestStartDate)
                                        .executes(this::executeCreate)
                                        .then(BrigadierCommand.requiredArgumentBuilder("amount", IntegerArgumentType.integer(0))
                                                .executes(this::executeCreate)
                                                .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.greedyString())
                                                        .executes(this::executeCreate)
                                                )
                                        )
                                        .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.greedyString())
                                                .executes(this::executeCreate)
                                        )
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("edit")
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> {
                                    excuseProvider.findAll().stream()
                                            .map(excuse -> String.valueOf(excuse.id()))
                                            .filter(id -> id.startsWith(builder.getRemaining()))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(BrigadierCommand.literalArgumentBuilder("start")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                                .suggests(this::suggestStartDate)
                                                .executes(context -> executeEdit(context, "start"))))
                                .then(BrigadierCommand.literalArgumentBuilder("amount")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", IntegerArgumentType.integer(0))
                                                .executes(context -> executeEdit(context, "amount"))))
                                .then(BrigadierCommand.literalArgumentBuilder("reason")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .executes(context -> executeEdit(context, "reason"))))
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("show")
                        .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                .suggests(this::suggestUsername)
                                .executes(context -> this.executeShow(context, 1))
                                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                                        .executes(context ->
                                                this.executeShow(context, IntegerArgumentType.getInteger(context, "page"))
                                        )
                                )
                        )
                )
                ;
    }

    private int executeCreate(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputUser = StringArgumentType.getString(context, "user");
            User user = getUser(inputUser);

            String inputStart = StringArgumentType.getString(context, "start");
            LocalDate startDate = parseStartDate(inputStart);
            int amount = getOptionalInt(context, "amount");
            String reason = getOptionalString(context, "reason");

            UserExcuse excuse = excuseProvider.create(user.id(), startDate, amount, reason, executor.id())
                    .orElseThrow(() -> new CommandException("Invalid excuse"));

            sendRawMessage(source, executor.languageId(),
                    "<#999999>Created excuse for <#00cc99><username></#00cc99> start at <#99cc00><start></#99cc00> and end at <#99cc00><end></#99cc00>.",
                    tagParsed("username", user.username()),
                    tagParsed("start", excuse.startDate()),
                    tagParsed("end", excuse.startDate().plusDays(excuse.extraDays()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeEdit(CommandContext<CommandSource> context, String field) {
        return runWithTiming(context, (source, executor) -> {
            int id = IntegerArgumentType.getInteger(context, "id");
            UserExcuse excuse = excuseProvider.findById(id)
                    .orElseThrow(() -> new CommandException("No excuse found with id: " + id));

            LocalDate startDate = excuse.startDate();
            int amount = excuse.extraDays();
            String reason = excuse.reason();
            switch (field) {
                case "start" -> startDate = parseStartDate(StringArgumentType.getString(context, "value"));
                case "amount" -> amount = IntegerArgumentType.getInteger(context, "value");
                case "reason" -> reason = StringArgumentType.getString(context, "value");
                default -> throw new CommandException("Unknown field: " + field);
            }

            excuseProvider.update(id, startDate, amount, reason, executor.id())
                    .orElseThrow(() -> new CommandException("Could not update excuse with id: " + id));
            sendRawMessage(source, executor.languageId(),
                    "<#999999>Updated excuse with ID <#00cc99><id></#00cc99>.",
                    tagParsed("id", id));
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private LocalDate parseStartDate(String input) {
        try {
            return LocalDate.parse(input, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException e) {
            throw new CommandException("Invalid start date: " + input);
        }
    }

    private int executeShow(CommandContext<CommandSource> context, int page) {
        return runWithTiming(context, (source, executor) -> {
            String inputUser = StringArgumentType.getString(context, "user");
            User user = getUser(inputUser);

            List<UserExcuse> excuses = excuseProvider.findByUserId(user.id());
            sendRawMessage(source, executor.languageId(),
                    "<#999999><username> <excuse>:</#999999>",
                    tagParsed("username", user.username()),
                    tagParsed("excuse", excuses.size() == 1 ? "excuse" : "excuses")
            );

            String message = "<#999999>-</#999999> <hover:show_text:'" +
                    "<#999999>Start date: <#0099cc><start></#0099cc></#999999> <br>" +
                    "<#999999>Extra days: <#0099cc><extra></#0099cc></#999999> <br>" +
                    "<#999999>End date: <#0099cc><end></#0099cc></#999999> <br>" +
                    "<#999999>Reason: <#0099cc><reason></#0099cc></#999999> <br>" +
                    "<#999999>Created by: <#0099cc><created_name></#0099cc> <#555555>(ID: <#0099cc><created_id></#0099cc>)</#555555> at <#0099cc><created_at></#0099cc></#999999>" +
                    "<changed>" +
                    "'><#00cc99><id></#00cc99></hover>";
            List<Component> messages = excuses.stream()
                    .map(excuse -> {
                        Integer changedBy = excuse.changedBy();
                        LocalDateTime changedAt = excuse.changedAt();
                        Component changed;
                        if (changedBy != null && changedAt != null) {
                            User changer = userProvider.findById(changedBy)
                                    .orElseThrow(() -> new CommandException("Invalid changed by: " + changedBy));

                            changed = component("<br><#999999>Changed by: <#0099cc><changed_name></#0099cc> <#555555>(ID: <#0099cc><changed_id></#0099cc>)</#555555> " +
                                            "at <#0099cc><changed_at></#0099cc></#999999>",
                                    tagParsed("changed_name", changer.username()),
                                    tagParsed("changed_id", changer.id()),
                                    tagParsed("changed_at", changedAt.format(getDateTimeFormatter(executor.languageId())))
                            );
                        } else {
                            changed = Component.empty();
                        }

                        User creator = userProvider.findById(excuse.createdBy())
                                .orElseThrow(() -> new CommandException("Invalid creator: " + excuse.createdBy()));

                        return component(message,
                                tagParsed("start", excuse.startDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))), // TODO: lang support
                                tagParsed("extra", excuse.extraDays()),
                                tagParsed("end", excuse.startDate().plusDays(excuse.extraDays()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                                tagParsed("reason", excuse.reason()),
                                tagParsed("created_name", creator.username()),
                                tagParsed("created_id", creator.id()),
                                tagParsed("created_at", excuse.createdAt().format(getDateTimeFormatter(executor.languageId()))),
                                Placeholder.component("changed", changed),
                                tagParsed("id", excuse.id())
                        );
                    })
                    .toList();

            sendPagedMessage(source, messages, "excuse show", page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private String getOptionalString(CommandContext<CommandSource> context, String argumentName) {
        try {
            return StringArgumentType.getString(context, argumentName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private int getOptionalInt(CommandContext<CommandSource> context, String argumentName) {
        try {
            return IntegerArgumentType.getInteger(context, argumentName);
        } catch (IllegalArgumentException ignored) {
            return 0;
        }
    }

    private CompletableFuture<Suggestions> suggestUsername(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        userProvider.findUsernames().stream()
                .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                .sorted()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestStartDate(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        builder.suggest(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        return builder.buildFuture();
    }
}
