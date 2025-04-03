package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.net.InetAddress;
import java.util.UUID;

public final class PunishmentUtil {
    private static final User user;
    private static final PunishmentIP punishmentIp;
    private static final PunishmentUser punishmentUser;

    static {
        user = MurmelAPI.getUser();
        punishmentIp = MurmelAPI.getPunishmentIP();
        punishmentUser = MurmelAPI.getPunishmentUser();
    }

    public static void disconnectPunishMessage(Player player, int userId, int punishId, boolean isIp, boolean autoPunish) {
        player.disconnect(MiniMessage.miniMessage().deserialize(punishMessage(player, userId, punishId, isIp, autoPunish)));
    }

    public static String punishMessage(Player player, int userId, int punishId, boolean isIp, boolean autoPunish) {
        InetAddress inetAddress = player.getRemoteAddress().getAddress();
        UUID logId;
        String reasonText;
        String expireDate;
        String startDate;
        String punisher;

        if (isIp) {
            logId = punishmentIp.getLogId(inetAddress, punishId);
            reasonText = punishmentIp.getReason(inetAddress, punishId);
            expireDate = punishmentIp.getExpiredAt(inetAddress, punishId) == null ? "never" : MurmelAPI.getDateFormat().format(punishmentIp.getExpiredAt(inetAddress, punishId));
            startDate = MurmelAPI.getDateFormat().format(punishmentIp.getCreatedAt(inetAddress, punishId));
            punisher = user.getUsername(punishmentIp.getCreatedBy(inetAddress, punishId));
        } else {
            logId = punishmentUser.getLogId(userId, punishId);
            reasonText = punishmentUser.getReason(userId, punishId);
            expireDate = punishmentUser.getExpiredAt(userId, punishId) == null ? "never" : MurmelAPI.getDateFormat().format(punishmentUser.getExpiredAt(userId, punishId));
            startDate = MurmelAPI.getDateFormat().format(punishmentUser.getCreatedAt(userId, punishId));
            punisher = user.getUsername(punishmentUser.getCreatedBy(userId, punishId));
        }

        boolean status = !autoPunish;
        return String.format("""
                <#990000>You are punished from the network.
                <#999999>Reason: <#009999>%s
                <#999999>Punisher: <#009999>%s
                <#999999>Start: <#009999>%s
                <#999999>Expires: <#009999>%s
                <#999999>PunishID: <#009999>%s
                <#999999>Mod-Punish: <#009999>%s
                """, reasonText, punisher, startDate, expireDate, logId.toString(), status);
    }
}
