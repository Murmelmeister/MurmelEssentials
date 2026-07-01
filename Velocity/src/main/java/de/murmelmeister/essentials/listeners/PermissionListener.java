package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.user.UserProvider;
import org.jetbrains.annotations.NotNull;

public final class PermissionListener {
    private final UserProvider userProvider;
    private final PermissionService permissionService;

    public PermissionListener(@NotNull MurmelEssentials plugin) {
        this.userProvider = plugin.getUserProvider();
        this.permissionService = plugin.getPermissionService();
    }

    @Subscribe
    public void handlePermission(@NotNull PermissionsSetupEvent event, @NotNull Continuation continuation) {
        if (!(event.getSubject() instanceof Player player)) {
            continuation.resume();
            return;
        }
        try {
            event.setProvider(new CustomPermission(userProvider, permissionService, player));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            continuation.resume();
        }
    }
}
