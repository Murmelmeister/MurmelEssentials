package de.murmelmeister.essentials.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.settings.GroupColorSettings;
import de.murmelmeister.murmelapi.group.settings.GroupColorType;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PermissionCommand extends CommandManager {
    private final Permission permission;
    private final Group group;
    private final User user;

    private final GroupParent groupParent;
    private final GroupPermission groupPermission;
    private final GroupColorSettings groupColorSettings;
    private final UserParent userParent;
    private final UserPermission userPermission;

    public PermissionCommand(Permission permission, Group group, User user) {
        this.permission = permission;
        this.group = group;
        this.user = user;
        this.groupParent = group.getParent();
        this.groupPermission = group.getPermission();
        this.groupColorSettings = group.getColorSettings();
        this.userParent = user.getParent();
        this.userPermission = user.getPermission();
    }

    @Override
    public void execute(Invocation invocation) {
        var args = invocation.arguments();
        var source = invocation.source();

        if (!source.hasPermission("murmelessentials.command.permission")) {
            sendSourceMessage(source, "§cYou do not have permission to use this command.");
            return;
        }

        var player = source instanceof Player ? (Player) source : null;
        var playerId = player != null ? player.getUniqueId() : null;

        try {
            if (args.length == 1) {
                switch (args[0]) {
                    case "groups" -> {
                        sendSourceMessage(source, "§3Groups: ");
                        for (var name : group.getNames())
                            sendSourceMessage(source, "§7- §e" + name);
                    }
                    case "users" -> {
                        sendSourceMessage(source, "§3Users: ");
                        for (var name : user.getUsernames())
                            sendSourceMessage(source, "§7- §e" + name);
                    }
                    default -> syntax(source);
                }
                return;
            }

            var creatorId = playerId == null ? -1 : user.getId(playerId);
            if (args.length >= 3) {
                switch (args[0]) {
                    case "group" -> groups(source, creatorId, args);
                    case "user" -> users(source, creatorId, args);
                    default -> syntax(source);
                }
            } else syntax(source);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        var args = invocation.arguments();
        try {
            if (args.length == 1)
                return Stream.of("user", "users", "group", "groups").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 2 && args[0].equals("group")) // Show all group names
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 2 && args[0].equals("user")) // Show all usernames
                return user.getUsernames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 3 && args[0].equals("group")) // Show all group commands
                return Stream.of("create", "delete", "rename", "parent", "permission", "edit").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 3 && args[0].equals("user")) // Show all user commands
                return Stream.of("parent", "permission").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && (args[0].equals("group") || args[0].equals("user")) && args[2].equals("parent")) // Show all group/user parent commands
                return Stream.of("add", "remove", "clear", "creator", "created", "expired").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && (args[0].equals("group") || args[0].equals("user")) && args[2].equals("permission")) // Show all group/user permission commands
                return Stream.of("all", "add", "remove", "clear", "creator", "created", "expired").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && args[0].equals("group") && args[2].equals("edit")) // Show all group edit commands
                return Stream.of("chat", "tab", "tag").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("edit") && (args[3].equals("chat") || args[3].equals("tab") || args[3].equals("tag"))) // Show all group edit subcommands
                return Stream.of("prefix", "suffix", "color").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("parent") && args[3].equals("add")) // Add group parent
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("parent") && (args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) // Remove/Creator/Created/Expired group parent
                return groupParent.getParentNames(group, group.getUniqueId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("permission") && (args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) // Remove/Creator/Created/Expired group permission
                return groupPermission.getPermissions(group.getUniqueId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("parent") && args[3].equals("add")) // Add user parent
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("parent") && (args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) // Remove/Creator/Created/Expired user parent
                return userParent.getParentNames(group, user.getId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("permission") && (args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) // Remove/Creator/Created/Expired user permission
                return userPermission.getPermissions(user.getId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 7 && (args[0].equals("group") || args[0].equals("user")) && (args[2].equals("parent") || args[2].equals("permission")) && args[3].equals("expired")) // Set/Remove/Expired time
                return Stream.of("set", "add", "remove").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    private void groups(CommandSource source, int creatorId, String[] args) throws SQLException {
        var groupName = args[1];
        if (!group.existsGroup(groupName)) {
            if (args[2].equals("create")) {
                group.createNewGroup(groupName, creatorId, Integer.parseInt(args[3]), args[4]); // TODO: Check if sortId is a number and teamId is a string and add edit
                group.getColorSettings().setColor(GroupColorType.TAG, group.getUniqueId(groupName), creatorId, "&7");
                sendSourceMessage(source, "§3Group §e%s §3is now created.", groupName);
            } else sendSourceMessage(source, "§cGroup does not exist.");
            return;
        }

        var groupId = group.getUniqueId(groupName);
        switch (args[2]) {
            case "delete" -> {
                group.deleteGroup(groupId);
                sendSourceMessage(source, "§3Group §e%s §3is now deleted.", groupName);
            }
            case "rename" -> {
                if (args.length == 3) {
                    syntax(source);
                    return;
                }
                group.rename(groupId, args[3]);
                sendSourceMessage(source, "§3Group is now renamed to §e%s", args[3]);
            }
            case "parent" -> groupParent(source, groupId, creatorId, args);
            case "permission" -> groupPermission(source, groupId, creatorId, args);
            case "edit" -> groupEdit(source, groupId, creatorId, args);
            default -> syntax(source);
        }
    }

    private void groupParent(CommandSource source, int groupId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            sendSourceMessage(source, "§3Parents: ");
            for (var groupIds : groupParent.getParentIds(groupId))
                sendSourceMessage(source, "§7- §e%s", group.getName(groupIds));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String parentName;
        int parentId;
        switch (args[3]) {
            case "add" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                if (args.length == 5) {
                    groupParent.addParent(groupId, creatorId, parentId, -1);
                    sendSourceMessage(source, "§3Parent §e%s §3is now added.", parentName);
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                groupParent.addParent(groupId, creatorId, parentId, time);
                sendSourceMessage(source, "§3Parent §e%s §3is now added for §e%s", parentName, groupParent.getExpiredDate(groupId, parentId));
            }
            case "remove" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                groupParent.removeParent(groupId, parentId);
                sendSourceMessage(source, "§3Parent §e%s §3is now removed.", parentName);
            }
            case "clear" -> {
                groupParent.clearParent(groupId);
                sendSourceMessage(source, "§3All parents are now cleared.");
            }
            case "creator" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                var creator = groupParent.getCreatorId(groupId, parentId);
                sendCreatorMessage(source, creator);
            }
            case "created" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                sendSourceMessage(source, "§3Created: §e%s", groupParent.getCreatedDate(groupId, parentId));
            }
            case "expired" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                if (args.length == 5) {
                    sendSourceMessage(source, "§3Expired: §e%s", groupParent.getExpiredDate(groupId, parentId));
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                switch (args[6]) {
                    case "set" -> {
                        groupParent.setExpiredTime(groupId, parentId, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, groupParent.getExpiredDate(groupId, parentId));
                    }
                    case "add" -> {
                        groupParent.addExpiredTime(groupId, parentId, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, groupParent.getExpiredDate(groupId, parentId));
                    }
                    case "remove" -> {
                        groupParent.removeExpiredTime(groupId, parentId, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, groupParent.getExpiredDate(groupId, parentId));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private void groupPermission(CommandSource source, int groupId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            sendSourceMessage(source, "§3Permissions: ");
            for (var all : groupPermission.getPermissions(groupId))
                sendSourceMessage(source, "§7- §e%s", all);
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String permission;
        switch (args[3]) {
            case "all" -> {
                sendSourceMessage(source, "§3All permissions: ");
                for (var all : groupPermission.getAllPermissions(groupParent, groupId))
                    sendSourceMessage(source, "§7- §e%s", all);
            }
            case "add" -> {
                permission = args[4];
                if (args.length == 5) {
                    groupPermission.addPermission(groupId, creatorId, permission, -1);
                    sendSourceMessage(source, "§3Permission §e%s §3is now added.", permission);
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                groupPermission.addPermission(groupId, creatorId, permission, time);
                sendSourceMessage(source, "§3Permission §e%s §3is now added for §e%s", permission, groupPermission.getExpiredDate(groupId, permission));
            }
            case "remove" -> {
                permission = args[4];
                groupPermission.removePermission(groupId, permission);
                sendSourceMessage(source, "§3Permission §e%s §3is now removed.", permission);
            }
            case "clear" -> {
                groupPermission.clearPermission(groupId);
                sendSourceMessage(source, "§3All permissions are now cleared.");
            }
            case "creator" -> {
                permission = args[4];
                var creator = groupPermission.getCreatorId(groupId, permission);
                sendCreatorMessage(source, creator);
            }
            case "created" -> {
                permission = args[4];
                sendSourceMessage(source, "§3Created: §e%s", groupPermission.getCreatedDate(groupId, permission));
            }
            case "expired" -> {
                permission = args[4];
                if (args.length == 5) {
                    sendSourceMessage(source, "§3Expired: §e%s", groupPermission.getExpiredDate(groupId, permission));
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                switch (args[6]) {
                    case "set" -> {
                        groupPermission.setExpiredTime(groupId, permission, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, groupPermission.getExpiredDate(groupId, permission));
                    }
                    case "add" -> {
                        groupPermission.addExpiredTime(groupId, permission, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, groupPermission.getExpiredDate(groupId, permission));
                    }
                    case "remove" -> {
                        groupPermission.removeExpiredTime(groupId, permission, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, groupPermission.getExpiredDate(groupId, permission));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private void groupEdit(CommandSource source, int groupId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            syntax(source);
            return;
        }
        var builder = new StringBuilder();
        for (int i = 5; i < args.length; i++)
            builder.append(args[i]).append(" ");
        var message = builder.toString().trim();
        message = message.replace("\"", "");

        switch (args[3]) {
            case "chat" -> sendGroupEdit(GroupColorType.CHAT, source, groupId, creatorId, message, args);
            case "tab" -> sendGroupEdit(GroupColorType.TAB, source, groupId, creatorId, message, args);
            case "tag" -> sendGroupEdit(GroupColorType.TAG, source, groupId, creatorId, message, args);
            default -> syntax(source);
        }
    }

    private void users(CommandSource source, int creatorId, String[] args) throws SQLException {
        var username = args[1];
        if (!user.existsUser(username)) {
            sendSourceMessage(source, "§cUser does not exist.");
            return;
        }

        var userId = user.getId(username);
        switch (args[2]) {
            case "parent" -> userParent(source, userId, creatorId, args);
            case "permission" -> userPermission(source, userId, creatorId, args);
            default -> syntax(source);
        }
    }

    private void userParent(CommandSource source, int userId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            sendSourceMessage(source, "§3Parents: ");
            for (var userIds : userParent.getParentIds(userId))
                sendSourceMessage(source, "§7- §e%s", group.getName(userIds));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String parentName;
        int parentId;
        switch (args[3]) {
            case "add" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                if (args.length == 5) {
                    userParent.addParent(userId, creatorId, parentId, -1);
                    sendSourceMessage(source, "§3Parent §e%s §3is now added.", parentName);
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                userParent.addParent(userId, creatorId, parentId, time);
                sendSourceMessage(source, "§3Parent §e%s §3is now added for §e%s", parentName, userParent.getExpiredDate(userId, parentId));
            }
            case "remove" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                userParent.removeParent(userId, parentId);
                sendSourceMessage(source, "§3Parent §e%s §3is now removed.", parentName);
            }
            case "clear" -> {
                userParent.clearParent(userId);
                sendSourceMessage(source, "§3All parents are now cleared.");
            }
            case "creator" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                var creator = userParent.getCreatorId(userId, parentId);
                sendCreatorMessage(source, creator);
            }
            case "created" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                sendSourceMessage(source, "§3Created: §e%s", userParent.getCreatedDate(userId, parentId));
            }
            case "expired" -> {
                parentName = args[4];
                parentId = group.getUniqueId(parentName);
                if (args.length == 5) {
                    sendSourceMessage(source, "§3Expired: §e%s", userParent.getExpiredDate(userId, parentId));
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                switch (args[6]) {
                    case "set" -> {
                        userParent.setExpiredTime(userId, parentId, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e", parentName, userParent.getExpiredDate(userId, parentId));
                    }
                    case "add" -> {
                        userParent.addExpiredTime(userId, parentId, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, userParent.getExpiredDate(userId, parentId));
                    }
                    case "remove" -> {
                        userParent.removeExpiredTime(userId, parentId, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, userParent.getExpiredDate(userId, parentId));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private void userPermission(CommandSource source, int userId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            sendSourceMessage(source, "§3Permissions: ");
            for (var all : userPermission.getPermissions(userId))
                sendSourceMessage(source, "§7- §e%s", all);
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String permission;
        switch (args[3]) {
            case "all" -> {
                sendSourceMessage(source, "§3Alle permissions: ");
                for (var all : this.permission.getPermissions(userId))
                    sendSourceMessage(source, "§7- §e%s", all);
            }
            case "add" -> {
                permission = args[4];
                if (args.length == 5) {
                    userPermission.addPermission(userId, creatorId, permission, -1);
                    sendSourceMessage(source, "§3Permission §e%s §3is now added.", permission);
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                userPermission.addPermission(userId, creatorId, permission, time);
                sendSourceMessage(source, "§3Permission §e%s §3is now added for §e%s", permission, userPermission.getExpiredDate(userId, permission));
            }
            case "remove" -> {
                permission = args[4];
                userPermission.removePermission(userId, permission);
                sendSourceMessage(source, "§3Permission §e%s §3is now removed.", permission);
            }
            case "clear" -> {
                userPermission.clearPermission(userId);
                sendSourceMessage(source, "§3All permissions are now cleared.");
            }
            case "creator" -> {
                permission = args[4];
                var creator = userPermission.getCreatorId(userId, permission);
                sendCreatorMessage(source, creator);
            }
            case "created" -> {
                permission = args[4];
                sendSourceMessage(source, "§3Created: §e%s", userPermission.getCreatedDate(userId, permission));
            }
            case "expired" -> {
                permission = args[4];
                if (args.length == 5) {
                    sendSourceMessage(source, "§3Expired: §e%s", userPermission.getExpiredDate(userId, permission));
                    break;
                }
                var time = TimeUtil.formatTime(args[5]);
                if (time == -2) {
                    sendSourceMessage(source, "§cNo negative value allowed");
                    break;
                }
                if (time == -3) {
                    sendSourceMessage(source, "§cInvalid time format");
                    break;
                }
                switch (args[6]) {
                    case "set" -> {
                        userPermission.setExpiredTime(userId, permission, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, userPermission.getExpiredDate(userId, permission));
                    }
                    case "add" -> {
                        userPermission.addExpiredTime(userId, permission, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, userPermission.getExpiredDate(userId, permission));
                    }
                    case "remove" -> {
                        userPermission.removeExpiredTime(userId, permission, time);
                        sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, userPermission.getExpiredDate(userId, permission));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private void sendGroupEdit(GroupColorType type, CommandSource source, int groupId, int creatorId, String message, String[] args) throws SQLException {
        var creator = groupColorSettings.getCreatorId(groupId) == -2 ? creatorId : groupColorSettings.getCreatorId(groupId);
        switch (args[4]) {
            case "prefix" -> {
                if (args.length == 5) {
                    sendCreatorMessage(source, creator);
                    sendSourceMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendSourceMessage(source, "§3Prefix: §e%s", groupColorSettings.getPrefix(type, groupId));
                    break;
                }
                groupColorSettings.setPrefix(type, groupId, creator, message);
                sendSourceMessage(source, "§3Prefix is now §e%s", message);
            }
            case "suffix" -> {
                if (args.length == 5) {
                    sendCreatorMessage(source, creator);
                    sendSourceMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendSourceMessage(source, "§3Suffix: §e%s", groupColorSettings.getSuffix(type, groupId));
                    break;
                }
                groupColorSettings.setSuffix(type, groupId, creator, message);
                sendSourceMessage(source, "§3Suffix is now §e%s", message);
            }
            case "color" -> {
                if (args.length == 5) {
                    sendCreatorMessage(source, creator);
                    sendSourceMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendSourceMessage(source, "§3Color: §e%s", groupColorSettings.getColor(type, groupId));
                    break;
                }
                groupColorSettings.setColor(type, groupId, creator, message);
                sendSourceMessage(source, "§3Color is now §e%s", message);
            }
            default -> syntax(source);
        }
    }

    private void sendCreatorMessage(CommandSource source, int creatorId) throws SQLException {
        sendSourceMessage(source, "§3Creator: ");
        sendSourceMessage(source, "§7- §3ID: §e%s", creatorId);
        sendSourceMessage(source, "§7- §3UUID: §e%s", user.getUniqueId(creatorId));
        sendSourceMessage(source, "§7- §3Name: §e%s", user.getUsername(creatorId));
    }

    private void syntax(CommandSource source) {
        sendSourceMessage(source, """
                - /permission group §9<group>§r create <sort> <team>
                - /permission group §9<group>§r delete
                - /permission group §9<group>§r rename <newName>
                - /permission group §9<group>§6 parent§r
                - /permission group §9<group>§6 parent§r add <parent> [duration]
                - /permission group §9<group>§6 parent§r remove <parent>
                - /permission group §9<group>§6 parent§r clear
                - /permission group §9<group>§6 parent§r creator <parent>
                - /permission group §9<group>§6 parent§r created <parent>
                - /permission group §9<group>§6 parent§r expired <parent>
                - /permission group §9<group>§6 parent§r expired <parent> <duration> <set|add|remove>
                - /permission group §9<group>§e permission§r
                - /permission group §9<group>§e permission§r all
                - /permission group §9<group>§e permission§r add <permission> [duration]
                - /permission group §9<group>§e permission§r remove <permission>
                - /permission group §9<group>§e permission§r clear
                - /permission group §9<group>§e permission§r creator <permission>
                - /permission group §9<group>§e permission§r created <permission>
                - /permission group §9<group>§e permission§r expired <permission>
                - /permission group §9<group>§e permission§r expired <permission> <duration> <set|add|remove>
                - /permission group §9<group>§c edit§r chat <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tab <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tag <prefix|suffix|color> <value>
                - /permission groups
                - /permission user §b<user>§6 parent§r
                - /permission user §b<user>§6 parent§r add <parent> [duration]
                - /permission user §b<user>§6 parent§r remove <parent>
                - /permission user §b<user>§6 parent§r clear
                - /permission user §b<user>§6 parent§r creator <parent>
                - /permission user §b<user>§6 parent§r created <parent>
                - /permission user §b<user>§6 parent§r expired <parent>
                - /permission user §b<user>§6 parent§r expired <parent> <duration> <set|add|remove>
                - /permission user §b<user>§e permission§r
                - /permission user §b<user>§e permission§r all
                - /permission user §b<user>§e permission§r add <permission> [duration]
                - /permission user §b<user>§e permission§r remove <permission>
                - /permission user §b<user>§e permission§r clear
                - /permission user §b<user>§e permission§r creator <permission>
                - /permission user §b<user>§e permission§r created <permission>
                - /permission user §b<user>§e permission§r expired <permission>
                - /permission user §b<user>§e permission§r expired <permission> <duration> <set|add|remove>
                - /permission users
                """);
    }
}
