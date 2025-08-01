package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;

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

    public void disconnectPunishMessage(Player player, int languageId, UUID logId) {
        player.disconnect(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, logId)));
    }

    public String getPunishMessage(int languageId, UUID logId) {
        DateTimeFormatter dateTime = plugin.getDateTimeFormatter(languageId);
        PunishmentLog log = logProvider.getLog(logId);
        String reasonText = log.reasonText();
        int punisherId = log.createdBy();
        User userPunisher = userProvider.findById(punisherId);
        String punisherName = userPunisher.username();
        String startTime = log.createdAt().format(dateTime);
        String expiresTime = log.expiresAt() != null
                ? log.expiresAt().format(dateTime)
                : messageService.getMessage(Messages.MESSAGE_NOT_EXPIRE, languageId);
        String autoPunish = log.reasonAutoPunish()
                ? messageService.getMessage(Messages.MESSAGE_YES, languageId)
                : messageService.getMessage(Messages.MESSAGE_NO, languageId);
        String modified = log.action() == PunishmentLog.Action.MODIFIED
                ? messageService.getMessage(Messages.MESSAGE_MODIFIED, languageId)
                : "";
        return messageService.getMessage(Messages.SEND_PUNISHMENT_MESSAGE, languageId)
                .replace("[REASON]", reasonText)
                .replace("[FROM]", punisherName)
                .replace("[START_TIME]", startTime)
                .replace("[EXPIRES_TIME]", expiresTime)
                .replace("[MODIFIED]", modified)
                .replace("[AUTO_PUNISH]", autoPunish)
                .replace("[PUNISHMENT_ID]", logId.toString());
    }
}
