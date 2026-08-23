package de.murmelmeister.essentials.commands.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.permission.PermissionService;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

public final class PermissionSubCommand extends PermissionUtil {
    private final PermissionProvider permissionProvider;
    private final PermissionService permissionService;

    private static final Pattern VALID_PERMISSION = Pattern.compile("^[a-zA-Z0-9_.*-]+(\\.[a-zA-Z0-9_.*-]+)*$");

    public PermissionSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.permissionProvider = plugin.getPermissionProvider();
        this.permissionService = plugin.getPermissionService();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return null;
    }

    public int executeGetPermissions(CommandContext<CommandSource> context, boolean isUser, int page, boolean isAll) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            PermissionTarget target;
            String commandBase;
            int targetId;
            String targetName;

            if (isUser) {
                // -/permission user <username> permission
                String inputUser = StringArgumentType.getString(context, "username");
                User user = getUser(inputUser);
                target = PermissionTarget.user(user.id());
                commandBase = "permission user " + user.username() + " permission";
                targetId = user.id();
                targetName = user.username();
            } else {
                // -/permission group <groupName> permission
                String inputGroup = StringArgumentType.getString(context, "groupName");
                Group group = getGroup(inputGroup);
                target = PermissionTarget.group(group.id());
                commandBase = "permission group " + group.groupName() + " permission";
                targetId = group.id();
                targetName = group.groupName();
            }

            List<Permission> permissions = isAll
                    ? permissionService.getPermissions(target)
                    : permissionProvider.findPermissions(target);
            if (permissions.isEmpty())
                throw new CommandException(isUser ? Message.PERMISSION_USER_PERMISSION_LIST_EMPTY : Message.PERMISSION_GROUP_PERMISSION_LIST_EMPTY,
                        tagParsed(isUser ? "username" : "group_name", getTargetName(target, isUser))
                );

            if (isAll)
                commandBase += " all";

            Message headerName = permissions.size() == 1
                    ? Message.PERMISSION_LIST_SINGULAR_PERMISSION
                    : Message.PERMISSION_LIST_PLURAL_PERMISSION;
            sendMessage(source, languageId,
                    isUser ? Message.PERMISSION_USER_LIST_HEADER : Message.PERMISSION_GROUP_LIST_HEADER,
                    tagParsed("header_name", languageId, headerName),
                    tagParsed(isUser ? "username" : "group_name", targetName),
                    tagParsed(isUser ? "user_id" : "group_id", targetId)
            );

            List<Component> permissionComponents = permissions.stream()
                    .map(permission -> {
                        String perm = permission.permission();
                        String clickMessage;
                        Component existGroup = Component.empty();
                        Integer groupId = permission.groupId();

                        if (isAll) {
                            if (groupId != null) {
                                Group group = getGroup(groupId);
                                clickMessage = "/permission group " + group.groupName() + " permission remove ";

                                boolean showGroup = (target.type() != PermissionTarget.TargetType.GROUP) || (groupId != target.id());
                                if (showGroup)
                                    existGroup = component(languageId, Message.PERMISSION_LIST_FROM_GROUP_PART, tagParsed("group_name", group.groupName()));
                            } else {
                                User user = getUser(target.id());
                                clickMessage = "/permission user " + user.username() + " permission remove ";
                            }
                        } else {
                            clickMessage = "/permission " + (isUser ? "user" : "group") + " " + targetName + " permission remove ";
                        }

                        return component(languageId, Message.PERMISSION_LIST_PERMISSION_MESSAGE,
                                tagParsed("permission", perm),
                                tagParsed("click_command", clickMessage + (perm.equals("*") || perm.endsWith(".*") ? "\"" + perm + "\"" : perm)),
                                Placeholder.component("group", existGroup),
                                Placeholder.component("expired", formatExpiredMessage(languageId, permission.expiresAt()))
                        );
                    })
                    .toList();

            sendPagedMessage(source, permissionComponents, commandBase, page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionAll(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("all")
                .executes(context ->
                        executeGetPermissions(context, isUser, 1, true)
                )
                .then(BrigadierCommand.requiredArgumentBuilder("page", IntegerArgumentType.integer(1))
                        .executes(context ->
                                executeGetPermissions(context, isUser, IntegerArgumentType.getInteger(context, "page"), true)
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionAdd(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> permission add <permission>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);
                                        PermissionTarget target = PermissionTarget.user(user.id());

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        Permission existing = permissionProvider.findPermission(target, permission).orElse(null);
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_USER_PERMISSION_EXISTS,
                                                    tagParsed("permission", existing.permission()),
                                                    tagParsed("user", user.username())
                                            );

                                        Permission success = permissionProvider.upsert(target, permission, -1, executor.id())
                                                .orElseThrow(() ->
                                                        new CommandException(Message.PERMISSION_USER_PERMISSION_ADD_FAILED,
                                                                tagParsed("permission", permission),
                                                                tagParsed("user", user.username())
                                                        )
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
                                        PermissionTarget target = PermissionTarget.group(group.id());

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        Permission existing = permissionProvider.findPermission(target, permission).orElse(null);
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_EXISTS,
                                                    tagParsed("permission", existing.permission()),
                                                    tagParsed("group", group.groupName())
                                            );

                                        Permission success = permissionProvider.upsert(target, permission, -1, executor.id())
                                                .orElseThrow(() ->
                                                        new CommandException(Message.PERMISSION_GROUP_PERMISSION_ADD_FAILED,
                                                                tagParsed("permission", permission),
                                                                tagParsed("group", group.groupName())
                                                        )
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
                                                PermissionTarget target = PermissionTarget.user(user.id());

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                Permission existing = permissionProvider.findPermission(target, permission).orElse(null);
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_EXISTS,
                                                            tagParsed("permission", existing.permission()),
                                                            tagParsed("user", user.username())
                                                    );

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Permission success = permissionProvider.upsert(target, permission, duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_USER_PERMISSION_ADD_FAILED,
                                                                        tagParsed("permission", permission),
                                                                        tagParsed("user", user.username())
                                                                )
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
                                                PermissionTarget target = PermissionTarget.group(group.id());

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                Permission existing = permissionProvider.findPermission(target, permission).orElse(null);
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PERMISSION_EXISTS,
                                                            tagParsed("permission", existing.permission()),
                                                            tagParsed("group", group.groupName())
                                                    );

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Permission success = permissionProvider.upsert(target, permission, duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_GROUP_PERMISSION_ADD_FAILED,
                                                                        tagParsed("permission", permission),
                                                                        tagParsed("group", group.groupName())
                                                                )
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
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> permission remove <permission>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);
                                        PermissionTarget target = PermissionTarget.user(user.id());

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        Permission userPermission = getUserPermission(user, permission);
                                        int result = permissionProvider.remove(target, userPermission.permission());
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
                                        PermissionTarget target = PermissionTarget.group(group.id());

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        Permission groupPermission = getGroupPermission(group, permission);
                                        int result = permissionProvider.remove(target, groupPermission.permission());
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
                                PermissionTarget target = PermissionTarget.user(user.id());

                                int result = permissionProvider.clear(target);
                                if (result < 1)
                                    throw new CommandException(Message.PERMISSION_USER_PERMISSION_CLEAR_FAILED, tagParsed("user", user.username()));

                                sendMessage(source, languageId, Message.PERMISSION_USER_PERMISSION_CLEAR_SUCCESS, tagParsed("user", user.username()));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            } else {
                                // -/permission group <groupName> permission clear
                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                int languageId = executor.languageId();
                                Group group = getGroup(inputGroup);
                                PermissionTarget target = PermissionTarget.group(group.id());

                                int result = permissionProvider.clear(target);
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

                                        Permission userPermission = getUserPermission(user, permission);
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
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    } else {
                                        // -/permission group <groupName> permission info <permission>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);

                                        String permission = StringArgumentType.getString(context, "permission");
                                        if (!VALID_PERMISSION.matcher(permission).matches())
                                            throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                        Permission groupPermission = getGroupPermission(group, permission);
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
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    }
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionTime(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests(getSuggestionPermission(isUser))
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            if (isUser) {
                                                // -/permission user <username> permission time <permission> <time>
                                                String inputUser = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(inputUser);
                                                PermissionTarget target = PermissionTarget.user(user.id());

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                Permission userPermission = getUserPermission(user, permission);
                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Permission success = permissionProvider.upsert(target, userPermission.permission(), duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_USER_PERMISSION_TIME_FAILED,
                                                                        tagParsed("permission", permission),
                                                                        tagParsed("user", user.username())
                                                                )
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
                                                PermissionTarget target = PermissionTarget.group(group.id());

                                                String permission = StringArgumentType.getString(context, "permission");
                                                if (!VALID_PERMISSION.matcher(permission).matches())
                                                    throw new CommandException(Message.PERMISSION_NOT_VALID_PERMISSION, tagUnparsed("permission", permission));

                                                Permission groupPermission = getGroupPermission(group, permission);
                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Permission success = permissionProvider.upsert(target, groupPermission.permission(), duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_GROUP_PERMISSION_TIME_FAILED,
                                                                        tagParsed("permission", permission),
                                                                        tagParsed("group", group.groupName())
                                                                )
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
            PermissionTarget target = getTarget(context, isUser);
            List<String> permissions = permissionProvider.findPermissions(target)
                    .stream().map(Permission::permission).toList();

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

    private @NotNull PermissionTarget getTarget(CommandContext<CommandSource> context, boolean isUser) {
        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        return isUser ? PermissionTarget.user(getUser(name).id()) : PermissionTarget.group(getGroup(name).id());
    }

    private String getTargetName(PermissionTarget target, boolean isUser) {
        return isUser ? getUser(target.id()).username() : getGroup(target.id()).groupName();
    }

    private @NotNull Permission getUserPermission(User user, String permission) {
        return permissionProvider.findPermission(PermissionTarget.user(user.id()), permission).orElseThrow(() ->
                new CommandException(Message.PERMISSION_USER_PERMISSION_NOT_EXISTS,
                        tagUnparsed("permission", permission),
                        tagParsed("user", user.username())
                )
        );
    }

    private @NotNull Permission getGroupPermission(Group group, String permission) {
        return permissionProvider.findPermission(PermissionTarget.group(group.id()), permission).orElseThrow(() ->
                new CommandException(Message.PERMISSION_GROUP_PERMISSION_NOT_EXISTS,
                        tagUnparsed("permission", permission),
                        tagParsed("group", group.groupName())
                )
        );
    }
}
