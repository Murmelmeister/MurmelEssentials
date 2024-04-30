package de.murmelmeister.essentials.api;

import de.murmelmeister.murmelapi.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class CustomPermission extends PermissibleBase {
    private final Player player;
    private final Permission permission;

    public CustomPermission(Player player, Permission permission) {
        super(player);
        this.player = player;
        this.permission = permission;
    }

    @Override
    public boolean hasPermission(@NotNull String perm) {
        try {
            return this.permission.hasPermission(this.player.getUniqueId(), perm);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
