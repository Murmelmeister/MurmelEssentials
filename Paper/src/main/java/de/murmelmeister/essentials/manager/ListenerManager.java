package de.murmelmeister.essentials.manager;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.listeners.ConnectListener;
import de.murmelmeister.essentials.listeners.CustomPermissionListener;
import de.murmelmeister.essentials.listeners.PlayerChatListener;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.user.User;
import org.bukkit.event.Listener;

public class ListenerManager implements Listener {
    public final MurmelEssentials instance;
    public final Permission permission;
    public final User user;
    public final PunishmentReason punishmentReason;
    public final PunishmentIP punishmentIp;
    public final PunishmentUser punishmentUser;

    public ListenerManager(MurmelEssentials instance) {
        this.instance = instance;
        this.permission = instance.getPermission();
        this.user = instance.getUser();
        this.punishmentReason = instance.getPunishmentReason();
        this.punishmentIp = instance.getPunishmentIP();
        this.punishmentUser = instance.getPunishmentUser();
    }

    public static void register(MurmelEssentials instance) {
        addListener(instance, new CustomPermissionListener(instance));
        addListener(instance, new ConnectListener(instance));
        addListener(instance, new PlayerChatListener(instance));
    }

    private static void addListener(MurmelEssentials instance, Listener listener) {
        instance.getServer().getPluginManager().registerEvents(listener, instance);
    }
}
