package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.subcommand.GroupEditSubCommand;
import de.murmelmeister.essentials.commands.subcommand.ParentSubCommand;
import de.murmelmeister.essentials.commands.subcommand.PermissionSubCommand;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PermissionCommand extends PermissionUtil {
    private final GroupColor groupColor;
    private final GroupEditSubCommand groupEditSub;
    private final ParentSubCommand parentSub;
    private final PermissionSubCommand permissionSub;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PermissionCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupColor = plugin.getGroup().getColor();
        this.groupEditSub = new GroupEditSubCommand(plugin);
        this.parentSub = new ParentSubCommand(plugin);
        this.permissionSub = new PermissionSubCommand(plugin);
    }

    @Override
    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> node = BrigadierCommand.literalArgumentBuilder("permission")
                .requires(source -> source.hasPermission("murmel.command.permission"))
                .executes(context -> {
                    sendMessage(context.getSource(), syntax());
                    return Command.SINGLE_SUCCESS;
                })
                .then(getGroupsCommand())
                .then(getGroupCommand())
                .then(getUsersCommand())
                .then(getUserCommand())
                .build();
        return new BrigadierCommand(node);
    }

    private LiteralArgumentBuilder<CommandSource> getGroupsCommand() {
        // -/permission groups
        return BrigadierCommand.literalArgumentBuilder("groups")
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    List<String> groupNames = group.getGroupNames();

                    if (groupNames.isEmpty()) {
                        sendMessage(source, "<#990000>No groups found.");
                        return -1;
                    }

                    sendMessage(source, "<#999999>%s: ", groupNames.size() == 1 ? "Group" : "Groups");
                    groupNames.forEach(name -> {
                        String clickMessage = "/permission group " + name + " info";
                        sendMessage(source, "<#999999>- <#00cc88>" +
                                            "<hover:show_text:'<#999999>Click to get <#00cc88>group information'>" +
                                            "<click:suggest_command:'%s'>%s</click></hover>", clickMessage, name);
                    });

                    int executorId = getExecutorId(source);
                    loggingToConsole(executorId, "/permission groups");
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>Groups command executed in %s ms", durationMs);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> getGroupCommand() {
        // -/permission group <groupName> ...
        return BrigadierCommand.literalArgumentBuilder("group")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("groupName", StringArgumentType.word())
                        .suggests(this::getGroupNames)
                        .executes(context -> {
                            sendMessage(context.getSource(), syntaxGroup());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getGroupInfoCommand())
                        .then(getGroupCreateCommand())
                        .then(getGroupDeleteCommand())
                        .then(getGroupRenameCommand())
                        .then(getGroupParentCommand())
                        .then(getGroupPermissionCommand())
                        .then(getGroupEditCommand())
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupInfoCommand() {
        // -/permission group <groupName> info
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    String groupName = StringArgumentType.getString(context, "groupName");

                    int groupId = getGroupId(source, groupName);
                    if (groupId == 0) return -1;

                    int priority = group.getPriority(groupId);
                    String teamSort = group.getTeamSort(groupId);
                    int createdBy = group.getCreatedBy(groupId);
                    String createdName = user.getUsername(createdBy);
                    String createdAt = group.getCreatedDate(groupId);
                    int updatedBy = group.getUpdatedBy(groupId);
                    String updatedName = user.getUsername(updatedBy);
                    String updatedAt = group.getUpdatedDate(groupId);

                    int executorId = getExecutorId(source);
                    String executorName = user.getUsername(executorId);
                    GroupColorType typeChat = GroupColorType.CHAT;
                    GroupColorType typeTab = GroupColorType.TAB;
                    GroupColorType typeTeam = GroupColorType.TEAM;

                    int colorCreatedBy = groupColor.getCreatedBy(groupId);
                    String colorCreatedName = user.getUsername(colorCreatedBy);
                    String colorCreatedAt = groupColor.getCreatedDate(groupId);
                    int colorUpdatedBy = groupColor.getUpdatedBy(groupId);
                    String colorUpdatedName = user.getUsername(colorUpdatedBy);
                    String colorUpdatedAt = groupColor.getUpdatedDate(groupId);

                    String chatPrefix = groupColor.getPrefix(groupId, typeChat);
                    String chatSuffix = groupColor.getSuffix(groupId, typeChat);
                    String chatColor = groupColor.getColor(groupId, typeChat);
                    String chatMessage = groupColor.getMessage(groupId);
                    String tabPrefix = groupColor.getPrefix(groupId, typeTab);
                    String tabSuffix = groupColor.getSuffix(groupId, typeTab);
                    String tabColor = groupColor.getColor(groupId, typeTab);
                    String teamPrefix = groupColor.getPrefix(groupId, typeTeam);
                    String teamSuffix = groupColor.getSuffix(groupId, typeTeam);
                    String teamColor = groupColor.getColor(groupId, typeTeam);
                    NamedTextColor textColor = teamColor != null ? NamedTextColor.NAMES.value(teamColor.toLowerCase()) : null;

                    String formatChat = (chatColor != null ? "<" + chatColor + ">" : "")
                                        + (chatPrefix != null ? chatPrefix : "")
                                        + executorName
                                        + (chatSuffix != null ? chatSuffix : "")
                                        + (chatMessage != null ? chatMessage : " ") + "message";
                    String formatTab = (tabColor != null ? "<" + tabColor + ">" : "")
                                       + (tabPrefix != null ? tabPrefix : "")
                                       + executorName
                                       + (tabSuffix != null ? tabSuffix : "");
                    String formatTeam = (teamPrefix != null ? teamPrefix : "")
                                        + (textColor != null ? "<" + textColor + ">" : "")
                                        + executorName
                                        + (teamSuffix != null ? teamSuffix : "");

                    String chatHover = """
                            <#999999>Chat Prefix: "<#00cc88>%s</#00cc88>"
                            <#999999>Chat Suffix: "<#00cc88>%s</#00cc88>"
                            <#999999>Chat Color: <#00cc88>%s
                            <#999999>Chat Message: "<#00cc88>%s</#00cc88>"
                            <#999999>Created By: <#00cc88>%s (%s)
                            <#999999>Created At: <#00cc88>%s
                            <#999999>Updated By: <#00cc88>%s (%s)
                            <#999999>Updated At: <#00cc88>%s"""
                            .formatted(chatPrefix, chatSuffix, chatColor, miniMessage.escapeTags(chatMessage == null ? "" : chatMessage),
                                    colorCreatedBy, colorCreatedName, colorCreatedAt, colorUpdatedBy, colorUpdatedName, colorUpdatedAt);
                    String tabHover = """
                            <#999999>Tab Prefix: "<#00cc88>%s</#00cc88>"
                            <#999999>Tab Suffix: "<#00cc88>%s</#00cc88>"
                            <#999999>Tab Color: <#00cc88>%s
                            <#999999>Created By: <#00cc88>%s (%s)
                            <#999999>Created At: <#00cc88>%s
                            <#999999>Updated By: <#00cc88>%s (%s)
                            <#999999>Updated At: <#00cc88>%s"""
                            .formatted(tabPrefix, tabSuffix, tabColor,
                                    colorCreatedBy, colorCreatedName, colorCreatedAt, colorUpdatedBy, colorUpdatedName, colorUpdatedAt);
                    String teamHover = """
                            <#999999>Team Prefix: "<#00cc88>%s</#00cc88>"
                            <#999999>Team Suffix: "<#00cc88>%s</#00cc88>"
                            <#999999>Team Color: <#00cc88>%s
                            <#999999>Created By: <#00cc88>%s (%s)
                            <#999999>Created At: <#00cc88>%s
                            <#999999>Updated By: <#00cc88>%s (%s)
                            <#999999>Updated At: <#00cc88>%s"""
                            .formatted(teamPrefix, teamSuffix, teamColor,
                                    colorCreatedBy, colorCreatedName, colorCreatedAt, colorUpdatedBy, colorUpdatedName, colorUpdatedAt);

                    String message = """
                            <#999999>===- Group Information:
                            <#999999>Group Name: <#00cc88>%s
                            <#999999>Group ID: <#00cc88>%s
                            <#999999>Priority: <#00cc88>%s
                            <#999999>Team Sort: <#00cc88>%s
                            <#999999>Created By: <#00cc88>%s (%s)
                            <#999999>Created At: <#00cc88>%s
                            <#999999>Updated By: <#00cc88>%s (%s)
                            <#999999>Updated At: <#00cc88>%s
                            <#999999>Chat Format: <hover:show_text:'%s'>%s</hover>
                            <#999999>Tab Format: <hover:show_text:'%s'>%s</hover>
                            <#999999>Team Format: <hover:show_text:'%s'>%s</hover>"""
                            .formatted(groupName, groupId, priority, teamSort, createdBy, createdName, createdAt, updatedBy, updatedName, updatedAt,
                                    chatHover, formatChat, tabHover, formatTab, teamHover, formatTeam);

                    sendMessage(source, message);

                    loggingToConsole(false, executorId, groupId, "/permission group " + groupName + " info");
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>Group info command executed in %s ms", durationMs);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> getGroupCreateCommand() {
        // -/permission group <groupName> create <priority> <teamId>
        return BrigadierCommand.literalArgumentBuilder("create")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("priority", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            sendMessage(context.getSource(), syntaxGroup());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("teamId", StringArgumentType.word())
                                .executes(context -> {
                                    long startTime = System.nanoTime();
                                    CommandSource source = context.getSource();
                                    String groupName = StringArgumentType.getString(context, "groupName");
                                    int priority = IntegerArgumentType.getInteger(context, "priority");
                                    String teamId = StringArgumentType.getString(context, "teamId");

                                    if (group.existsGroup(groupName)) {
                                        sendMessage(source, "<#990000>Group %s already exists.", groupName);
                                        return -1;
                                    }

                                    int executorId = getExecutorId(source);
                                    int row = group.createGroup(groupName, priority, teamId, executorId);

                                    int groupId = group.getId(groupName);
                                    int colorRow = 0;
                                    if (!groupColor.existsGroup(groupId))
                                        colorRow = groupColor.createGroup(groupId, executorId);
                                    sendMessage(source, "<#00cc88>Group %s was created.", groupName, row);

                                    RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                                    loggingToConsole(false, executorId, groupId, "/permission group " + groupName + " create " + priority + " " + teamId);
                                    if (user.isDebugMode(executorId)) {
                                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                        sendDebugMessage(source, "<#999900>Group create command executed in %s ms", durationMs);
                                        sendDebugMessage(source, "<#999900>Created row: %s", row + colorRow);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupDeleteCommand() {
        // -/permission group <groupName> delete
        return BrigadierCommand.literalArgumentBuilder("delete")
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    String groupName = StringArgumentType.getString(context, "groupName");

                    int groupId = getGroupId(source, groupName);
                    if (groupId == 0) return -1;

                    if (groupId == group.getId("default")) {
                        sendMessage(source, "<#990000>You cannot delete the default group.");
                        return -2;
                    }

                    int row = groupColor.deleteGroup(groupId)
                              + group.getPermission().clearPermission(groupId)
                              + group.getParent().clearParent(groupId)
                              + group.getParent().clearOtherParent(groupId)
                              + user.getParent().clearOtherParent(groupId);
                    int finalRow = group.deleteGroup(groupId) + row;
                    sendMessage(source, "<#00cc88>Group %s was deleted.", groupName);

                    RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                    int executorId = getExecutorId(source);
                    loggingToConsole(false, executorId, groupId, "/permission group " + groupName + " delete");
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>Group delete command executed in %s ms", durationMs);
                        sendDebugMessage(source, "<#999900>Deleted row: %s", finalRow);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> getGroupRenameCommand() {
        // -/permission group <groupName> rename <newName>
        return BrigadierCommand.literalArgumentBuilder("rename")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("newName", StringArgumentType.word())
                        .executes(context -> {
                            long startTime = System.nanoTime();
                            CommandSource source = context.getSource();
                            String groupName = StringArgumentType.getString(context, "groupName");
                            String newName = StringArgumentType.getString(context, "newName");

                            int groupId = getGroupId(source, groupName);
                            if (groupId == 0) return -1;

                            if (groupId == group.getId("default")) {
                                sendMessage(source, "<#990000>You cannot rename the default group.");
                                return -2;
                            }

                            if (group.existsGroup(newName)) {
                                sendMessage(source, "<#990000>Group %s already exists.", newName);
                                return -3;
                            }

                            String teamSort = group.getTeamSort(groupId);
                            int executorId = getExecutorId(source);

                            int row = group.rename(groupId, newName, executorId)
                                      + group.setTeamSort(groupId, teamSort.replace(groupName, newName), executorId);
                            sendMessage(source, "<#00cc88>Group %s was renamed to %s.", groupName, newName);

                            RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                            loggingToConsole(false, executorId, groupId, "/permission group " + groupName + " rename " + newName);
                            if (user.isDebugMode(executorId)) {
                                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                sendDebugMessage(source, "<#999900>Group rename command executed in %s ms", durationMs);
                                sendDebugMessage(source, "<#999900>Renamed row: %s", row);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupParentCommand() {
        // -/permission group <groupName> parent ...
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context -> parentSub.getParents(context, false))
                .then(parentSub.getParentAdd(false))
                .then(parentSub.getParentRemove(false))
                .then(parentSub.getParentClear(false))
                .then(parentSub.getParentInfo(false))
                .then(parentSub.getParentTime(false));
    }

    private LiteralArgumentBuilder<CommandSource> getGroupPermissionCommand() {
        // -/permission group <groupName> permission ...
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context -> permissionSub.getPermissions(context, false))
                .then(permissionSub.getPermissionAll(false))
                .then(permissionSub.getPermissionAdd(false))
                .then(permissionSub.getPermissionRemove(false))
                .then(permissionSub.getPermissionClear(false))
                .then(permissionSub.getPermissionInfo(false))
                .then(permissionSub.getPermissionTime(false));
    }

    private LiteralArgumentBuilder<CommandSource> getGroupEditCommand() {
        // -/permission group <groupName> edit ...
        return BrigadierCommand.literalArgumentBuilder("edit")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                .then(groupEditSub.getEditedChatCommand())
                .then(groupEditSub.getEditedTabCommand())
                .then(groupEditSub.getEditedTeamCommand())
                .then(groupEditSub.getEditedPriorityCommand());
    }

    private LiteralArgumentBuilder<CommandSource> getUsersCommand() {
        // -/permission users
        return BrigadierCommand.literalArgumentBuilder("users")
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    List<String> usernames = user.getUsernames();

                    if (usernames.isEmpty()) {
                        sendMessage(source, "<#990000>No users found.");
                        return -1;
                    }

                    sendMessage(source, "<#999999>%s: ", usernames.size() == 1 ? "User" : "Users");
                    usernames.forEach(username -> {
                        String clickMessage = "/permission user " + username + " info";
                        sendMessage(source, "<#999999>- <#00cc88>" +
                                            "<hover:show_text:'<#999999>Click to get <#00cc88>user information'>" +
                                            "<click:suggest_command:'%s'>%s</click></hover>", clickMessage, username);
                    });

                    int executorId = getExecutorId(source);
                    loggingToConsole(executorId, "/permission users");
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>Users command executed in %s ms", durationMs);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> getUserCommand() {
        // -/permission user <username> ...
        return BrigadierCommand.literalArgumentBuilder("user")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxUser());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                        .suggests(this::getUsernames)
                        .executes(context -> {
                            sendMessage(context.getSource(), syntaxUser());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getUserInfoCommand())
                        .then(getUserParentCommand())
                        .then(getUserPermissionCommand())
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUserInfoCommand() {
        // -/permission user <username> info
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    int executorId = getExecutorId(source);
                    if (executorId == -2) return -1;

                    String username = StringArgumentType.getString(context, "username");
                    int userId = getUserId(source, username);
                    if (userId == -2) return -2;

                    UUID mojangId = user.getUniqueId(userId);
                    String firstJoinDate = user.getFirstJoinDate(userId);
                    String isDebugUser = user.isDebugUser(userId) ? "<#00cc88>Yes" : "<#990000>No";
                    String isDebugMode = user.isDebugActive(userId) ? "<#00cc88>Yes" : "<#990000>No";

                    String message = """
                            <#999999>===- User information:
                            <#999999>Username: <#00cc88>%s
                            <#999999>User ID: <#00cc88>%s
                            <#999999>Mojang ID: <#00cc88>%s
                            <#999999>First join: <#00cc88>%s
                            <#999999>Debug user: <#00cc88>%s
                            <#999999>Debug mode: <#00cc88>%s"""
                            .formatted(username, userId, mojangId.toString(), firstJoinDate, isDebugUser, isDebugMode);
                    sendMessage(source, message);

                    loggingToConsole(executorId, "/permission user " + username + " info");
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>User info command executed in %s ms", durationMs);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> getUserParentCommand() {
        // -/permission user <username> parent ...
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context -> parentSub.getParents(context, true))
                .then(parentSub.getParentAdd(true))
                .then(parentSub.getParentRemove(true))
                .then(parentSub.getParentClear(true))
                .then(parentSub.getParentInfo(true))
                .then(parentSub.getParentTime(true));
    }

    private LiteralArgumentBuilder<CommandSource> getUserPermissionCommand() {
        // -/permission user <username> permission ...
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context -> permissionSub.getPermissions(context, true))
                .then(permissionSub.getPermissionAll(true))
                .then(permissionSub.getPermissionAdd(true))
                .then(permissionSub.getPermissionRemove(true))
                .then(permissionSub.getPermissionClear(true))
                .then(permissionSub.getPermissionInfo(true))
                .then(permissionSub.getPermissionTime(true));
    }

    private CompletableFuture<Suggestions> getGroupNames(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        group.getGroupNames().stream()
                .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                .sorted()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> getUsernames(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        user.getUsernames().stream()
                .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                .sorted()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
