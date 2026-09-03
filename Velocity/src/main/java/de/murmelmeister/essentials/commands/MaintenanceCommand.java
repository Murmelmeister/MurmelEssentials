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
import de.murmelmeister.murmelapi.maintenance.Maintenance;
import de.murmelmeister.murmelapi.maintenance.MaintenanceProvider;
import de.murmelmeister.murmelapi.maintenance.MaintenanceType;
import de.murmelmeister.murmelapi.maintenance.whitelist.MaintenanceWhitelist;
import de.murmelmeister.murmelapi.maintenance.whitelist.MaintenanceWhitelistProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@CommandConfig(id = "maintenance", name = "maintenance")
public final class MaintenanceCommand extends CommandManager {
    private final MaintenanceProvider maintenanceProvider;
    private final MaintenanceWhitelistProvider whitelistProvider;
    private final UserProvider userProvider;

    /*
     * Maintenance Command: (Admin Command)
     * /maintenance create <start> <end> [title] [reason] - create a new maintenance (planned)
     *  - every maintenance claims its complete start/end period; overlapping periods are not allowed
     *  - directly adjacent periods are allowed (existing end equals new start)
     * /maintenance cancel <id> - cancel a maintenance
     * /maintenance edit <id> <start|end|title|reason> <value> - edit a maintenance
     * /maintenance show [id] - show all maintenances / show a maintenance all whitelisted users
     *  - filter for only "PLANNED/ACTIVE/ENDED/CANCELED", relevant or something TODO: right now not implemented
     * /maintenance list add <user> <id> [start] [end] [note] - add a user to a maintenance whitelist
     * /maintenance list remove <user> <id> - remove a user from a maintenance whitelist
     * /maintenance list edit <user> <id> <start|end|note> <value> - edit a user from a maintenance whitelist
     */

    public MaintenanceCommand(@NotNull MurmelEssentials plugin) {
        super(plugin);
        this.maintenanceProvider = plugin.getMaintenanceProvider();
        this.whitelistProvider = plugin.getMaintenanceWhitelistProvider();
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "maintenance"))
                .then(BrigadierCommand.literalArgumentBuilder("create")
                        .then(BrigadierCommand.requiredArgumentBuilder("start", StringArgumentType.string())
                                .suggests(this::suggestionDateTime)
                                .then(BrigadierCommand.requiredArgumentBuilder("end", StringArgumentType.string())
                                        .suggests(this::suggestionDateTime)
                                        .executes(this::executeCreate)
                                        .then(BrigadierCommand.requiredArgumentBuilder("title", StringArgumentType.string())
                                                .executes(this::executeCreate)
                                                .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.greedyString())
                                                        .executes(this::executeCreate)
                                                )
                                        )
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("cancel")
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                .suggests(this::suggestionMaintenanceIdsNoCancel)
                                .executes(this::executeCancel)
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("edit")
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                .suggests(this::suggestionMaintenanceIdsNoCancel)
                                .then(BrigadierCommand.literalArgumentBuilder("start")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .suggests(this::suggestionDateTime)
                                                .executes(context -> this.executeEdit(context, 0))
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("end")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .suggests(this::suggestionDateTime)
                                                .executes(context -> this.executeEdit(context, 1))
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("title")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .executes(context -> this.executeEdit(context, 2))
                                        )
                                )
                                .then(BrigadierCommand.literalArgumentBuilder("reason")
                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                .executes(context -> this.executeEdit(context, 3))
                                        )
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("show")
                        .executes(context -> this.executeShow(context, 1))
                        .then(BrigadierCommand.literalArgumentBuilder("pages")
                                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                                        .executes(context ->
                                                this.executeShow(context, IntegerArgumentType.getInteger(context, "page"))
                                        )
                                )
                        )
                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                .suggests(this::suggestionMaintenanceIds)
                                .executes(context -> this.executeShowTarget(context, 1))
                                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                                        .executes(context ->
                                                this.executeShowTarget(context, IntegerArgumentType.getInteger(context, "page"))
                                        )
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("list")
                        .then(BrigadierCommand.literalArgumentBuilder("add")
                                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                        .suggests(this::suggestionUsername)
                                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                                .suggests(this::suggestionMaintenanceIds)
                                                .executes(this::executeListAdd)
                                                .then(BrigadierCommand.requiredArgumentBuilder("start", StringArgumentType.string())
                                                        .suggests(this::suggestionDateTime)
                                                        .executes(this::executeListAdd)
                                                        .then(BrigadierCommand.requiredArgumentBuilder("end", StringArgumentType.string())
                                                                .suggests(this::suggestionDateTime)
                                                                .executes(this::executeListAdd)
                                                                .then(BrigadierCommand.requiredArgumentBuilder("note", StringArgumentType.greedyString())
                                                                        .executes(this::executeListAdd)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(BrigadierCommand.literalArgumentBuilder("remove")
                                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                        .suggests(this::suggestionUsername)
                                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                                .suggests(this::suggestionMaintenanceIds)
                                                .executes(this::executeListRemove)
                                        )
                                )
                        )
                        .then(BrigadierCommand.literalArgumentBuilder("edit")
                                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                        .suggests(this::suggestionUsername)
                                        .then(BrigadierCommand.requiredArgumentBuilder("id", IntegerArgumentType.integer(1))
                                                .suggests(this::suggestionMaintenanceIds)
                                                .then(BrigadierCommand.literalArgumentBuilder("start")
                                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                                .suggests(this::suggestionDateTime)
                                                                .executes(context -> this.executeListEdit(context, 0))
                                                        )
                                                )
                                                .then(BrigadierCommand.literalArgumentBuilder("end")
                                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                                .suggests(this::suggestionDateTime)
                                                                .executes(context -> this.executeListEdit(context, 1))
                                                        )
                                                )
                                                .then(BrigadierCommand.literalArgumentBuilder("note")
                                                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.greedyString())
                                                                .executes(context -> this.executeListEdit(context, 2))
                                                        )
                                                )
                                        )
                                )
                        )
                )
                ;
    }

    private int executeCreate(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String startInput = StringArgumentType.getString(context, "start");
            LocalDateTime startDate;
            try {
                startDate = LocalDateTime.parse(startInput, getDateTimeFormatter(executor.languageId()));
            } catch (DateTimeParseException e) {
                throw new CommandException("Invalid start date: " + startInput);
            }

            String endInput = StringArgumentType.getString(context, "end");
            LocalDateTime endDate;
            try {
                endDate = LocalDateTime.parse(endInput, getDateTimeFormatter(executor.languageId()));
            } catch (DateTimeParseException e) {
                throw new CommandException("Invalid end date: " + endInput);
            }

            if (!startDate.isBefore(endDate))
                throw new CommandException("The start date must be before the end date.");

            maintenanceProvider.findAll().stream()
                    .filter(maintenance ->
                            startDate.isBefore(maintenance.endAt())
                                    && endDate.isAfter(maintenance.startAt())
                                    && maintenance.status() != MaintenanceType.CANCELLED
                    )
                    .findFirst()
                    .ifPresent(maintenance -> {
                        throw new CommandException(
                                "The selected period overlaps with maintenance #" + maintenance.id() + "."
                        );
                    });

            String titleInput = getOptionalString(context, "title");
            String reasonInput = getOptionalString(context, "reason");

            Maintenance maintenance = maintenanceProvider.create(executor.id(), startDate, endDate, titleInput, reasonInput)
                    .orElseThrow(() -> new CommandException("Invalid maintenance"));

            sendRawMessage(source, executor.languageId(),
                    "<#999999>Maintenance was created start at <start> and end <end>.",
                    tagParsed("start", maintenance.startAt().format(getDateTimeFormatter(executor.languageId()))),
                    tagParsed("end", maintenance.endAt().format(getDateTimeFormatter(executor.languageId())))
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeCancel(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            int inputId = IntegerArgumentType.getInteger(context, "id");

            Maintenance maintenance = maintenanceProvider.findById(inputId)
                    .orElseThrow(() -> new CommandException("Invalid maintenanceId: " + inputId));

            if (maintenance.status() == MaintenanceType.CANCELLED)
                throw new CommandException("The maintenance has been cancelled.");

            Maintenance updated = maintenanceProvider.update(maintenance.id(), executor.id(),
                            builder -> builder.status(MaintenanceType.CANCELLED))
                    .orElseThrow(() -> new CommandException("Invalid maintenanceId: " + inputId));

            sendRawMessage(source, executor.languageId(),
                    "<#999999>The maintenance <#0099cc>#<id></#0099cc> is now <#00cc99><status></#00cc99>.",
                    tagParsed("id", updated.id()),
                    tagParsed("status", updated.status())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeEdit(CommandContext<CommandSource> context, int action) {
        return runWithTiming(context, (source, executor) -> {
            int inputId = IntegerArgumentType.getInteger(context, "id");
            Maintenance maintenance = maintenanceProvider.findById(inputId)
                    .orElseThrow(() -> new CommandException("Invalid maintenanceId: " + inputId));

            if (maintenance.status() == MaintenanceType.CANCELLED)
                throw new CommandException("The maintenance has been cancelled and cannot be edited.");

            Maintenance updated = maintenanceProvider.update(maintenance.id(), executor.id(), builder -> {
                        switch (action) {
                            case 0 -> {
                                String input = StringArgumentType.getString(context, "value");
                                LocalDateTime startDate;
                                try {
                                    startDate = LocalDateTime.parse(input, getDateTimeFormatter(executor.languageId()));
                                } catch (DateTimeParseException e) {
                                    throw new CommandException("Invalid start date: " + input);
                                }

                                builder.startAt(startDate);
                            }
                            case 1 -> {
                                String input = StringArgumentType.getString(context, "value");
                                LocalDateTime endDate;
                                try {
                                    endDate = LocalDateTime.parse(input, getDateTimeFormatter(executor.languageId()));
                                } catch (DateTimeParseException e) {
                                    throw new CommandException("Invalid end date: " + input);
                                }

                                builder.endAt(endDate);
                            }
                            case 2 -> {
                                String input = StringArgumentType.getString(context, "value");
                                builder.title(input);
                            }
                            case 3 -> {
                                String input = StringArgumentType.getString(context, "value");
                                builder.reason(input);
                            }
                            default -> throw new CommandException("Invalid action.");
                        }
                    })
                    .orElseThrow(() -> new CommandException("Invalid maintenanceId: " + inputId));

            sendRawMessage(source, executor.languageId(),
                    "<#999999>The maintenance <#0099cc>#<id></#0099cc> was edited <#99cc00><edit></#99cc00> to '<#00cc99><value></#00cc99>'.",
                    tagParsed("id", updated.id()),
                    tagParsed("edit", switch (action) {
                        case 0 -> "start";
                        case 1 -> "end";
                        case 2 -> "title";
                        case 3 -> "reason";
                        default -> throw new CommandException("Invalid action: " + action);
                    }),
                    tagParsed("value", switch (action) {
                        case 0 -> updated.startAt().format(getDateTimeFormatter(executor.languageId()));
                        case 1 -> updated.endAt().format(getDateTimeFormatter(executor.languageId()));
                        case 2 -> updated.title();
                        case 3 -> updated.reason();
                        default -> throw new CommandException("Invalid action: " + action);
                    })
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeShow(CommandContext<CommandSource> context, int page) {
        return runWithTiming(context, (source, executor) -> {
            List<Maintenance> maintenances = maintenanceProvider.findAll();
            String message = "<#999999>- <#99cc00><hover:show_text:'" +
                    "<#999999>Title: <#0099cc><title></#0099cc></#999999> <br>" +
                    "<#999999>Reason: <#0099cc><reason></#0099cc></#999999> <br>" +
                    "<#999999>Status: <#0099cc><status></#0099cc></#999999> <br>" +
                    "<#999999>Start date: <#0099cc><start></#0099cc></#999999> <br>" +
                    "<#999999>End date: <#0099cc><end></#0099cc></#999999> <br>" +
                    "<#999999>Created by: <#0099cc><created_name></#0099cc> <#555555>(ID: <#0099cc><created_id></#0099cc>)</#555555> at <#0099cc><created_at></#0099cc></#999999>" +
                    "<changed>" +
                    "'><id></hover>";

            sendRawMessage(source, executor.languageId(), maintenances.size() == 1 ? "<#999999>Maintenance:" : "<#999999>Maintenances:");
            List<Component> messages = maintenanceProvider.findAll().stream()
                    .map(maintenance -> {
                        Integer changedBy = maintenance.changedBy();
                        LocalDateTime changedAt = maintenance.changedAt();
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

                        User creator = userProvider.findById(maintenance.createdBy())
                                .orElseThrow(() -> new CommandException("Invalid creator: " + maintenance.createdBy()));
                        return component(message,
                                tagParsed("title", maintenance.title()),
                                tagParsed("reason", maintenance.reason()),
                                tagParsed("status", maintenance.status()),
                                tagParsed("start", maintenance.startAt().format(getDateTimeFormatter(executor.languageId()))),
                                tagParsed("end", maintenance.endAt().format(getDateTimeFormatter(executor.languageId()))),
                                tagParsed("created_name", creator.username()),
                                tagParsed("created_id", creator.id()),
                                tagParsed("created_at", maintenance.createdAt().format(getDateTimeFormatter(executor.languageId()))),
                                Placeholder.component("changed", changed),
                                tagParsed("id", maintenance.id())
                        );
                    })
                    .toList();
            sendPagedMessage(source, messages, "maintenance show pages", page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private int executeShowTarget(CommandContext<CommandSource> context, int page) {
        return runWithTiming(context, (source, executor) -> {
            int inputId = IntegerArgumentType.getInteger(context, "id");
            Maintenance maintenance = maintenanceProvider.findById(inputId)
                    .orElseThrow(() -> new CommandException("Invalid maintenance: " + inputId));

            List<MaintenanceWhitelist> whitelists = whitelistProvider.findByMaintenanceId(maintenance.id());
            sendRawMessage(source, executor.languageId(),
                    (whitelists.size() == 1 ? "<#999999>Maintenance Whitelist <id>:" : "<#999999>Maintenance Whitelists <#99cc00><id></#99cc00>: <#999999>"),
                    tagParsed("id", maintenance.id())
            );

            List<Component> messages = whitelists.stream()
                    .map(whitelist -> {
                        User user = userProvider.findById(whitelist.userId())
                                .orElseThrow(() -> new CommandException("Invalid user: " + whitelist.userId()));
                        return component("<#999999>- <#99cc00><username></#99cc00> <#454545>(ID: <#99cc00><user_id></#99cc00>)</#454545>",
                                tagParsed("username", user.username()),
                                tagParsed("user_id", user.id())
                        );
                    })
                    .toList();
            sendPagedMessage(source, messages, "maintenance show " + maintenance.id(), page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private int executeListAdd(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputName = StringArgumentType.getString(context, "user");
            User user = getUser(inputName);

            int inputId = IntegerArgumentType.getInteger(context, "id");
            Maintenance maintenance = maintenanceProvider.findById(inputId)
                    .orElseThrow(() -> new CommandException("Invalid maintenance: " + inputId));

            if (maintenance.status() == MaintenanceType.CANCELLED)
                throw new CommandException("The maintenance has been cancelled.");
            if (maintenance.status() == MaintenanceType.ENDED)
                throw new CommandException("The maintenance has already ended.");

            whitelistProvider.findByUserId(user.id()).stream()
                    .filter(whitelist -> whitelist.maintenanceId() == maintenance.id())
                    .findAny()
                    .ifPresent(whitelist -> {
                        throw new CommandException("The user is already on the whitelist.");
                    });

            String start = getOptionalString(context, "start");
            LocalDateTime startDate;
            if (start != null) {
                try {
                    startDate = LocalDateTime.parse(start, getDateTimeFormatter(executor.languageId()));
                } catch (DateTimeParseException e) {
                    throw new CommandException("Invalid start date: " + start);
                }
            } else startDate = null;

            String end = getOptionalString(context, "end");
            LocalDateTime endDate;
            if (end != null) {
                try {
                    endDate = LocalDateTime.parse(end, getDateTimeFormatter(executor.languageId()));
                } catch (DateTimeParseException e) {
                    throw new CommandException("Invalid end date: " + end);
                }
            } else endDate = null;

            validateWhitelistPeriod(maintenance, startDate, endDate);

            String note = getOptionalString(context, "note");

            MaintenanceWhitelist whitelist = whitelistProvider.create(maintenance.id(), user.id(), startDate, endDate, note, executor.id())
                    .orElseThrow(() -> new CommandException("Invalid whitelist: " + maintenance.id()));

            sendRawMessage(source, executor.languageId(),
                    "<#999999>User <#00cc88><username></#00cc88> was added to whitelist <#88cc00>#<id></#88cc00>.",
                    tagParsed("username", user.username()),
                    tagParsed("id", whitelist.maintenanceId())
                    );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeListRemove(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputName = StringArgumentType.getString(context, "user");
            User user = getUser(inputName);

            int inputId = IntegerArgumentType.getInteger(context, "id");
            Maintenance maintenance = maintenanceProvider.findById(inputId)
                    .orElseThrow(() -> new CommandException("Invalid maintenance: " + inputId));

            MaintenanceWhitelist whitelist = whitelistProvider.findByMaintenanceId(maintenance.id()).stream()
                    .filter(w -> w.userId() == user.id())
                    .findFirst()
                    .orElseThrow(() -> new CommandException("Invalid maintenance id: " + maintenance.id()));

            int result = whitelistProvider.delete(whitelist.id());
            if (result < 1)
                throw new CommandException("The user was removed from the whitelist of the maintenance.");

            sendRawMessage(source, executor.languageId(), "The user was removed from the whitelist of the maintenance.");
            return CommandResult.of(Command.SINGLE_SUCCESS, result);
        });
    }

    private int executeListEdit(CommandContext<CommandSource> context, int action) {
        return runWithTiming(context, (source, executor) -> {
            String inputName = StringArgumentType.getString(context, "user");
            User user = getUser(inputName);

            int inputId = IntegerArgumentType.getInteger(context, "id");
            Maintenance maintenance = maintenanceProvider.findById(inputId)
                    .orElseThrow(() -> new CommandException("Invalid maintenance: " + inputId));

            if (maintenance.status() == MaintenanceType.CANCELLED)
                throw new CommandException("The maintenance has been cancelled.");
            if (maintenance.status() == MaintenanceType.ENDED)
                throw new CommandException("The maintenance has already ended.");

            MaintenanceWhitelist whitelist = whitelistProvider.findByMaintenanceId(maintenance.id()).stream()
                    .filter(entry -> entry.userId() == user.id())
                    .findFirst()
                    .orElseThrow(() -> new CommandException("The user is not on this maintenance whitelist."));

            String input = StringArgumentType.getString(context, "value");
            LocalDateTime startDate = whitelist.startAt();
            LocalDateTime endDate = whitelist.endAt();
            String note = whitelist.note();

            switch (action) {
                case 0 -> startDate = parseWhitelistDate(input, "start", executor.languageId());
                case 1 -> endDate = parseWhitelistDate(input, "end", executor.languageId());
                case 2 -> note = input;
                default -> throw new CommandException("Invalid action: " + action);
            }

            validateWhitelistPeriod(maintenance, startDate, endDate);

            MaintenanceWhitelist updated = whitelistProvider.update(
                            whitelist.id(), startDate, endDate, note, executor.id())
                    .orElseThrow(() -> new CommandException("Could not update whitelist entry: " + whitelist.id()));

            sendRawMessage(source, executor.languageId(),
                    "<#999999>The whitelist entry for <#00cc88><username></#00cc88> was updated.",
                    tagParsed("username", user.username())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private LocalDateTime parseWhitelistDate(String input, String field, int languageId) {
        try {
            return LocalDateTime.parse(input, getDateTimeFormatter(languageId));
        } catch (DateTimeParseException exception) {
            throw new CommandException("Invalid " + field + " date: " + input);
        }
    }

    private void validateWhitelistPeriod(Maintenance maintenance, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && startDate.isBefore(maintenance.startAt()))
            throw new CommandException("The whitelist start date must not be before the maintenance start date.");
        if (startDate != null && !startDate.isBefore(maintenance.endAt()))
            throw new CommandException("The whitelist start date must be before the maintenance end date.");
        if (endDate != null && !endDate.isAfter(maintenance.startAt()))
            throw new CommandException("The whitelist end date must be after the maintenance start date.");
        if (endDate != null && endDate.isAfter(maintenance.endAt()))
            throw new CommandException("The whitelist end date must not be after the maintenance end date.");
        if (startDate != null && endDate != null && !startDate.isBefore(endDate))
            throw new CommandException("The whitelist start date must be before the end date.");
    }

    private String getOptionalString(CommandContext<CommandSource> context, String argumentName) {
        try {
            return StringArgumentType.getString(context, argumentName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private CompletableFuture<Suggestions> suggestionDateTime(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        User executor = getExecutor(context.getSource());
        builder.suggest(LocalDateTime.now().format(getDateTimeFormatter(executor.languageId())));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestionMaintenanceIds(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        maintenanceProvider.findAll().stream()
                .map(Maintenance::id)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestionMaintenanceIdsNoCancel(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        maintenanceProvider.findAll().stream()
                .filter(maintenance -> maintenance.status() != MaintenanceType.CANCELLED)
                .map(Maintenance::id)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestionUsername(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        userProvider.findUsernames().forEach(builder::suggest);
        return builder.buildFuture();
    }
}
