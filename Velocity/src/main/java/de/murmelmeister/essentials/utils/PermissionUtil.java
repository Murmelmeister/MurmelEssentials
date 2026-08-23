package de.murmelmeister.essentials.utils;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.messages.Message;
import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;

public abstract class PermissionUtil extends CommandManager {
    public PermissionUtil(MurmelEssentials plugin) {
        super(plugin);
    }

    public Component formatExpiredMessage(int languageId, LocalDateTime expiresAt) {
        String now = LocalDateTime.now().format(getDateTimeFormatter(languageId));
        String formattedTime = formatTimeUntil(languageId, expiresAt);
        return expiresAt == null ? Component.empty() : component(languageId, Message.PERMISSION_FORMAT_EXPIRED_TIME,
                tagParsed("expired_time", formattedTime),
                tagParsed("current_time", now),
                tagParsed("expired_at", expiresAt.format(getDateTimeFormatter(languageId)))
        );
    }

    public Component formatExpiredInfoMessage(int languageId, LocalDateTime expiresAt) {
        String now = LocalDateTime.now().format(getDateTimeFormatter(languageId));
        String formattedTime = formatTimeUntil(languageId, expiresAt);
        return expiresAt == null ? component(languageId, Message.MESSAGE_NOT_EXPIRE)
                : component(languageId, Message.PERMISSION_FORMAT_EXPIRED_INFO_TIME,
                tagParsed("expired_time", formattedTime),
                tagParsed("current_time", now),
                tagParsed("expired_at", expiresAt.format(getDateTimeFormatter(languageId)))
        );
    }
}
