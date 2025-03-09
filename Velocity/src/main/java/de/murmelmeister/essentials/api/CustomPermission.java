package de.murmelmeister.essentials.api;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;

import java.util.concurrent.TimeUnit;

public final class CustomPermission implements PermissionProvider, PermissionFunction {
    private final Permission permission;
    private final Player player;
    private static ScheduledTask task;

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

    public static void updatePermission(MurmelEssentials plugin, ProxyServer server) {
        if (task != null && task.status() != TaskStatus.CANCELLED) task.cancel();
        Group group = plugin.getGroup();
        User user = plugin.getUser();
        task = server.getScheduler().buildTask(plugin, () -> {
            group.getParent().loadExpired(group);
            group.getPermission().loadExpired(group);
            user.getParent().loadExpired(user);
            user.getPermission().loadExpired(user);
        }).repeat(10, TimeUnit.SECONDS).schedule();
    }
}
