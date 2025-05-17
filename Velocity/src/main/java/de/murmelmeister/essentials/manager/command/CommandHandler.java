package de.murmelmeister.essentials.manager.command;

import com.velocitypowered.api.command.CommandSource;

@FunctionalInterface
public interface CommandHandler {
    CommandResult handle(CommandSource source, int executorId);
}
