package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.subcommand.GroupEditSubCommand;
import de.murmelmeister.essentials.commands.subcommand.ParentSubCommand;
import de.murmelmeister.essentials.commands.subcommand.PermissionSubCommand;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.permission.parent.ParentProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;

@CommandConfig(id = "permission", name = "permission", aliases = {"perms"})
public final class PermissionCommand extends PermissionUtil {
    private final UserProvider userProvider;
    private final GroupProvider groupProvider;
    private final GroupColorProvider groupColorProvider;
    private final ParentProvider parentProvider;
    private final PermissionProvider permissionProvider;
    private final MessageService messageService;
    private final GroupEditSubCommand groupEditSub;
    private final ParentSubCommand parentSub;
    private final PermissionSubCommand permissionSub;

    public PermissionCommand(MurmelEssentials plugin) {
        super(plugin);
        this.userProvider = plugin.getUserProvider();
        this.groupProvider = plugin.getGroupProvider();
        this.groupColorProvider = plugin.getGroupColorProvider();
        this.parentProvider = plugin.getParentProvider();
        this.permissionProvider = plugin.getPermissionProvider();
        this.messageService = plugin.getMessageService();
        this.groupEditSub = new GroupEditSubCommand(plugin);
        this.parentSub = new ParentSubCommand(plugin);
        this.permissionSub = new PermissionSubCommand(plugin);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source.hasPermission(MurmelEssentials.BASE_PERMISSION_COMMAND + "permission"))
                .then(getGroupsCommand())
                .then(getGroupCommand())
                .then(getUsersCommand())
                .then(getUserCommand());
    }

    private LiteralArgumentBuilder<CommandSource> getGroupsCommand() {
        // -/permission groups
        return BrigadierCommand.literalArgumentBuilder("groups")
                .executes(context ->
                        executeGroups(context, 1)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                executeGroups(context, IntegerArgumentType.getInteger(context, "page")
                                )
                        )
                );
    }

    private int executeGroups(CommandContext<CommandSource> context, int page) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            List<String> groupNames = groupProvider.findAllGroupNames();

            if (groupNames.isEmpty())
                throw new CommandException(Message.PERMISSION_LIST_GROUP_EMPTY);

            Message headerName = groupNames.size() == 1
                    ? Message.PERMISSION_LIST_SINGULAR_GROUP
                    : Message.PERMISSION_LIST_PLURAL_GROUP;
            sendMessage(source, languageId, Message.PERMISSION_LIST_GROUP_HEADER, tagParsed("header_name", languageId, headerName));

            List<Component> groupComponents = groupNames.stream()
                    .map(groupName -> {
                        String clickMessage = "/permission group " + groupName + " info";
                        return component(languageId, Message.PERMISSION_LIST_GROUP_MESSAGE,
                                tagParsed("click_command", clickMessage),
                                tagParsed("group_name", groupName)
                        );
                    })
                    .toList();

            sendPagedMessage(source, groupComponents, "permission groups", page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private LiteralArgumentBuilder<CommandSource> getGroupCommand() {
        // -/permission group <groupName> ...
        return BrigadierCommand.literalArgumentBuilder("group")
                .then(BrigadierCommand.requiredArgumentBuilder("groupName", StringArgumentType.word())
                        .suggests(getGroupNames())
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
                            String inputGroup = StringArgumentType.getString(context, "groupName");
                            Group group = getGroup(inputGroup);

                            int groupId = group.id();
                            User creator = getUser(group.createdBy());
                            User changer = group.changedBy() == null ? null : getUser(group.changedBy());
                            String createdDate = group.createdAt().format(getDateTimeFormatter(languageId));
                            String changedDate = group.changedAt() == null ? null : group.changedAt().format(getDateTimeFormatter(languageId));

                            String executorName = executor.username();
                            GroupColor chatPrefix = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_PREFIX.getId()).orElse(null);
                            GroupColor chatSuffix = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_SUFFIX.getId()).orElse(null);
                            GroupColor chatColor = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_COLOR.getId()).orElse(null);
                            GroupColor chatMessage = groupColorProvider.findGroupColor(groupId, GroupColorType.CHAT_MESSAGE.getId()).orElse(null);

                            GroupColor tabPrefix = groupColorProvider.findGroupColor(groupId, GroupColorType.TAB_PREFIX.getId()).orElse(null);
                            GroupColor tabSuffix = groupColorProvider.findGroupColor(groupId, GroupColorType.TAB_SUFFIX.getId()).orElse(null);
                            GroupColor tabColor = groupColorProvider.findGroupColor(groupId, GroupColorType.TAB_COLOR.getId()).orElse(null);

                            GroupColor teamPrefix = groupColorProvider.findGroupColor(groupId, GroupColorType.TEAM_PREFIX.getId()).orElse(null);
                            GroupColor teamSuffix = groupColorProvider.findGroupColor(groupId, GroupColorType.TEAM_SUFFIX.getId()).orElse(null);
                            GroupColor teamColor = groupColorProvider.findGroupColor(groupId, GroupColorType.TEAM_COLOR.getId()).orElse(null);
                            NamedTextColor textColor = teamColor != null ? NamedTextColor.NAMES.value(teamColor.value().toLowerCase()) : null;

                            Component chatFormat = component(languageId, Message.PERMISSION_GROUP_INFO_FORMAT_CHAT,
                                    tagParsed("color", chatColor != null ? chatColor.value() : ""),
                                    tagParsed("prefix", chatPrefix != null ? chatPrefix.value() : ""),
                                    tagParsed("username", executorName),
                                    tagParsed("suffix", chatSuffix != null ? chatSuffix.value() : ""),
                                    tagParsed("message", chatMessage != null ? chatMessage.value() : " » ")
                            );
                            Component tabFormat = component(languageId, Message.PERMISSION_GROUP_INFO_FORMAT_TAB,
                                    tagParsed("color", tabColor != null ? tabColor.value() : ""),
                                    tagParsed("prefix", tabPrefix != null ? tabPrefix.value() : ""),
                                    tagParsed("username", executorName),
                                    tagParsed("suffix", tabSuffix != null ? tabSuffix.value() : "")
                            );
                            Component teamFormat = component(languageId, Message.PERMISSION_GROUP_INFO_FORMAT_TEAM,
                                    tagParsed("prefix", teamPrefix != null ? teamPrefix.value() : ""),
                                    tagParsed("username", textColor != null ? "<" + textColor + ">" + executorName + "</" + textColor + ">" : executorName),
                                    tagParsed("suffix", teamSuffix != null ? teamSuffix.value() : "")
                            );

                            Component chatHover = component(languageId, Message.PERMISSION_GROUP_INFO_HOVER_CHAT,
                                    tagUnparsed("prefix", chatPrefix == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : chatPrefix.value()),
                                    tagUnparsed("suffix", chatSuffix == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : chatSuffix.value()),
                                    tagUnparsed("color", chatColor == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : chatColor.value()),
                                    tagUnparsed("message", chatMessage == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : chatMessage.value())
                            );
                            Component tabHover = component(languageId, Message.PERMISSION_GROUP_INFO_HOVER_DEFAULT,
                                    tagUnparsed("prefix", tabPrefix == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : tabPrefix.value()),
                                    tagUnparsed("suffix", tabSuffix == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : tabSuffix.value()),
                                    tagUnparsed("color", tabColor == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : tabColor.value())
                            );
                            Component teamHover = component(languageId, Message.PERMISSION_GROUP_INFO_HOVER_DEFAULT,
                                    tagUnparsed("prefix", teamPrefix == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : teamPrefix.value()),
                                    tagUnparsed("suffix", teamSuffix == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : teamSuffix.value()),
                                    tagUnparsed("color", teamColor == null ? messageService.getMessage(Message.MESSAGE_VALUE_NULL.getTag(), languageId) : teamColor.value())
                            );

                            Component changedText = (changer == null || changedDate == null) ? Component.empty() :
                                    component(messageService.getMessage(Message.PERMISSION_INFO_CHANGE_STUFF.getTag(), languageId),
                                            tagParsed("changed_name", changer.username()),
                                            tagParsed("changed_id", changer.id()),
                                            tagParsed("changed_at", changedDate));

                            sendMessage(source, languageId, Message.PERMISSION_GROUP_INFO_MESSAGE,
                                    tagParsed("group_name", group.groupName()),
                                    tagParsed("group_id", group.id()),
                                    tagParsed("priority", group.priority()),
                                    tagParsed("created_name", creator.username()),
                                    tagParsed("created_id", creator.id()),
                                    tagParsed("created_at", createdDate),
                                    Placeholder.component("changed", changedText),
                                    Placeholder.component("chat_hover", chatHover),
                                    Placeholder.component("chat_format", chatFormat),
                                    Placeholder.component("tab_hover", tabHover),
                                    Placeholder.component("tab_format", tabFormat),
                                    Placeholder.component("team_hover", teamHover),
                                    Placeholder.component("team_format", teamFormat)
                            );
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupCreateCommand() {
        // -/permission group <groupName> create <priority>
        return BrigadierCommand.literalArgumentBuilder("create")
                .then(BrigadierCommand.requiredArgumentBuilder("priority", IntegerArgumentType.integer(1))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String inputGroup = StringArgumentType.getString(context, "groupName");
                                    int inputPriority = IntegerArgumentType.getInteger(context, "priority");

                                    Group group = groupProvider.findByName(inputGroup).orElse(null);
                                    if (group != null)
                                        throw new CommandException(Message.PERMISSION_GROUP_EXISTS, tagParsed("group_name", group.groupName()));

                                    Group success = groupProvider.create(inputGroup, inputPriority, executor.id()).orElseThrow(() ->
                                            new CommandException(Message.PERMISSION_GROUP_CREATE_FAILED,
                                                    tagUnparsed("group_name", inputGroup),
                                                    tagUnparsed("priority", inputPriority)
                                            )
                                    );

                                    sendMessage(source, languageId, Message.PERMISSION_GROUP_CREATE_SUCCESS,
                                            tagParsed("group_name", success.groupName()),
                                            tagParsed("group_id", success.id()),
                                            tagParsed("priority", success.priority())
                                    );
                                    return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupDeleteCommand() {
        // -/permission group <groupName> delete
        return BrigadierCommand.literalArgumentBuilder("delete")
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            int languageId = executor.languageId();
                            String inputGroup = StringArgumentType.getString(context, "groupName");

                            Group group = getGroup(inputGroup);
                            int groupId = group.id();

                            if (groupId == getDefaultGroup().id())
                                throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_DELETE);

                            int result = 0;
                            result += permissionProvider.clear(PermissionTarget.group(groupId));
                            result += parentProvider.clear(PermissionTarget.group(groupId));
                            result += groupColorProvider.clear(groupId);
                            result += groupProvider.delete(groupId);
                            sendMessage(source, languageId, Message.PERMISSION_GROUP_DELETE, tagUnparsed("group_name", inputGroup));
                            return CommandResult.of(Command.SINGLE_SUCCESS, result < 1 ? null : result);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupRenameCommand() {
        // -/permission group <groupName> rename <newName>
        return BrigadierCommand.literalArgumentBuilder("rename")
                .then(BrigadierCommand.requiredArgumentBuilder("newName", StringArgumentType.word())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String inputGroup = StringArgumentType.getString(context, "groupName");
                                    Group group = getGroup(inputGroup);

                                    if (group.id() == getDefaultGroup().id())
                                        throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_RENAME);

                                    String inputNewGroup = StringArgumentType.getString(context, "newName");
                                    if (group.groupName().equals(inputNewGroup) || groupProvider.findByName(inputNewGroup).orElse(null) != null)
                                        throw new CommandException(Message.PERMISSION_GROUP_EXISTS, tagParsed("group_name", group.groupName()));

                                    Group success = groupProvider.update(group.id(), inputNewGroup, group.priority(), executor.id()).orElseThrow(() ->
                                            new CommandException(Message.PERMISSION_GROUP_RENAME_FAILED,
                                                    tagUnparsed("group_name", inputGroup),
                                                    tagUnparsed("new_group_name", inputNewGroup)
                                            )
                                    );

                                    sendMessage(source, languageId, Message.PERMISSION_GROUP_RENAME_SUCCESS,
                                            tagParsed("group_name", group.groupName()),
                                            tagParsed("new_group_name", success.groupName())
                                    );
                                    return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupParentCommand() {
        // -/permission group <groupName> parent ...
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context ->
                        parentSub.executeGetParents(context, false, 1)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                parentSub.executeGetParents(context, false, IntegerArgumentType.getInteger(context, "page"))
                        )
                )
                .then(parentSub.getParentAdd(false))
                .then(parentSub.getParentRemove(false))
                .then(parentSub.getParentClear(false))
                .then(parentSub.getParentInfo(false))
                .then(parentSub.getParentTime(false));
    }

    private LiteralArgumentBuilder<CommandSource> getGroupPermissionCommand() {
        // -/permission group <groupName> permission ...
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context ->
                        permissionSub.executeGetPermissions(context, false, 1, false)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                permissionSub.executeGetPermissions(context, false, IntegerArgumentType.getInteger(context, "page"), false)
                        )
                )
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
                .then(groupEditSub.getEditedChatCommand())
                .then(groupEditSub.getEditedTabCommand())
                .then(groupEditSub.getEditedTeamCommand())
                .then(groupEditSub.getEditedPriorityCommand());
    }

    private LiteralArgumentBuilder<CommandSource> getUsersCommand() {
        // -/permission users
        return BrigadierCommand.literalArgumentBuilder("users")
                .executes(context ->
                        executeUsers(context, 1)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                executeUsers(context, IntegerArgumentType.getInteger(context, "page"))
                        )
                );
    }

    private int executeUsers(CommandContext<CommandSource> context, int page) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            List<String> usernames = userProvider.findUsernames();

            if (usernames.isEmpty())
                throw new CommandException(Message.PERMISSION_LIST_USER_EMPTY);

            Message headerName = usernames.size() == 1
                    ? Message.PERMISSION_LIST_SINGULAR_USER
                    : Message.PERMISSION_LIST_PLURAL_USER;
            sendMessage(source, languageId, Message.PERMISSION_LIST_USER_HEADER, tagParsed("header_name", languageId, headerName));

            List<Component> userComponents = usernames.stream()
                    .map(username -> {
                        String clickMessage = "/userinfo " + username;
                        return component(languageId, Message.PERMISSION_LIST_USER_MESSAGE,
                                tagParsed("click_command", clickMessage),
                                tagParsed("username", username)
                        );
                    })
                    .toList();

            sendPagedMessage(source, userComponents, "permission users", page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    private LiteralArgumentBuilder<CommandSource> getUserCommand() {
        // -/permission user <username> ...
        return BrigadierCommand.literalArgumentBuilder("user")
                .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                        .suggests(getUsernames())
                        .then(getUserParentCommand())
                        .then(getUserPermissionCommand())
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUserParentCommand() {
        // -/permission user <username> parent ...
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context ->
                        parentSub.executeGetParents(context, true, 1)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                parentSub.executeGetParents(context, true, IntegerArgumentType.getInteger(context, "page"))
                        )
                )
                .then(parentSub.getParentAdd(true))
                .then(parentSub.getParentRemove(true))
                .then(parentSub.getParentClear(true))
                .then(parentSub.getParentInfo(true))
                .then(parentSub.getParentTime(true));
    }

    private LiteralArgumentBuilder<CommandSource> getUserPermissionCommand() {
        // -/permission user <username> permission ...
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context ->
                        permissionSub.executeGetPermissions(context, true, 1, false)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                permissionSub.executeGetPermissions(context, true, IntegerArgumentType.getInteger(context, "page"), false)
                        )
                )
                .then(permissionSub.getPermissionAll(true))
                .then(permissionSub.getPermissionAdd(true))
                .then(permissionSub.getPermissionRemove(true))
                .then(permissionSub.getPermissionClear(true))
                .then(permissionSub.getPermissionInfo(true))
                .then(permissionSub.getPermissionTime(true));
    }

    private SuggestionProvider<CommandSource> getGroupNames() {
        return (_, builder) -> {
            String prefix = builder.getRemaining();
            groupProvider.findAllGroupNames().stream()
                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private SuggestionProvider<CommandSource> getUsernames() {
        return (_, builder) -> {
            String prefix = builder.getRemaining();
            userProvider.findUsernames().stream()
                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
