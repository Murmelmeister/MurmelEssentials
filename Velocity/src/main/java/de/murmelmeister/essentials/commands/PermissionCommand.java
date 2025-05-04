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
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.utils.StringUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PermissionCommand extends PermissionUtil {
    private final GroupColor groupColor;

    public PermissionCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupColor = plugin.getGroup().getColor();
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

                    sendMessage(source, "<#999999>Groups: ");
                    for (String groupName : groupNames)
                        sendMessage(source, "<#999999>- <#00cc88>%s", groupName); // TODO: Add click to clipboard? => -/permission group <groupName>

                    // TODO: Add logging - groups
                    int executorId = getExecutorId(source);
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendMessage(source, "<#999900>Groups command executed in %s ms", durationMs);
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
                    String chatColorMessage = groupColor.getColor(groupId, GroupColorType.CHAT_MESSAGE);
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
                                        + " : " + (chatColorMessage != null ? "<" + chatColorMessage + ">" : "") + "message";
                    String formatTab = (tabColor != null ? "<" + tabColor + ">" : "")
                                       + (tabPrefix != null ? tabPrefix : "")
                                       + executorName
                                       + (tabSuffix != null ? tabSuffix : "");
                    String formatTeam = (teamPrefix != null ? teamPrefix : "")
                                        + (textColor != null ? "<" + textColor + ">" : "")
                                        + executorName
                                        + (teamSuffix != null ? teamSuffix : "");

                    String chatHover = """
                            <#999999>Chat Prefix: <#00cc88>%s
                            <#999999>Chat Suffix: <#00cc88>%s
                            <#999999>Chat Color: <#00cc88>%s
                            <#999999>Chat Message Color: <#00cc88>%s
                            <#999999>Created By: <#00cc88>%s (%s)
                            <#999999>Created At: <#00cc88>%s
                            <#999999>Updated By: <#00cc88>%s (%s)
                            <#999999>Updated At: <#00cc88>%s"""
                            .formatted(chatPrefix, chatSuffix, chatColor, chatColorMessage,
                                    colorCreatedBy, colorCreatedName, colorCreatedAt, colorUpdatedBy, colorUpdatedName, colorUpdatedAt);
                    String tabHover = """
                            <#999999>Tab Prefix: <#00cc88>%s
                            <#999999>Tab Suffix: <#00cc88>%s
                            <#999999>Tab Color: <#00cc88>%s
                            <#999999>Created By: <#00cc88>%s (%s)
                            <#999999>Created At: <#00cc88>%s
                            <#999999>Updated By: <#00cc88>%s (%s)
                            <#999999>Updated At: <#00cc88>%s"""
                            .formatted(tabPrefix, tabSuffix, tabColor,
                                    colorCreatedBy, colorCreatedName, colorCreatedAt, colorUpdatedBy, colorUpdatedName, colorUpdatedAt);
                    String teamHover = """
                            <#999999>Team Prefix: <#00cc88>%s
                            <#999999>Team Suffix: <#00cc88>%s
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
                    // TODO: Add logging - group info
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendMessage(source, "<#999900>Group info command executed in %s ms", durationMs);
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
                                    // TODO: Add logging - group create
                                    if (user.isDebugMode(executorId)) {
                                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                        sendMessage(source, "<#999900>Group create command executed in %s ms", durationMs);
                                        sendMessage(source, "<#999900>Created row: %s", row + colorRow);
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
                    // TODO: Add logging - group delete
                    int executorId = getExecutorId(source);
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendMessage(source, "<#999900>Group delete command executed in %s ms", durationMs);
                        sendMessage(source, "<#999900>Deleted row: %s", finalRow);
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
                            // TODO: Add logging - group rename
                            if (user.isDebugMode(executorId)) {
                                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                sendMessage(source, "<#999900>Group rename command executed in %s ms", durationMs);
                                sendMessage(source, "<#999900>Renamed row: %s", row);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupParentCommand() {
        // -/permission group <groupName> parent ...
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxParent(false));
                    return Command.SINGLE_SUCCESS;
                })
                // TODO: Implement group parent logic
                ;
    }

    private LiteralArgumentBuilder<CommandSource> getGroupPermissionCommand() {
        // -/permission group <groupName> permission ...
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(false));
                    return Command.SINGLE_SUCCESS;
                })
                // TODO: Implement group permission logic
                ;
    }

    private LiteralArgumentBuilder<CommandSource> getGroupEditCommand() {
        // -/permission group <groupName> edit ...
        return BrigadierCommand.literalArgumentBuilder("edit")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                // TODO: Implement group edit logic
                ;
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

                    sendMessage(source, "<#999999>Users: ");
                    for (String username : usernames)
                        sendMessage(source, "<#999999>- <#00cc88>%s", username); // TODO: Add click to clipboard? => -/permission user <username>

                    int executorId = getExecutorId(source);
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendMessage(source, "<#999900>Users command executed in %s ms", durationMs);
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
                        // TODO: Implement user info command logic
                        .then(getUserParentCommand())
                        .then(getUserPermissionCommand())
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUserParentCommand() {
        // -/permission user <username> parent ...
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxParent(true));
                    return Command.SINGLE_SUCCESS;
                })
                // TODO: Implement user parent logic
                ;
    }

    private LiteralArgumentBuilder<CommandSource> getUserPermissionCommand() {
        // -/permission user <username> permission ...
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(true));
                    return Command.SINGLE_SUCCESS;
                })
                // TODO: Implement user permission logic
                ;
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

    private int getGroupId(CommandSource source, String groupName) {
        if (!existsGroup(source, groupName)) return 0;
        return group.getId(groupName);
    }

    private int getUserId(CommandSource source, String username) {
        if (!existsUser(source, username)) return -2;
        return user.getId(username);
    }
}
