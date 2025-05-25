package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
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
                .executes(context ->
                        runWithTiming(context, (source, executorId) -> {
                            RefreshUtil.globalRefresh();
                            sendMessage(source, "<#00cc88>All caches refreshed.");
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.literalArgumentBuilder("permissions")
                        .executes(context ->
                                runWithTiming(context, (source, executorId) -> {
                                    RefreshUtil.markAsRefreshed(RefreshType.PERMISSIONS);
                                    sendMessage(source, "<#00cc88>All caches refreshed.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("messages") // Maybe in another command?
                    .executes(context ->
                                runWithTiming(context, (source, executorId) -> {
                                    messagesService.reload();
                                    sendMessage(source, "<#00cc88>Reloaded messages.");
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }
}
