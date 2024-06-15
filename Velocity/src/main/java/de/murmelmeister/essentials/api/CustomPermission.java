package de.murmelmeister.essentials.api;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.permission.Permission;

import java.util.concurrent.TimeUnit;

public final class CustomPermission implements PermissionProvider, PermissionFunction {
    private final Permission permission;
    private final Player player;

    public CustomPermission(Permission permission, Player player) {
        this.permission = permission;
        this.player = player;
    }

    @Override
    public Tristate getPermissionValue(String perm) {
        return Tristate.fromBoolean(permission.hasPermission(player.getUniqueId(), perm));
    }

    @Override
    public PermissionFunction createFunction(PermissionSubject permissionSubject) {
        Preconditions.checkArgument(permissionSubject == player);
        return this;
    }

    public static void updatePermission(ProxyServer server, MurmelEssentials instance) {
        server.getScheduler().buildTask(instance, () -> {
            instance.getGroup().loadExpired();
            instance.getUser().loadExpired();
        }).repeat(10, TimeUnit.SECONDS).schedule();
    }
}
