package de.murmelmeister.essentials.api;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;

import java.sql.SQLException;
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
        try {
            return Tristate.fromBoolean(permission.hasPermission(player.getUniqueId(), perm));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PermissionFunction createFunction(PermissionSubject permissionSubject) {
        Preconditions.checkArgument(permissionSubject == player);
        return this;
    }

    public static void updatePermissions(ProxyServer server, MurmelEssentials instance) {
        Group group = instance.getGroup();
        User user = instance.getUser();
        server.getScheduler().buildTask(instance, () -> {
            try {
                group.loadExpired();
                user.loadExpired();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).repeat(10 * 1000L, TimeUnit.MILLISECONDS).schedule();
    }
}
