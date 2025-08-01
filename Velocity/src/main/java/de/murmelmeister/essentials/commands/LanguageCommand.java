package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.language.Language;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public final class LanguageCommand extends CommandManager {
    private final LanguageProvider languageProvider;
    private final UserProvider userProvider;

    public LanguageCommand(MurmelEssentials plugin) {
        super(plugin);
        this.languageProvider = plugin.getLanguageProvider();
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("language")
                .requires(source -> source.hasPermission("murmel.command.language"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            List<Language> languages = languageProvider.getLanguages();

                            if (languages.isEmpty()) {
                                sendMessage(source, "<#990000>No languages available.");
                                return CommandResult.of(-2);
                            }

                            int languageId = executor.languageId();
                            Language language = languageProvider.get(languageId);

                            if (language == null) {
                                sendMessage(source, "<#990000>Language not found.");
                                return CommandResult.of(-3);
                            }

                            sendMessage(source, "<#999999>===- %s:", languages.size() == 1 ? "Language" : "Languages");
                            languages.forEach(lang -> {
                                boolean isCurrent = lang.id() == languageId;
                                String hoverText = isCurrent ? "<#00cc88>Current" : "<#999999>Available";
                                String langName = isCurrent ? "<#00cc88>" + lang.name() : "<#999999>" + lang.name();
                                sendMessage(source, "<#999999>- <#00cc88><hover:show_text:'%s'>%s</hover>", hoverText, langName);
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("language", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            languageProvider.getLanguages().forEach(lang ->
                                    builder.suggest(lang.name().toLowerCase(),
                                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + lang.name())))
                            );
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    String languageName = StringArgumentType.getString(context, "language");
                                    Language language = languageProvider.get(languageName);

                                    if (language == null) {
                                        sendMessage(source, "<#990000>Language %s does not exist.", languageName);
                                        return CommandResult.of(-2);
                                    }

                                    userProvider.update(executor.id(), executor.username(), executor.firstLogin(),
                                            executor.debugUser(), executor.debugEnabled(), language.id()); // Update the user in the database
                                    sendMessage(source, "<#999999>Language set to <#00cc88>%s</#00cc88>.", language.name());
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }
}
