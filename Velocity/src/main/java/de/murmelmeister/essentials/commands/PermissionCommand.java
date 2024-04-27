package de.murmelmeister.essentials.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.settings.GroupColorSettings;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.utils.StringUtil;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PermissionCommand implements SimpleCommand {
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
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        Player player = source instanceof Player ? (Player) source : null;
        UUID playerId = player != null ? player.getUniqueId() : null;

        if (!source.hasPermission("murmelessentials.command.permission")) {
            source.sendMessage(Component.text("§cYou do not have permission to use this command."));
            return;
        }

        try {
            if (args.length == 1) {
                switch (args[0]) {
                    case "groups" -> {
                        source.sendMessage(Component.text("§3Groups: "));
                        for (String name : group.getNames())
                            source.sendMessage(Component.text("§7- §e" + name));
                    }
                    case "users" -> {
                        source.sendMessage(Component.text("§3Users: "));
                        for (String name : user.getUsernames())
                            source.sendMessage(Component.text("§7- §e" + name));
                    }
                    default -> syntax(source);
                }
                return;
            }

            int creatorId = playerId == null ? -1 : user.getId(playerId);
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
        String[] args = invocation.arguments();
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
        String groupName = args[1];
        if (!group.existsGroup(groupName)) {
            if (args[2].equals("create")) {
                try {
                    group.createNewGroup(groupName, creatorId, Integer.parseInt(args[3]));
                    source.sendMessage(Component.text("§3Group §e" + groupName + " §3is now created."));
                } catch (NumberFormatException e) {
                    source.sendMessage(Component.text("§cInvalid group weight."));
                }
            } else source.sendMessage(Component.text("§cGroup does not exist."));
            return;
        }

        int groupId = group.getUniqueId(groupName);
        switch (args[2]) {
            case "delete" -> group.deleteGroup(groupId);
            case "rename" -> {
                if (args.length == 3) {
                    syntax(source);
                    return;
                }
                group.rename(groupId, args[3]);
                source.sendMessage(Component.text("§3Group is now renamed to §e" + args[3]));
            }
            case "parent" -> groupParent(source, groupId, creatorId, args);
            case "permission" -> groupPermission(source, groupId, creatorId, args);
            case "edit" -> groupEdit(source, groupId, creatorId, args);
            default -> syntax(source);
        }
    }

    private void groupParent(CommandSource source, int groupId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            source.sendMessage(Component.text("§3Parents: "));
            for (int groupIds : groupParent.getParentIds(groupId))
                source.sendMessage(Component.text("§7- §e" + group.getName(groupIds)));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String parentName = args[4];
        int parentId = group.getUniqueId(parentName);
        switch (args[3]) {
            case "add" -> {
                if (args.length == 5) {
                    groupParent.addParent(groupId, creatorId, parentId, -1);
                    source.sendMessage(Component.text("§3Parent §e" + parentName + " §3is now added."));
                    break;
                }
                groupParent.addParent(groupId, creatorId, parentId, formatTime(source, args[5]));
                source.sendMessage(Component.text("§3Parent §e" + parentName + " §3is now added for §e" + groupParent.getExpiredDate(groupId, parentId)));
            }
            case "remove" -> {
                groupParent.removeParent(groupId, parentId);
                source.sendMessage(Component.text("§3Parent §e" + parentName + " §3is now removed."));
            }
            case "clear" -> {
                groupParent.clearParent(groupId);
                source.sendMessage(Component.text("§3All parents are now cleared."));
            }
            case "creator" -> {
                int creator = groupParent.getCreatorId(groupId, parentId);
                UUID creatorUUID = user.getUniqueId(creator);
                String creatorName = user.getUsername(creator);
                source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                source.sendMessage(Component.text("§3CreatorUUID: §e" + creatorUUID));
                source.sendMessage(Component.text("§3Creator: §e" + creatorName));
            }
            case "created" ->
                    source.sendMessage(Component.text("§3Created: §e" + groupParent.getExpiredDate(groupId, parentId)));
            case "expired" -> {
                if (args.length == 5) {
                    source.sendMessage(Component.text("§3Expired: §e" + groupParent.getExpiredDate(groupId, parentId)));
                    break;
                }
                long time = formatTime(source, args[5]);
                switch (args[6]) {
                    case "set" -> {
                        groupParent.setExpiredTime(groupId, parentId, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + parentName + " §3is now §e" + groupParent.getExpiredDate(groupId, parentId)));
                    }
                    case "add" -> {
                        groupParent.addExpiredTime(groupId, parentId, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + parentName + " §3is now §e" + groupParent.getExpiredDate(groupId, parentId)));
                    }
                    case "remove" -> {
                        groupParent.removeExpiredTime(groupId, parentId, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + parentName + " §3is now §e" + groupParent.getExpiredDate(groupId, parentId)));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private void groupPermission(CommandSource source, int groupId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            source.sendMessage(Component.text("§3Permissions: "));
            for (String all : groupPermission.getPermissions(groupId))
                source.sendMessage(Component.text("§7- §e" + all));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String permission = args[4];
        switch (args[3]) {
            case "all" -> {
                source.sendMessage(Component.text("§3All permissions: "));
                for (String all : groupPermission.getAllPermissions(groupParent, groupId))
                    source.sendMessage(Component.text("§7- §e" + all));
            }
            case "add" -> {
                if (args.length == 5) {
                    groupPermission.addPermission(groupId, creatorId, permission, -1);
                    source.sendMessage(Component.text("§3Permission §e" + permission + " §3is now added."));
                    break;
                }
                groupPermission.addPermission(groupId, creatorId, permission, formatTime(source, args[5]));
                source.sendMessage(Component.text("§3Permission §e" + permission + " §3is now added for §e" + groupPermission.getExpiredDate(groupId, permission)));
            }
            case "remove" -> {
                groupPermission.removePermission(groupId, permission);
                source.sendMessage(Component.text("§3Permission §e" + permission + " §3is now removed."));
            }
            case "clear" -> {
                groupPermission.clearPermission(groupId);
                source.sendMessage(Component.text("§3All permissions are now cleared."));
            }
            case "creator" -> {
                int creator = groupPermission.getCreatorId(groupId, permission);
                UUID creatorUUID = user.getUniqueId(creator);
                String creatorName = user.getUsername(creator);
                source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                source.sendMessage(Component.text("§3CreatorUUID: §e" + creatorUUID));
                source.sendMessage(Component.text("§3Creator: §e" + creatorName));
            }
            case "created" ->
                    source.sendMessage(Component.text("§3Created: §e" + groupPermission.getCreatedDate(groupId, permission)));
            case "expired" -> {
                if (args.length == 5) {
                    source.sendMessage(Component.text("§3Expired: §e" + groupPermission.getExpiredDate(groupId, permission)));
                    break;
                }
                long time = formatTime(source, args[5]);
                switch (args[6]) {
                    case "set" -> {
                        groupPermission.setExpiredTime(groupId, permission, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + permission + " §3is now §e" + groupPermission.getExpiredDate(groupId, permission)));
                    }
                    case "add" -> {
                        groupPermission.addExpiredTime(groupId, permission, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + permission + " §3is now §e" + groupPermission.getExpiredDate(groupId, permission)));
                    }
                    case "remove" -> {
                        groupPermission.removeExpiredTime(groupId, permission, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + permission + " §3is now §e" + groupPermission.getExpiredDate(groupId, permission)));
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
        StringBuilder builder = new StringBuilder();
        for (int i = 5; i < args.length; i++)
            builder.append(args[i]).append(" ");
        String message = builder.toString().trim();
        message = message.replace("\"", "").replace("#", "");

        int creator = groupColorSettings.getCreatorId(groupId);
        switch (args[3]) {
            case "chat" -> {
                switch (args[4]) {
                    case "prefix" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Prefix: §e" + groupColorSettings.getChatPrefix(groupId)));
                            break;
                        }
                        groupColorSettings.setChatPrefix(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Prefix is now §e" + message));
                    }
                    case "suffix" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Suffix: §e" + groupColorSettings.getChatSuffix(groupId)));
                            break;
                        }
                        groupColorSettings.setChatSuffix(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Suffix is now §e" + message));
                    }
                    case "color" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Color: §e" + groupColorSettings.getChatColor(groupId)));
                            break;
                        }
                        groupColorSettings.setChatColor(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Color is now §e" + message));
                    }
                    default -> syntax(source);
                }
            }
            case "tab" -> {
                switch (args[4]) {
                    case "prefix" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Prefix: §e" + groupColorSettings.getTabPrefix(groupId)));
                            break;
                        }
                        groupColorSettings.setTabPrefix(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Prefix is now §e" + message));
                    }
                    case "suffix" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Suffix: §e" + groupColorSettings.getTabSuffix(groupId)));
                            break;
                        }
                        groupColorSettings.setTabSuffix(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Suffix is now §e" + message));
                    }
                    case "color" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Color: §e" + groupColorSettings.getTabColor(groupId)));
                            break;
                        }
                        groupColorSettings.setTabColor(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Color is now §e" + message));
                    }
                    default -> syntax(source);
                }
            }
            case "tag" -> {
                switch (args[4]) {
                    case "prefix" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Prefix: §e" + groupColorSettings.getTagPrefix(groupId)));
                            break;
                        }
                        groupColorSettings.setTagPrefix(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Prefix is now §e" + message));
                    }
                    case "suffix" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Suffix: §e" + groupColorSettings.getTagSuffix(groupId)));
                            break;
                        }
                        groupColorSettings.setTagSuffix(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Suffix is now §e" + message));
                    }
                    case "color" -> {
                        if (args.length == 5) {
                            source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                            source.sendMessage(Component.text("§3CreatorUUID: §e" + user.getUniqueId(creator)));
                            source.sendMessage(Component.text("§3Creator: §e" + user.getUsername(creator)));
                            source.sendMessage(Component.text("§3EditedTime: §e" + groupColorSettings.getEditedDate(groupId)));
                            source.sendMessage(Component.text("§3Color: §e" + groupColorSettings.getTagColor(groupId)));
                            break;
                        }
                        groupColorSettings.setTagColor(groupId, creatorId, message);
                        source.sendMessage(Component.text("§3Color is now §e" + message));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private void users(CommandSource source, int creatorId, String[] args) throws SQLException {
        String username = args[1];
        if (!user.existsUser(username)) {
            source.sendMessage(Component.text("§cUser does not exist."));
            return;
        }

        int userId = user.getId(username);
        switch (args[2]) {
            case "parent" -> userParent(source, userId, creatorId, args);
            case "permission" -> userPermission(source, userId, creatorId, args);
            default -> syntax(source);
        }
    }

    private void userParent(CommandSource source, int userId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            source.sendMessage(Component.text("§3Parents: "));
            for (int userIds : userParent.getParentIds(userId))
                source.sendMessage(Component.text("§7- §e" + group.getName(userIds)));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String parentName = args[4];
        int parentId = group.getUniqueId(parentName);
        switch (args[3]) {
            case "add" -> {
                if (args.length == 5) {
                    userParent.addParent(userId, creatorId, parentId, -1);
                    source.sendMessage(Component.text("§3Parent §e" + parentName + " §3is now added."));
                    break;
                }
                userParent.addParent(userId, creatorId, parentId, formatTime(source, args[5]));
                source.sendMessage(Component.text("§3Parent §e" + parentName + " §3is now added for §e" + userParent.getExpiredDate(userId, parentId)));
            }
            case "remove" -> {
                userParent.removeParent(userId, parentId);
                source.sendMessage(Component.text("§3Parent §e" + parentName + " §3is now removed."));
            }
            case "clear" -> {
                userParent.clearParent(userId);
                source.sendMessage(Component.text("§3All parents are now cleared."));
            }
            case "creator" -> {
                int creator = userParent.getCreatorId(userId, parentId);
                UUID creatorUUID = user.getUniqueId(creator);
                String creatorName = user.getUsername(creator);
                source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                source.sendMessage(Component.text("§3CreatorUUID: §e" + creatorUUID));
                source.sendMessage(Component.text("§3Creator: §e" + creatorName));
            }
            case "created" ->
                    source.sendMessage(Component.text("§3Created: §e" + userParent.getCreatedDate(userId, parentId)));
            case "expired" -> {
                if (args.length == 5) {
                    source.sendMessage(Component.text("§3Expired: §e" + userParent.getExpiredDate(userId, parentId)));
                    break;
                }
                long time = formatTime(source, args[5]);
                switch (args[6]) {
                    case "set" -> {
                        userParent.setExpiredTime(userId, parentId, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + parentName + " §3is now §e" + userParent.getExpiredDate(userId, parentId)));
                    }
                    case "add" -> {
                        userParent.addExpiredTime(userId, parentId, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + parentName + " §3is now §e" + userParent.getExpiredDate(userId, parentId)));
                    }
                    case "remove" -> {
                        userParent.removeExpiredTime(userId, parentId, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + parentName + " §3is now §e" + userParent.getExpiredDate(userId, parentId)));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private void userPermission(CommandSource source, int userId, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            source.sendMessage(Component.text("§3Permissions: "));
            for (String all : userPermission.getPermissions(userId))
                source.sendMessage(Component.text("§7- §e" + all));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("creator") || args[3].equals("created") || args[3].equals("expired"))) {
            syntax(source);
            return;
        }

        String permission = args[4];
        switch (args[3]) {
            case "all" -> {
                source.sendMessage(Component.text("§3Alle permissions: "));
                for (String all : this.permission.getPermissions(userId))
                    source.sendMessage(Component.text("§7- §e" + all));
            }
            case "add" -> {
                if (args.length == 5) {
                    userPermission.addPermission(userId, creatorId, permission, -1);
                    source.sendMessage(Component.text("§3Permission §e" + permission + " §3is now added."));
                    break;
                }
                userPermission.addPermission(userId, creatorId, permission, formatTime(source, args[5]));
                source.sendMessage(Component.text("§3Permission §e" + permission + " §3is now added for §e" + userPermission.getExpiredDate(userId, permission)));
            }
            case "remove" -> {
                userPermission.removePermission(userId, permission);
                source.sendMessage(Component.text("§3Permission §e" + permission + " §3is now removed."));
            }
            case "clear" -> {
                userPermission.clearPermission(userId);
                source.sendMessage(Component.text("§3All permissions are now cleared."));
            }
            case "creator" -> {
                int creator = userPermission.getCreatorId(userId, permission);
                UUID creatorUUID = user.getUniqueId(creator);
                String creatorName = user.getUsername(creator);
                source.sendMessage(Component.text("§3CreatorID: §e" + creator));
                source.sendMessage(Component.text("§3CreatorUUID: §e" + creatorUUID));
                source.sendMessage(Component.text("§3Creator: §e" + creatorName));
            }
            case "created" ->
                    source.sendMessage(Component.text("§3Created: §e" + userPermission.getCreatedDate(userId, permission)));
            case "expired" -> {
                if (args.length == 5) {
                    source.sendMessage(Component.text("§3Expired: §e" + userPermission.getExpiredDate(userId, permission)));
                    break;
                }
                long time = formatTime(source, args[5]);
                switch (args[6]) {
                    case "set" -> {
                        userPermission.setExpiredTime(userId, permission, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + permission + " §3is now §e" + userPermission.getExpiredDate(userId, permission)));
                    }
                    case "add" -> {
                        userPermission.addExpiredTime(userId, permission, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + permission + " §3is now §e" + userPermission.getExpiredDate(userId, permission)));
                    }
                    case "remove" -> {
                        userPermission.removeExpiredTime(userId, permission, time);
                        source.sendMessage(Component.text("§3Expired time for §e" + permission + " §3is now §e" + userPermission.getExpiredDate(userId, permission)));
                    }
                    default -> syntax(source);
                }
            }
            default -> syntax(source);
        }
    }

    private long formatTime(CommandSource source, String args) {
        try {
            String format = args.substring(args.length() - 1);
            long duration = Long.parseLong(args.substring(0, args.length() - 1));
            long time = 0L;

            switch (format) {
                case "s" -> time = duration * 1000L;
                case "m" -> time = duration * 1000L * 60L;
                case "h" -> time = duration * 1000L * 60L * 60L;
                case "d" -> time = duration * 1000L * 60L * 60L * 24L;
                case "w" -> time = duration * 1000L * 60L * 60L * 24L * 7L;
                case "M" -> time = duration * 1000L * 60L * 60L * 24L * 30L;
                case "y" -> time = duration * 1000L * 60L * 60L * 24L * 365L;
                default -> source.sendMessage(Component.text("§cWrong valid format."));
            }
            return time;
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    private void syntax(CommandSource source) {
        source.sendMessage(Component.text("""
                - /permission group §9<group>§r create <sort>
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
                """));
    }
}
