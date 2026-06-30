package de.murmelmeister.essentials.listeners;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.Ranks;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class PlayerChatListener implements Listener {
    private final MurmelEssentials plugin;
    private final Ranks ranks;
    private final UserProvider userProvider;
    private final PunishmentService punishmentService;
    private final int typeMuteId = PunishmentType.MUTE.getId();
    private final int typeIpMuteId = PunishmentType.IP_MUTE.getId();

    // TODO: Show in the velocity punish stuff and change this class (outdated?)
    // Maybe block the player to write to signs (outdated?)

    public PlayerChatListener(@NotNull MurmelEssentials instance) {
        this.plugin = instance;
        this.ranks = instance.getRanks();
        this.userProvider = instance.getUserProvider();
        this.punishmentService = instance.getPunishmentService();
    }

    @EventHandler
    public void handlePlayerChat(@NotNull AsyncChatEvent event) {
        checkPunishment(event.getPlayer(), event);
        ranks.setChatFormat(event);
    }

    private void checkPunishment(@NotNull Player player, @NotNull AsyncChatEvent event) {
        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) {
            event.setCancelled(true);
            player.sendMessage("<red>Something went wrong, please try again...");
            return;
        }
        UUID mojangId = user.mojangId();
        int languageId = user.languageId();

        if (punishmentService.checkUserPunishment(mojangId, typeMuteId, audit -> {
            event.setCancelled(true);
            player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, audit)));
        })) return;

        if (punishmentService.checkUserPunishment(mojangId, typeIpMuteId, audit -> {
            event.setCancelled(true);
            player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, audit)));
        })) return;

        // User is not muted

        checkPunishmentIp(player, languageId, event);
    }

    private void checkPunishmentIp(@NotNull Player player, int languageId, @NotNull AsyncChatEvent event) {
        InetSocketAddress inetSocketAddress = player.getAddress();
        if (inetSocketAddress == null) {
            event.setCancelled(true);
            player.sendMessage("<red>Something went wrong, please try again...");
            return;
        }
        InetAddress ipAddress = inetSocketAddress.getAddress();

        if (punishmentService.checkIpPunishment(ipAddress, typeMuteId, audit -> {
            event.setCancelled(true);
            player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, audit)));
        })) return;

        if (punishmentService.checkIpPunishment(ipAddress, typeIpMuteId, audit -> {
            event.setCancelled(true);
            player.sendMessage(MiniMessage.miniMessage().deserialize(getPunishMessage(languageId, audit)));
        })) return;

        // IP is not muted
    }

    private @NotNull String getPunishMessage(int languageId, @NotNull PunishmentAudit audit) {
        DateTimeFormatter dateTime = plugin.getDateTimeFormatter(languageId);
        String reasonText = audit.reasonText();
        int punisherId = audit.createdBy();
        User userPunisher = userProvider.findById(punisherId).orElseThrow(() -> new IllegalArgumentException("Punisher not found"));
        String punisherName = userPunisher.username();
        String startTime = audit.createdAt().format(dateTime);
        String expiresTime = audit.expiresAt() != null
                ? audit.expiresAt().format(dateTime) : "Never";
        String modified = audit.action() == PunishmentAudit.Action.MODIFIED
                ? "<#999999>(modified)" : "";
        return """
                <#990000>You are blocked from the network.
                <#999999>Reason: <#999900>[REASON]
                <#999999>From: <#999900>[FROM]
                <#999999>Start: <#999900>[START_TIME]
                <#999999>Expires: <#999900>[EXPIRES_TIME] [MODIFIED]
                <#999999>Punishment-ID: <#999900>[PUNISHMENT_ID]"""
                .replace("[REASON]", reasonText)
                .replace("[FROM]", punisherName)
                .replace("[START_TIME]", startTime)
                .replace("[EXPIRES_TIME]", expiresTime)
                .replace("[MODIFIED]", modified)
                .replace("[PUNISHMENT_ID]", audit.id().toString());
    }
}
