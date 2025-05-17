package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;

public final class UserInfoCommand extends CommandManager {
    public UserInfoCommand(MurmelEssentials plugin) {
        super(plugin);
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("userinfo")
                .requires(source -> source.hasPermission("murmel.command.userinfo"))
                // TODO: Implement the userinfo command logic
                .build();
        return new BrigadierCommand(node);
    }
}
