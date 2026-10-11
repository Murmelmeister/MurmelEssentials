package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@CommandConfig(id = "debug", name = "debugs")
public final class DebugCommand extends CommandManager {
    private final UserProvider userProvider;

    public DebugCommand(@NotNull MurmelEssentials plugin) {
        super(plugin);
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> {
                    User user = getExecutor(source);
                    return user.debugUser();
                })
                .executes(this::execute)
                ;
    }

    private int execute(CommandContext<CommandSource> context) {
        return runWithTiming(context, (source, executor) -> {
            if (!executor.debugUser())
                throw new CommandException(Message.COMMAND_DEBUG_PERMISSION);

            User updated = userProvider.update(executor.id(),
                    executor.username(),
                    executor.firstLogin(),
                    executor.debugUser(),
                    !executor.debugEnabled(),
                    executor.languageId()
            ).orElseThrow(() -> new CommandException(Message.COMMAND_DEBUG_FAILED));

            Component status = updated.debugEnabled()
                    ? component(executor.languageId(), Message.MESSAGE_ENABLED)
                    : component(executor.languageId(), Message.MESSAGE_DISABLED);

            sendMessage(source, executor.languageId(),
                    Message.COMMAND_DEBUG_SUCCESS,
                    tagComponent("status", status),
                    tagParsed("username", updated.username())
            );
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }
}
