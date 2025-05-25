package de.murmelmeister.essentials.utils;

import de.murmelmeister.murmelapi.language.Message;
import de.murmelmeister.murmelapi.language.MessageProvider;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MessagesService {
    private final Logger logger;
    private final MessageProvider provider;

    public MessagesService(Logger logger, MessageProvider provider) {
        this.logger = logger;
        this.provider = provider;
    }

    public void reload() {
        provider.loadData();
    }

    public void checkAndLoad() {
        Set<String> existingKeys = provider.loadData().stream()
                .map(message -> message.getTag() + "_" + message.getLanguageId())
                .collect(Collectors.toSet());

        for (Messages messages : Messages.VALUES) {
            String tag = messages.getTag();
            for (Map.Entry<Integer, String> entry : messages.getMessagesMap().entrySet()) {
                int languageId = entry.getKey();
                String messageText = entry.getValue();
                String key = tag + "_" + languageId;
                if (existingKeys.contains(key)) continue;
                if (provider.getMessage(tag, languageId) != null) continue;
                provider.createMessage(tag, languageId, messageText);
            }
        }
    }

    public String getMessage(Messages messages, int languageId) {
        Message message = provider.getMessage(messages.getTag(), languageId);
        if (message == null) {
            logger.warn("Message with tag '{}' and language ID '{}' not found.", messages.getTag(), languageId);
            message = provider.getMessage(messages.getTag(), 1);
            return message.getMessage();
        }
        return message.getMessage();
    }
}
