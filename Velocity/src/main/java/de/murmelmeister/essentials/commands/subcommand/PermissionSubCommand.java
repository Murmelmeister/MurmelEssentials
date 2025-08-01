package de.murmelmeister.essentials.commands.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.utils.Messages;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDateTime;
import java.util.*;

public final class PermissionSubCommand extends PermissionUtil {
    private final GroupParentProvider groupParentProvider;
    private final GroupPermissionProvider groupPermissionProvider;
    private final UserParentProvider userParentProvider;
    private final UserPermissionProvider userPermissionProvider;

    private final MessageService messageService;

    public PermissionSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupParentProvider = plugin.getGroupParentProvider();
        this.groupPermissionProvider = plugin.getGroupPermissionProvider();
        this.userParentProvider = plugin.getUserParentProvider();
        this.userPermissionProvider = plugin.getUserPermissionProvider();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public BrigadierCommand createCommand() {
        return null;
    }

    public int getPermissions(CommandContext<CommandSource> context, boolean isUser) {
        return runWithTiming(context, (source, executor) -> {
            if (isUser) {
                String username = StringArgumentType.getString(context, "username");
                int languageId = executor.languageId();
                User user = getUser(languageId, username);

                List<UserPermission> permissions = userPermissionProvider.getPermissions(user.id());
                if (permissions.isEmpty()) {
                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_LIST_EMPTY, languageId)
                            .replace("[USER_NAME]", username));
                    return CommandResult.of(-2);
                }

                String headerName = permissions.size() == 1
                        ? messageService.getMessage(Messages.PERMISSIONS_LIST_SINGULAR, languageId)
                        : messageService.getMessage(Messages.PERMISSIONS_LIST_PLURAL, languageId);
                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_LIST_HEADER, languageId)
                        .replace("[HEADER_NAME]", headerName)
                        .replace("[USER_NAME]", username)
                        .replace("[USER_ID]", String.valueOf(user.id())));
                String clickMessage = "/permission user " + username + " permission remove ";
                permissions.forEach(userPermission -> sendPermissionMessage(source, clickMessage, languageId, userPermission.permission(), userPermission.expiresAt()));
                return CommandResult.of(Command.SINGLE_SUCCESS);
            } else {
                String groupName = StringArgumentType.getString(context, "groupName");
                int languageId = executor.languageId();
                Group group = getGroup(languageId, groupName);

                List<GroupPermission> permissions = groupPermissionProvider.getPermissions(group.id());
                if (permissions.isEmpty()) {
                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_LIST_EMPTY, languageId)
                            .replace("[GROUP_NAME]", groupName));
                    return CommandResult.of(-2);
                }

                String headerName = permissions.size() == 1 ? messageService.getMessage(Messages.PERMISSIONS_LIST_SINGULAR, languageId)
                        : messageService.getMessage(Messages.PERMISSIONS_LIST_PLURAL, languageId);
                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_LIST_HEADER, languageId)
                        .replace("[HEADER_NAME]", headerName)
                        .replace("[GROUP_NAME]", groupName)
                        .replace("[GROUP_ID]", String.valueOf(group.id())));
                String clickMessage = "/permission group " + groupName + " permission remove ";
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
                                String username = StringArgumentType.getString(context, "username");
                                int languageId = executor.languageId();
                                User user = getUser(languageId, username);

                                //List<String> permissions = permissionProvider.getPermissions(user.getId());
                                List<UserPermission> permissions = userPermissionProvider.getPermissions(user.id());
                                List<GroupPermission> groupPermissions = getAllGroupPermissionsForUser(user.id());
                                if (permissions.isEmpty() && groupPermissions.isEmpty()) {
                                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_LIST_EMPTY, languageId)
                                            .replace("[USER_NAME]", username));
                                    return CommandResult.of(-2);
                                }
                                String headerName = permissions.size() == 1 ? messageService.getMessage(Messages.PERMISSIONS_LIST_SINGULAR, languageId)
                                        : messageService.getMessage(Messages.PERMISSIONS_LIST_PLURAL, languageId);
                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_LIST_HEADER, languageId)
                                        .replace("[HEADER_NAME]", headerName)
                                        .replace("[USER_NAME]", username)
                                        .replace("[USER_ID]", String.valueOf(user.id())));
                                sendAllGroupPermission(source, groupPermissions, languageId, null);

                                String clickMessage = "/permission user " + username + " permission remove ";
                                permissions.forEach(userPermission -> sendPermissionMessage(source, clickMessage, languageId, userPermission.permission(), userPermission.expiresAt()));
                                return CommandResult.of(Command.SINGLE_SUCCESS);
                            } else {
                                String groupName = StringArgumentType.getString(context, "groupName");
                                int languageId = executor.languageId();
                                Group group = getGroup(languageId, groupName);

                                List<GroupPermission> permissions = getAllGroupPermissionsForGroup(group.id());
                                if (permissions.isEmpty()) {
                                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_LIST_EMPTY, languageId)
                                            .replace("[GROUP_NAME]", groupName));
                                    return CommandResult.of(-2);
                                }

                                String headerName = permissions.size() == 1 ? messageService.getMessage(Messages.PERMISSIONS_LIST_SINGULAR, languageId)
                                        : messageService.getMessage(Messages.PERMISSIONS_LIST_PLURAL, languageId);
                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_LIST_HEADER, languageId)
                                        .replace("[HEADER_NAME]", headerName)
                                        .replace("[GROUP_NAME]", groupName)
                                        .replace("[GROUP_ID]", String.valueOf(group.id())));
                                sendAllGroupPermission(source, permissions, executor.languageId(), group.id());
                                return CommandResult.of(Command.SINGLE_SUCCESS);
                            }
                        })
                );
    }

    private void sendPermissionMessage(CommandSource source, String clickMessage, int executorLang, String permission, LocalDateTime expiredAt) {
        String expiredMessage = formatExpiredMessage(executorLang, expiredAt);
        sendMessage(source, (messageService.getMessage(Messages.PERMISSION_LIST_MESSAGE, executorLang)
                                     .replace("[PERMISSION]", permission)
                                     .replace("[CLICK_COMMAND]", clickMessage + (permission.equals("*") || permission.endsWith(".*") ? "\"" + permission + "\"" : permission))
                             + " " + expiredMessage).trim());
    }

    private void sendAllGroupPermission(CommandSource source, List<GroupPermission> permissions, int executorLang, Integer groupId) {
        permissions.forEach(groupPermission -> {
            Group group = getGroup(executorLang, groupPermission.groupId());
            String permission = groupPermission.permission();
            String expiredMessage = formatExpiredMessage(executorLang, groupPermission.expiresAt());
            String clickMessage = "/permission group " + group.groupName() + " permission remove ";
            String existGroup = groupId == null || groupId == group.id() ? ""
                    : messageService.getMessage(Messages.PERMISSION_LIST_FROM_GROUP_PART, executorLang)
                    .replace("[GROUP_NAME]", group.groupName());
            sendMessage(source, (messageService.getMessage(Messages.PERMISSION_LIST_MESSAGE, executorLang)
                                         .replace("[PERMISSION]", permission)
                                         .replace("[CLICK_COMMAND]", clickMessage + (permission.equals("*") || permission.endsWith(".*") ? "\"" + permission + "\"" : permission))
                                 + " " + existGroup
                                 + " " + expiredMessage).trim());
        });
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionAdd(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        String username = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(languageId, username);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        UserPermission userPermission = userPermissionProvider.getPermission(user.id(), permission);
                                        if (userPermission != null) {
                                            // Permission already exists
                                            sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_EXISTS, languageId)
                                                    .replace("[PERMISSION]", permission)
                                                    .replace("[USER]", username));
                                            return CommandResult.of(-2);
                                        }

                                        UserPermission success = userPermissionProvider.add(user.id(), permission, -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_ADD_FAILED, languageId)
                                                    .replace("[PERMISSION]", permission)
                                                    .replace("[USER]", username));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_ADD_SUCCESS, languageId)
                                                .replace("[PERMISSION]", permission)
                                                .replace("[USER]", username)
                                                .replace("[EXPIRED]", formatExpiredMessage(languageId, success.expiresAt())));
                                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                    } else {
                                        String groupName = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(languageId, groupName);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        GroupPermission groupPermission = groupPermissionProvider.getPermission(group.id(), permission);
                                        if (groupPermission != null) {
                                            // Permission already exists
                                            sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_EXISTS, languageId)
                                                    .replace("[PERMISSION]", permission)
                                                    .replace("[GROUP]", groupName));
                                            return CommandResult.of(-2);
                                        }

                                        GroupPermission success = groupPermissionProvider.add(group.id(), permission, -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_ADD_FAILED, languageId)
                                                    .replace("[PERMISSION]", permission)
                                                    .replace("[GROUP]", groupName));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_ADD_SUCCESS, languageId)
                                                .replace("[PERMISSION]", permission)
                                                .replace("[GROUP]", groupName)
                                                .replace("[EXPIRED]", formatExpiredMessage(languageId, success.expiresAt())));
                                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                    }
                                })
                        )
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            if (isUser) {
                                                String username = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(languageId, username);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                UserPermission userPermission = userPermissionProvider.getPermission(user.id(), permission);
                                                if (userPermission != null) {
                                                    // Permission already exists
                                                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_EXISTS, languageId)
                                                            .replace("[PERMISSION]", permission)
                                                            .replace("[USER]", username));
                                                    return CommandResult.of(-2);
                                                }

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(languageId, time);

                                                UserPermission success = userPermissionProvider.add(user.id(), permission, duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_ADD_FAILED, languageId)
                                                            .replace("[PERMISSION]", permission)
                                                            .replace("[USER]", username));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_ADD_SUCCESS, languageId)
                                                        .replace("[PERMISSION]", permission)
                                                        .replace("[USER]", username)
                                                        .replace("[EXPIRED]", formatExpiredMessage(languageId, success.expiresAt())));
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                String groupName = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(languageId, groupName);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                GroupPermission groupPermission = groupPermissionProvider.getPermission(group.id(), permission);
                                                if (groupPermission != null) {
                                                    // Permission already exists
                                                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_EXISTS, languageId)
                                                            .replace("[PERMISSION]", permission)
                                                            .replace("[GROUP]", groupName));
                                                    return CommandResult.of(-2);
                                                }

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(languageId, time);

                                                GroupPermission success = groupPermissionProvider.add(group.id(), permission, duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_ADD_FAILED, languageId)
                                                            .replace("[PERMISSION]", permission)
                                                            .replace("[GROUP]", groupName));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_ADD_SUCCESS, languageId)
                                                        .replace("[PERMISSION]", permission)
                                                        .replace("[GROUP]", groupName)
                                                        .replace("[EXPIRED]", formatExpiredMessage(languageId, success.expiresAt())));
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
                    sendMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        String username = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(languageId, username);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        UserPermission userPermission = getUserPermission(languageId, user, permission);

                                        int result = userPermissionProvider.remove(userPermission.userId(), userPermission.permission());
                                        if (result < 1)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_REMOVE_FAILED, languageId)
                                                    .replace("[PERMISSION]", permission)
                                                    .replace("[USER]", username));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_REMOVE_SUCCESS, languageId)
                                                .replace("[PERMISSION]", permission)
                                                .replace("[USER]", username));
                                        return CommandResult.of(Command.SINGLE_SUCCESS, result);
                                    } else {
                                        String groupName = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(languageId, groupName);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        GroupPermission groupPermission = getGroupPermission(languageId, group, permission);

                                        int result = groupPermissionProvider.remove(groupPermission.groupId(), groupPermission.permission());
                                        if (result < 1)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_REMOVE_FAILED, languageId)
                                                    .replace("[PERMISSION]", permission)
                                                    .replace("[GROUP]", groupName));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_REMOVE_SUCCESS, languageId)
                                                .replace("[PERMISSION]", permission)
                                                .replace("[GROUP]", groupName));
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
                                String username = StringArgumentType.getString(context, "username");
                                int languageId = executor.languageId();
                                User user = getUser(languageId, username);

                                int result = userPermissionProvider.clear(user.id());
                                if (result < 1)
                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_CLEAR_FAILED, languageId)
                                            .replace("[USER]", username));
                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_CLEAR_SUCCESS, languageId)
                                        .replace("[USER]", username));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            } else {
                                String groupName = StringArgumentType.getString(context, "groupName");
                                int languageId = executor.languageId();
                                Group group = getGroup(languageId, groupName);

                                int result = groupPermissionProvider.clear(group.id());
                                if (result < 1)
                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_CLEAR_FAILED, languageId)
                                            .replace("[GROUP]", groupName));
                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_CLEAR_SUCCESS, languageId)
                                        .replace("[GROUP]", groupName));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            }
                        })
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionInfo(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        String username = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(languageId, username);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        UserPermission userPermission = getUserPermission(languageId, user, permission);

                                        User creator = getUser(languageId, userPermission.createdBy());
                                        User changer = userPermission.changedBy() == null ? null : getUser(languageId, userPermission.changedBy());
                                        String createdDate = userPermission.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = userPermission.changedAt() == null ? null : userPermission.changedAt().format(getDateTimeFormatter(languageId));
                                        String expiredMessage = formatExpiredInfoMessage(languageId, userPermission.expiresAt());

                                        String changedText = (changer == null || changedDate == null) ? null :
                                                messageService.getMessage(Messages.PERMISSION_INFO_CHANGE_STUFF, languageId)
                                                        .replace("[CHANGED_NAME]", changer.username())
                                                        .replace("[CHANGED_ID]", String.valueOf(changer.id()))
                                                        .replace("[CHANGED_AT]", changedDate);

                                        sendMessage(source, (messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_INFO_MESSAGE, languageId)
                                                .replace("[USER_NAME]", user.username())
                                                .replace("[USER_ID]", String.valueOf(user.id()))
                                                .replace("[PERMISSION]", permission)
                                                .replace("[EXPIRES]", expiredMessage)
                                                .replace("[CREATED_NAME]", creator.username())
                                                .replace("[CREATED_ID]", String.valueOf(creator.id()))
                                                .replace("[CREATED_AT]", createdDate)
                                                .replace("[CHANGED]", changedText == null ? "" : changedText)).trim());
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    } else {
                                        String groupName = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(languageId, groupName);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        GroupPermission groupPermission = getGroupPermission(languageId, group, permission);

                                        User creator = getUser(languageId, groupPermission.createdBy());
                                        User changer = groupPermission.changedBy() == null ? null : getUser(languageId, groupPermission.changedBy());
                                        String createdDate = groupPermission.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = groupPermission.changedAt() == null ? null : groupPermission.changedAt().format(getDateTimeFormatter(languageId));
                                        String expiredMessage = formatExpiredInfoMessage(languageId, groupPermission.expiresAt());

                                        String changedText = (changer == null || changedDate == null) ? null :
                                                messageService.getMessage(Messages.PERMISSION_INFO_CHANGE_STUFF, languageId)
                                                        .replace("[CHANGED_NAME]", changer.username())
                                                        .replace("[CHANGED_ID]", String.valueOf(changer.id()))
                                                        .replace("[CHANGED_AT]", changedDate);

                                        sendMessage(source, (messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_INFO_MESSAGE, languageId)
                                                .replace("[GROUP_NAME]", group.groupName())
                                                .replace("[GROUP_ID]", String.valueOf(group.id()))
                                                .replace("[PERMISSION]", permission)
                                                .replace("[EXPIRES]", expiredMessage)
                                                .replace("[CREATED_NAME]", creator.username())
                                                .replace("[CREATED_ID]", String.valueOf(creator.id()))
                                                .replace("[CREATED_AT]", createdDate)
                                                .replace("[CHANGED]", changedText == null ? "" : changedText)).trim());
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    }
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionTime(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .executes(context -> {
                            sendMessage(context.getSource(), syntaxPermission(isUser));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            if (isUser) {
                                                String username = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(languageId, username);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                UserPermission userPermission = getUserPermission(languageId, user, permission);

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(languageId, time);

                                                UserPermission success = userPermissionProvider.update(userPermission.userId(), userPermission.permission(),
                                                        duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_TIME_FAILED, languageId)
                                                            .replace("[PERMISSION]", permission)
                                                            .replace("[USER]", username));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_TIME_SUCCESS, languageId)
                                                        .replace("[PERMISSION]", permission)
                                                        .replace("[USER]", username)
                                                        .replace("[EXPIRED]", formatExpiredInfoMessage(languageId, success.expiresAt())));
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                String groupName = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(languageId, groupName);

                                                String permission = StringArgumentType.getString(context, "permission");
                                                GroupPermission groupPermission = getGroupPermission(languageId, group, permission);

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(languageId, time);

                                                GroupPermission success = groupPermissionProvider.update(groupPermission.groupId(), groupPermission.permission(),
                                                        duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_TIME_FAILED, languageId)
                                                            .replace("[PERMISSION]", permission)
                                                            .replace("[GROUP]", groupName));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_TIME_SUCCESS, languageId)
                                                        .replace("[PERMISSION]", permission)
                                                        .replace("[GROUP]", groupName)
                                                        .replace("[EXPIRED]", formatExpiredInfoMessage(languageId, success.expiresAt())));
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
            User executor = getExecutor(context.getSource());
            int id = getId(context, executor.languageId(), isUser);
            List<String> permissions = isUser ? userPermissionProvider.getPermissions(id)
                    .stream().map(UserPermission::permission).toList()
                    : groupPermissionProvider.getPermissions(id)
                    .stream().map(GroupPermission::permission).toList();

            if (permissions.isEmpty())
                return builder.buildFuture();

            // TODO: Maybe add expires date in the tooltip
            permissions.stream()
                    .map(permission -> {
                        if ("*".equals(permission) || permission.endsWith(".*"))
                            return "\"" + permission + "\"";
                        return permission;
                    })
                    .filter(permission -> permission.startsWith(prefix))
                    .forEach(permission -> builder.suggest(permission,
                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + permission))));
            return builder.buildFuture();
        };
    }

    private int getId(CommandContext<CommandSource> context, int languageId, boolean isUser) {
        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        return isUser ? getUser(languageId, name).id() : getGroup(languageId, name).id();
    }

    private UserPermission getUserPermission(int executorLang, User user, String permission) {
        UserPermission userPermission = userPermissionProvider.getPermission(user.id(), permission);
        if (userPermission == null)
            throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PERMISSION_NOT_EXISTS, executorLang)
                    .replace("[PERMISSION]", permission)
                    .replace("[USER]", user.username()));
        return userPermission;
    }

    private GroupPermission getGroupPermission(int executorLang, Group group, String permission) {
        GroupPermission groupPermission = groupPermissionProvider.getPermission(group.id(), permission);
        if (groupPermission == null)
            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PERMISSION_NOT_EXISTS, executorLang)
                    .replace("[PERMISSION]", permission)
                    .replace("[GROUP]", group.groupName()));
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
