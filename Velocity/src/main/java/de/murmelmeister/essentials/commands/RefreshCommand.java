package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

public final class RefreshCommand extends CommandManager {
    public RefreshCommand(MurmelEssentials plugin) {
        super(plugin);
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("refresh")
                .requires(source -> source.hasPermission("murmel.command.refresh"))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    RefreshUtil.globalRefresh();
                    sendMessage(source, "<#00cc88>All caches refreshed.");
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("permissions")
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            RefreshUtil.markAsRefreshed(RefreshType.PERMISSIONS);
                            sendMessage(source, "<#00cc88>All caches refreshed.");
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
