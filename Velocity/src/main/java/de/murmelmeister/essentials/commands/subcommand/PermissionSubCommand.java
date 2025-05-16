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
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class PermissionSubCommand extends PermissionUtil {
    private final GroupParent groupParent;
    private final GroupPermission groupPermission;
    private final UserPermission userPermission;

    public PermissionSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupParent = group.getParent();
        this.groupPermission = group.getPermission();
        this.userPermission = user.getPermission();
    }

    @Override
    public BrigadierCommand createCommand() {
        return null;
    }

    public int getPermissions(CommandContext<CommandSource> context, boolean isUser) {
        long startTime = System.nanoTime();
        CommandSource source = context.getSource();
        int executorId = getExecutorId(source);
        if (executorId == -2) return -1;

        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        int id = isUser ? getUserId(source, name) : getGroupId(source, name);
        if (isUser ? id == -2 : id == 0) return -2;

        List<String> permissions = isUser ? userPermission.getPermissions(id) : groupPermission.getPermissions(id);
        if (permissions.isEmpty()) {
            sendMessage(source, "<#999900>%s <#990000>has no permissions.", name);
            return -3;
        }

        sendMessage(source, "<#999999>%s of <#00cc88>%s</#00cc88>:", permissions.size() == 1 ? "Permission" : "Permissions", name);
        String clickMessage = isUser ? "/permission user " + name + " permission remove " : "/permission group " + name + " permission remove ";
        for (String permission : permissions) {
            String expiredDate = isUser ? userPermission.getExpiredDate(id, permission) : groupPermission.getExpiredDate(id, permission);
            String expiredMessage = expiredDate == null ? "" : "<#999999>- Expired date: <#00cc88>" + expiredDate;
            sendMessage(source, "<#999999>- <#999900>" +
                                "<hover:show_text:'<#990000>Click to remove <#999900>\"%s\"'>" +
                                "<click:suggest_command:%s>%s</click></hover> %s",
                    permission, clickMessage + (permission.equals("*") || permission.endsWith(".*") ? "\"" + permission + "\"" : permission),
                    permission, expiredMessage);
        }

        logging(isUser, executorId, id, "Get permissions", "");
        if (user.isDebugMode(executorId)) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
        }
        return Command.SINGLE_SUCCESS;
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionAll(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("all")
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    int executorId = getExecutorId(source);
                    if (executorId == -2) return -1;

                    String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                    int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                    if (isUser ? id == -2 : id == 0) return -2;

                    List<String> permissions = isUser ? permission.getPermissions(id) : groupPermission.getAllPermissions(groupParent, id);
                    if (permissions.isEmpty()) {
                        sendMessage(source, "<#999900>%s <#990000>has no permissions.", name);
                        return -3;
                    }

                    sendMessage(source, "<#999999>%s of <#00cc88>%s</#00cc88>:", permissions.size() == 1 ? "Permission" : "Permissions", name);
                    for (String permission : permissions) {
                        String expiredDate = isUser ? userPermission.getExpiredDate(id, permission) : groupPermission.getExpiredDate(id, permission);
                        String expiredMessage = expiredDate == null ? "" : "<#999999>- Expired date: <#00cc88>" + expiredDate;
                        sendMessage(source, "<#999999>- <#999900>%s %s", permission, expiredMessage);
                    }

                    logging(isUser, executorId, id, "Get all permissions", "");
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionAdd(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .executes(context -> {
                            long startTime = System.nanoTime();
                            CommandSource source = context.getSource();
                            int executorId = getExecutorId(source);
                            if (executorId == -2) return -1;

                            String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                            int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                            if (isUser ? id == -2 : id == 0) return -2;

                            String permission = StringArgumentType.getString(context, "permission");
                            boolean exists = isUser ? userPermission.existsPermission(id, permission) : groupPermission.existsPermission(id, permission);
                            if (exists) {
                                sendMessage(source, "<#990000>Permission <#999900>%s</#999900> already exists.", permission);
                                return -3;
                            }

                            int row;
                            if (isUser) row = userPermission.addPermission(id, permission, -1, executorId);
                            else row = groupPermission.addPermission(id, permission, -1, executorId);
                            sendMessage(source, "<#999999>Permission <#009999>%s</#009999> is now added to <#990099>%s</#990099>.", permission, name);

                            RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                            logging(isUser, executorId, id, "Add permission", "add " + permission);
                            if (user.isDebugMode(executorId)) {
                                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
                                sendDebugMessage(source, "<#999900>Rows: %s", row);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(this::getSuggestionTime)
                                .executes(context -> {
                                    long startTime = System.nanoTime();
                                    CommandSource source = context.getSource();
                                    int executorId = getExecutorId(source);
                                    if (executorId == -2) return -1;

                                    String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                                    int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                                    if (isUser ? id == -2 : id == 0) return -2;

                                    String permission = StringArgumentType.getString(context, "permission");
                                    boolean exists = isUser ? userPermission.existsPermission(id, permission) : groupPermission.existsPermission(id, permission);
                                    if (exists) {
                                        sendMessage(source, "<#990000>Permission <#999900>%s</#999900> already exists.", permission);
                                        return -3;
                                    }

                                    String time = StringArgumentType.getString(context, "time");
                                    long timeValue = TimeUtil.formatTime(time);

                                    if (timeValue == -2) {
                                        sendMessage(source, "<#990000>No negative value allowed");
                                        return -4;
                                    }

                                    if (timeValue == -3) {
                                        sendMessage(source, "<#990000>Invalid time format");
                                        return -5;
                                    }

                                    int row;
                                    if (isUser)
                                        row = userPermission.addPermission(id, permission, timeValue, executorId);
                                    else row = groupPermission.addPermission(id, permission, timeValue, executorId);
                                    sendMessage(source, "<#999999>Permission <#009999>%s</#009999> is now added to <#990099>%s</#990099> for <#009999>%s</#009999>.", permission, name, time);

                                    RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                                    logging(isUser, executorId, id, "Add permission", "add " + permission + " " + time);
                                    if (user.isDebugMode(executorId)) {
                                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                        sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
                                        sendDebugMessage(source, "<#999900>Rows: %s", row);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
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
                        .suggests((context, builder) -> getSuggestionPermission(context, builder, isUser))
                        .executes(context -> {
                            long startTime = System.nanoTime();
                            CommandSource source = context.getSource();
                            int executorId = getExecutorId(source);
                            if (executorId == -2) return -1;

                            String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                            int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                            if (isUser ? id == -2 : id == 0) return -2;

                            String permission = StringArgumentType.getString(context, "permission");
                            if (isPermissionNotExist(source, isUser, id, permission)) return -3;

                            int row;
                            if (isUser) row = userPermission.removePermission(id, permission);
                            else row = groupPermission.removePermission(id, permission);
                            sendMessage(source, "<#999999>Permission <#009999>%s</#009999> is now removed from <#990099>%s</#990099>.", permission, name);

                            RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                            logging(isUser, executorId, id, "Remove permission", "remove " + permission);
                            if (user.isDebugMode(executorId)) {
                                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
                                sendDebugMessage(source, "<#999900>Rows: %s", row);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionClear(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("clear")
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();
                    int executorId = getExecutorId(source);
                    if (executorId == -2) return -1;

                    String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                    int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                    if (isUser ? id == -2 : id == 0) return -2;

                    int row;
                    if (isUser) row = userPermission.clearPermission(id);
                    else row = groupPermission.clearPermission(id);
                    sendMessage(source, "<#999999>All permissions are now removed from <#990099>%s</#990099>.", name);

                    RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                    logging(isUser, executorId, id, "Clear permissions", "clear");
                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
                        sendDebugMessage(source, "<#999900>Rows: %s", row);
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionInfo(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests((context, builder) -> getSuggestionPermission(context, builder, isUser))
                        .executes(context -> {
                            long startTime = System.nanoTime();
                            CommandSource source = context.getSource();
                            int executorId = getExecutorId(source);
                            if (executorId == -2) return -1;

                            String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                            int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                            if (isUser ? id == -2 : id == 0) return -2;

                            String permission = StringArgumentType.getString(context, "permission");
                            if (isPermissionNotExist(source, isUser, id, permission)) return -3;

                            String expiredDate = isUser ? userPermission.getExpiredDate(id, permission) : groupPermission.getExpiredDate(id, permission);
                            String expiredMessage = expiredDate == null ? "never" : expiredDate;
                            int createdBy = isUser ? userPermission.getCreatedBy(id, permission) : groupPermission.getCreatedBy(id, permission);
                            String creatorName = user.getUsername(createdBy);
                            String createdDate = isUser ? userPermission.getCreatedDate(id, permission) : groupPermission.getCreatedDate(id, permission);
                            int updatedBy = isUser ? userPermission.getUpdatedBy(id, permission) : groupPermission.getUpdatedBy(id, permission);
                            String updaterName = user.getUsername(updatedBy);
                            String updatedDate = isUser ? userPermission.getUpdatedDate(id, permission) : groupPermission.getUpdatedDate(id, permission);
                            String nameType = isUser ? "Username" : "Group Name";
                            String typeId = isUser ? "User ID" : "Group ID";

                            String message = """
                                    <#999999>===- Permission info
                                    <#999999>%s: <#00cc88>%s
                                    <#999999>%s: <#00cc88>%s
                                    <#999999>Permission: <#00cc88>%s
                                    <#999999>Created by: <#00cc88>%s (%s)
                                    <#999999>Created date: <#00cc88>%s
                                    <#999999>Updated by: <#00cc88>%s (%s)
                                    <#999999>Updated date: <#00cc88>%s
                                    <#999999>Expired date: <#00cc88>%s"""
                                    .formatted(nameType, name, typeId, id, permission, createdBy, creatorName, createdDate, updatedBy, updaterName, updatedDate, expiredMessage);
                            sendMessage(source, message);

                            logging(isUser, executorId, id, "Get permission info", "info " + permission);
                            if (user.isDebugMode(executorId)) {
                                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    public LiteralArgumentBuilder<CommandSource> getPermissionTime(boolean isUser) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxPermission(isUser));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests((context, builder) -> getSuggestionPermission(context, builder, isUser))
                        .executes(context -> {
                            sendMessage(context.getSource(), syntaxPermission(isUser));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(this::getSuggestionTime)
                                .executes(context -> {
                                    long startTime = System.nanoTime();
                                    CommandSource source = context.getSource();
                                    int executorId = getExecutorId(source);
                                    if (executorId == -2) return -1;

                                    String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
                                    int id = isUser ? getUserId(source, name) : getGroupId(source, name);
                                    if (isUser ? id == -2 : id == 0) return -2;

                                    String permission = StringArgumentType.getString(context, "permission");
                                    if (isPermissionNotExist(source, isUser, id, permission)) return -3;

                                    String time = StringArgumentType.getString(context, "time");
                                    long timeValue = TimeUtil.formatTime(time);

                                    if (timeValue == -2) {
                                        sendMessage(source, "<#990000>No negative value allowed");
                                        return -4;
                                    }

                                    if (timeValue == -3) {
                                        sendMessage(source, "<#990000>Invalid time format");
                                        return -5;
                                    }

                                    int row;
                                    if (isUser)
                                        row = userPermission.setExpiredAt(id, permission, timeValue, executorId);
                                    else row = groupPermission.setExpiredAt(id, permission, timeValue, executorId);
                                    sendMessage(source, "<#999999>Permission <#009999>%s</#009999> is now set to <#009999>%s</#009999>.", permission, time);

                                    RefreshUtil.markAsRefreshed(RefreshType.GLOBAL); // TODO: changing the right cache name
                                    logging(isUser, executorId, id, "Set expired date", "time " + permission + " " + time);
                                    if (user.isDebugMode(executorId)) {
                                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                        sendDebugMessage(source, "<#999900>Permission command executed in %s ms", durationMs);
                                        sendDebugMessage(source, "<#999900>Rows: %s", row);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    private CompletableFuture<Suggestions> getSuggestionPermission(CommandContext<CommandSource> context, SuggestionsBuilder builder, boolean isUser) {
        String prefix = builder.getRemaining();
        String name = isUser ? StringArgumentType.getString(context, "username") : StringArgumentType.getString(context, "groupName");
        int id = isUser ? getUserId(context.getSource(), name) : getGroupId(context.getSource(), name);
        List<String> permissions = isUser ? userPermission.getPermissions(id) : groupPermission.getPermissions(id);
        if (permissions.isEmpty()) return builder.buildFuture();
        permissions.stream()
                .map(permission -> {
                    if ("*".equals(permission) || permission.endsWith(".*"))
                        return "\"" + permission + "\"";
                    return permission;
                })
                .filter(permission -> permission.startsWith(prefix))
                .forEach(permission -> builder.suggest(permission, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<#00cc88>" + permission))));
        return builder.buildFuture();
    }

    private boolean isPermissionNotExist(CommandSource source, boolean isUser, int id, String permission) {
        boolean exist = isUser ? userPermission.existsPermission(id, permission) : groupPermission.existsPermission(id, permission);
        if (!exist) {
            sendMessage(source, "<#990000>Permission <#999900>%s</#999900> does not exist.", permission);
            return true;
        } else return false;
    }

    private void logging(boolean isUser, int executorId, int id, String doing, String fullCommand) {
        String command = isUser ? "/permission user " + user.getUsername(id) + " permission " : "/permission group " + group.getGroupName(id) + " permission ";
        loggingToConsole(isUser, executorId, id, doing, command + fullCommand);
    }
}
