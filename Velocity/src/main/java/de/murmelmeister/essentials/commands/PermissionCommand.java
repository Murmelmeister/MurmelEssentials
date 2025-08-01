package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.subcommand.GroupEditSubCommand;
import de.murmelmeister.essentials.commands.subcommand.ParentSubCommand;
import de.murmelmeister.essentials.commands.subcommand.PermissionSubCommand;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.utils.StringUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public final class PermissionCommand extends PermissionUtil {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final UserProvider userProvider;
    private final GroupProvider groupProvider;
    private final GroupColorProvider groupColorProvider;
    private final GroupParentProvider groupParentProvider;
    private final GroupPermissionProvider groupPermissionProvider;
    private final MessageService messageService;
    private final GroupEditSubCommand groupEditSub;
    private final ParentSubCommand parentSub;
    private final PermissionSubCommand permissionSub;

    public PermissionCommand(MurmelEssentials plugin) {
        super(plugin);
        this.userProvider = plugin.getUserProvider();
        this.groupProvider = plugin.getGroupProvider();
        this.groupColorProvider = plugin.getGroupColorProvider();
        this.groupParentProvider = plugin.getGroupParentProvider();
        this.groupPermissionProvider = plugin.getGroupPermissionProvider();
        this.messageService = plugin.getMessageService();
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
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            List<String> groupNames = groupProvider.findAllGroupNames();

                            if (groupNames.isEmpty()) {
                                sendMessage(source, "<#990000>No groups found.");
                                return CommandResult.of(-2);
                            }

                            sendMessage(source, "<#999999>%s: ", groupNames.size() == 1 ? "Group" : "Groups");
                            groupNames.forEach(name -> {
                                String clickMessage = "/permission group " + name + " info";
                                sendMessage(source, "<#999999>- <#00cc88>" +
                                                    "<hover:show_text:'<#999999>Click to get <#00cc88>group information'>" +
                                                    "<click:suggest_command:'%s'>%s</click></hover>", clickMessage, name);
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupCommand() {
        // -/permission group <groupName> ...
        return BrigadierCommand.literalArgumentBuilder("group")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("groupName", StringArgumentType.word())
                        .suggests(getGroupNames())
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
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            int languageId = executor.languageId();
                            String groupName = StringArgumentType.getString(context, "groupName");
                            Group group = getGroup(languageId, groupName);

                            int groupId = group.id();
                            int priority = group.priority();
                            String teamTagId = group.teamTagId();
                            User creator = getUser(languageId, group.createdBy());
                            User changer = group.changedBy() == null ? null : getUser(languageId, group.changedBy());
                            String createdDate = group.createdAt().format(getDateTimeFormatter(languageId));
                            String changedDate = group.changedAt() == null ? null : group.changedAt().format(getDateTimeFormatter(languageId));

                            String executorName = executor.username();
                            GroupColor chatPrefix = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_PREFIX.getId());
                            GroupColor chatSuffix = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_SUFFIX.getId());
                            GroupColor chatColor = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_COLOR.getId());
                            GroupColor chatMessage = groupColorProvider.getGroupColor(groupId, GroupColorType.CHAT_MESSAGE.getId());

                            GroupColor tabPrefix = groupColorProvider.getGroupColor(groupId, GroupColorType.TAB_PREFIX.getId());
                            GroupColor tabSuffix = groupColorProvider.getGroupColor(groupId, GroupColorType.TAB_SUFFIX.getId());
                            GroupColor tabColor = groupColorProvider.getGroupColor(groupId, GroupColorType.TAB_COLOR.getId());

                            GroupColor teamPrefix = groupColorProvider.getGroupColor(groupId, GroupColorType.TEAM_PREFIX.getId());
                            GroupColor teamSuffix = groupColorProvider.getGroupColor(groupId, GroupColorType.TEAM_SUFFIX.getId());
                            GroupColor teamColor = groupColorProvider.getGroupColor(groupId, GroupColorType.TEAM_COLOR.getId());
                            NamedTextColor textColor = teamColor != null ? NamedTextColor.NAMES.value(teamColor.value().toLowerCase()) : null;

                            String formatChat = (chatColor != null ? "<" + chatColor.value() + ">" : "")
                                                + (chatPrefix != null ? chatPrefix.value() : "")
                                                + executorName
                                                + (chatSuffix != null ? chatSuffix.value() : "")
                                                + (chatMessage != null ? chatMessage.value() : " » ") + "message";
                            String formatTab = (tabColor != null ? "<" + tabColor.value() + ">" : "")
                                               + (tabPrefix != null ? tabPrefix.value() : "")
                                               + executorName
                                               + (tabSuffix != null ? tabSuffix.value() : "");
                            String formatTeam = (teamPrefix != null ? teamPrefix.value() : "")
                                                + (textColor != null ? "<" + textColor + ">" : "")
                                                + executorName
                                                + (teamSuffix != null ? teamSuffix.value() : "");

                            // TODO: Add created & changed stuff information for chat, tab, and team colors
                            String chatHover = """
                                    <#999999>Prefix: "<#00cc88>%s</#00cc88>"
                                    <#999999>Suffix: "<#00cc88>%s</#00cc88>"
                                    <#999999>Color: <#00cc88>%s
                                    <#999999>Message: "<#00cc88>%s</#00cc88>"""
                                    .formatted(chatPrefix, chatSuffix, chatColor, miniMessage.escapeTags(chatMessage == null ? "" : chatMessage.value()));
                            String tabHover = """
                                    <#999999>Prefix: "<#00cc88>%s</#00cc88>"
                                    <#999999>Suffix: "<#00cc88>%s</#00cc88>"
                                    <#999999>Color: <#00cc88>%s"""
                                    .formatted(tabPrefix, tabSuffix, tabColor);
                            String teamHover = """
                                    <#999999>Prefix: "<#00cc88>%s</#00cc88>"
                                    <#999999>Suffix: "<#00cc88>%s</#00cc88>"
                                    <#999999>Color: <#00cc88>%s"""
                                    .formatted(teamPrefix, teamSuffix, teamColor);

                            String changedText = (changer == null || group.changedAt() == null) ? null :
                                    "<#999999>Changed by <#00cc88>%s (%s)</#00cc88> on <#00cc88>%s"
                                            .formatted(changer.username(), changer.id(), changedDate);
                            String message = """
                                    <#999999>===- Group Information:
                                    <#999999>Group Name: <#00cc88>%s
                                    <#999999>Group ID: <#00cc88>%s
                                    <#999999>Priority: <#00cc88>%s
                                    <#999999>Team Sort: <#00cc88>%s
                                    <#999999>Created by <#00cc88>%s (%s)</#00cc88> on <#00cc88>%s %s
                                    <#999999>Chat Format: <hover:show_text:'%s'>%s</hover>
                                    <#999999>Tab Format: <hover:show_text:'%s'>%s</hover>
                                    <#999999>Team Format: <hover:show_text:'%s'>%s</hover>"""
                                    .formatted(groupName, groupId, priority, teamTagId,
                                            creator.username(), creator.id(), createdDate,
                                            changedText == null ? "" : "\n" + changedText,
                                            chatHover, formatChat,
                                            tabHover, formatTab,
                                            teamHover, formatTeam);

                            sendMessage(source, message);
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                );
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
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            String groupName = StringArgumentType.getString(context, "groupName");
                                            int priority = IntegerArgumentType.getInteger(context, "priority");
                                            String teamTagId = StringArgumentType.getString(context, "teamId");

                                            Group group = groupProvider.findByName(groupName);
                                            if (group != null) {
                                                sendMessage(source, "<#990000>Group %s already exists.", groupName);
                                                return CommandResult.of(-2);
                                            }

                                            Group success = groupProvider.create(groupName, priority, teamTagId, executor.id());
                                            sendMessage(source, "<#00cc88>Group %s was created.", groupName);
                                            return CommandResult.of(Command.SINGLE_SUCCESS, success != null ? 1 : null);
                                        })
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupDeleteCommand() {
        // -/permission group <groupName> delete
        return BrigadierCommand.literalArgumentBuilder("delete")
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            int languageId = executor.languageId();
                            String groupName = StringArgumentType.getString(context, "groupName");

                            Group group = getGroup(languageId, groupName);
                            int groupId = group.id();

                            if (groupId == getDefaultGroup(languageId).id()) {
                                sendMessage(source, "<#990000>You cannot delete the default group.");
                                return CommandResult.of(-2);
                            }

                            int result = 0;
                            result += groupPermissionProvider.clear(groupId);
                            result += groupParentProvider.clear(groupId);
                            result += groupColorProvider.clear(groupId);
                            result += groupProvider.delete(groupId);
                            sendMessage(source, "<#00cc88>Group %s was deleted.", groupName);
                            return CommandResult.of(Command.SINGLE_SUCCESS, result < 1 ? null : result);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupRenameCommand() {
        // -/permission group <groupName> rename <newName>
        return BrigadierCommand.literalArgumentBuilder("rename")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("newName", StringArgumentType.word())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String groupName = StringArgumentType.getString(context, "groupName");
                                    Group group = getGroup(languageId, groupName);

                                    if (group.id() == getDefaultGroup(languageId).id()) {
                                        sendMessage(source, "<#990000>You cannot rename the default group.");
                                        return CommandResult.of(-2);
                                    }

                                    String newName = StringArgumentType.getString(context, "newName");
                                    if (group.groupName().equals(newName)) {
                                        sendMessage(source, "<#990000>Group %s already exists.", newName);
                                        return CommandResult.of(-3);
                                    }

                                    String teamTagId = group.teamTagId();
                                    String newTeamTagId = teamTagId.replace(groupName, newName);

                                    Group success = groupProvider.update(group.id(), newName, group.priority(), newTeamTagId, executor.id());
                                    sendMessage(source, "<#00cc88>Group %s was renamed to %s.", groupName, newName);
                                    return CommandResult.of(Command.SINGLE_SUCCESS, success != null ? 1 : null);
                                })
                        )
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
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            List<String> usernames = userProvider.findUsernames(); // You can also use userProvider.findAll() to get User objects

                            if (usernames.isEmpty()) {
                                sendMessage(source, "<#990000>No users found.");
                                return CommandResult.of(-2);
                            }

                            sendMessage(source, "<#999999>%s: ", usernames.size() == 1 ? "User" : "Users");
                            usernames.forEach(username -> {
                                String clickMessage = "/permission user " + username + " info";
                                sendMessage(source, "<#999999>- <#00cc88>" +
                                                    "<hover:show_text:'<#999999>Click to get <#00cc88>user information'>" +
                                                    "<click:suggest_command:'%s'>%s</click></hover>", clickMessage, username);
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUserCommand() {
        // -/permission user <username> ...
        return BrigadierCommand.literalArgumentBuilder("user")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxUser());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                        .suggests(getUsernames())
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
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            String username = StringArgumentType.getString(context, "username");
                            User user = getUser(executor.languageId(), username);

                            String firstJoinDate = user.firstLogin().format(getDateTimeFormatter(executor.languageId()));
                            String isDebugUser = user.debugUser() ? "<#00cc88>Yes" : "<#990000>No";
                            String isDebugMode = user.debugEnabled() ? "<#00cc88>Yes" : "<#990000>No";

                            String message = """
                                    <#999999>===- User information:
                                    <#999999>Username: <#00cc88>%s
                                    <#999999>User ID: <#00cc88>%s
                                    <#999999>Mojang ID: <#00cc88>%s
                                    <#999999>First join: <#00cc88>%s
                                    <#999999>Debug user: <#00cc88>%s
                                    <#999999>Debug mode: <#00cc88>%s"""
                                    .formatted(user.username(), user.id(), user.mojangId().toString(), firstJoinDate, isDebugUser, isDebugMode);
                            sendMessage(source, message);
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                );
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

    private SuggestionProvider<CommandSource> getGroupNames() {
        return (context, builder) -> {
            String prefix = builder.getRemaining();
            groupProvider.findAllGroupNames().stream()
                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSource> getUsernames() {
        return (context, builder) -> {
            String prefix = builder.getRemaining();
            userProvider.findUsernames().stream()
                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
