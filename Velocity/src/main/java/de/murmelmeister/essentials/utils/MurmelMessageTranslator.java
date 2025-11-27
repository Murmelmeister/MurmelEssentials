package de.murmelmeister.essentials.utils;

import de.murmelmeister.murmelapi.language.message.MessageService;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class MurmelMessageTranslator extends MiniMessageTranslator {
    private static final Key TRANSLATION_KEY = Key.key("murmelessentials", "mini_message_translato");
    private final MessageService messageService;

    public MurmelMessageTranslator(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    protected @Nullable String getMiniMessageString(@NotNull String key, @NotNull Locale locale) {
        if (!key.startsWith("murmel."))
            return null;

        try {
            return messageService.getMessage(key, locale.toLanguageTag());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @NotNull Key name() {
        return TRANSLATION_KEY;
    }
}
