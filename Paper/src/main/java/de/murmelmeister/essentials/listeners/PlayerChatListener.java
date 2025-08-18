package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIp;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.net.InetSocketAddress;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class PlayerChatListener implements Listener {
    private final MurmelEssentials plugin;
    private final Ranks ranks;
    private final UserProvider userProvider;
    private final PunishmentLogProvider logProvider;
    private final PunishmentCurrentUserProvider punishedUserProvider;
    private final PunishmentCurrentIpProvider punishedIpProvider;
    private final PunishmentService punishmentService;
    private final int typeMuteId = PunishmentType.MUTE.getId();
    private final int typeIpMuteId = PunishmentType.IP_MUTE.getId();

    public PlayerChatListener(MurmelEssentials instance) {
        this.plugin = instance;
        this.ranks = instance.getRanks();
        this.userProvider = instance.getUserProvider();
        this.logProvider = instance.getPunishmentLogProvider();
        this.punishedUserProvider = instance.getPunishmentUserProvider();
        this.punishedIpProvider = instance.getPunishmentIpProvider();
        this.punishmentService = instance.getPunishmentService();
    }

    @EventHandler
    public void handlePlayerChat(AsyncChatEvent event) {
        checkPunishment(event.getPlayer(), event);
        ranks.setChatFormat(event);
    }

    private void checkPunishment(Player player, AsyncChatEvent event) {
        User user = userProvider.findByMojangId(player.getUniqueId());
        if (user == null) {
            event.setCancelled(true);
            player.sendMessage("<red>Something went wrong, please try again...");
            return;
        }
        int userId = user.id();
        int languageId = user.languageId();

        if (punishmentService.isPunishedUser(userId, typeMuteId)) {
            PunishmentCurrentUser punishedUser = punishedUserProvider.getPunishedUser(userId, typeMuteId);
            UUID logId = punishedUser.logId();
            if (punishmentService.isExpiredUser(logId))
                punishmentService.autoUnpunishedUser(userId, typeMuteId);
            else {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, logId)));
                return;
            }
        }

        if (punishmentService.isPunishedUser(userId, typeIpMuteId)) {
            PunishmentCurrentUser punishedUser = punishedUserProvider.getPunishedUser(userId, typeIpMuteId);
            UUID logId = punishedUser.logId();
            if (punishmentService.isExpiredUser(logId))
                punishmentService.autoUnpunishedUser(userId, typeIpMuteId);
            else {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, logId)));
                return;
            }
        }

        checkPunishmentIp(player, languageId, event);
    }

    private void checkPunishmentIp(Player player, int languageId, AsyncChatEvent event) {
        InetSocketAddress inetSocketAddress = player.getAddress();
        if (inetSocketAddress == null) {
            event.setCancelled(true);
            player.sendMessage("<red>Something went wrong, please try again...");
            return;
        }
        String ipAddress = inetSocketAddress.getAddress().getHostAddress();

        if (punishmentService.isPunishedIp(ipAddress, typeMuteId)) {
            PunishmentCurrentIp punishedIp = punishedIpProvider.getPunishedIp(ipAddress, typeMuteId);
            UUID logId = punishedIp.logId();
            if (punishmentService.isExpiredIp(logId))
                punishmentService.autoUnpunishedIp(ipAddress, typeMuteId);
            else {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, logId)));
                return;
            }
        }

        if (punishmentService.isPunishedIp(ipAddress, typeIpMuteId)) {
            PunishmentCurrentIp punishedIp = punishedIpProvider.getPunishedIp(ipAddress, typeIpMuteId);
            UUID logId = punishedIp.logId();
            if (punishmentService.isExpiredIp(logId))
                punishmentService.autoUnpunishedIp(ipAddress, typeIpMuteId);
            else {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, logId)));
            }
        }
    }

    private String getPunishMessage(int languageId, UUID logId) {
        DateTimeFormatter dateTime = plugin.getDateTimeFormatter(languageId);
        PunishmentLog log = logProvider.getLog(logId);
        String reasonText = log.reasonText();
        int punisherId = log.createdBy();
        User userPunisher = userProvider.findById(punisherId);
        String punisherName = userPunisher.username();
        String startTime = log.createdAt().format(dateTime);
        String expiresTime = log.expiresAt() != null
                ? log.expiresAt().format(dateTime) : "Never";
        String autoPunish = log.reasonAutoPunish()
                ? "<#00cc88>Yes</#00cc88>" : "<#cc0088>No</#cc0088>";
        String modified = log.action() == PunishmentLog.Action.MODIFIED
                ? "<#999999>(modified)" : "";
        return """
                <#990000>You are blocked from the network.
                <#999999>Reason: <#999900>[REASON]
                <#999999>From: <#999900>[FROM]
                <#999999>Start: <#999900>[START_TIME]
                <#999999>Expires: <#999900>[EXPIRES_TIME] [MODIFIED]
                <#999999>Auto-Punish: <#999900>[AUTO_PUNISH]
                <#999999>Punishment-ID: <#999900>[PUNISHMENT_ID]"""
                .replace("[REASON]", reasonText)
                .replace("[FROM]", punisherName)
                .replace("[START_TIME]", startTime)
                .replace("[EXPIRES_TIME]", expiresTime)
                .replace("[MODIFIED]", modified)
                .replace("[AUTO_PUNISH]", autoPunish)
                .replace("[PUNISHMENT_ID]", logId.toString());
    }
}
