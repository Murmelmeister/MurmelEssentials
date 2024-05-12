package de.murmelmeister.essentials.commands.subcomamnd;

import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.utils.PermissionSyntaxUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.utils.TimeUtil;

import java.sql.SQLException;

import static de.murmelmeister.essentials.manager.CommandManager.sendSourceMessage;

public final class SubParent {
    private final CommandManager commandManager;
    private final Group group;
    private final User user;
    private final GroupParent groupParent;
    private final UserParent userParent;

    public SubParent(CommandManager commandManager, Group group, User user, GroupParent groupParent, UserParent userParent) {
        this.commandManager = commandManager;
        this.group = group;
        this.user = user;
        this.groupParent = groupParent;
        this.userParent = userParent;
    }

    public void parent(CommandSource source, boolean isUser, int id, int creatorId, String[] args) throws SQLException {
        if (args.length == 3) {
            sendSourceMessage(source, "§3Parents: ");
            var parents = isUser ? userParent.getParentIds(id) : groupParent.getParentIds(id);
            for (var groupIds : parents)
                sendSourceMessage(source, "§7- §e%s", group.getName(groupIds));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) {
            PermissionSyntaxUtil.syntaxParent(source, isUser);
            return;
        }

        switch (args[3]) {
            case "add" -> addParent(source, isUser, id, creatorId, args);
            case "remove" -> removeParent(source, isUser, id, args);
            case "clear" -> clearParent(source, isUser, id);
            case "info" -> infoParent(source, isUser, id, args);
            case "time" -> timeParent(source, isUser, id, args);
            default -> PermissionSyntaxUtil.syntaxParent(source, isUser);
        }
    }

    private void addParent(CommandSource source, boolean isUser, int id, int creatorId, String[] args) throws SQLException {
        var parentName = args[4];
        if (isGroupNotExist(source, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (args.length == 5) {
            if (isUser) userParent.addParent(id, creatorId, parentId, -1);
            else groupParent.addParent(id, creatorId, parentId, -1);
            sendSourceMessage(source, "§3Parent §e%s §3is now added.", parentName);
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
        if (isUser) userParent.addParent(id, creatorId, parentId, time);
        else groupParent.addParent(id, creatorId, parentId, time);
        sendSourceMessage(source, "§3Parent §e%s §3is now added for §e%s", parentName, getParentExpiredDate(isUser, id, parentId));
    }

    private void removeParent(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        if (args.length < 5) {
            PermissionSyntaxUtil.syntaxParent(source, isUser);
            return;
        }
        var parentName = args[4];
        if (isGroupNotExist(source, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (isParentNotExist(source, isUser, id, parentId)) return;

        if (isUser) {
            if (parentId == group.getDefaultGroup()) {
                sendSourceMessage(source, "§cYou can't remove the default group.");
                return;
            }
            userParent.removeParent(id, parentId);
        } else groupParent.removeParent(id, parentId);
        sendSourceMessage(source, "§3Parent §e%s §3is now removed.", parentName);
    }

    private void clearParent(CommandSource source, boolean isUser, int id) throws SQLException {
        if (isUser) {
            userParent.clearParent(id);
            userParent.addParent(id, -1, group.getDefaultGroup(), -1);
        } else groupParent.clearParent(id);
        sendSourceMessage(source, "§3All parents are now cleared.");
    }

    private void infoParent(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        if (args.length < 5) {
            PermissionSyntaxUtil.syntaxParent(source, isUser);
            return;
        }
        var parentName = args[4];
        if (isGroupNotExist(source, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (isParentNotExist(source, isUser, id, parentId)) return;

        var creator = isUser ? userParent.getCreatorId(id, parentId) : groupParent.getCreatorId(id, parentId);
        var createdDate = isUser ? userParent.getCreatedDate(id, parentId) : groupParent.getCreatedDate(id, parentId);
        var name = isUser ? "§3Username: §e%s" : "§3Rank: §e%s";
        sendSourceMessage(source, "§8--- §3Info parent: §e%s §8---", parentName);
        sendSourceMessage(source, name, args[1]);
        commandManager.sendCreatorMessage(source, user, creator);
        sendSourceMessage(source, "§3Created date: §e%s", createdDate);
        sendSourceMessage(source, "§3Expired date: §e%s", getParentExpiredDate(isUser, id, parentId));
    }

    private void timeParent(CommandSource source, boolean isUser, int id, String[] args) throws SQLException {
        if (args.length < 6) {
            PermissionSyntaxUtil.syntax(source, isUser);
            return;
        }

        var parentName = args[4];
        if (isGroupNotExist(source, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (isParentNotExist(source, isUser, id, parentId)) return;
        var time = TimeUtil.formatTime(args[5]);

        if (parentId == group.getDefaultGroup() && isUser) {
            sendSourceMessage(source, "§cYou can't change the time of the default group.");
            return;
        }

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
                var expiredDate = isUser ? userParent.setExpiredTime(id, parentId, time) : groupParent.setExpiredTime(id, parentId, time);
                sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, expiredDate);
            }
            case "add" -> {
                var expiredDate = isUser ? userParent.addExpiredTime(id, parentId, time) : groupParent.addExpiredTime(id, parentId, time);
                sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, expiredDate);
            }
            case "remove" -> {
                var expiredDate = isUser ? userParent.removeExpiredTime(id, parentId, time) : groupParent.removeExpiredTime(id, parentId, time);
                sendSourceMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, expiredDate);
            }
            default -> PermissionSyntaxUtil.syntaxParent(source, isUser);
        }
    }

    private boolean isGroupNotExist(CommandSource source, String groupName) throws SQLException {
        if (!group.existsGroup(groupName)) {
            sendSourceMessage(source, "§cGroup does not exist.");
            return true;
        } else return false;
    }

    private boolean isParentNotExist(CommandSource source, boolean isUser, int id, int parentId) throws SQLException {
        var exist = isUser ? userParent.existsParent(id, parentId) : groupParent.existsParent(id, parentId);
        if (!exist) {
            sendSourceMessage(source, "§cParent does not exist.");
            return true;
        } else return false;
    }

    private String getParentExpiredDate(boolean isUser, int id, int parentId) throws SQLException {
        return isUser ? userParent.getExpiredDate(id, parentId) : groupParent.getExpiredDate(id, parentId);
    }
}
