package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.essentials.manager.ListenerManager;
import de.murmelmeister.murmelapi.punishment.PunishmentType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.net.InetAddress;
import java.util.UUID;

public final class PlayerChatListener extends ListenerManager {
    public PlayerChatListener(MurmelEssentials instance) {
        super(instance);
    }

    @EventHandler
    public void handlePlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        checkPunishments(player, userId, event);
        Ranks.setChatFormat(event, this.instance.getGroup(), this.instance.getUser());
    }


    private void checkPunishments(Player player, int userId, AsyncChatEvent event) {
        InetAddress inetAddress = player.getAddress().getAddress();

        // maybe a type list of punishment types
        int muteId = PunishmentType.MUTE.getId();
        int ipMuteId = PunishmentType.IP_MUTE.getId();

        if (punishmentUser.exists(userId, muteId)) {
            if (punishmentUser.isPunished(userId, muteId)) {
                punishedMessage(player, userId, muteId, false, false, event);
                return;
            } else {
                punishmentUser.unpunished(userId, muteId);
            }
        }

        checkIpPunishment(player, userId, inetAddress, muteId, event);
        checkIpPunishment(player, userId, inetAddress, ipMuteId, event);
    }

    private void checkIpPunishment(Player player, int userId, InetAddress inetAddress, int punishId, AsyncChatEvent event) {
        if (punishmentIp.isPunished(inetAddress, punishId)) {
            int reasonId = punishmentIp.getReasonId(inetAddress, punishId);
            boolean autoPunish = punishmentReason.getAutoPunish(reasonId, punishId);
            if (autoPunish && !punishmentUser.exists(userId, punishId))
                punishmentUser.punish(userId, punishId, -1, inetAddress, reasonId);
            punishedMessage(player, userId, punishId, true, autoPunish, event);
        } else {
            punishmentIp.unpunished(inetAddress, punishId);
        }
    }

    private void punishedMessage(Player player, int userId, int punishId, boolean isIp, boolean autoPunish, AsyncChatEvent event) {
        InetAddress inetAddress = player.getAddress().getAddress();
        UUID logId;
        String reasonText;
        String expireDate;
        String startDate;
        String punisher;

        if (isIp) {
            logId = punishmentIp.getLogId(inetAddress, punishId);
            reasonText = punishmentIp.getReason(inetAddress, punishId);
            expireDate = punishmentIp.getExpiredDate(inetAddress, punishId);
            startDate = punishmentIp.getCreatedAt(inetAddress, punishId).toString();
            punisher = user.getUsername(punishmentIp.getCreatedBy(inetAddress, punishId));
        } else {
            logId = punishmentUser.getLogId(userId, punishId);
            reasonText = punishmentUser.getReason(userId, punishId);
            expireDate = punishmentUser.getExpiredDate(userId, punishId);
            startDate = punishmentUser.getCreatedAt(userId, punishId).toString();
            punisher = user.getUsername(punishmentUser.getCreatedBy(userId, punishId));
        }

        boolean status = !autoPunish;
        String message = String.format("""
                <#990000>You are muted from the network.
                <#999999>Reason: <#009999>%s
                <#999999>Punisher: <#009999>%s
                <#999999>Start: <#009999>%s
                <#999999>Expires: <#009999>%s
                <#999999>PunishID: <#009999>%s
                <#999999>Mod-Punish: <#009999>%s
                """, reasonText, punisher, startDate, expireDate, logId.toString(), status);
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
        event.setCancelled(true);
    }
}
