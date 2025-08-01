package de.murmelmeister.essentials.manager.command;

import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.murmelapi.user.User;

@FunctionalInterface
public interface CommandHandler {
    CommandResult handle(CommandSource source, User executor);
}
