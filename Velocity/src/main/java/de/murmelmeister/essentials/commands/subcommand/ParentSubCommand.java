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
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.utils.StringUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDateTime;
import java.util.List;

import static de.murmelmeister.murmelapi.user.UserProviderImpl.CONSOLE_USER_ID;

public final class ParentSubCommand extends PermissionUtil {
    private final GroupProvider groupProvider;
    private final GroupParentProvider groupParentProvider;
    private final UserParentProvider userParentProvider;

    private final MessageService messageService;

    public ParentSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupProvider = plugin.getGroupProvider();
        this.groupParentProvider = plugin.getGroupParentProvider();
        this.userParentProvider = plugin.getUserParentProvider();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public BrigadierCommand createCommand() {
        return null;
    }

    public int getParents(CommandContext<CommandSource> context, boolean isUser) {
        return runWithTiming(context, (source, executor) -> {
            if (isUser) {
                String username = StringArgumentType.getString(context, "username");
                int languageId = executor.languageId();
                User user = getUser(languageId, username);

                List<UserParent> parents = userParentProvider.getParents(user.id());
                if (parents.isEmpty()) {
                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_LIST_EMPTY, languageId)
                            .replace("[USER_NAME]", username));
                    return CommandResult.of(-2);
                }

                String headerName = parents.size() == 1
                        ? messageService.getMessage(Messages.PARENTS_LIST_SINGULAR, languageId)
                        : messageService.getMessage(Messages.PARENTS_LIST_PLURAL, languageId);
                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_LIST_HEADER, languageId)
                        .replace("[HEADER_NAME]", headerName)
                        .replace("[USER_NAME]", username)
                        .replace("[USER_ID]", String.valueOf(user.id())));
                String clickMessage = "/permission user " + username + " parent remove ";
                parents.forEach(parent -> sendParentsMessage(source, clickMessage, executor.languageId(), parent.parentId(), parent.expiresAt()));
                return CommandResult.of(Command.SINGLE_SUCCESS);
            } else {
                String groupName = StringArgumentType.getString(context, "groupName");
                int languageId = executor.languageId();
                Group group = getGroup(languageId, groupName);

                List<GroupParent> parents = groupParentProvider.getParents(group.id());
                if (parents.isEmpty()) {
                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_LIST_EMPTY, languageId)
                            .replace("[GROUP_NAME]", groupName));
                    return CommandResult.of(-2);
                }

                String headerName = parents.size() == 1
                        ? messageService.getMessage(Messages.PARENTS_LIST_SINGULAR, languageId)
                        : messageService.getMessage(Messages.PARENTS_LIST_PLURAL, languageId);
                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_LIST_HEADER, languageId)
                        .replace("[HEADER_NAME]", headerName)
                        .replace("[GROUP_NAME]", groupName)
                        .replace("[GROUP_ID]", String.valueOf(group.id())));
                String clickMessage = "/permission group " + groupName + " parent remove ";
                parents.forEach(parent -> sendParentsMessage(source, clickMessage, executor.languageId(), parent.parentId(), parent.expiresAt()));
                return CommandResult.of(Command.SINGLE_SUCCESS);
            }
        });
    }

    private void sendParentsMessage(CommandSource source, String clickMessage, int executorLang, int parentId, LocalDateTime expiresAt) {
        Group parentGroup = groupProvider.findById(parentId);
        String parentName = parentGroup.groupName();

        if (parentId == getDefaultGroup(executorLang).id()) {
            sendMessage(source, messageService.getMessage(Messages.PARENT_LIST_DEFAULT_MESSAGE, executorLang)
                    .replace("[DEFAULT_PARENT]", parentName));
            return;
        }

        String expiredMessage = formatExpiredMessage(executorLang, expiresAt);
        sendMessage(source, (messageService.getMessage(Messages.PARENT_LIST_MESSAGE, executorLang)
                                     .replace("[PARENT]", parentName)
                                     .replace("[CLICK_COMMAND]", clickMessage + parentName)
                             + " " + expiredMessage).trim());
    }

    public LiteralArgumentBuilder<CommandSource> getParentAdd(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String prefix = builder.getRemaining();
                            User executor = getExecutor(context.getSource());
                            int id = getId(context, executor.languageId(), isUser);
                            List<Integer> haveParents = isUser ? userParentProvider.getParents(id)
                                    .stream().map(UserParent::parentId).toList()
                                    : groupParentProvider.getParents(id)
                                    .stream().map(GroupParent::parentId).toList();
                            groupProvider.findAll().stream()
                                    .filter(group -> isUser || group.id() != id)
                                    .filter(group -> !haveParents.contains(group.id()))
                                    .filter(group -> StringUtil.startsWithIgnoreCase(group.groupName(), prefix))
                                    .forEach(group -> builder.suggest(group.groupName(),
                                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + group.groupName()))));
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        String username = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(languageId, username);

                                        String parentName = StringArgumentType.getString(context, "parent");
                                        Group parentGroup = getGroup(languageId, parentName);

                                        UserParent userParent = userParentProvider.getParent(user.id(), parentGroup.id());
                                        if (userParent != null) {
                                            sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_EXISTS, languageId)
                                                    .replace("[PARENT]", parentName)
                                                    .replace("[USER]", username));
                                            return CommandResult.of(-2);
                                        }

                                        if (parentGroup.id() == getDefaultGroup(languageId).id()) {
                                            sendMessage(source, messageService.getMessage(Messages.DEFAULT_GROUP_ADD, languageId));
                                            return CommandResult.of(-3);
                                        }

                                        UserParent success = userParentProvider.add(user.id(), parentGroup.id(), -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PARENT_ADD_FAILED, languageId)
                                                    .replace("[PARENT]", parentName)
                                                    .replace("[USER]", username));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_ADD_SUCCESS, languageId)
                                                .replace("[PARENT]", parentName)
                                                .replace("[USER]", username)
                                                .replace("[EXPIRED]", formatExpiredMessage(languageId, success.expiresAt())));
                                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                    } else {
                                        String groupName = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(languageId, groupName);

                                        String parentName = StringArgumentType.getString(context, "parent");
                                        Group parentGroup = getGroup(languageId, parentName);

                                        GroupParent groupParent = groupParentProvider.getParent(group.id(), parentGroup.id());
                                        if (groupParent != null) {
                                            sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_EXISTS, languageId)
                                                    .replace("[PARENT]", parentName)
                                                    .replace("[GROUP]", groupName));
                                            return CommandResult.of(-2);
                                        }

                                        if (parentGroup.id() == getDefaultGroup(languageId).id()) {
                                            sendMessage(source, messageService.getMessage(Messages.DEFAULT_GROUP_ADD, languageId));
                                            return CommandResult.of(-3);
                                        }

                                        GroupParent success = groupParentProvider.add(group.id(), parentGroup.id(), -1, executor.id());
                                        if (success == null)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_ADD_FAILED, languageId)
                                                    .replace("[PARENT]", parentName)
                                                    .replace("[GROUP]", groupName));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_ADD_SUCCESS, languageId)
                                                .replace("[PARENT]", parentName)
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

                                                String parentName = StringArgumentType.getString(context, "parent");
                                                Group parentGroup = getGroup(languageId, parentName);

                                                UserParent userParent = userParentProvider.getParent(user.id(), parentGroup.id());
                                                if (userParent != null) {
                                                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_EXISTS, languageId)
                                                            .replace("[PARENT]", parentName)
                                                            .replace("[USER]", username));
                                                    return CommandResult.of(-2);
                                                }

                                                if (parentGroup.id() == getDefaultGroup(languageId).id()) {
                                                    sendMessage(source, messageService.getMessage(Messages.DEFAULT_GROUP_ADD, languageId));
                                                    return CommandResult.of(-3);
                                                }

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(executor.languageId(), time);

                                                UserParent success = userParentProvider.add(user.id(), parentGroup.id(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PARENT_ADD_FAILED, languageId)
                                                            .replace("[PARENT]", parentName)
                                                            .replace("[USER]", username));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_ADD_SUCCESS, languageId)
                                                        .replace("[PARENT]", parentName)
                                                        .replace("[USER]", username)
                                                        .replace("[EXPIRED]", formatExpiredMessage(languageId, success.expiresAt())));
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                String groupName = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(languageId, groupName);

                                                String parentName = StringArgumentType.getString(context, "parent");
                                                Group parentGroup = getGroup(languageId, parentName);

                                                GroupParent groupParent = groupParentProvider.getParent(group.id(), parentGroup.id());
                                                if (groupParent != null) {
                                                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_EXISTS, languageId)
                                                            .replace("[PARENT]", parentName)
                                                            .replace("[GROUP]", groupName));
                                                    return CommandResult.of(-2);
                                                }

                                                if (parentGroup.id() == getDefaultGroup(languageId).id()) {
                                                    sendMessage(source, messageService.getMessage(Messages.DEFAULT_GROUP_ADD, languageId));
                                                    return CommandResult.of(-3);
                                                }

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(executor.languageId(), time);

                                                GroupParent success = groupParentProvider.add(group.id(), parentGroup.id(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_ADD_FAILED, languageId)
                                                            .replace("[PARENT]", parentName)
                                                            .replace("[GROUP]", groupName));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_ADD_SUCCESS, languageId)
                                                        .replace("[PARENT]", parentName)
                                                        .replace("[GROUP]", groupName)
                                                        .replace("[EXPIRED]", formatExpiredMessage(languageId, success.expiresAt())));
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
                    sendMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, false))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        String username = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(languageId, username);

                                        String parentName = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(languageId, parentName);

                                        UserParent userParent = getUserParent(languageId, user, parent);
                                        if (userParent.parentId() == getDefaultGroup(languageId).id()) {
                                            sendMessage(source, messageService.getMessage(Messages.DEFAULT_GROUP_REMOVE, languageId));
                                            return CommandResult.of(-2);
                                        }

                                        int result = userParentProvider.remove(userParent.userId(), userParent.parentId());
                                        if (result < 1)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PARENT_REMOVE_FAILED, languageId)
                                                    .replace("[PARENT]", parentName)
                                                    .replace("[USER]", username));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_REMOVE_SUCCESS, languageId)
                                                .replace("[PARENT]", parentName)
                                                .replace("[USER]", username));
                                        return CommandResult.of(Command.SINGLE_SUCCESS, result);
                                    } else {
                                        String groupName = StringArgumentType.getString(context, "groupName");
                                        int languageId = executor.languageId();
                                        Group group = getGroup(languageId, groupName);

                                        String parentName = StringArgumentType.getString(context, "parent");
                                        Group parent = getGroup(languageId, parentName);

                                        GroupParent groupParent = getGroupParent(languageId, group, parent);

                                        int result = groupParentProvider.remove(groupParent.groupId(), groupParent.parentId());
                                        if (result < 1)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_REMOVE_FAILED, languageId)
                                                    .replace("[PARENT]", parentName)
                                                    .replace("[GROUP]", groupName));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_REMOVE_SUCCESS, languageId)
                                                .replace("[PARENT]", parentName)
                                                .replace("[GROUP]", groupName));
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
                                String username = StringArgumentType.getString(context, "username");
                                int languageId = executor.languageId();
                                User user = getUser(languageId, username);
                                Group defaultGroup = getDefaultGroup(languageId);

                                int result = userParentProvider.clear(user.id());
                                if (result < 1)
                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PARENT_CLEAR_FAILED, languageId)
                                            .replace("[USER]", username));
                                UserParent success = userParentProvider.add(user.id(), defaultGroup.id(), -1, CONSOLE_USER_ID); // Add default parent back
                                result += success != null ? 1 : 0;
                                if (result < 2)
                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PARENT_ADD_FAILED, languageId)
                                            .replace("[PARENT]", defaultGroup.groupName())
                                            .replace("[USER]", username));

                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_CLEAR_SUCCESS, languageId)
                                        .replace("[USER]", username));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            } else {
                                String groupName = StringArgumentType.getString(context, "groupName");
                                int languageId = executor.languageId();
                                Group group = getGroup(languageId, groupName);

                                int result = groupParentProvider.clear(group.id());
                                if (result < 1)
                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_CLEAR_FAILED, languageId)
                                            .replace("[GROUP]", groupName));
                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_CLEAR_SUCCESS, languageId)
                                        .replace("[GROUP]", groupName));
                                return CommandResult.of(Command.SINGLE_SUCCESS, result);
                            }
                        })
                );
    }

    public LiteralArgumentBuilder<CommandSource> getParentInfo(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, true))
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    if (isUser) {
                                        String username = StringArgumentType.getString(context, "username");
                                        int languageId = executor.languageId();
                                        User user = getUser(languageId, username);

                                        String parentName = StringArgumentType.getString(context, "parent");
                                        Group parentGroup = getGroup(languageId, parentName);

                                        UserParent userParent = getUserParent(languageId, user, parentGroup);

                                        User creator = getUser(languageId, userParent.createdBy());
                                        User changer = userParent.changedBy() == null ? null : getUser(languageId, userParent.changedBy());
                                        String createdDate = userParent.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = userParent.changedAt() == null ? null : userParent.changedAt().format(getDateTimeFormatter(languageId));
                                        String expiredMessage = formatExpiredInfoMessage(languageId, userParent.expiresAt());

                                        String changedText = (changer == null || changedDate == null) ? null :
                                                messageService.getMessage(Messages.PERMISSION_INFO_CHANGE_STUFF, languageId)
                                                        .replace("[CHANGED_NAME]", changer.username())
                                                        .replace("[CHANGED_ID]", String.valueOf(changer.id()))
                                                        .replace("[CHANGED_AT]", changedDate);

                                        sendMessage(source, (messageService.getMessage(Messages.PERMISSION_USER_PARENT_INFO_MESSAGE, languageId)
                                                .replace("[USER_NAME]", user.username())
                                                .replace("[USER_ID]", String.valueOf(user.id()))
                                                .replace("[PARENT_NAME]", parentGroup.groupName())
                                                .replace("[PARENT_ID]", String.valueOf(parentGroup.id()))
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

                                        String parentName = StringArgumentType.getString(context, "parent");
                                        Group parentGroup = getGroup(languageId, parentName);

                                        GroupParent groupParent = getGroupParent(languageId, group, parentGroup);

                                        User creator = getUser(languageId, groupParent.createdBy());
                                        User changer = groupParent.changedBy() == null ? null : getUser(languageId, groupParent.changedBy());
                                        String createdDate = groupParent.createdAt().format(getDateTimeFormatter(languageId));
                                        String changedDate = groupParent.changedAt() == null ? null : groupParent.changedAt().format(getDateTimeFormatter(languageId));
                                        String expiredMessage = formatExpiredInfoMessage(languageId, groupParent.expiresAt());

                                        String changedText = (changer == null || changedDate == null) ? null :
                                                messageService.getMessage(Messages.PERMISSION_INFO_CHANGE_STUFF, languageId)
                                                        .replace("[CHANGED_NAME]", changer.username())
                                                        .replace("[CHANGED_ID]", String.valueOf(changer.id()))
                                                        .replace("[CHANGED_AT]", changedDate);

                                        sendMessage(source, (messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_INFO_MESSAGE, languageId)
                                                .replace("[GROUP_NAME]", group.groupName())
                                                .replace("[GROUP_ID]", String.valueOf(group.id()))
                                                .replace("[PARENT_NAME]", parentGroup.groupName())
                                                .replace("[PARENT_ID]", String.valueOf(parentGroup.id()))
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

    public LiteralArgumentBuilder<CommandSource> getParentTime(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxParent(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests(getSuggestionParent(isUser, false))
                        .executes(context -> {
                            sendMessage(context.getSource(), syntaxParent(isUser));
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

                                                String parentName = StringArgumentType.getString(context, "parent");
                                                Group parentGroup = getGroup(languageId, parentName);

                                                UserParent userParent = getUserParent(languageId, user, parentGroup);
                                                if (userParent.parentId() == getDefaultGroup(languageId).id()) {
                                                    sendMessage(source, messageService.getMessage(Messages.DEFAULT_GROUP_TIME, languageId));
                                                    return CommandResult.of(-2);
                                                }

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(executor.languageId(), time);

                                                UserParent success = userParentProvider.update(userParent.userId(), userParent.parentId(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PARENT_TIME_FAILED, languageId)
                                                            .replace("[PARENT]", parentName)
                                                            .replace("[USER]", username));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_USER_PARENT_TIME_SUCCESS, languageId)
                                                        .replace("[PARENT]", parentName)
                                                        .replace("[USER]", username)
                                                        .replace("[EXPIRED]", formatExpiredInfoMessage(languageId, success.expiresAt())));
                                                return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                            } else {
                                                String groupName = StringArgumentType.getString(context, "groupName");
                                                int languageId = executor.languageId();
                                                Group group = getGroup(languageId, groupName);

                                                String parentName = StringArgumentType.getString(context, "parent");
                                                Group parentGroup = getGroup(languageId, parentName);

                                                GroupParent groupParent = getGroupParent(languageId, group, parentGroup);

                                                String time = StringArgumentType.getString(context, "time");
                                                long duration = parseTime(executor.languageId(), time);

                                                GroupParent success = groupParentProvider.update(groupParent.groupId(), groupParent.parentId(), duration, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_TIME_FAILED, languageId)
                                                            .replace("[PARENT]", parentName)
                                                            .replace("[GROUP]", groupName));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_TIME_SUCCESS, languageId)
                                                        .replace("[PARENT]", parentName)
                                                        .replace("[GROUP]", groupName)
                                                        .replace("[EXPIRED]", formatExpiredInfoMessage(languageId, success.expiresAt())));
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
            User executor = getExecutor(context.getSource());
            int id = getId(context, executor.languageId(), isUser);
            List<Group> parents = isUser ? userParentProvider.getParents(id)
                    .stream().map(UserParent::parentId).map(groupProvider::findById).toList()
                    : groupParentProvider.getParents(id)
                    .stream().map(GroupParent::parentId).map(groupProvider::findById).toList();

            if (parents.isEmpty())
                return builder.buildFuture();

            parents.stream()
                    .filter(group -> (!isUser || isDefaultAllowed) || group.id() != 1)
                    .filter(group -> StringUtil.startsWithIgnoreCase(group.groupName(), prefix))
                    .forEach(group -> builder.suggest(group.groupName(),
                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + group.groupName()))));
            return builder.buildFuture();
        };
    }

    private int getId(CommandContext<CommandSource> context, int languageId, boolean isUser) {
        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        return isUser ? getUser(languageId, name).id() : getGroup(languageId, name).id();
    }

    private UserParent getUserParent(int languageId, User user, Group parent) {
        UserParent userParent = userParentProvider.getParent(user.id(), parent.id());
        if (userParent == null)
            throw new CommandException(messageService.getMessage(Messages.PERMISSION_USER_PARENT_NOT_EXISTS, languageId)
                    .replace("[PARENT]", parent.groupName())
                    .replace("[USER]", user.username()));
        return userParent;
    }

    private GroupParent getGroupParent(int languageId, Group group, Group parent) {
        GroupParent groupParent = groupParentProvider.getParent(group.id(), parent.id());
        if (groupParent == null)
            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_PARENT_NOT_EXISTS, languageId)
                    .replace("[PARENT]", parent.groupName())
                    .replace("[GROUP]", group.groupName()));
        return groupParent;
    }
}
