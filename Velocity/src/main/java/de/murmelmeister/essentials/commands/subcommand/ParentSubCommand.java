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
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.library.utils.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.time.LocalDateTime;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ParentSubCommand extends PermissionUtil {
    private final GroupProvider groupProvider;
    private final GroupParentProvider groupParentProvider;
    private final UserParentProvider userParentProvider;

    public ParentSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupProvider = plugin.getGroupProvider();
        this.groupParentProvider = plugin.getGroupParentProvider();
        this.userParentProvider = plugin.getUserParentProvider();
    }

    @Override
    public BrigadierCommand createCommand() {
        return null;
    }

    public int getParents(CommandContext<CommandSource> context, boolean isUser) {
        return runWithTiming(context, (source, executor) -> {
            if (isUser) {
                // -/permission user <username> parent
                String inputUser = StringArgumentType.getString(context, "username");
                int languageId = executor.languageId();
                User user = getUser(inputUser);

                List<UserParent> parents = userParentProvider.getParents(user.id());
                if (parents.isEmpty())
                    throw new CommandException(Message.PERMISSION_USER_PARENT_LIST_EMPTY, tagParsed("username", user.username()));

                Message headerName = parents.size() == 1
                        ? Message.PERMISSION_LIST_SINGULAR_PARENT
                        : Message.PERMISSION_LIST_PLURAL_PARENT;
                sendMessage(source, languageId, Message.PERMISSION_USER_LIST_HEADER,
                        tagParsed("header_name", languageId, headerName),
                        tagParsed("username", user.username()),
                        tagParsed("user_id", user.id())
                );

                String clickMessage = "/permission user " + user.username() + " parent remove ";
                parents.forEach(parent -> sendParentsMessage(source, clickMessage, executor.languageId(), parent.parentId(), parent.expiresAt()));
                return CommandResult.of(Command.SINGLE_SUCCESS);
            } else {
                // -/permission group <groupName> parent
                String inputGroup = StringArgumentType.getString(context, "groupName");
                int languageId = executor.languageId();
                Group group = getGroup(inputGroup);

                List<GroupParent> parents = groupParentProvider.getParents(group.id());
                if (parents.isEmpty())
                    throw new CommandException(Message.PERMISSION_GROUP_PARENT_LIST_EMPTY, tagParsed("group_name", group.groupName()));

                Message headerName = parents.size() == 1
                        ? Message.PERMISSION_LIST_SINGULAR_PARENT
                        : Message.PERMISSION_LIST_PLURAL_PARENT;
                sendMessage(source, languageId, Message.PERMISSION_GROUP_LIST_HEADER,
                        tagParsed("header_name", languageId, headerName),
                        tagParsed("group_name", group.groupName()),
                        tagParsed("group_id", group.id())
                );

                String clickMessage = "/permission group " + group.groupName() + " parent remove ";
                parents.forEach(parent -> sendParentsMessage(source, clickMessage, executor.languageId(), parent.parentId(), parent.expiresAt()));
                return CommandResult.of(Command.SINGLE_SUCCESS);
            }
        });
    }

    private void sendParentsMessage(CommandSource source, String clickMessage, int executorLang, int parentId, LocalDateTime expiresAt) {
        Group parentGroup = groupProvider.findById(parentId);
        String parentName = parentGroup.groupName();

        if (parentId == getDefaultGroup().id()) {
            sendMessage(source, executorLang, Message.PERMISSION_DEFAULT_LIST_PARENT, tagParsed("default_parent", parentName));
            return;
        }

        sendMessage(source, executorLang, Message.PERMISSION_LIST_PARENT_MESSAGE,
                tagParsed("parent", parentName),
                tagParsed("click_command", clickMessage + parentName),
                Placeholder.component("expired", formatExpiredMessage(executorLang, expiresAt))
        ); // .trim()
    }

    public LiteralArgumentBuilder<CommandSource> getParentAdd(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String prefix = builder.getRemaining();
                            int id = getId(context, isUser);
                            List<Integer> haveParents = isUser ? userParentProvider.getParents(id)
                                    .stream().map(UserParent::parentId).toList()
                                    : groupParentProvider.getParents(id)
                                    .stream().map(GroupParent::parentId).toList();
                            groupProvider.findAll().stream()
                                    .filter(group -> isUser || group.id() != id)
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

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        UserParent existing = userParentProvider.getParent(user.id(), parent.id());
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_USER_PARENT_EXISTS,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("user", user.username())
                                            );

                                        if (parent.id() == getDefaultGroup().id())
                                            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                        UserParent success = userParentProvider.add(user.id(), parent.id(), -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(Message.PERMISSION_USER_PARENT_ADD_FAILED,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("user", user.username())
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

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        GroupParent existing = groupParentProvider.getParent(group.id(), parent.id());
                                        if (existing != null)
                                            throw new CommandException(Message.PERMISSION_GROUP_PARENT_EXISTS,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("group", group.groupName())
                                            );

                                        if (parent.id() == getDefaultGroup().id())
                                            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                        GroupParent success = groupParentProvider.add(group.id(), parent.id(), -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(Message.PERMISSION_GROUP_PARENT_ADD_FAILED,
                                                    tagParsed("parent", parent.groupName()),
                                                    tagParsed("group", group.groupName())
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

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                UserParent existing = userParentProvider.getParent(user.id(), parent.id());
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_USER_PARENT_EXISTS,
                                                            tagParsed("parent", parent.groupName()),
                                                            tagParsed("user", user.username())
                                                    );

                                                if (parent.id() == getDefaultGroup().id())
                                                    throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                UserParent success = userParentProvider.add(user.id(), parent.id(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_USER_PARENT_ADD_FAILED,
                                                            tagParsed("parent", parent.groupName()),
                                                            tagParsed("user", user.username())
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

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                GroupParent existing = groupParentProvider.getParent(group.id(), parent.id());
                                                if (existing != null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PARENT_EXISTS,
                                                            tagParsed("parent", parent.groupName()),
                                                            tagParsed("group", group.groupName())
                                                    );

                                                if (parent.id() == getDefaultGroup().id())
                                                    throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_ADD);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                GroupParent success = groupParentProvider.add(group.id(), parent.id(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PARENT_ADD_FAILED,
                                                            tagParsed("parent", parent.groupName()),
                                                            tagParsed("group", group.groupName())
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
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, false))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        // -/permission user <username> parent remove <parent>
                                        String inputUser = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(inputUser);

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        UserParent userParent = getUserParent(user, parent);
                                        if (userParent.parentId() == getDefaultGroup().id())
                                            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_REMOVE);

                                        int result = userParentProvider.remove(userParent.userId(), userParent.parentId());
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

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        GroupParent groupParent = getGroupParent(group, parent);

                                        int result = groupParentProvider.remove(groupParent.groupId(), groupParent.parentId());
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

                                int result = userParentProvider.clear(user.id());
                                if (result < 1)
                                    throw new CommandException(Message.PERMISSION_USER_PARENT_CLEAR_FAILED, tagParsed("user", user.username()));

                                Group defaultGroup = getDefaultGroup();
                                UserParent success = userParentProvider.add(user.id(), defaultGroup.id(), -1, CONSOLE_USER_ID); // Add default parent back
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

                                int result = groupParentProvider.clear(group.id());
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
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
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

                                        UserParent userParent = getUserParent(user, parent);

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
                                        ); // .trim()

                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    } else {
                                        // -/permission group <groupName> parent info <parent>
                                        String inputGroup = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(inputGroup);

                                        String inputParent = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(inputParent);

                                        GroupParent groupParent = getGroupParent(group, parent);

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
                                        ); // .trim()
                                        return CommandResult.of(Command.SINGLE_SUCCESS);
                                    }
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getParentTime(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .executes(context -> {
                    sendRawMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, false))
                        .executes(context -> {
                            sendRawMessage(context.getSource(), syntaxParent(isUser));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executor) -> {
                                            if (isUser) {
                                                // -/permission user <username> parent time <parent> <time>
                                                String inputUser = StringArgumentType.getString(context, "username");
                                                int languageId = executor.languageId();
                                                User user = getUser(inputUser);

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                UserParent userParent = getUserParent(user, parent);
                                                if (userParent.parentId() == getDefaultGroup().id())
                                                    throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_TIME);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                UserParent success = userParentProvider.update(userParent.userId(), userParent.parentId(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_USER_PARENT_TIME_FAILED,
                                                            tagParsed("parent", parent.groupName()), tagParsed("user", user.username())
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

                                                String inputParent = StringArgumentType.getString(context, "parent");
                                                Group parent = getGroup(inputParent);

                                                GroupParent groupParent = getGroupParent(group, parent);

                                                String inputTime = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(inputTime);

                                                GroupParent success = groupParentProvider.update(groupParent.groupId(), groupParent.parentId(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(Message.PERMISSION_GROUP_PARENT_TIME_FAILED,
                                                            tagParsed("parent", parent.groupName()),
                                                            tagParsed("group", group.groupName())
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

    private SuggestionProvider<CommandSource> getSuggestionParent(boolean isUser, boolean isDefaultAllowed) {
        return (context, builder) -> {
            String prefix = builder.getRemaining();
            int id = getId(context, isUser);
            List<Group> parents = isUser ? userParentProvider.getParents(id)
                    .stream().map(UserParent::parentId).map(groupProvider::findById).toList()
                    : groupParentProvider.getParents(id)
                    .stream().map(GroupParent::parentId).map(groupProvider::findById).toList();

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

    private int getId(CommandContext<CommandSource> context, boolean isUser) {
        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        return isUser ? getUser(name).id() : getGroup(name).id();
    }

    private UserParent getUserParent(User user, Group parent) {
        UserParent userParent = userParentProvider.getParent(user.id(), parent.id());
        if (userParent == null)
            throw new CommandException(Message.PERMISSION_USER_PARENT_NOT_EXISTS,
                    tagParsed("parent", parent.groupName()),
                    tagParsed("user", user.username())
            );
        return userParent;
    }

    private GroupParent getGroupParent(Group group, Group parent) {
        GroupParent groupParent = groupParentProvider.getParent(group.id(), parent.id());
        if (groupParent == null)
            throw new CommandException(Message.PERMISSION_GROUP_PARENT_NOT_EXISTS,
                    tagParsed("parent", parent.groupName()),
                    tagParsed("group", group.groupName())
            );
        return groupParent;
    }
}
