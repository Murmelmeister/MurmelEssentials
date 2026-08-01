package de.murmelmeister.essentials.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.api.CustomPermission;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.user.User;
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
    public void handlePermission(@NotNull PermissionsSetupEvent event) {
        if (!(event.getSubject() instanceof Player player)) return;

        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) return;

        event.setProvider(new CustomPermission(permissionService, player, user.id()));
    }
}
