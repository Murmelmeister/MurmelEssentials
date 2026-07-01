package de.murmelmeister.essentials.manager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;

public interface CommandBrigadier {
    LiteralArgumentBuilder<CommandSource> createCommand();
}
