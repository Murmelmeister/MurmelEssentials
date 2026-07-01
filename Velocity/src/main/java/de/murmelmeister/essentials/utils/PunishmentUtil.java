package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;

public final class PunishmentUtil {
    private final MurmelEssentials plugin;
    private final UserProvider userProvider;
    private final MessageService messageService;
    private final PermissionService permissionService;

    public PunishmentUtil(MurmelEssentials plugin) {
        this.plugin = plugin;
        this.userProvider = plugin.getUserProvider();
        this.messageService = plugin.getMessageService();
        this.permissionService = plugin.getPermissionService();
    }

    public void disconnectPunishMessage(@NotNull LoginEvent event, int userId, int languageId, @NotNull PunishmentAudit audit) {
        if (!permissionService.hasPermission(PermissionTarget.user(userId), MurmelEssentials.PUNISHMENT_IMMUNITY_PERMISSION))
            event.setResult(ResultedEvent.ComponentResult.denied(getPunishMessage(languageId, audit)));
    }

    public void disconnectPunishMessage(@NotNull Player player, int userId, int languageId, @NotNull PunishmentAudit audit) {
        if (!permissionService.hasPermission(PermissionTarget.user(userId), MurmelEssentials.PUNISHMENT_IMMUNITY_PERMISSION))
            player.disconnect(getPunishMessage(languageId, audit));
    }

    public @NotNull Component getPunishMessage(int languageId, @NotNull PunishmentAudit audit) {
        DateTimeFormatter dateTime = plugin.getDateTimeFormatter(languageId);
        String reasonText = audit.reasonText();
        Integer punisherId = audit.createdBy();
        User userPunisher = punisherId != null ? userProvider.findById(punisherId).orElse(null) : null;
        String punisherName = userPunisher != null ? userPunisher.username() : "?";
        String startTime = audit.createdAt().format(dateTime);
        String expiresTime = audit.expiresAt() != null
                ? audit.expiresAt().format(dateTime)
                : messageService.getMessage(Message.MESSAGE_NOT_EXPIRE.getTag(), languageId);
        String modified = audit.action() == PunishmentAudit.Action.MODIFIED
                ? messageService.getMessage(Message.MESSAGE_MODIFIED.getTag(), languageId)
                : "";
        return MiniMessage.miniMessage().deserialize(messageService.getMessage(Message.MESSAGE_PUNISHMENT_SEND.getTag(), languageId),
                Placeholder.parsed("reason", reasonText),
                Placeholder.parsed("from", punisherName),
                Placeholder.parsed("start_time", startTime),
                Placeholder.parsed("expires_time", expiresTime), Placeholder.unparsed("modified", modified),
                Placeholder.parsed("punishment_id", audit.id().toString())
        );
    }
}
