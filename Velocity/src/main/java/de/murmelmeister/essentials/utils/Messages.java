package de.murmelmeister.essentials.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.murmelmeister.essentials.utils.Messages.LanguageMessage.of;

public enum Messages {
    PLAY_TIME_COMMAND_USE(
            of(1, "<#999999>PlayTime: <#00cc88>[TIME]"),
            of(2, "<#999999>Spielzeit: <#00cc88>[TIME]")
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

    public String getTag() {
        return name().toUpperCase();
    }

    public String getMessage(int languageId) {
        return messagesMap.getOrDefault(languageId, messagesMap.get(1)); // Default to language ID 1 if not found
    }

    public Map<Integer, String> getMessagesMap() {
        return messagesMap;
    }

    record LanguageMessage(int languageId, String message) {
        public static LanguageMessage of(int languageId, String message) {
            return new LanguageMessage(languageId, message);
        }
    }
}
