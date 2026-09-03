package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import de.murmelmeister.murmelapi.color.PrefixColor;
import de.murmelmeister.murmelapi.color.PrefixColorProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.color.UserPrefixColor;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@CommandConfig(id = "color", name = "color")
public final class ColorCommand extends CommandManager {
    private static final Pattern VALID_COLOR = Pattern.compile("^<#[a-fA-F0-9]{6}>$");
    private static final Pattern VALID_COLOR_ANIMATED = Pattern.compile("^<#[a-fA-F0-9]{6}>(,<#[a-fA-F0-9]{6}>)+$");

    private final PrefixColorProvider colorProvider;
    private final UserPrefixColorProvider userColorProvider;
    private final UserProvider userProvider;

    /*
     * Command Tree (Admin Command):
     * /color create <id> <animated> <color>
     * /color delete <id>
     * /color set <id> <animated> <color>
     * /color
     * /color add <user> <color>
     * /color remove <user> <color>
     */

    public ColorCommand(MurmelEssentials plugin) {
        super(plugin);
        this.colorProvider = plugin.getPrefixColorProvider();
        this.userColorProvider = plugin.getUserPrefixColorProvider();
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "color"))
                .executes(this::executeList)
                .then(BrigadierCommand.literalArgumentBuilder("create")
                        .then(BrigadierCommand.requiredArgumentBuilder("id", StringArgumentType.word())
                                .then(BrigadierCommand.requiredArgumentBuilder("animated", BoolArgumentType.bool())
                                        .then(BrigadierCommand.requiredArgumentBuilder("color", StringArgumentType.string())
                                                .executes(this::executeCreate)
                                        )
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("delete")
                        .then(BrigadierCommand.requiredArgumentBuilder("id", StringArgumentType.word())
                                .suggests(this::colorSuggestion)
                                .executes(this::executeDelete)
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("set")
                        .then(BrigadierCommand.requiredArgumentBuilder("id", StringArgumentType.word())
                                .suggests(this::colorSuggestion)
                                .then(BrigadierCommand.requiredArgumentBuilder("animated", BoolArgumentType.bool())
                                        .then(BrigadierCommand.requiredArgumentBuilder("color", StringArgumentType.string())
                                                .executes(this::executeSet)
                                        )
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("add")
                        .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                .suggests(this::userSuggestion)
                                .then(BrigadierCommand.requiredArgumentBuilder("color", StringArgumentType.word())
                                        .suggests(this::colorSuggestion)
                                        .executes(this::executeAdd)
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("remove")
                        .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                                .suggests(this::userSuggestion)
                                .then(BrigadierCommand.requiredArgumentBuilder("color", StringArgumentType.word())
                                        .suggests(this::userColorSuggestion)
                                        .executes(this::executeRemove)
                                )
                        )
                );
    }

    private int executeList(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            List<PrefixColor> colors = colorProvider.findAll();
            if (colors.isEmpty())
                throw new CommandException("No colors found.");

            String headerName = colors.size() == 1 ? "Color" : "Colors";
            sendRawMessage(source, executor.languageId(), "<#999999><header_name>:", tagParsed("header_name", headerName));
            colors.forEach(color -> {
                // Add created / changed etc.
                String message = "<#999999>- <#009999><hover:show_text:'<#999999>Animated: <#009999><animated></#009999> <br>Color: <color><username>'><prefix_color></hover>";
                sendRawMessage(source, executor.languageId(), message,
                        tagParsed("prefix_color", color.id()),
                        tagParsed("animated", color.animated()),
                        tagParsed("color", color.color()),
                        tagParsed("username", executor.username())
                );
            });
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private int executeCreate(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputId = StringArgumentType.getString(context, "id");
            if (colorProvider.findById(inputId).isPresent())
                throw new CommandException("Color with id '" + inputId + "' already exists.");

            boolean animated = BoolArgumentType.getBool(context, "animated");
            String inputColor = StringArgumentType.getString(context, "color");

            if (animated) {
                if (!VALID_COLOR_ANIMATED.matcher(inputColor).matches())
                    throw new CommandException("Invalid animated color format. Expected format: <#RRGGBB>,<#RRGGBB>,...");
            } else {
                if (!VALID_COLOR.matcher(inputColor).matches())
                    throw new CommandException("Invalid color format. Expected format: <#RRGGBB>");
            }

            PrefixColor prefixColor = colorProvider.upsert(inputId, inputColor, animated, executor.id())
                    .orElseThrow(() -> new CommandException("Failed to create color."));
            sendRawMessage(source, executor.languageId(), "<#999999>Color <#009999><prefix_color></#009999> created successfully.",
                    tagParsed("prefix_color", prefixColor.id())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeDelete(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputId = StringArgumentType.getString(context, "id");

            PrefixColor prefixColor = colorProvider.findById(inputId)
                    .orElseThrow(() -> new CommandException("Color with id '" + inputId + "' not found."));

            int result = colorProvider.delete(prefixColor.id());
            if (result < 1)
                throw new CommandException("Failed to delete color.");
            sendRawMessage(source, executor.languageId(), "<#999999>Color <#009999><prefix_color></#009999> deleted successfully.",
                    tagParsed("prefix_color", prefixColor.id())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, result);
        });
    }

    private int executeSet(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputId = StringArgumentType.getString(context, "id");
            if (colorProvider.findById(inputId).isEmpty())
                throw new CommandException("Color with id '" + inputId + "' not found.");

            boolean animated = BoolArgumentType.getBool(context, "animated");
            String inputColor = StringArgumentType.getString(context, "color");

            if (animated) {
                if (!VALID_COLOR_ANIMATED.matcher(inputColor).matches())
                    throw new CommandException("Invalid animated color format. Expected format: <#RRGGBB>,<#RRGGBB>,...");
            } else {
                if (!VALID_COLOR.matcher(inputColor).matches())
                    throw new CommandException("Invalid color format. Expected format: <#RRGGBB>");
            }

            PrefixColor prefixColor = colorProvider.upsert(inputId, inputColor, animated, executor.id())
                    .orElseThrow(() -> new CommandException("Failed to update color."));
            sendRawMessage(source, executor.languageId(), "<#999999>Color <#009999><prefix_color></#009999> updated successfully.",
                    tagParsed("prefix_color", prefixColor.id())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeAdd(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputUser = StringArgumentType.getString(context, "user");
            User user = getUser(inputUser);

            String inputColor = StringArgumentType.getString(context, "color");
            PrefixColor prefixColor = colorProvider.findById(inputColor)
                    .orElseThrow(() -> new CommandException("Color with id '" + inputColor + "' not found."));

            if (userColorProvider.findById(user.id(), prefixColor.id()).isPresent())
                throw new CommandException("User '" + user.username() + "' already has color '" + inputColor + "'.");

            UserPrefixColor userColor = userColorProvider.upsert(user.id(), prefixColor.id(), false)
                    .orElseThrow(() -> new CommandException("Failed to add color to user."));
            sendRawMessage(source, executor.languageId(), "<#999999>Color <#009999><prefix_color></#009999> added to user <#009999><user></#009999> successfully.",
                    tagParsed("prefix_color", userColor.colorId()),
                    tagParsed("user", user.username())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private int executeRemove(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            String inputUser = StringArgumentType.getString(context, "user");
            User user = getUser(inputUser);

            String inputColor = StringArgumentType.getString(context, "color");
            UserPrefixColor userColor = userColorProvider.findById(user.id(), inputColor)
                    .orElseThrow(() -> new CommandException("User '" + user.username() + "' does not have color '" + inputColor + "'."));

            int result = userColorProvider.delete(userColor.userId(), userColor.colorId());
            if (result < 1)
                throw new CommandException("Failed to remove color from user.");
            sendRawMessage(source, executor.languageId(), "<#999999>Color <#009999><prefix_color></#009999> removed from user <#009999><user></#009999> successfully.",
                    tagParsed("prefix_color", userColor.colorId()),
                    tagParsed("user", user.username())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS, result);
        });
    }

    private @NotNull CompletableFuture<Suggestions> colorSuggestion(@NotNull CommandContext<CommandSource> context, @NotNull SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        colorProvider.findAll().stream()
                .map(PrefixColor::id)
                .filter(colorId -> StringUtil.startsWithIgnoreCase(colorId, prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private @NotNull CompletableFuture<Suggestions> userColorSuggestion(@NotNull CommandContext<CommandSource> context, @NotNull SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        String username  = StringArgumentType.getString(context, "user");
        User user = getUser(username);
        userColorProvider.findByUserId(user.id()).stream()
                .map(UserPrefixColor::colorId)
                .filter(color -> StringUtil.startsWithIgnoreCase(color, prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private @NotNull CompletableFuture<Suggestions> userSuggestion(@NotNull CommandContext<CommandSource> context, @NotNull SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        userProvider.findUsernames().stream()
                .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
