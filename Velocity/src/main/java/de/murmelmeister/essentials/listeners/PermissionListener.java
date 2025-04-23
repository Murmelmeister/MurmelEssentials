package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;

public final class PermissionListener {
    private final User user;
    private final Permission permission;

    public PermissionListener(User user, Permission permission) {
        this.user = user;
        this.permission = permission;
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
    public void handleLogin(LoginEvent event) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        permission.preloadAsync(userId);
    }

    @Subscribe
    public void handleDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        int userId = user.getId(player.getUniqueId());
        permission.invalidate(userId);
    }
}
