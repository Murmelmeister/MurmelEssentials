package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class PunishmentUtil {
    private final MurmelEssentials plugin;
    private final UserProvider userProvider;
    private final PunishmentLogProvider logProvider;
    private final MessageService messageService;

    public PunishmentUtil(MurmelEssentials plugin) {
        this.plugin = plugin;
        this.userProvider = plugin.getUserProvider();
        this.logProvider = plugin.getPunishmentLogProvider();
        this.messageService = plugin.getMessageService();
    }

    public void disconnectPunishMessage(LoginEvent event, int languageId, UUID logId) {
        event.setResult(ResultedEvent.ComponentResult.denied(getPunishMessage(languageId, logId)));
    }

    public void disconnectPunishMessage(Player player, int languageId, UUID logId) {
        player.disconnect(getPunishMessage(languageId, logId));
    }

    public Component getPunishMessage(int languageId, UUID logId) {
        DateTimeFormatter dateTime = plugin.getDateTimeFormatter(languageId);
        PunishmentLog log = logProvider.getLog(logId); // Maybe log id check? (it can be null)
        String reasonText = log.reasonText();
        int punisherId = log.createdBy();
        User userPunisher = userProvider.findById(punisherId);
        String punisherName = userPunisher.username();
        String startTime = log.createdAt().format(dateTime);
        String expiresTime = log.expiresAt() != null
                ? log.expiresAt().format(dateTime)
                : messageService.getMessage(Message.MESSAGE_NOT_EXPIRE.getTag(), languageId);
        String autoPunish = log.reasonAutoPunish()
                ? messageService.getMessage(Message.MESSAGE_YES.getTag(), languageId)
                : messageService.getMessage(Message.MESSAGE_NO.getTag(), languageId);
        String modified = log.action() == PunishmentLog.Action.MODIFIED
                ? messageService.getMessage(Message.MESSAGE_MODIFIED.getTag(), languageId)
                : "";
        return MiniMessage.miniMessage().deserialize(messageService.getMessage(Message.MESSAGE_PUNISHMENT_SEND.getTag(), languageId),
                Placeholder.parsed("reason", reasonText),
                Placeholder.parsed("from", punisherName),
                Placeholder.parsed("start_time", startTime),
                Placeholder.parsed("expires_time", expiresTime), Placeholder.unparsed("modified", modified),
                Placeholder.parsed("auto_punish", autoPunish),
                Placeholder.parsed("punishment_id", log.id().toString())
        );
    }
}
