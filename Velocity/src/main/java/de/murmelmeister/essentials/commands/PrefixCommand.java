package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
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
import de.murmelmeister.murmelapi.user.color.UserPrefixColor;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@CommandConfig(id = "prefix", name = "prefix")
public final class PrefixCommand extends CommandManager {
    private final PrefixColorProvider colorProvider;
    private final UserPrefixColorProvider userColorProvider;

    /*
     * Command Tree (User Command):
     * /prefix <color>
     * /prefix
     */

    public PrefixCommand(MurmelEssentials plugin) {
        super(plugin);
        this.colorProvider = plugin.getPrefixColorProvider();
        this.userColorProvider = plugin.getUserPrefixColorProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "prefix"))
                .executes(this::executeList)
                .then(BrigadierCommand.requiredArgumentBuilder("color", StringArgumentType.word())
                        .suggests(this::colorSuggestion)
                        .executes(this::executeSet)
                );
    }

    private int executeList(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            // Show all colors that the user has and show which one is activated + show multipage
            List<UserPrefixColor> prefixColors = userColorProvider.findByUserId(executor.id());
            if (prefixColors.isEmpty())
                throw new CommandException("You don't have any prefix colors.");

            String headerName = prefixColors.size() == 1 ? "Prefix" : "Prefixes";
            sendRawMessage(source, executor.languageId(), "<#999999><header_name>:", tagParsed("header_name", headerName));
            // TODO: Add multipage
            prefixColors.forEach(color -> {
                String message = color.active()
                        ? "<#999999>- <#999900><hover:show_text:'<#cc0088>Click to deactivate'><click:suggest_command:/prefix <color>><color></click></hover>"
                        : "<#999999>- <hover:show_text:'<#00cc88>Click to activate'><click:suggest_command:/prefix <color>><color></click></hover>";
                sendRawMessage(source, executor.languageId(), message, tagParsed("color", color.colorId()));
            });
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private int executeSet(@NotNull CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            // Active or deactivate the selected color
            String input = StringArgumentType.getString(context, "color");

            PrefixColor color = colorProvider.findById(input)
                    .orElseThrow(() -> new CommandException("Color '" + input + "' not found."));

            UserPrefixColor userColor = userColorProvider.findById(executor.id(), color.id())
                    .orElseThrow(() -> new CommandException("You don't have the color '" + input + "'."));

            // Deactivate the active color
            Optional<UserPrefixColor> activeColor = userColorProvider.findActiveById(executor.id());
            activeColor.ifPresent(active -> userColorProvider.upsert(executor.id(), active.colorId(), !active.active())
                    .orElseThrow(() -> new CommandException("Failed to change '" + input + "' color.")));

            // Activate the selected color
            UserPrefixColor result = userColorProvider.upsert(executor.id(), color.id(), !userColor.active())
                    .orElseThrow(() -> new CommandException("Failed to change '" + input + "' color."));

            String activeMessage = result.active() ? "<#00cc88>activated</#00cc88>" : "<#cc0088>deactivated</#cc0088>";
            sendRawMessage(source, executor.languageId(), "<#999999>Color <#009999><color></#009999> is now <active>.",
                    tagParsed("color", result.colorId()), tagParsed("active", activeMessage));
            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
        });
    }

    private @NotNull CompletableFuture<Suggestions> colorSuggestion(@NotNull CommandContext<CommandSource> context, @NotNull SuggestionsBuilder builder) {
        // Show all color ids that the user has
        String prefix = builder.getRemaining();
        User executor = getExecutor(context.getSource());
        userColorProvider.findByUserId(executor.id()).stream()
                .filter(color -> StringUtil.startsWithIgnoreCase(color.colorId(), prefix))
                .forEach(color -> builder.suggest(color.colorId()));
        return builder.buildFuture();
    }
}
