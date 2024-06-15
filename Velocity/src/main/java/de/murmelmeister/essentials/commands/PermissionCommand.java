package de.murmelmeister.essentials.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.murmelmeister.essentials.commands.subcomamnd.SubGroupEdit;
import de.murmelmeister.essentials.commands.subcomamnd.SubParent;
import de.murmelmeister.essentials.commands.subcomamnd.SubPermission;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.utils.PermissionSyntaxUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.settings.GroupColorSettings;
import de.murmelmeister.murmelapi.group.settings.GroupSettings;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.utils.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PermissionCommand extends CommandManager {
    private final Group group;
    private final User user;

    private final GroupSettings groupSettings;
    private final GroupParent groupParent;
    private final GroupPermission groupPermission;
    private final UserParent userParent;
    private final UserPermission userPermission;

    private final SubGroupEdit subGroupEdit;
    private final SubParent subParent;
    private final SubPermission subPermission;

    public PermissionCommand(Permission permission, Group group, User user) {
        this.group = group;
        this.user = user;
        this.groupSettings = group.getSettings();
        GroupColorSettings groupColorSettings = group.getColorSettings();
        this.groupParent = group.getParent();
        this.groupPermission = group.getPermission();
        this.userParent = user.getParent();
        this.userPermission = user.getPermission();
        this.subGroupEdit = new SubGroupEdit(this, group, user, groupSettings, groupColorSettings);
        this.subParent = new SubParent(this, group, user, groupParent, userParent);
        this.subPermission = new SubPermission(this, permission, user, groupParent, groupPermission, userPermission);
    }

    @Override
    public void execute(Invocation invocation) {
        var args = invocation.arguments();
        var source = invocation.source();

        if (!source.hasPermission("murmelessentials.command.permission")) {
            sendSourceMessage(source, "§cYou do not have permission to use this command.");
            return;
        }

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
                    default -> PermissionSyntaxUtil.syntax(source);
                }
                return;
            }

            var player = source instanceof Player ? (Player) source : null;
            var playerId = player != null ? player.getUniqueId() : null;
            var creatorId = playerId == null ? -1 : user.getId(playerId);
            if (args.length >= 3) {
                switch (args[0]) {
                    case "group" -> groups(source, creatorId, args);
                    case "user" -> users(source, creatorId, args);
                    default -> PermissionSyntaxUtil.syntax(source);
                }
            } else PermissionSyntaxUtil.syntax(source);
        } catch (IllegalArgumentException e) {
            sendSourceMessage(source, "§cError: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            var args = invocation.arguments();
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
                return Stream.of("add", "remove", "clear", "info", "time").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && (args[0].equals("group") || args[0].equals("user")) && args[2].equals("permission")) // Show all group/user permission commands
                return Stream.of("all", "add", "remove", "clear", "info", "time").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && args[0].equals("group") && args[2].equals("edit")) // Show all group edit commands
                return Stream.of("chat", "tab", "tag", "sort", "team").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("edit") && (args[3].equals("chat") || args[3].equals("tab") || args[3].equals("tag"))) // Show all group edit subcommands
                return Stream.of("prefix", "suffix", "color").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("parent") && args[3].equals("add")) // Add group parent
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("parent") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired group parent
                return groupParent.getParentNames(group, group.getUniqueId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("permission") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired group permission
                return groupPermission.getPermissions(group.getUniqueId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("parent") && args[3].equals("add")) // Add user parent
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("parent") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired user parent
                return userParent.getParentNames(group, user.getId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("permission") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired user permission
                return userPermission.getPermissions(user.getId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 7 && (args[0].equals("group") || args[0].equals("user")) && (args[2].equals("parent") || args[2].equals("permission")) && args[3].equals("time")) // Set/Remove/Expired time
                return Stream.of("set", "add", "remove").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            return Collections.emptyList();
        });
    }

    private void groups(CommandSource source, int creatorId, String[] args) {
        var groupName = args[1];
        if (!group.existsGroup(groupName)) {
            if (args[2].equals("create")) createGroup(source, groupName, creatorId, args);
            else sendSourceMessage(source, "§cGroup does not exist.");
            return;
        }

        var groupId = group.getUniqueId(groupName);
        switch (args[2]) {
            case "create" -> sendSourceMessage(source, "§cGroup already exists.");
            case "delete" -> deleteGroup(source, groupId, groupName);
            case "rename" -> renameGroup(source, groupId, args);
            case "parent" -> subParent.parent(source, false, groupId, creatorId, args);
            case "permission" -> subPermission.permission(source, false, groupId, creatorId, args);
            case "edit" -> subGroupEdit.groupEdit(source, groupId, creatorId, args);
            default -> PermissionSyntaxUtil.syntax(source, false);
        }
    }

    private void users(CommandSource source, int creatorId, String[] args) {
        var username = args[1];
        if (isUserNotExist(source, user, username)) return;

        var userId = user.getId(username);
        switch (args[2]) {
            case "parent" -> subParent.parent(source, true, userId, creatorId, args);
            case "permission" -> subPermission.permission(source, true, userId, creatorId, args);
            default -> PermissionSyntaxUtil.syntax(source, true);
        }
    }

    private void createGroup(CommandSource source, String groupName, int creatorId, String[] args) {
        if (args.length == 3) {
            PermissionSyntaxUtil.syntax(source, false);
            return;
        }
        try {
            Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sendSourceMessage(source, "§cInvalid sort id");
            return;
        }
        if (args.length == 4) {
            PermissionSyntaxUtil.syntax(source, false);
            return;
        }
        try {
            Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sendSourceMessage(source, "§cInvalid team id");
            return;
        }
        group.createNewGroup(groupName, creatorId, Integer.parseInt(args[3]), args[4]);
        sendSourceMessage(source, "§3Group §e%s §3is now created.", groupName);
    }

    private void deleteGroup(CommandSource source, int groupId, String groupName) {
        if (groupId == group.getDefaultGroup()) {
            sendSourceMessage(source, "§cYou can not delete the default group.");
            return;
        }
        group.deleteGroup(groupId);
        sendSourceMessage(source, "§3Group §e%s §3is now deleted.", groupName);
    }

    private void renameGroup(CommandSource source, int groupId, String[] args) {
        if (args.length == 3) {
            PermissionSyntaxUtil.syntax(source, false);
            return;
        }
        var oldName = group.getName(groupId);
        var newName = args[3];
        var teamId = groupSettings.getTeamId(groupId);
        group.rename(groupId, newName);
        groupSettings.setTeamId(groupId, teamId.replace(oldName, newName));
        sendSourceMessage(source, "§3Group is now renamed to §e%s", args[3]);
    }
}
