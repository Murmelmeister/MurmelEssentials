package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.murmelapi.language.LanguageType;
import de.murmelmeister.murmelapi.language.LanguageTypeProvider;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;

public final class LanguageCommand extends CommandManager {
    private final LanguageTypeProvider languageProvider;
    private final UserProvider userProvider;

    // TODO: Remove the class, because it is not used anymore

    public LanguageCommand(MurmelEssentials plugin) {
        super(plugin);
        this.languageProvider = plugin.getLanguageProvider();
        this.userProvider = plugin.getUserProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "language"))
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            List<LanguageType> languages = languageProvider.findAll();

                            if (languages.isEmpty())
                                throw new CommandException("No languages available.");

                            int languageId = executor.languageId();
                            LanguageType language = languageProvider.findById(languageId).orElse(null);

                            if (language == null)
                                throw new CommandException("Language not found.");

                            sendRawMessage(source, languageId, "<#999999>===- <header_name>:", tagParsed("header_name", languages.size() == 1 ? "Language" : "Languages"));
                            languages.forEach(lang -> {
                                boolean isCurrent = lang.id() == languageId;
                                String hoverText = isCurrent ? "<#00cc88>Current" : "<#999999>Available";
                                String langName = isCurrent ? "<#00cc88>" + lang.code() : "<#999999>" + lang.code();
                                sendRawMessage(source, languageId, "<#999999>- <#00cc88><hover:show_text:'<hover_text>'><language></hover>",
                                        tagParsed("hover_text", hoverText), tagParsed("language", langName));
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("language", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            languageProvider.findAll().forEach(lang ->
                                    builder.suggest(lang.code().toLowerCase(),
                                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + lang.code())))
                            );
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String languageName = StringArgumentType.getString(context, "language");
                                    LanguageType language = languageProvider.findByCode(languageName).orElse(null);

                                    if (language == null) {
                                        sendRawMessage(source, languageId, "<#990000>Language <language> does not exist.", Placeholder.unparsed("language", languageName));
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    }

                                    userProvider.update(executor.id(), executor.username(), executor.firstLogin(),
                                            executor.debugUser(), executor.debugEnabled(), language.id()); // Update the user in the database
                                    sendRawMessage(source, languageId, "<#999999>Language set to <#00cc88><language></#00cc88>.", tagParsed("language", language.code()));
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
                                })
                        )
                );
    }
}
