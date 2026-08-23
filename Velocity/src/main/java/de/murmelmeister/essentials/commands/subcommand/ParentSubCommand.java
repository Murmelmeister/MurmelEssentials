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
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
import de.murmelmeister.murmelapi.permission.parent.Parent;
import de.murmelmeister.murmelapi.permission.parent.ParentProvider;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ParentSubCommand extends PermissionUtil {
    private final GroupProvider groupProvider;
    private final ParentProvider parentProvider;

    public ParentSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupProvider = plugin.getGroupProvider();
        this.parentProvider = plugin.getParentProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return null;
    }

    public int executeGetParents(CommandContext<CommandSource> context, boolean isUser, int page) {
        return runWithTiming(context, (source, executor) -> {
            int languageId = executor.languageId();
            PermissionTarget target;
            String commandBase;
            int targetId;
            String targetName;

            if (isUser) {
                // -/permission user <username> parent
                String inputUser = StringArgumentType.getString(context, "username");
                User user = getUser(inputUser);
                target = PermissionTarget.user(user.id());
                commandBase = "permission user " + user.username() + " parent";
                targetId = user.id();
                targetName = user.username();
            } else {
                // -/permission group <groupName> parent
                String inputGroup = StringArgumentType.getString(context, "groupName");
                Group group = getGroup(inputGroup);
                target = PermissionTarget.group(group.id());
                commandBase = "permission group " + group.groupName() + " parent";
                targetId = group.id();
                targetName = group.groupName();
            }

            List<Parent> parents = parentProvider.findParents(target);
            if (parents.isEmpty())
                throw new CommandException(isUser ? Message.PERMISSION_USER_PARENT_LIST_EMPTY : Message.PERMISSION_GROUP_PARENT_LIST_EMPTY,
                        tagParsed(isUser ? "username" : "group_name", getTargetName(target, isUser))
                );

            Message headerName = parents.size() == 1
                    ? Message.PERMISSION_LIST_SINGULAR_PARENT
                    : Message.PERMISSION_LIST_PLURAL_PARENT;
            sendMessage(source, languageId,
                    isUser ? Message.PERMISSION_USER_LIST_HEADER : Message.PERMISSION_GROUP_LIST_HEADER,
                    tagParsed("header_name", languageId, headerName),
                    tagParsed(isUser ? "username" : "group_name", targetName),
                    tagParsed(isUser ? "user_id" : "group_id", targetId)
            );

            List<Component> parentComponents = parents.stream()
                    .map(parent -> {
                        Group group = groupProvider.findById(parent.parentId()).orElse(null);
                        if (group == null) return Component.empty();
                        String name = group.groupName();

                        if (parent.parentId() == getDefaultGroup().id())
                            return component(
                                    languageId,
                                    Message.PERMISSION_DEFAULT_LIST_PARENT,
                                    tagParsed("default_parent", name)
                            );

                        String clickMessage = "/permission " + (isUser ? "user" : "group") + " " + targetName + " parent remove " + name;
                        return component(
                                languageId,
                                Message.PERMISSION_LIST_PARENT_MESSAGE,
                                tagParsed("parent", name),
                                tagParsed("click_command", clickMessage),
                                Placeholder.component("expired", formatExpiredMessage(languageId, parent.expiresAt()))
                        );
                    })
                    .toList();

            sendPagedMessage(source, parentComponents, commandBase, page);
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
    }

    public LiteralArgumentBuilder<CommandSource> getParentAdd(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String prefix = builder.getRemaining();
                            PermissionTarget target = getTarget(context, isUser);
                            List<Integer> haveParents = parentProvider.findParents(target).stream()
                                    .map(Parent::parentId).toList();
                            groupProvider.findAll().stream()
                                    .filter(group -> isUser || group.id() != target.id())
                                    .filter(group -> !haveParents.contains(group.id()))
                                    .filter(group -> StringUtil.startsWithIgnoreCase(group.groupName(), prefix))
                                    .forEach(group -> builder.suggest(group.groupName()));
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> parent add <parent>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);
                                        PermissionTarget target = PermissionTarget.user(user.id());

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        Parent existing = parentProvider.findParent(target, parent.id()).orElse(null);
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_USER_PARENT_EXISTS,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("user", user.username())
                                            );

                                        if (parent.id() == getDefaultGroup().id())
                                            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                        Parent success = parentProvider.upsert(target, parent.id(), -1, executor.id())
                                                .orElseThrow(() ->
                                                        new CommandException(Message.PERMISSION_USER_PARENT_ADD_FAILED,
                                                                tagParsed("parent", parent.groupName()),
                                                                tagParsed("user", user.username())
                                                        )
                                                );

                                        sendMessage(source, languageId, Message.PERMISSION_USER_PARENT_ADD_SUCCESS,
                                                tagParsed("parent", parent.groupName()),
                                                tagParsed("user", user.username()),
                                                Placeholder.component("expired", formatExpiredMessage(languageId, success.expiresAt()))
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                    } else {
                                        // -/permission group <groupName> parent add <parent>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);
                                        PermissionTarget target = PermissionTarget.group(group.id());

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        Parent existing = parentProvider.findParent(target, parent.id()).orElse(null);
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_GROUP_PARENT_EXISTS,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("group", group.groupName())
                                            );

                                        if (parent.id() == getDefaultGroup().id())
                                            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                        Parent success = parentProvider.upsert(target, parent.id(), -1, executor.id())
                                                .orElseThrow(() ->
                                                        new CommandException(Message.PERMISSION_GROUP_PARENT_ADD_FAILED,
                                                                tagParsed("parent", parent.groupName()),
                                                                tagParsed("group", group.groupName())
                                                        )
                                                );

                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_PARENT_ADD_SUCCESS,
                                                tagParsed("parent", parent.groupName()),
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
                                                // -/permission user <username> parent add <parent> <time>
                                                String inputUser = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(inputUser);
                                                PermissionTarget target = PermissionTarget.user(user.id());

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                Parent existing = parentProvider.findParent(target, parent.id()).orElse(null);
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_USER_PARENT_EXISTS,
                                                            tagParsed("parent", parent.groupName()),
                                                            tagParsed("user", user.username())
                                                    );

                                                if (parent.id() == getDefaultGroup().id())
                                                    throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Parent success = parentProvider.upsert(target, parent.id(), duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_USER_PARENT_ADD_FAILED,
                                                                        tagParsed("parent", parent.groupName()),
                                                                        tagParsed("user", user.username())
                                                                )
                                                        );

                                                sendMessage(source, languageId, Message.PERMISSION_USER_PARENT_ADD_SUCCESS,
                                                        tagParsed("parent", parent.groupName()),
                                                        tagParsed("user", user.username()),
                                                        Placeholder.component("expired", formatExpiredMessage(languageId, success.expiresAt()))
                                                );
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                // -/permission group <groupName> parent add <parent> <time>
                                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(inputGroup);
                                                PermissionTarget target = PermissionTarget.group(group.id());

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                Parent existing = parentProvider.findParent(target, parent.id()).orElse(null);
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PARENT_EXISTS,
                                                            tagParsed("parent", parent.groupName()),
                                                            tagParsed("group", group.groupName())
                                                    );

                                                if (parent.id() == getDefaultGroup().id())
                                                    throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Parent success = parentProvider.upsert(target, parent.id(), duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_GROUP_PARENT_ADD_FAILED,
                                                                        tagParsed("parent", parent.groupName()),
                                                                        tagParsed("group", group.groupName())
                                                                )
                                                        );

                                                sendMessage(source, languageId, Message.PERMISSION_GROUP_PARENT_ADD_SUCCESS,
                                                        tagParsed("parent", parent.groupName()),
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

    public LiteralArgumentBuilder<CommandSource> getParentRemove(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("remove")
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, false))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> parent remove <parent>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);
                                        PermissionTarget target = PermissionTarget.user(user.id());

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        Parent userParent = getUserParent(user, parent);
                                        if (userParent.parentId() == getDefaultGroup().id())
                                            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_REMOVE);

                                        int result = parentProvider.remove(target, userParent.parentId());
                                        if (result < 1)
                                            throw new CommandException(Message.PERMISSION_USER_PARENT_REMOVE_FAILED,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("user", user.username())
                                            );

                                        sendMessage(source, languageId, Message.PERMISSION_USER_PARENT_REMOVE_SUCCESS,
                                                tagParsed("parent", parent.groupName()),
                                                tagParsed("user", user.username())
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS, result);
                                    } else {
                                        // -/permission group <groupName> parent remove <parent>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);
                                        PermissionTarget target = PermissionTarget.group(group.id());

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        Parent groupParent = getGroupParent(group, parent);

                                        int result = parentProvider.remove(target, groupParent.parentId());
                                        if (result < 1)
                                            throw new CommandException(Message.PERMISSION_GROUP_PARENT_REMOVE_FAILED,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("group", group.groupName())
                                            );

                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_PARENT_REMOVE_SUCCESS,
                                                tagParsed("parent", parent.groupName()),
                                                tagParsed("group", group.groupName())
                                        );
                                        return CommandResult.of(Command.SINGLE_SUCCESS, result);
                                    }
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getParentClear(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("clear")
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            if (isUser) {
                                // -/permission user <username> parent clear
                                String inputUser = StringArgumentType.getString(context, "username");
                                int languageId = executor.languageId();
                                User user = getUser(inputUser);
                                PermissionTarget target = PermissionTarget.user(user.id());

                                int result = parentProvider.clear(target);
                                if (result < 1)
                                    throw new CommandException(Message.PERMISSION_USER_PARENT_CLEAR_FAILED, tagParsed("user", user.username()));

                                Group defaultGroup = getDefaultGroup();
                                Parent success = parentProvider.upsert(target, defaultGroup.id(), -1, CONSOLE_USER_ID).orElse(null); // Add default parent back
                                result += success != null ? 1 : 0;
                                if (result < 2)
                                    throw new CommandException(Message.PERMISSION_USER_PARENT_ADD_FAILED,
                                            tagParsed("parent", defaultGroup.groupName()),
                                            tagParsed("user", user.username())
                                    );

                                sendMessage(source, languageId, Message.PERMISSION_USER_PARENT_CLEAR_SUCCESS, tagParsed("user", user.username()));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            } else {
                                // -/permission group <groupName> parent clear
                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                int languageId = executor.languageId();
                                Group group = getGroup(inputGroup);
                                PermissionTarget target = PermissionTarget.group(group.id());

                                int result = parentProvider.clear(target);
                                if (result < 1)
                                    throw new CommandException(Message.PERMISSION_GROUP_PARENT_CLEAR_FAILED, tagParsed("group", group.groupName()));

                                sendMessage(source, languageId, Message.PERMISSION_GROUP_PARENT_CLEAR_SUCCESS, tagParsed("group", group.groupName()));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            }
                        })
                );
    }

    public LiteralArgumentBuilder<CommandSource> getParentInfo(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("info")
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, true))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> parent info <parent>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        Parent userParent = getUserParent(user, parent);

                                        User creator = getUser(userParent.createdBy());
                                        User changer = userParent.changedBy() == null ? null : getUser(userParent.changedBy());
                                        String createdDate = userParent.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = userParent.changedAt() == null ? null : userParent.changedAt().format(getDateTimeFormatter(languageId));
                                        Component expires = formatExpiredInfoMessage(languageId, userParent.expiresAt());

                                        Component changedText = (changer == null || changedDate == null) ? Component.empty() :
                                                component(languageId, Message.PERMISSION_INFO_CHANGE_STUFF,
                                                        tagParsed("changed_name", changer.username()),
                                                        tagParsed("changed_id", changer.id()),
                                                        tagParsed("changed_at", changedDate)
                                                );

                                        sendMessage(source, languageId, Message.PERMISSION_USER_PARENT_INFO,
                                                tagParsed("username", user.username()),
                                                tagParsed("user_id", user.id()),
                                                tagParsed("parent_name", parent.groupName()),
                                                tagParsed("parent_id", parent.id()),
                                                Placeholder.component("expires", expires),
                                                tagParsed("created_name", creator.username()),
                                                tagParsed("created_id", creator.id()),
                                                tagParsed("created_at", createdDate),
                                                Placeholder.component("changed", changedText)
                                        );

                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    } else {
                                        // -/permission group <groupName> parent info <parent>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        Parent groupParent = getGroupParent(group, parent);

                                        User creator = getUser(groupParent.createdBy());
                                        User changer = groupParent.changedBy() == null ? null : getUser(groupParent.changedBy());
                                        String createdDate = groupParent.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = groupParent.changedAt() == null ? null : groupParent.changedAt().format(getDateTimeFormatter(languageId));
                                        Component expires = formatExpiredInfoMessage(languageId, groupParent.expiresAt());

                                        Component changedText = (changer == null || changedDate == null) ? Component.empty() :
                                                component(languageId, Message.PERMISSION_INFO_CHANGE_STUFF,
                                                        tagParsed("changed_name", changer.username()),
                                                        tagParsed("changed_id", changer.id()),
                                                        tagParsed("changed_at", changedDate)
                                                );

                                        sendMessage(source, languageId, Message.PERMISSION_GROUP_PARENT_INFO,
                                                tagParsed("group_name", group.groupName()),
                                                tagParsed("group_id", group.id()),
                                                tagParsed("parent_name", parent.groupName()),
                                                tagParsed("parent_id", parent.id()),
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

    public LiteralArgumentBuilder<CommandSource> getParentTime(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, false))
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            if (isUser) {
                                                // -/permission user <username> parent time <parent> <time>
                                                String inputUser = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(inputUser);
                                                PermissionTarget target = PermissionTarget.user(user.id());

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                Parent userParent = getUserParent(user, parent);
                                                if (userParent.parentId() == getDefaultGroup().id())
                                                    throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_TIME);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Parent success = parentProvider.upsert(target, userParent.parentId(), duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_USER_PARENT_TIME_FAILED,
                                                                        tagParsed("parent", parent.groupName()), tagParsed("user", user.username())
                                                                )
                                                        );

                                                sendMessage(source, languageId, Message.PERMISSION_USER_PARENT_TIME_SUCCESS,
                                                        tagParsed("parent", parent.groupName()),
                                                        tagParsed("user", user.username()),
                                                        Placeholder.component("expired", formatExpiredMessage(languageId, success.expiresAt()))
                                                );
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                // -/permission group <groupName> parent time <parent> <time>
                                                String inputGroup = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(inputGroup);
                                                PermissionTarget target = PermissionTarget.group(group.id());

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                Parent groupParent = getGroupParent(group, parent);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                Parent success = parentProvider.upsert(target, groupParent.parentId(), duration, executor.id())
                                                        .orElseThrow(() ->
                                                                new CommandException(Message.PERMISSION_GROUP_PARENT_TIME_FAILED,
                                                                        tagParsed("parent", parent.groupName()),
                                                                        tagParsed("group", group.groupName())
                                                                )
                                                        );

                                                sendMessage(source, languageId, Message.PERMISSION_GROUP_PARENT_TIME_SUCCESS,
                                                        tagParsed("parent", parent.groupName()),
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

    private @NotNull SuggestionProvider<CommandSource> getSuggestionParent(boolean isUser, boolean isDefaultAllowed) {
        return (context, builder) -> {
            String prefix = builder.getRemaining();
            PermissionTarget target = getTarget(context, isUser);
            List<Group> parents = parentProvider.findParents(target)
                    .stream()
                    .map(Parent::parentId)
                    .map(groupProvider::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            if (parents.isEmpty())
                return Suggestions.empty();

            // TODO: When UserPermission & GroupPermission are merged, then add to tooltip the expires time
            parents.stream()
                    .filter(group -> (!isUser || isDefaultAllowed) || group.id() != 1)
                    .filter(group -> StringUtil.startsWithIgnoreCase(group.groupName(), prefix))
                    .forEach(group -> builder.suggest(group.groupName()));
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

    private @NotNull Parent getUserParent(User user, Group parent) {
        return parentProvider.findParent(PermissionTarget.user(user.id()), parent.id()).orElseThrow(() ->
                new CommandException(Message.PERMISSION_USER_PARENT_NOT_EXISTS,
                        tagParsed("parent", parent.groupName()),
                        tagParsed("user", user.username())
                )
        );
    }

    private @NotNull Parent getGroupParent(Group group, Group parent) {
        return parentProvider.findParent(PermissionTarget.group(group.id()), parent.id()).orElseThrow(() ->
                new CommandException(Message.PERMISSION_GROUP_PARENT_NOT_EXISTS,
                        tagParsed("parent", parent.groupName()),
                        tagParsed("group", group.groupName())
                )
        );
    }
}
