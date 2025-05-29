package de.murmelmeister.essentials.commands.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ParentSubCommand extends PermissionUtil {
    private final GroupParent groupParent;
    private final UserParent userParent;

    public ParentSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupParent = group.getParent();
        this.userParent = user.getParent();
    }

    @Override
    public BrigadierCommand createCommand() {
        return null;
    }

    public int getParents(CommandContext<CommandSource> context, boolean isUser) {
        return runWithTiming(context, (source, executorId) -> {
            String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
            int id = isUser ? getUserId(source, name) : getGroupId(source, name);
            if (id == -2) return CommandResult.of(-2);

            List<Integer> parents = isUser ? userParent.getParentIds(id) : groupParent.getParentIds(id);
            if (parents.isEmpty()) {
                sendMessage(source, "<#999900>%s <#990000>has no parents.", name);
                return CommandResult.of(-3);
            }

            sendMessage(source, "<#999999>%s of <#00cc88>%s</#00cc88>:", parents.size() == 1 ? "Parent" : "Parents", name);
            String clickMessage = isUser ? "/permission user " + name + " parent remove " : "/permission group " + name + " parent remove ";
            parents.forEach(parent -> {
                String parentName = group.getGroupName(parent);
                if (isUser && (parent == group.getId("default"))) {
                    sendMessage(source, "<#999999>- <#999900>%s", parentName);
                    return;
                }

                Timestamp expiredAt = isUser ? userParent.getExpiredAt(id, parent) : groupParent.getExpiredAt(id, parent);
                String expiredDate = isUser ? userParent.getExpiredDate(id, parent) : groupParent.getExpiredDate(id, parent);
                sendMessage(source, "<#999999>- <#999900>" +
                                    "<hover:show_text:'<#990000>Click to remove <#999900>\"%s\"'>" +
                                    "<click:suggest_command:%s>%s</click></hover> %s",
                        parentName, clickMessage + parentName, parentName, formatExpiredMessage(expiredAt, expiredDate));
            });

            logging(isUser, executorId, id, "");
            return CommandResult.of(Command.SINGLE_SUCCESS);
        });
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
                            int id = getId(context, isUser);
                            List<String> haveParents = isUser ? userParent.getParentNames(group, id) : groupParent.getParentNames(group, id);
                            group.getGroupNames().stream()
                                    .filter(parent -> isUser || group.getId(parent) != id)
                                    .filter(parent -> !haveParents.contains(parent))
                                    .filter(parent -> StringUtil.startsWithIgnoreCase(parent, prefix))
                                    .forEach(parent -> builder.suggest(parent, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + parent))));
                            return builder.buildFuture();
                        })
                        .executes(context ->
                                runWithTiming(context, (source, executorId) -> {
                                    String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                                    int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                                    if (id == -2) return CommandResult.of(-2);

                                    String parentName = StringArgumentType.getString(context, "parent");
                                    int parentId = group.getId(parentName);
                                    boolean existParent = isUser ? userParent.existsParent(id, parentId) : groupParent.existsParent(id, parentId);
                                    if (existParent) {
                                        sendMessage(source, "<#990000>%s <#999999>is already a parent of <#00cc88>%s</#00cc88>.", parentName, name);
                                        return CommandResult.of(-4);
                                    }

                                    if (!isUser && parentId == id) {
                                        sendMessage(source, "<#990000>You cannot add the group as a parent to itself.");
                                        return CommandResult.of(-5);
                                    }

                                    int row;
                                    if (isUser) row = userParent.addParent(id, parentId, -1, executorId);
                                    else row = groupParent.addParent(id, parentId, -1, executorId);
                                    sendMessage(source, "<#999999>Parent <#00cc88>%s</#00cc88> added to <#999900>%s</#999900>.", parentName, name);

                                    RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                                    logging(isUser, executorId, id, "add " + parentName);
                                    return CommandResult.of(Command.SINGLE_SUCCESS, row);
                                })
                        )
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executorId) -> {
                                            String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                                            int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                                            if (id == -2) return CommandResult.of(-2);

                                            String parentName = StringArgumentType.getString(context, "parent");
                                            int parentId = group.getId(parentName);
                                            boolean existParent = isUser ? userParent.existsParent(id, parentId) : groupParent.existsParent(id, parentId);
                                            if (existParent) {
                                                sendMessage(source, "<#990000>%s <#999999>is already a parent of <#00cc88>%s</#00cc88>.", parentName, name);
                                                return CommandResult.of(-3);
                                            }

                                            if (!isUser && parentId == id) {
                                                sendMessage(source, "<#990000>You cannot add the group as a parent to itself.");
                                                return CommandResult.of(-4);
                                            }

                                            String time = StringArgumentType.getString(context, "time");
                                            long timeValue = TimeUtil.formatTime(time);

                                            if (timeValue == -2) {
                                                sendMessage(source, "<#990000>No negative value allowed");
                                                return CommandResult.of(-5);
                                            }

                                            if (timeValue == -3) {
                                                sendMessage(source, "<#990000>Invalid time format");
                                                return CommandResult.of(-6);
                                            }

                                            int row;
                                            if (isUser) row = userParent.addParent(id, parentId, timeValue, executorId);
                                            else row = groupParent.addParent(id, parentId, timeValue, executorId);
                                            sendMessage(source, "<#999999>Parent <#00cc88>%s</#00cc88> added to <#999900>%s</#999900> for <#009999>%s</#009999>.", parentName, name, time);

                                            RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                                            logging(isUser, executorId, id, "add " + parentName + " " + time);
                                            return CommandResult.of(Command.SINGLE_SUCCESS, row);
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
                        .suggests((context, builder) -> getSuggestionParent(context, builder, isUser, false))
                        .executes(context ->
                                runWithTiming(context, (source, executorId) -> {
                                    String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                                    int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                                    if (id == -2) return CommandResult.of(-2);

                                    String parentName = StringArgumentType.getString(context, "parent");
                                    int parentId = group.getId(parentName);
                                    if (isParentNotExist(source, isUser, id, parentId)) return CommandResult.of(-4);

                                    if (isUser && (parentId == group.getId("default"))) {
                                        sendMessage(source, "<#990000>You cannot remove default parent.");
                                        return CommandResult.of(-3);
                                    }

                                    int row;
                                    if (isUser) row = userParent.removeParent(id, parentId);
                                    else row = groupParent.removeParent(id, parentId);
                                    sendMessage(source, "<#999999>Parent <#00cc88>%s</#00cc88> removed from <#999900>%s</#999900>.", parentName, name);

                                    RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                                    logging(isUser, executorId, id, "remove " + parentName);
                                    return CommandResult.of(Command.SINGLE_SUCCESS, row);
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getParentClear(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("clear")
                .executes(context ->
                        runWithTiming(context, (source, executorId) -> {
                            String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                            int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                            if (id == -2) return CommandResult.of(-2);

                            int row;
                            if (isUser) {
                                row = userParent.clearParent(id) +
                                      userParent.addParent(id, group.getId("default"), -1, executorId);
                            } else row = groupParent.clearParent(id);
                            sendMessage(source, "<#999999>All parents removed from <#999900>%s</#999900>.", name);

                            RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                            logging(isUser, executorId, id, "clear");
                            return CommandResult.of(Command.SINGLE_SUCCESS, row);
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
                        .suggests((context, builder) -> getSuggestionParent(context, builder, isUser, true))
                        .executes(context ->
                                runWithTiming(context, (source, executorId) -> {
                                    String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                                    int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                                    if (id == -2) return CommandResult.of(-2);

                                    String parentName = StringArgumentType.getString(context, "parent");
                                    int parentId = group.getId(parentName);
                                    if (isParentNotExist(source, isUser, id, parentId)) return CommandResult.of(-3);

                                    Timestamp expiredAt = isUser ? userParent.getExpiredAt(id, parentId) : groupParent.getExpiredAt(id, parentId);
                                    String expiredDate = isUser ? userParent.getExpiredDate(id, parentId) : groupParent.getExpiredDate(id, parentId);

                                    int createdBy = isUser ? userParent.getCreatedBy(id, parentId) : groupParent.getCreatedBy(id, parentId);
                                    String creatorName = user.getUsername(createdBy);
                                    String createdDate = isUser ? userParent.getCreatedDate(id, parentId) : groupParent.getCreatedDate(id, parentId);
                                    int updatedBy = isUser ? userParent.getUpdatedBy(id, parentId) : groupParent.getUpdatedBy(id, parentId);
                                    String updaterName = user.getUsername(updatedBy);
                                    String updatedDate = isUser ? userParent.getUpdatedDate(id, parentId) : groupParent.getUpdatedDate(id, parentId);
                                    String nameType = isUser ? "Username" : "Group Name";
                                    String typeId = isUser ? "User ID" : "Group ID";

                                    String message = """
                                            <#999999>===- Parent info
                                            <#999999>%s: <#00cc88>%s
                                            <#999999>%s: <#00cc88>%s
                                            <#999999>Parent: <#00cc88>%s
                                            <#999999>Created by: <#00cc88>%s (%s)
                                            <#999999>Created date: <#00cc88>%s
                                            <#999999>Updated by: <#00cc88>%s (%s)
                                            <#999999>Updated date: <#00cc88>%s
                                            <#999999>Expired date: <#00cc88>%s"""
                                            .formatted(nameType, name, typeId, id, parentName,
                                                    createdBy, creatorName, createdDate,
                                                    updatedBy, updaterName, updatedDate, formatExpiredInfoMessage(expiredAt, expiredDate));
                                    sendMessage(source, message);

                                    logging(isUser, executorId, id, "info " + parentName);
                                    return CommandResult.of(Command.SINGLE_SUCCESS);
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
                        .suggests((context, builder) -> getSuggestionParent(context, builder, isUser, false))
                        .executes(context -> {
                            sendMessage(context.getSource(), syntaxParent(isUser));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(getSuggestionTime())
                                .executes(context ->
                                        runWithTiming(context, (source, executorId) -> {
                                            String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                                            int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                                            if (id == -2) return CommandResult.of(-2);

                                            String parentName = StringArgumentType.getString(context, "parent");
                                            int parentId = group.getId(parentName);
                                            if (isParentNotExist(source, isUser, id, parentId))
                                                return CommandResult.of(-3);

                                            if (isUser && (parentId == group.getId("default"))) {
                                                sendMessage(source, "<#990000>You cannot set time for default parent.");
                                                return CommandResult.of(-4);
                                            }

                                            String time = StringArgumentType.getString(context, "time");
                                            long timeValue = TimeUtil.formatTime(time);

                                            if (timeValue == -2) {
                                                sendMessage(source, "<#990000>No negative value allowed");
                                                return CommandResult.of(-5);
                                            }

                                            if (timeValue == -3) {
                                                sendMessage(source, "<#990000>Invalid time format");
                                                return CommandResult.of(-6);
                                            }

                                            int row;
                                            if (isUser)
                                                row = userParent.setExpiredAt(id, parentId, timeValue, executorId);
                                            else row = groupParent.setExpiredAt(id, parentId, timeValue, executorId);
                                            sendMessage(source, "<#999999>Parent <#00cc88>%s</#00cc88> time set to <#009999>%s</#009999>.", parentName, time);

                                            RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                                            logging(isUser, executorId, id, "time " + parentName + " " + time);
                                            return CommandResult.of(Command.SINGLE_SUCCESS, row);
                                        })
                                )
                        )
                );
    }

    private CompletableFuture<Suggestions> getSuggestionParent(CommandContext<CommandSource> context, SuggestionsBuilder builder, boolean isUser, boolean isDefaultAllowed) {
        String prefix = builder.getRemaining();
        int id = getId(context, isUser);
        List<String> parents = isUser ? userParent.getParentNames(group, id) : groupParent.getParentNames(group, id);
        parents.stream()
                .filter(parent -> (!isUser || isDefaultAllowed) || !parent.equals("default"))
                .filter(parent -> StringUtil.startsWithIgnoreCase(parent, prefix))
                .forEach(parent -> builder.suggest(parent, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + parent))));
        return builder.buildFuture();
    }

    private int getId(CommandContext<CommandSource> context, boolean isUser) {
        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        return isUser ? getUserId(context.getSource(), name) : getGroupId(context.getSource(), name);
    }

    private boolean isParentNotExist(CommandSource source, boolean isUser, int id, int parentId) {
        boolean exist = isUser ? userParent.existsParent(id, parentId) : groupParent.existsParent(id, parentId);
        if (!exist) {
            sendMessage(source, "<#990000>Parent <#999999>%s</#999999> does not exist.", parentId);
            return true;
        } else return false;
    }

    private void logging(boolean isUser, int executorId, int id, String fullCommand) {
        String command = isUser ? "/permission user " + user.getUsername(id) + " parent " : "/permission group " + group.getGroupName(id) + " parent ";
        loggingToConsole(isUser, executorId, id, command + fullCommand);
    }
}
