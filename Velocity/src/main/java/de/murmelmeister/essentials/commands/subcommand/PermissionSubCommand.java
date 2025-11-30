package de.murmelmeister.essentials.commands.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

public final class PermissionSubCommand extends PermissionUtil {
    private final GroupParentProvider groupParentProvider;
    private final GroupPermissionProvider groupPermissionProvider;
    private final UserParentProvider userParentProvider;
    private final UserPermissionProvider userPermissionProvider;

    private static final Pattern VALID_PERMISSION = Pattern.compile("^[a-zA-Z0-9_.*-]+(\\.[a-zA-Z0-9_.*-]+)*$");

    public PermissionSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupParentProvider = plugin.getGroupParentProvider();
        this.groupPermissionProvider = plugin.getGroupPermissionProvider();
        this.userParentProvider = plugin.getUserParentProvider();
        this.userPermissionProvider = plugin.getUserPermissionProvider();
    }

    @Override
    public BrigadierCommand createCommand() {
        return null;
    }

    public int getPermissions(CommandContext<CommandSource> context, boolean isUser) {
        return runWithTiming(context, (source, executor) -> {
            if (isUser) {
                // -/permission user <username> permission
                String inputUser = StringArgumentType.getString(context, "username");
                int languageId = executor.languageId();
                User user = getUser(inputUser);

                List<UserPermission> permissions = userPermissionProvider.getPermissions(user.id());
                if (permissions.isEmpty())
                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_LIST_EMPTY, tagParsed("username", user.username()));

                Message headerName = permissions.size() == 1
                        ? Message.PERMISSION_LIST_SINGULAR_PERMISSION
                        : Message.PERMISSION_LIST_PLURAL_PERMISSION;
                sendMessage(source, languageId, Message.PERMISSION_USER_LIST_HEADER,
                        tagParsed("header_name", languageId, headerName),
                        tagParsed("username", user.username()),
                        tagParsed("user_id", user.id())
                );

                String clickMessage = "/permission user " + user.username() + " permission remove ";
                permissions.forEach(userPermission -> sendPermissionMessage(source, clickMessage, languageId, userPermission.permission(), userPermission.expiresAt()));
                return CommandResult.of(Command.SINGLE_SUCCESS);
            } else {
                // -/permission group <groupName> permission
                String inputGroup = StringArgumentType.getString(context, "groupName");
                int languageId = executor.languageId();
                Group group = getGroup(inputGroup);

                List<GroupPermission> permissions = groupPermissionProvider.getPermissions(group.id());
                if (permissions.isEmpty())
                    throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_LIST_EMPTY, tagParsed("group_name", group.groupName()));

                Message headerName = permissions.size() == 1 ? Message.PERMISSION_LIST_SINGULAR_PERMISSION
                        : Message.PERMISSION_LIST_PLURAL_PERMISSION;
                sendMessage(source, languageId, Message.PERMISSION_GROUP_LIST_HEADER,
                        tagParsed("header_name", languageId, headerName),
                        tagParsed("group_name", group.groupName()),
                        tagParsed("group_id", group.id())
                );

                String clickMessage = "/permission group " + group.groupName() + " permission remove ";
                permissions.forEach(groupPermission -> sendPermissionMessage(source, clickMessage, executor.languageId(), groupPermission.permission(), groupPermission.expiresAt()));
                return CommandResult.of(Command.SINGLE_SUCCESS);
            }
        });
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionAll(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("all")
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            if (isUser) {
                                // -/permission user <username> permission all
                                String inputUser = StringArgumentType.getString(context, "username");
                                int languageId = executor.languageId();
                                User user = getUser(inputUser);

                                //List<String> permissions = permissionProvider.getPermissions(user.getId());
                                List<UserPermission> permissions = userPermissionProvider.getPermissions(user.id());
                                List<GroupPermission> groupPermissions = getAllGroupPermissionsForUser(user.id());
                                if (permissions.isEmpty() && groupPermissions.isEmpty())
                                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_LIST_EMPTY, tagParsed("username", user.username()));

                                Message headerName = permissions.size() == 1 ? Message.PERMISSION_LIST_SINGULAR_PERMISSION
                                        : Message.PERMISSION_LIST_PLURAL_PERMISSION;
                                sendMessage(source, languageId, Message.PERMISSION_USER_LIST_HEADER,
                                        tagParsed("header_name", languageId, headerName),
                                        tagParsed("username", user.username()),
                                        tagParsed("user_id", user.id())
                                );

                                sendAllGroupPermission(source, groupPermissions, languageId, null, true);

                                String clickMessage = "/permission user " + user.username() + " permission remove ";
                                permissions.forEach(userPermission -> sendPermissionMessage(source, clickMessage, languageId, userPermission.permission(), userPermission.expiresAt()));
                                return CommandResult.of(Command.SINGLE_SUCCESS);
                            } else {
                                // -/permission group <groupName> permission all
                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                int languageId = executor.languageId();
                                Group group = getGroup(inputGroup);

                                List<GroupPermission> permissions = getAllGroupPermissionsForGroup(group.id());
                                if (permissions.isEmpty())
                                    throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_LIST_EMPTY, tagParsed("group_name", group.groupName()));

                                Message headerName = permissions.size() == 1 ? Message.PERMISSION_LIST_SINGULAR_PERMISSION
                                        : Message.PERMISSION_LIST_PLURAL_PERMISSION;
                                sendMessage(source, languageId, Message.PERMISSION_GROUP_LIST_HEADER,
                                        tagParsed("header_name", languageId, headerName),
                                        tagParsed("group_name", group.groupName()),
                                        tagParsed("group_id", group.id())
                                );

                                sendAllGroupPermission(source, permissions, executor.languageId(), group.id(), false);
                                return CommandResult.of(Command.SINGLE_SUCCESS);
                            }
                        })
                );
    }

    private void sendPermissionMessage(CommandSource source, String clickMessage, int executorLang, String permission, LocalDateTime expiredAt) {
        sendMessage(source, executorLang, Message.PERMISSION_LIST_PERMISSION_MESSAGE,
                tagParsed("permission", permission),
                tagParsed("click_command", clickMessage + (permission.equals("*") || permission.endsWith(".*") ? "\"" + permission + "\"" : permission)),
                tagParsed("group", ""),
                Placeholder.component("expired", formatExpiredMessage(executorLang, expiredAt))
        );
    }

    private void sendAllGroupPermission(CommandSource source, List<GroupPermission> permissions, int executorLang, Integer groupId, boolean isUser) {
        permissions.forEach(groupPermission -> {
            Group group = getGroup(groupPermission.groupId());
            String permission = groupPermission.permission();
            String clickMessage = "/permission group " + group.groupName() + " permission remove ";
            Component existGroup = (groupId == null && !isUser) || (groupId != null && groupId == group.id()) ? Component.empty()
                    : component(executorLang, Message.PERMISSION_LIST_FROM_GROUP_PART, tagParsed("group_name", group.groupName()));
            sendMessage(source, executorLang, Message.PERMISSION_LIST_PERMISSION_MESSAGE,
                    tagParsed("permission", permission),
                    tagParsed("click_command", clickMessage + (permission.equals("*") || permission.endsWith(".*") ? "\"" + permission + "\"" : permission)),
                    Placeholder.component("group", existGroup),
                    Placeholder.component("expired", formatExpiredMessage(executorLang, groupPermission.expiresAt()))
            );
        });
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionAdd(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> permission add <permission>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        UserPermission existing = userPermissionProvider.getPermission(user.id(), permission);
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_USER_PERMISSION_EXISTS,
                                                    tagParsed("permission", existing.permission()),
                                                    tagParsed("user", user.username())
                                            );

                                        UserPermission success = userPermissionProvider.add(user.id(), permission, -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(Message.PERMISSION_USER_PERMISSION_ADD_FAILED,
                                                    tagParsed("permission", permission),
                                                    tagParsed("user", user.username())
                                            );
                                        sendMessage(source, languageId, Message.PERMISSION_USER_PERMISSION_ADD_SUCCESS,
                                                tagParsed("permission", success.permission()),
                                                tagParsed("user", user.username()),
                                                Placeholder.component("expired", formatExpiredMessage(languageId, success.expiresAt()))
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                    } else {
                                        // -/permission group <groupName> permission add <permission>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        GroupPermission existing = groupPermissionProvider.getPermission(group.id(), permission);
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_EXISTS,
                                                    tagParsed("permission", existing.permission()),
                                                    tagParsed("group", group.groupName())
                                            );

                                        GroupPermission success = groupPermissionProvider.add(group.id(), permission, -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_ADD_FAILED,
                                                    tagParsed("permission", permission),
                                                    tagParsed("group", group.groupName())
                                            );
                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_PERMISSION_ADD_SUCCESS,
                                                tagParsed("permission", success.permission()),
                                                tagParsed("group", group.groupName()),
                                                Placeholder.component("expired", formatExpiredMessage(languageId, success.expiresAt()))
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                    }
                                })
                        )
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            if (isUser) {
                                                // -/permission user <username> permission add <permission> <time>
                                                String inputUser = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(inputUser);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                UserPermission existing = userPermissionProvider.getPermission(user.id(), permission);
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_EXISTS,
                                                            tagParsed("permission", existing.permission()),
                                                            tagParsed("user", user.username())
                                                    );

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                UserPermission success = userPermissionProvider.add(user.id(), permission, duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_ADD_FAILED,
                                                            tagParsed("permission", permission),
                                                            tagParsed("user", user.username())
                                                    );
                                                sendMessage(source, languageId, Message.PERMISSION_USER_PERMISSION_ADD_SUCCESS,
                                                        tagParsed("permission", success.permission()),
                                                        tagParsed("user", user.username()),
                                                        Placeholder.component("expired", formatExpiredMessage(languageId, success.expiresAt()))
                                                );
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                // -/permission group <groupName> permission add <permission> <time>
                                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(inputGroup);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                GroupPermission existing = groupPermissionProvider.getPermission(group.id(), permission);
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_EXISTS,
                                                            tagParsed("permission", existing.permission()),
                                                            tagParsed("group", group.groupName())
                                                    );

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                GroupPermission success = groupPermissionProvider.add(group.id(), permission, duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_ADD_FAILED,
                                                            tagParsed("permission", permission),
                                                            tagParsed("group", group.groupName())
                                                    );
                                                sendMessage(source, languageId, Message.PERMISSION_GROUP_PERMISSION_ADD_SUCCESS,
                                                        tagParsed("permission", success.permission()),
                                                        tagParsed("group", group.groupName()),
                                                        Placeholder.component("expired", formatExpiredMessage(languageId, success.expiresAt()))
                                                );
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            }
                                        })
                                )
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionRemove(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("remove")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> permission remove <permission>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        UserPermission userPermission = getUserPermission(user, permission);
                                        int result = userPermissionProvider.remove(userPermission.userId(), userPermission.permission());
                                        if (result < 1)
                                            throw new CommandException(Message.PERMISSION_USER_PERMISSION_REMOVE_FAILED,
                                                    tagParsed("permission", permission),
                                                    tagParsed("user", user.username())
                                            );
                                        sendMessage(source, languageId, Message.PERMISSION_USER_PERMISSION_REMOVE_SUCCESS,
                                                tagParsed("permission", userPermission.permission()),
                                                tagParsed("user", user.username())
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS, result);
                                    } else {
                                        // -/permission group <groupName> permission remove <permission>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        GroupPermission groupPermission = getGroupPermission(group, permission);
                                        int result = groupPermissionProvider.remove(groupPermission.groupId(), groupPermission.permission());
                                        if (result < 1)
                                            throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_REMOVE_FAILED,
                                                    tagParsed("permission", permission),
                                                    tagParsed("group", group.groupName())
                                            );
                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_PERMISSION_REMOVE_SUCCESS,
                                                tagParsed("permission", groupPermission.permission()),
                                                tagParsed("group", group.groupName())
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS, result);
                                    }
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionClear(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("clear")
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            if (isUser) {
                                // -/permission user <username> permission clear
                                String inputUser = StringArgumentType.getString(context, "username");
                                int languageId = executor.languageId();
                                User user = getUser(inputUser);

                                int result = userPermissionProvider.clear(user.id());
                                if (result < 1)
                                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_CLEAR_FAILED, tagParsed("user", user.username()));
                                sendMessage(source, languageId, Message.PERMISSION_USER_PERMISSION_CLEAR_SUCCESS, tagParsed("user", user.username()));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            } else {
                                // -/permission group <groupName> permission clear
                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                int languageId = executor.languageId();
                                Group group = getGroup(inputGroup);

                                int result = groupPermissionProvider.clear(group.id());
                                if (result < 1)
                                    throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_CLEAR_FAILED, tagParsed("group", group.groupName()));
                                sendMessage(source, languageId, Message.PERMISSION_GROUP_PERMISSION_CLEAR_SUCCESS, tagParsed("group", group.groupName()));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            }
                        })
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionInfo(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> permission info <permission>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        UserPermission userPermission = getUserPermission(user, permission);
                                        User creator = getUser(userPermission.createdBy());
                                        User changer = userPermission.changedBy() == null ? null : getUser(userPermission.changedBy());
                                        String createdDate = userPermission.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = userPermission.changedAt() == null ? null : userPermission.changedAt().format(getDateTimeFormatter(languageId));
                                        Component expires = formatExpiredInfoMessage(languageId, userPermission.expiresAt());

                                        Component changedText = (changer == null || changedDate == null) ? Component.empty() :
                                                component(languageId, Message.PERMISSION_INFO_CHANGE_STUFF,
                                                        tagParsed("changed_name", changer.username()),
                                                        tagParsed("changed_id", changer.id()),
                                                        tagParsed("changed_at", changedDate)
                                                );

                                        sendMessage(source, languageId, Message.PERMISSION_USER_PERMISSION_INFO,
                                                tagParsed("username", user.username()),
                                                tagParsed("user_id", user.id()),
                                                tagParsed("permission", userPermission.permission()),
                                                Placeholder.component("expires", expires),
                                                tagParsed("created_name", creator.username()),
                                                tagParsed("created_id", creator.id()),
                                                tagParsed("created_at", createdDate),
                                                Placeholder.component("changed", changedText)
                                        ); // .trim()
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    } else {
                                        // -/permission group <groupName> permission info <permission>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        GroupPermission groupPermission = getGroupPermission(group, permission);
                                        User creator = getUser(groupPermission.createdBy());
                                        User changer = groupPermission.changedBy() == null ? null : getUser(groupPermission.changedBy());
                                        String createdDate = groupPermission.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = groupPermission.changedAt() == null ? null : groupPermission.changedAt().format(getDateTimeFormatter(languageId));
                                        Component expires = formatExpiredInfoMessage(languageId, groupPermission.expiresAt());

                                        Component changedText = (changer == null || changedDate == null) ? Component.empty() :
                                                component(languageId, Message.PERMISSION_INFO_CHANGE_STUFF,
                                                        tagParsed("changed_name", changer.username()),
                                                        tagParsed("changed_id", changer.id()),
                                                        tagParsed("changed_at", changedDate)
                                                );

                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_PERMISSION_INFO,
                                                tagParsed("group_name", group.groupName()),
                                                tagParsed("group_id", group.id()),
                                                tagParsed("permission", groupPermission.permission()),
                                                Placeholder.component("expires", expires),
                                                tagParsed("created_name", creator.username()),
                                                tagParsed("created_id", creator.id()),
                                                tagParsed("created_at", createdDate),
                                                Placeholder.component("changed", changedText)
                                        ); // .trim()
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    }
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionTime(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .executes(context -> {
                            sendRawMessage(context.getSource(), syntaxPermission(isUser));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            if (isUser) {
                                                // -/permission user <username> permission time <permission> <time>
                                                String inputUser = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(inputUser);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                UserPermission userPermission = getUserPermission(user, permission);
                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                UserPermission success = userPermissionProvider.update(userPermission.userId(), userPermission.permission(),
                                                        duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_TIME_FAILED,
                                                            tagParsed("permission", permission),
                                                            tagParsed("user", user.username())
                                                    );
                                                sendMessage(source, languageId, Message.PERMISSION_USER_PERMISSION_TIME_SUCCESS,
                                                        tagParsed("permission", userPermission.permission()),
                                                        tagParsed("user", user.username()),
                                                        Placeholder.component("expired", formatExpiredInfoMessage(languageId, success.expiresAt()))
                                                );
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                // -/permission group <groupName> permission time <permission> <time>
                                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(inputGroup);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                GroupPermission groupPermission = getGroupPermission(group, permission);
                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                GroupPermission success = groupPermissionProvider.update(groupPermission.groupId(), groupPermission.permission(),
                                                        duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_TIME_FAILED,
                                                            tagParsed("permission", permission),
                                                            tagParsed("group", group.groupName())
                                                    );
                                                sendMessage(source, languageId, Message.PERMISSION_GROUP_PERMISSION_TIME_SUCCESS,
                                                        tagParsed("permission", groupPermission.permission()),
                                                        tagParsed("group", group.groupName()),
                                                        Placeholder.component("expired", formatExpiredInfoMessage(languageId, success.expiresAt()))
                                                );
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            }
                                        })
                                )
                        )
                );
    }

    private SuggestionProvider<CommandSource> getSuggestionPermission(boolean isUser) {
        return (context, builder) -> {
            String prefix = builder.getRemaining();
            int id = getId(context, isUser);
            List<String> permissions = isUser ? userPermissionProvider.getPermissions(id)
                    .stream().map(UserPermission::permission).toList()
                    : groupPermissionProvider.getPermissions(id)
                    .stream().map(GroupPermission::permission).toList();

            if (permissions.isEmpty())
                return Suggestions.empty();

            // TODO: When UserPermission & GroupPermission are merged, then add to tooltip the expires time
            permissions.stream()
                    .map(permission -> {
                        if ("*".equals(permission) || permission.endsWith(".*"))
                            return "\"" + permission + "\"";
                        return permission;
                    })
                    .filter(permission -> permission.startsWith(prefix))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private int getId(CommandContext<CommandSource> context, boolean isUser) {
        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        return isUser ? getUser(name).id() : getGroup(name).id();
    }

    private UserPermission getUserPermission(User user, String permission) {
        UserPermission userPermission = userPermissionProvider.getPermission(user.id(), permission);
        if (userPermission == null)
            throw new CommandException(Message.PERMISSION_USER_PERMISSION_NOT_EXISTS,
                    tagUnparsed("permission", permission),
                    tagParsed("user", user.username())
            );
        return userPermission;
    }

    private GroupPermission getGroupPermission(Group group, String permission) {
        GroupPermission groupPermission = groupPermissionProvider.getPermission(group.id(), permission);
        if (groupPermission == null)
            throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_NOT_EXISTS,
                    tagUnparsed("permission", permission),
                    tagParsed("group", group.groupName())
            );
        return groupPermission;
    }

    private List<GroupPermission> getAllGroupPermissionsForUser(int userId) {
        Set<Integer> visited = new HashSet<>();
        Map<String, GroupPermission> permissionMap = new LinkedHashMap<>();
        userParentProvider.getParents(userId).forEach(parent -> collectPermissionRec(parent.parentId(), visited, permissionMap));
        return new ArrayList<>(permissionMap.values());
    }

    private List<GroupPermission> getAllGroupPermissionsForGroup(int groupId) {
        Set<Integer> visited = new HashSet<>();
        Map<String, GroupPermission> permissionMap = new LinkedHashMap<>();
        collectPermissionRec(groupId, visited, permissionMap);
        return new ArrayList<>(permissionMap.values());
    }

    private void collectPermissionRec(int groupId, Set<Integer> visited, Map<String, GroupPermission> permissionMap) {
        if (!visited.add(groupId)) return;
        groupPermissionProvider.getPermissions(groupId).forEach(permission -> permissionMap.putIfAbsent(permission.permission(), permission));
        groupParentProvider.getParents(groupId).forEach(parent -> collectPermissionRec(parent.parentId(), visited, permissionMap));
    }
}
