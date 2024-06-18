package de.murmelmeister.essentials.commands.subcomamnd;

import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.utils.PermissionSyntaxUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.settings.GroupColorSettings;
import de.murmelmeister.murmelapi.group.settings.GroupColorType;
import de.murmelmeister.murmelapi.group.settings.GroupSettings;
import de.murmelmeister.murmelapi.user.User;

import static de.murmelmeister.essentials.manager.CommandManager.sendSourceMessage;

public final class SubGroupEdit {
    private final CommandManager commandManager;
    private final Group group;
    private final User user;
    private final GroupSettings groupSettings;
    private final GroupColorSettings groupColorSettings;

    public SubGroupEdit(CommandManager commandManager, Group group, User user, GroupSettings groupSettings, GroupColorSettings groupColorSettings) {
        this.commandManager = commandManager;
        this.group = group;
        this.user = user;
        this.groupSettings = groupSettings;
        this.groupColorSettings = groupColorSettings;
    }

    public void groupEdit(CommandSource source, int groupId, int creatorId, String[] args) {
        if (args.length == 3) {
            PermissionSyntaxUtil.syntax(source, false);
            return;
        }
        var builder = new StringBuilder();
        for (int i = 5; i < args.length; i++)
            builder.append(args[i]).append(" ");
        var message = builder.toString().trim();
        message = message.replace("\"", "");

        switch (args[3]) {
            case "chat" -> groupColor(GroupColorType.CHAT, source, groupId, creatorId, message, args);
            case "tab" -> groupColor(GroupColorType.TAB, source, groupId, creatorId, message, args);
            case "tag" -> groupColor(GroupColorType.TAG, source, groupId, creatorId, message, args);
            case "sort" -> sortId(source, groupId, args);
            case "team" -> teamId(source, groupId, args);
            default -> PermissionSyntaxUtil.syntaxGroupEdit(source);
        }
    }

    private void groupColor(GroupColorType type, CommandSource source, int groupId, int creatorId, String message, String[] args) {
        var creator = groupColorSettings.getCreatorId(groupId) == -2 ? creatorId : groupColorSettings.getCreatorId(groupId);
        if (args.length == 4) {
            PermissionSyntaxUtil.syntaxGroupEdit(source);
            return;
        }
        switch (args[4]) {
            case "prefix" -> {
                if (args.length == 5) {
                    commandManager.sendCreatorMessage(source, user, creator);
                    sendSourceMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendSourceMessage(source, "§3Prefix: §e%s", groupColorSettings.getPrefix(type, groupId));
                    break;
                }
                groupColorSettings.setPrefix(type, groupId, creator, message);
                sendSourceMessage(source, "§3Prefix is now §e%s", message);
            }
            case "suffix" -> {
                if (args.length == 5) {
                    commandManager.sendCreatorMessage(source, user, creator);
                    sendSourceMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendSourceMessage(source, "§3Suffix: §e%s", groupColorSettings.getSuffix(type, groupId));
                    break;
                }
                groupColorSettings.setSuffix(type, groupId, creator, message);
                sendSourceMessage(source, "§3Suffix is now §e%s", message);
            }
            case "color" -> {
                if (args.length == 5) {
                    commandManager.sendCreatorMessage(source, user, creator);
                    sendSourceMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendSourceMessage(source, "§3Color: §e%s", groupColorSettings.getColor(type, groupId));
                    break;
                }
                groupColorSettings.setColor(type, groupId, creator, message);
                sendSourceMessage(source, "§3Color is now §e%s", message);
            }
            default -> PermissionSyntaxUtil.syntaxGroupEdit(source);
        }
    }

    private void sortId(CommandSource source, int groupId, String[] args) {
        if (args.length == 4) {
            sendSourceMessage(source, "§3SortID: §e%s", groupSettings.getSortId(groupId));
            return;
        }
        try {
            groupSettings.setSortId(groupId, Integer.parseInt(args[4]));
            sendSourceMessage(source, "§3Sort id is now set to §e%s", args[4]);
        } catch (NumberFormatException e) {
            sendSourceMessage(source, "§cInvalid sort id");
        }
    }

    private void teamId(CommandSource source, int groupId, String[] args) {
        if (args.length == 4) {
            sendSourceMessage(source, "§3TeamID: §e%s", groupSettings.getTeamId(groupId));
            return;
        }
        try {
            var id = Integer.parseInt(args[4]);
            var teamId = id + group.getName(groupId);
            groupSettings.setTeamId(groupId, teamId);
            sendSourceMessage(source, "§3Team id is now set to §e%s", teamId);
        } catch (NumberFormatException e) {
            sendSourceMessage(source, "§cInvalid team id");
        }
    }
}
