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
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;

import java.util.concurrent.TimeUnit;

public final class CustomPermission implements PermissionProvider, PermissionFunction {
    private final PermissionService permissionService;
    private final Player player;
    private final PermissionTarget target;

    private static ScheduledTask task;

    public CustomPermission(PermissionService permissionService, Player player, int userId) {
        this.permissionService = permissionService;
        this.player = player;
        this.target = PermissionTarget.user(userId);
    }

    @Override
    public Tristate getPermissionValue(String perm) {
        return Tristate.fromBoolean(permissionService.hasPermission(target, perm));
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
                    int updatedRows = plugin.getPermissionService().loadExpired();
                    if (updatedRows > 0)
                        plugin.getLogger().info("Updated {} expired permissions.", updatedRows);
                })
                .repeat(1, TimeUnit.MINUTES).schedule();
    }
}
