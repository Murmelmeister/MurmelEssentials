package de.murmelmeister.essentials.utils;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.language.message.MessageDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.murmelmeister.murmelapi.language.message.MessageDefinition.LanguageMessage.of;

public enum Messages implements MessageDefinition {
    PLAY_TIME_COMMAND_USE(
            of(getLanguageId(Lang.ENGLISH), "<#999999>PlayTime: <#00cc88>[TIME]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Spielzeit: <#00cc88>[TIME]")
    ),
    ;
    public static final Messages[] VALUES = values();
    private final Map<Integer, String> messagesMap;

    Messages(LanguageMessage... entries) {
        Map<Integer, String> map = new HashMap<>();
        for (LanguageMessage entry : entries)
            map.put(entry.languageId(), entry.message());
        this.messagesMap = Collections.unmodifiableMap(map);
    }

    @Override
    public String getTag() {
        return name().toUpperCase();
    }

    @Override
    public String getMessage(int languageId) {
        return messagesMap.getOrDefault(languageId, messagesMap.get(1)); // Default to language ID 1 if not found
    }

    @Override
    public Map<Integer, String> getMessagesMap() {
        return messagesMap;
    }

    private static int getLanguageId(Lang lang) {
        return lang.getId();
    }

    private enum Lang {
        ENGLISH,
        GERMAN;
        private final LanguageProvider provider = MurmelAPI.getLanguage();

        public int getId() {
            return provider.get(this.name().toLowerCase()).getId();
        }
    }
}
