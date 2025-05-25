package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.language.Language;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public final class LanguageCommand extends CommandManager {
    public LanguageCommand(MurmelEssentials plugin) {
        super(plugin);
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("language")
                .requires(source -> source.hasPermission("murmel.command.language"))
                .executes(context ->
                        runWithTiming(context, (source, executorId) -> {
                            List<Language> languages = languageProvider.getLanguages();

                            if (languages.isEmpty()) {
                                sendMessage(source, "<#990000>No languages available.");
                                return CommandResult.of(-2);
                            }

                            int languageId = user.getLanguage(executorId);
                            Language language = languageProvider.getLanguage(languageId);

                            if (language == null) {
                                sendMessage(source, "<#990000>Language not found.");
                                return CommandResult.of(-3);
                            }

                            sendMessage(source, "<#999999>===- %s:", languages.size() == 1 ? "Language" : "Languages");
                            languages.forEach(lang -> {
                                String status = lang.getId() == languageId ? "<#00cc88>Current" : "<#999999>Available";
                                sendMessage(source, "<#999999>- <#00cc88><hover:show_text:'%s'>%s</hover>", status, lang.getName());
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("language", IntegerArgumentType.integer(1))
                        .suggests((context, builder) -> {
                            languageProvider.getLanguages().forEach(lang ->
                                    builder.suggest(lang.getId(), VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + lang.getName())))
                            );
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executorId) -> {
                                    int languageId = IntegerArgumentType.getInteger(context, "language");
                                    Language language = languageProvider.getLanguage(languageId);

                                    if (language == null) {
                                        sendMessage(source, "<#990000>Language %s does not exist.", languageId);
                                        return CommandResult.of(-2);
                                    }

                                    user.setLanguage(executorId, language.getId());
                                    sendMessage(source, "<#00cc88>Language set to %s.", language.getName());
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }
}
