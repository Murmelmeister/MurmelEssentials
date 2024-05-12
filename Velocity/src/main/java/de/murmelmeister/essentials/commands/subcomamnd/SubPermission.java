package de.murmelmeister.essentials.commands.subcomamnd;

import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.utils.PermissionSyntaxUtil;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.utils.TimeUtil;

import java.sql.SQLException;

import static de.murmelmeister.essentials.manager.CommandManager.sendSourceMessage;

public final class SubPermission {
    private final CommandManager commandManager;
    private final Permission permission;
    private final User user;
    private final GroupParent groupParent;
    private final GroupPermission groupPermission;
    private final UserPermission userPermission;

    public SubPermission(CommandManager commandManager, Permission permission, User user, GroupParent groupParent, GroupPermission groupPermission, UserPermission userPermission) {
        this.commandManager = commandManager;
        this.permission = permission;
        this.user = user;
        this.groupParent = groupParent;
        this.groupPermission = groupPermission;
        this.userPermission = userPermission;
    }

    public void permission(CommandSource source, boolean isUser, int id, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            sendSourceMessage(source, "§3Permissions: ");
            var permissions = isUser ? userPermission.getPermissions(id) : groupPermission.getPermissions(id);
            for (var all : permissions)
                sendSourceMessage(source, "§7- §e%s", all);
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) {
            PermissionSyntaxUtil.syntaxPermission(source, isUser);
            return;
        }

        switch (args[3]) {
            case "all" -> allPermission(source, isUser, id, args);
            case "add" -> addPermission(source, isUser, id, creatorId, args);
            case "remove" -> removePermission(source, isUser, id, args);
            case "clear" -> clearPermission(source, isUser, id, args);
            case "info" -> infoPermission(source, isUser, id, args);
            case "time" -> timePermission(source, isUser, id, args);
            default -> PermissionSyntaxUtil.syntaxPermission(source, isUser);
        }
    }

    private void allPermission(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        var permissions = isUser ? this.permission.getPermissions(id) : groupPermission.getAllPermissions(groupParent, id);
        sendSourceMessage(source, "§3All permissions: ");
        for (var all : permissions)
            sendSourceMessage(source, "§7- §e%s", all);
    }

    private void addPermission(CommandSource source, boolean isUser, int id, int creatorId, String[] args) throws SQLException {
        var permission = args[4];
        if (args.length == 5) {
            if (isUser) userPermission.addPermission(id, creatorId, permission, -1);
            else groupPermission.addPermission(id, creatorId, permission, -1);
            sendSourceMessage(source, "§3Permission §e%s §3is now added.", permission);
            return;
        }
        var time = TimeUtil.formatTime(args[5]);
        if (time == -2) {
            sendSourceMessage(source, "§cNo negative value allowed");
            return;
        }
        if (time == -3) {
            sendSourceMessage(source, "§cInvalid time format");
            return;
        }
        if (isUser) userPermission.addPermission(id, creatorId, permission, time);
        else groupPermission.addPermission(id, creatorId, permission, time);
        sendSourceMessage(source, "§3Permission §e%s §3is now added for §e%s", permission, getPermissionExpiredDate(isUser, id, permission));
    }

    private void removePermission(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        if (args.length < 5) {
            PermissionSyntaxUtil.syntaxPermission(source, isUser);
            return;
        }
        var permission = args[4];
        if (isPermissionNotExist(source, isUser, id, permission)) return;

        if (isUser) userPermission.removePermission(id, permission);
        else groupPermission.removePermission(id, permission);
        sendSourceMessage(source, "§3Permission §e%s §3is now removed.", permission);
    }

    private void clearPermission(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        if (isUser) userPermission.clearPermission(id);
        else groupPermission.clearPermission(id);
        sendSourceMessage(source, "§3All permissions are now cleared.");
    }

    private void infoPermission(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        if (args.length < 5) {
            PermissionSyntaxUtil.syntaxPermission(source, isUser);
            return;
        }
        var permission = args[4];
        if (isPermissionNotExist(source, isUser, id, permission)) return;

        var creator = isUser ? userPermission.getCreatorId(id, permission) : groupPermission.getCreatorId(id, permission);
        var createdDate = isUser ? userPermission.getCreatedDate(id, permission) : groupPermission.getCreatedDate(id, permission);
        var name = isUser ? "§3Username: §e%s" : "§3Rank: §e%s";
        sendSourceMessage(source, "§8--- §3Info permission: §e%s §8---", permission);
        sendSourceMessage(source, name, args[1]);
        commandManager.sendCreatorMessage(source, user, creator);
        sendSourceMessage(source, "§3Created date: §e%s", createdDate);
        sendSourceMessage(source, "§3Expired date: §e%s", getPermissionExpiredDate(isUser, id, permission));
    }

    private void timePermission(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        if (args.length < 6) {
            PermissionSyntaxUtil.syntaxPermission(source, isUser);
            return;
        }
        var permission = args[4];
        if (isPermissionNotExist(source, isUser, id, permission)) return;
        var time = TimeUtil.formatTime(args[5]);

        if (time == -2) {
            sendSourceMessage(source, "§cNo negative value allowed");
            return;
        }
        if (time == -3) {
            sendSourceMessage(source, "§cInvalid time format");
            return;
        }

        switch (args[6]) {
            case "set" -> {
                var expiredDate = isUser ? userPermission.setExpiredTime(id, permission, time) : groupPermission.setExpiredTime(id, permission, time);
                sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, expiredDate);
            }
            case "add" -> {
                var expiredDate = isUser ? userPermission.addExpiredTime(id, permission, time) : groupPermission.addExpiredTime(id, permission, time);
                sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, expiredDate);
            }
            case "remove" -> {
                var expiredDate = isUser ? userPermission.removeExpiredTime(id, permission, time) : groupPermission.removeExpiredTime(id, permission, time);
                sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, expiredDate);
            }
            default -> PermissionSyntaxUtil.syntaxPermission(source, isUser);
        }
    }

    private String getPermissionExpiredDate(boolean isUser, int id, String permission) throws SQLException {
        return isUser ? userPermission.getExpiredDate(id, permission) : groupPermission.getExpiredDate(id, permission);
    }

    private boolean isPermissionNotExist(CommandSource source, boolean isUser, int id, String permission) throws SQLException {
        var exist = isUser ? userPermission.existsPermission(id, permission) : groupPermission.existsPermission(id, permission);
        if (!exist) {
            sendSourceMessage(source, "§cPermission does not exist.");
            return true;
        } else return false;
    }
}
