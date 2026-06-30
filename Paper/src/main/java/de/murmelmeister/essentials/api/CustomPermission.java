package de.murmelmeister.essentials.api;

import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;

public final class CustomPermission extends PermissibleBase {
    private final Player player;
    private final UserProvider userProvider;
    private final PermissionService permissionService;

    public CustomPermission(Player player, UserProvider userProvider, PermissionService permissionService) {
        super(player);
        this.player = player;
        this.userProvider = userProvider;
        this.permissionService = permissionService;
    }

    @Override
    public boolean hasPermission(@NotNull String perm) {
        User user = userProvider.findByMojangId(player.getUniqueId()).orElse(null);
        if (user == null) return false;

        return permissionService.hasPermission(PermissionTarget.user(user.id()), perm);
    }
}
