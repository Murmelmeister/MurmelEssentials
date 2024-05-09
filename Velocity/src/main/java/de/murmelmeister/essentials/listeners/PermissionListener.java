package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;

import java.sql.SQLException;

public final class PermissionListener {
    private final Permission permission;
    private final Group group;
    private final User user;

    public PermissionListener(Permission permission, Group group, User user) {
        this.permission = permission;
        this.group = group;
        this.user = user;
    }

    @Subscribe
    public void handlePermission(PermissionsSetupEvent event, Continuation continuation) {
        if (!(event.getSubject() instanceof Player player)) {
            continuation.resume();
            return;
        }
        try {
            event.setProvider(new CustomPermission(permission, player));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            continuation.resume();
        }
    }

    @Subscribe
    public void handleConnection(ServerConnectedEvent event) throws SQLException {
        var player = event.getPlayer();
        user.joinUser(player.getUniqueId(), player.getUsername());
        var uid = user.getId(player.getUniqueId());
        user.getParent().addParent(uid, -1, group.getDefaultGroup(), -1);
    }
}
