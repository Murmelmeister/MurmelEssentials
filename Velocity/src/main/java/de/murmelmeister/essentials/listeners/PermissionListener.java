package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.murmelapi.permission.Permission;

public final class PermissionListener {
    private final Permission permission;

    public PermissionListener(Permission permission) {
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
}
