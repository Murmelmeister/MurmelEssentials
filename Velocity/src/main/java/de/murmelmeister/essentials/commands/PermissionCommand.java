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
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
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
import de.murmelmeister.library.utils.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;

public final class PermissionCommand extends PermissionUtil {
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
                    sendRawMessage(context.getSource(), syntax());
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
                            int languageId = executor.languageId();
                            List<String> groupNames = groupProvider.findAllGroupNames();

                            if (groupNames.isEmpty()) {
                                sendMessage(source, languageId, Message.PERMISSION_LIST_GROUP_EMPTY);
                                return CommandResult.of(-2);
                            }

                            Message headerName = groupNames.size() == 1
                                    ? Message.PERMISSION_LIST_SINGULAR_GROUP
                                    : Message.PERMISSION_LIST_PLURAL_GROUP;
                            sendMessage(source, languageId, Message.PERMISSION_LIST_GROUP_HEADER, tagParsed("header_name", languageId, headerName));
                            groupNames.forEach(name -> {
                                String clickMessage = "/permission group " + name + " info";
                                sendMessage(source, languageId, Message.PERMISSION_LIST_GROUP_MESSAGE,
                                        tagParsed("click_command", clickMessage),
                                        tagParsed("group_name", name)
                                ); // .trim()
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupCommand() {
        // -/permission group <groupName> ...
        return BrigadierCommand.literalArgumentBuilder("group")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("groupName", StringArgumentType.word())
                        .suggests(getGroupNames())
                        .executes(context -> {
                            sendRawMessage(context.getSource(), syntaxGroup());
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
                            String inputGroup = StringArgumentType.getString(context, "groupName");
                            Group group = getGroup(inputGroup);

                            int groupId = group.id();
                            User creator = getUser(group.createdBy());
                            User changer = group.changedBy() == null ? null : getUser(group.changedBy());
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
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("priority", IntegerArgumentType.integer(1))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String inputGroup = StringArgumentType.getString(context, "groupName");
                                    int inputPriority = IntegerArgumentType.getInteger(context, "priority");

                                    Group group = groupProvider.findByName(inputGroup);
                                    if (group != null) {
                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_EXISTS, tagParsed("group_name", group.groupName()));
                                        return CommandResult.of(-2);
                                    }

                                    Group success = groupProvider.create(inputGroup, inputPriority, executor.id());
                                    if (success == null)
                                        throw new CommandException(Message.PERMISSION_GROUP_CREATE_FAILED,
                                                tagUnparsed("group_name", inputGroup),
                                                tagUnparsed("priority", inputPriority)
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

                            if (groupId == getDefaultGroup().id()) {
                                sendMessage(source, languageId, Message.PERMISSION_DEFAULT_GROUP_DELETE);
                                return CommandResult.of(-2);
                            }

                            int result = 0;
                            result += groupPermissionProvider.clear(groupId);
                            result += groupParentProvider.clear(groupId);
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
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxGroup());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("newName", StringArgumentType.word())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    int languageId = executor.languageId();
                                    String inputGroup = StringArgumentType.getString(context, "groupName");
                                    Group group = getGroup(inputGroup);

                                    if (group.id() == getDefaultGroup().id()) {
                                        sendMessage(source, languageId, Message.PERMISSION_DEFAULT_GROUP_RENAME);
                                        return CommandResult.of(-2);
                                    }

                                    String inputNewGroup = StringArgumentType.getString(context, "newName");
                                    if (group.groupName().equals(inputNewGroup) || groupProvider.findByName(inputNewGroup) != null) {
                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_EXISTS, tagParsed("group_name", group.groupName()));
                                        return CommandResult.of(-3);
                                    }

                                    Group success = groupProvider.update(group.id(), inputNewGroup, group.priority(), executor.id());
                                    if (success == null)
                                        throw new CommandException(Message.PERMISSION_GROUP_RENAME_FAILED,
                                                tagUnparsed("group_name", inputGroup),
                                                tagUnparsed("new_group_name", inputNewGroup)
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
                    sendRawMessage(context.getSource(), syntaxGroupEdit());
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
                            int languageId = executor.languageId();
                            List<String> usernames = userProvider.findUsernames(); // You can also use userProvider.findAll() to get User objects

                            if (usernames.isEmpty()) {
                                sendMessage(source, languageId, Message.PERMISSION_LIST_USER_EMPTY);
                                return CommandResult.of(-2);
                            }

                            Message headerName = usernames.size() == 1
                                    ? Message.PERMISSION_LIST_SINGULAR_USER
                                    : Message.PERMISSION_LIST_PLURAL_USER;
                            sendMessage(source, languageId, Message.PERMISSION_LIST_USER_HEADER, tagParsed("header_name", languageId, headerName));
                            usernames.forEach(username -> {
                                String clickMessage = "/permission user " + username + " info";
                                sendMessage(source, languageId, Message.PERMISSION_LIST_USER_MESSAGE,
                                        tagParsed("click_command", clickMessage),
                                        tagParsed("username", username)
                                );
                            });
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUserCommand() {
        // -/permission user <username> ...
        return BrigadierCommand.literalArgumentBuilder("user")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxUser());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("username", StringArgumentType.word())
                        .suggests(getUsernames())
                        .executes(context -> {
                            sendRawMessage(context.getSource(), syntaxUser());
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
                            int languageId = executor.languageId();
                            String inputUser = StringArgumentType.getString(context, "username");
                            User user = getUser(inputUser);

                            String firstJoinDate = user.firstLogin().format(getDateTimeFormatter(languageId));

                            Message yes = Message.MESSAGE_YES,
                                    no = Message.MESSAGE_NO;

                            Message isDebugUser = user.debugUser() ? yes : no;
                            Message isDebugMode = user.debugEnabled() ? yes : no;

                            sendMessage(source, languageId, Message.PERMISSION_USER_INFO,
                                    tagParsed("username", user.username()),
                                    tagParsed("user_id", user.id()),
                                    tagParsed("mojang_id", user.mojangId().toString()),
                                    tagParsed("first_join", firstJoinDate),
                                    tagParsed("debug_user", languageId, isDebugUser),
                                    tagParsed("debug_mode", languageId, isDebugMode)
                            );
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
