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
import de.murmelmeister.murmelapi.permission.Permission;

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
        if (task != null && task.status() != TaskStatus.CANCELLED)
            task.cancel();
        task = server.getScheduler().buildTask(plugin, () -> {
                    int updatedRows = plugin.getPermission().loadExpired();
                    if (updatedRows > 0)
                        plugin.getLogger().info("Updated {} expired permissions.", updatedRows);
                })
                .repeat(1, TimeUnit.SECONDS).schedule();
    }
}
