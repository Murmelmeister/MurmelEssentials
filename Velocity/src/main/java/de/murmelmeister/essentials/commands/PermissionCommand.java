package de.murmelmeister.essentials.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.commands.subcomamnd.SubGroupEdit;
import de.murmelmeister.essentials.commands.subcomamnd.SubParent;
import de.murmelmeister.essentials.commands.subcomamnd.SubPermission;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.StringUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class PermissionCommand extends PermissionUtil {
    private final Logger logger;

    private final User user;
    private final Group group;

    private final SubGroupEdit subGroupEdit;
    private final SubParent subParent;
    private final SubPermission subPermission;

    public PermissionCommand(Logger logger, Permission permission, User user, Group group) {
        this.logger = logger;
        this.user = user;
        this.group = group;
        GroupColor groupColor = group.getColor();
        this.subGroupEdit = new SubGroupEdit(logger, user, group, groupColor);
        this.subParent = new SubParent(logger, user, group);
        this.subPermission = new SubPermission(logger, permission, user, group);
    }

    public BrigadierCommand createCommand() {
        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder("permission")
                .requires(source -> source.hasPermission("murmelessentials.command.permission"))
                .executes(context -> {
                    syntax(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(getGroups())
                .then(getUsers())
                .then(getGroup())
                .then(getUser())
                .build();
        return new BrigadierCommand(rootNode);
    }

    private LiteralArgumentBuilder<CommandSource> getGroups() {
        return BrigadierCommand.literalArgumentBuilder("groups")
                .executes(context -> {
                    int executorId = getExecutorId(context);
                    logging(executorId, "Get all groups", "/permission groups");

                    CommandSource source = context.getSource();
                    sendMessage(source, "<#009999>Groups: ");
                    for (String name : group.getNames())
                        sendMessage(source, "<#999999>- <#999900>" + name);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> getUsers() {
        return BrigadierCommand.literalArgumentBuilder("users")
                .executes(context -> {
                    int executorId = getExecutorId(context);
                    logging(executorId, "Get all users", "/permission users");

                    CommandSource source = context.getSource();
                    sendMessage(source, "<#009999>Users: ");
                    for (String name : user.getUsernames())
                        sendMessage(source, "<#999999>- <#999900>" + name);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private LiteralArgumentBuilder<CommandSource> getGroup() {
        return BrigadierCommand.literalArgumentBuilder("group")
                .executes(context -> {
                    syntax(context.getSource(), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(getGroupPermission())
                .then(getGroupParent())
                .then(getGroupEdit())
                .then(getGroupRename(this::getExecutorId, this::getGroupId))
                .then(getGroupDelete(this::getExecutorId, this::getGroupId))
                .then(getGroupCreate(this::getExecutorId))
                .then(getGroupInfo());
    }

    private LiteralArgumentBuilder<CommandSource> getUser() {
        return BrigadierCommand.literalArgumentBuilder("user")
                .executes(context -> {
                    syntax(context.getSource(), true);
                    return Command.SINGLE_SUCCESS;
                })
                .then(getUserPermission())
                .then(getUserParent());
    }

    private LiteralArgumentBuilder<CommandSource> getGroupInfo() {
        // -/permission group info <group>
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    syntax(context.getSource(), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.word())
                        .suggests(this::getGroupNames)
                        .executes(context -> {
                            int groupId = getGroupId(context);
                            if (groupId == -2) return -1;

                            String groupName = group.getName(groupId);
                            int executorId = getExecutorId(context);
                            logging(false, executorId, groupId, "Get group infos", "/permission group info " + groupName);
                            CommandSource source = context.getSource();

                            sendMessage(source, "<#999999>Group <#009999>%s</#009999> information:", groupName);
                            sendMessage(source, "<#999999>GroupId: <#009999>%d</#009999>", groupId);
                            sendMessage(source, "<#999999>Priority: <#009999>%d</#009999>", group.getPriority(groupId));
                            sendMessage(source, "<#999999>Team: <#009999>%s</#009999>", group.getTeamSort(groupId));
                            sendMessage(source, "<#999999>Created by: <#009999>%s</#009999>", user.getUsername(group.getCreatedBy(groupId)));
                            sendMessage(source, "<#999999>Created at: <#009999>%s</#009999>", group.getCreatedAt(groupId).toString());
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupEdit() {
        // -/permission group edit <group>
        return BrigadierCommand.literalArgumentBuilder("edit")
                .executes(context -> {
                    syntaxGroupEdit(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.word())
                        .suggests(this::getGroupNames)
                        .executes(context -> {
                            syntaxGroupEdit(context.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(subGroupEdit.getEditChat(this::getExecutorId, this::getGroupId))
                        .then(subGroupEdit.getEditTab(this::getExecutorId, this::getGroupId))
                        .then(subGroupEdit.getEditTeam(this::getExecutorId, this::getGroupId))
                        .then(subGroupEdit.getEditPriority(this::getExecutorId, this::getGroupId))
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupParent() {
        // -/permission group parent <group>
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context -> {
                    syntaxParent(context.getSource(), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.word())
                        .suggests(this::getGroupNames)
                        .executes(context -> subParent.getParents(false, context, this::getExecutorId, this::getGroupId))
                        .then(subParent.getParentAdd(false, this::getExecutorId, this::getGroupId))
                        .then(subParent.getParentRemove(false, this::getExecutorId, this::getGroupId))
                        .then(subParent.getParentClear(false, this::getExecutorId, this::getGroupId))
                        .then(subParent.getParentInfo(false, this::getExecutorId, this::getGroupId))
                        .then(subParent.getParentTime(false, this::getExecutorId, this::getGroupId))
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupPermission() {
        // -/permission group permission <group>
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context -> {
                    syntaxPermission(context.getSource(), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.word())
                        .suggests(this::getGroupNames)
                        .executes(context -> subPermission.getPermissions(false, context, this::getExecutorId, this::getGroupId))
                        .then(subPermission.getPermissionAll(false, this::getExecutorId, this::getGroupId))
                        .then(subPermission.getPermissionAdd(false, this::getExecutorId, this::getGroupId))
                        .then(subPermission.getPermissionRemove(false, this::getExecutorId, this::getGroupId))
                        .then(subPermission.getPermissionClear(false, this::getExecutorId, this::getGroupId))
                        .then(subPermission.getPermissionInfo(false, this::getExecutorId, this::getGroupId))
                        .then(subPermission.getPermissionTime(false, this::getExecutorId, this::getGroupId))
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupRename(Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> groupSupplier) {
        // -/permission group rename <group> <newName>
        return BrigadierCommand.literalArgumentBuilder("rename")
                .executes(context -> {
                    syntax(context.getSource(), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.word())
                        .suggests(this::getGroupNames)
                        .executes(context -> {
                            syntax(context.getSource(), false);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("newName", StringArgumentType.word())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);

                                    String oldName = group.getName(groupId);
                                    String newName = StringArgumentType.getString(context, "newName");
                                    logging(false, executorId, groupId, "Rename group", "/permission group rename " + oldName + " " + newName);
                                    String teamId = group.getTeamSort(groupId);

                                    group.rename(executorId, groupId, newName);
                                    group.setTeamSort(executorId, groupId, teamId.replace(oldName, newName));
                                    sendMessage(context.getSource(), "<#999999>Group <#009999>%s</#009999> is now renamed to <#999900>%s</#999900>.", oldName, newName);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupDelete(Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> groupSupplier) {
        // -/permission group delete <group>
        return BrigadierCommand.literalArgumentBuilder("delete")
                .executes(context -> {
                    syntax(context.getSource(), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.word())
                        .suggests(this::getGroupNames)
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int groupId = groupSupplier.apply(context);
                            String groupName = group.getName(groupId);
                            logging(false, executorId, groupId, "Delete group", "/permission group delete " + groupName);

                            if (groupId == group.getUniqueId("default")) {
                                sendMessage(context.getSource(), "<#990000>You can not delete the default group.");
                                return Command.SINGLE_SUCCESS;
                            }

                            group.deleteGroup(executorId, groupId); // TODO: Delete the group overall because it is not deleted in the database
                            sendMessage(context.getSource(), "<#999999>Group <#999900>%s</#999900> is now deleted.", groupName);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private LiteralArgumentBuilder<CommandSource> getGroupCreate(Function<CommandContext<CommandSource>, Integer> executorSupplier) {
        // -/permission group create <group> <priority> <teamId>
        return BrigadierCommand.literalArgumentBuilder("create")
                .executes(context -> {
                    syntax(context.getSource(), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("group", StringArgumentType.word())
                        .executes(context -> {
                            syntax(context.getSource(), false);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("priority", IntegerArgumentType.integer())
                                .executes(context -> {
                                    syntax(context.getSource(), false);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(BrigadierCommand.requiredArgumentBuilder("teamId", StringArgumentType.word())
                                        .executes(context -> {
                                            int executorId = executorSupplier.apply(context);
                                            String groupName = StringArgumentType.getString(context, "group");

                                            if (group.existsGroup(groupName)) {
                                                sendMessage(context.getSource(), "<#990000>Group already exists.");
                                                return -1;
                                            }

                                            int priority = IntegerArgumentType.getInteger(context, "priority");
                                            String teamId = StringArgumentType.getString(context, "teamId");

                                            group.createNewGroup(groupName, executorId, priority, teamId);
                                            logging(false, executorId, group.getUniqueId(groupName), "Create group", "/permission group create " + groupName + " " + priority + " " + teamId);
                                            sendMessage(context.getSource(), "<#999999>Group <#009999>%s</#009999> is now created.", groupName);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUserParent() {
        // -/permission user parent <user>
        return BrigadierCommand.literalArgumentBuilder("parent")
                .executes(context -> {
                    syntaxParent(context.getSource(), true);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                        .suggests(this::getUsernames)
                        .executes(context -> subParent.getParents(true, context, this::getExecutorId, this::getUserId))
                        .then(subParent.getParentAdd(true, this::getExecutorId, this::getUserId))
                        .then(subParent.getParentRemove(true, this::getExecutorId, this::getUserId))
                        .then(subParent.getParentClear(true, this::getExecutorId, this::getUserId))
                        .then(subParent.getParentInfo(true, this::getExecutorId, this::getUserId))
                        .then(subParent.getParentTime(true, this::getExecutorId, this::getUserId))
                );
    }

    private LiteralArgumentBuilder<CommandSource> getUserPermission() {
        // -/permission user permission <user>
        return BrigadierCommand.literalArgumentBuilder("permission")
                .executes(context -> {
                    syntaxPermission(context.getSource(), true);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("user", StringArgumentType.word())
                        .suggests(this::getUsernames)
                        .executes(context -> subPermission.getPermissions(true, context, this::getExecutorId, this::getUserId))
                        .then(subPermission.getPermissionAll(true, this::getExecutorId, this::getUserId))
                        .then(subPermission.getPermissionAdd(true, this::getExecutorId, this::getUserId))
                        .then(subPermission.getPermissionRemove(true, this::getExecutorId, this::getUserId))
                        .then(subPermission.getPermissionClear(true, this::getExecutorId, this::getUserId))
                        .then(subPermission.getPermissionInfo(true, this::getExecutorId, this::getUserId))
                        .then(subPermission.getPermissionTime(true, this::getExecutorId, this::getUserId))
                );
    }

    private int getExecutorId(CommandContext<CommandSource> context) {
        return getExecutorId(context, user);
    }

    private int getGroupId(CommandContext<CommandSource> context) {
        String groupName = StringArgumentType.getString(context, "group");
        if (isGroupNotExist(context.getSource(), group, groupName)) return -2;
        return group.getUniqueId(groupName);
    }

    private int getUserId(CommandContext<CommandSource> context) {
        String username = StringArgumentType.getString(context, "user");
        if (isUserNotExist(context.getSource(), user, username)) return -2;
        return user.getId(username);
    }

    private CompletableFuture<Suggestions> getGroupNames(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        group.getNames().stream()
                .parallel()
                .filter(s -> StringUtil.startsWithIgnoreCase(s, prefix))
                .sorted().toList().forEach(name -> builder.suggest(name,
                        VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + name))));
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> getUsernames(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        user.getUsernames().stream()
                .parallel()
                .filter(s -> StringUtil.startsWithIgnoreCase(s, prefix))
                .sorted().toList().forEach(username -> builder.suggest(username,
                        VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + username))));
        return builder.buildFuture();
    }

    private void logging(boolean isUser, int executorId, int id, String doing, String fullCommand) {
        loggingToConsole(logger, user, group, isUser, executorId, id, doing, fullCommand);
    }

    private void logging(int executorId, String doing, String fullCommand) {
        loggingToConsole(logger, user, executorId, doing, fullCommand);
    }

    /*@Override
    public void execute(Invocation invocation) {
        var args = invocation.arguments();
        var source = invocation.source();

        if (!source.hasPermission("murmelessentials.command.permission")) {
            sendMessage(source, "§cYou do not have permission to use this command.");
            return;
        }

        try {
            if (args.length == 1) {
                switch (args[0]) {
                    case "groups" -> {
                        sendMessage(source, "§3Groups: ");
                        for (var name : group.getNames())
                            sendMessage(source, "§7- §e" + name);
                    }
                    case "users" -> {
                        sendMessage(source, "§3Users: ");
                        for (var name : user.getUsernames())
                            sendMessage(source, "§7- §e" + name);
                    }
                    default -> syntax(source);
                }
                return;
            }

            var player = source instanceof Player ? (Player) source : null;
            var playerId = player != null ? player.getUniqueId() : null;
            var creatorId = playerId == null ? -1 : user.getId(playerId);
            if (args.length >= 3) {
                switch (args[0]) {
                    case "group" -> groups(source, creatorId, args);
                    case "user" -> users(source, creatorId, args);
                    default -> syntax(source);
                }
            } else syntax(source);
        } catch (IllegalArgumentException e) {
            sendMessage(source, "§cError: " + e.getMessage());
        }
    }*/

    /*@Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> {
            var args = invocation.arguments();
            if (args.length == 1)
                return Stream.of("user", "users", "group", "groups").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 2 && args[0].equals("group")) // Show all group names
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 2 && args[0].equals("user")) // Show all usernames
                return user.getUsernames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 3 && args[0].equals("group")) // Show all group commands
                return Stream.of("create", "delete", "rename", "parent", "permission", "edit").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 3 && args[0].equals("user")) // Show all user commands
                return Stream.of("parent", "permission").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && (args[0].equals("group") || args[0].equals("user")) && args[2].equals("parent")) // Show all group/user parent commands
                return Stream.of("add", "remove", "clear", "info", "time").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && (args[0].equals("group") || args[0].equals("user")) && args[2].equals("permission")) // Show all group/user permission commands
                return Stream.of("all", "add", "remove", "clear", "info", "time").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 4 && args[0].equals("group") && args[2].equals("edit")) // Show all group edit commands
                return Stream.of("chat", "tab", "tag", "sort", "team").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("edit") && (args[3].equals("chat") || args[3].equals("tab") || args[3].equals("tag"))) // Show all group edit subcommands
                return Stream.of("prefix", "suffix", "color").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("parent") && args[3].equals("add")) // Add group parent
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("parent") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired group parent
                return groupParent.getParentNames(group, group.getUniqueId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("group") && args[2].equals("permission") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired group permission
                return groupPermission.getPermissions(group.getUniqueId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("parent") && args[3].equals("add")) // Add user parent
                return group.getNames().stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("parent") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired user parent
                return userParent.getParentNames(group, user.getId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 5 && args[0].equals("user") && args[2].equals("permission") && (args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) // Remove/Creator/Created/Expired user permission
                return userPermission.getPermissions(user.getId(args[1])).stream().filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            if (args.length == 7 && (args[0].equals("group") || args[0].equals("user")) && (args[2].equals("parent") || args[2].equals("permission")) && args[3].equals("time")) // Set/Remove/Expired time
                return Stream.of("set", "add", "remove").filter(s -> StringUtil.startsWithIgnoreCase(s, args[args.length - 1])).sorted().collect(Collectors.toList());
            return Collections.emptyList();
        });
    }*/

    /*private void groups(CommandSource source, int userLog, String[] args) {
        var groupName = args[1];
        if (!group.existsGroup(groupName)) {
            if (args[2].equals("create")) createGroup(source, groupName, userLog, args);
            else sendMessage(source, "§cGroup does not exist.");
            return;
        }

        var groupId = group.getUniqueId(groupName);
        switch (args[2]) {
            case "create" -> sendMessage(source, "§cGroup already exists.");
            case "delete" -> deleteGroup(source, groupId, userLog, groupName);
            case "rename" -> renameGroup(source, groupId, userLog, args);
            case "parent" -> subParent.parent(source, false, userLog, groupId, args);
            case "permission" -> subPermission.permission(source, false, userLog, groupId, args);
            case "edit" -> subGroupEdit.groupEdit(source, userLog, groupId, args);
            default -> syntax(source, false);
        }
    }*/

    /*private void users(CommandSource source, int userLog, String[] args) {
        var username = args[1];
        if (isUserNotExist(source, user, username)) return;

        var userId = user.getId(username);
        switch (args[2]) {
            case "parent" -> subParent.parent(source, true, userLog, userId, args);
            case "permission" -> subPermission.permission(source, true, userLog, userId, args);
            default -> syntax(source, true);
        }
    }*/

    /*private void createGroup(CommandSource source, String groupName, int createdBy, String[] args) {
        if (args.length == 3) {
            syntax(source, false);
            return;
        }
        try {
            Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sendMessage(source, "§cInvalid sort id");
            return;
        }
        if (args.length == 4) {
            syntax(source, false);
            return;
        }
        try {
            Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sendMessage(source, "§cInvalid team id");
            return;
        }
        group.createNewGroup(groupName, createdBy, Integer.parseInt(args[3]), args[4]);
        //MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Group §e%s §3is now created.", groupName);
    }*/

    /*private void deleteGroup(CommandSource source, int userLog, int groupId, String groupName) {
        if (groupId == group.getUniqueId("default")) {
            sendMessage(source, "§cYou can not delete the default group.");
            return;
        }
        group.deleteGroup(userLog, groupId);
        //MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Group §e%s §3is now deleted.", groupName);
    }*/

    /*private void renameGroup(CommandSource source, int userLog, int groupId, String[] args) {
        if (args.length == 3) {
            syntax(source, false);
            return;
        }
        var oldName = group.getName(groupId);
        var newName = args[3];
        var teamId = group.getTeamSort(groupId);
        group.rename(userLog, groupId, newName);
        group.setTeamSort(userLog, groupId, teamId.replace(oldName, newName));
        //MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Group is now renamed to §e%s", args[3]);
    }*/
}
