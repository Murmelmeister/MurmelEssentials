package de.murmelmeister.essentials.commands.subcomamnd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class SubPermission extends PermissionUtil {
    private final Logger logger;
    private final Permission permission;
    private final User user;
    private final Group group;
    private final GroupParent groupParent;
    private final UserPermission userPermission;
    private final GroupPermission groupPermission;

    public SubPermission(Logger logger, Permission permission, User user, Group group) {
        this.logger = logger;
        this.permission = permission;
        this.user = user;
        this.group = group;
        this.groupParent = group.getParent();
        this.userPermission = user.getPermission();
        this.groupPermission = group.getPermission();
    }

    public int getPermissions(boolean isUser, CommandContext<CommandSource> context, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        int id = idSupplier.apply(context);
        if (id == -2) return -1;

        int executorId = executorSupplier.apply(context);
        logging(isUser, executorId, id, "Get all permissions", "");

        CommandSource source = context.getSource();
        List<String> permissions = isUser ? userPermission.getPermissions(id) : groupPermission.getPermissions(id);

        if (permissions.isEmpty()) {
            sendMessage(source, "<#990000>No permissions found.");
            return -2;
        }

        String clickMessage = isUser ? "/permission user permission " + user.getUsername(id) + " remove " : "/permission group permission " + group.getName(id) + " remove ";
        sendMessage(source, "<#009999>Permissions: ");
        for (String all : permissions) {
            Timestamp duration = isUser ? userPermission.getExpiredAt(id, all) : groupPermission.getExpiredAt(id, all);
            String expiredDate = duration == null ? "" : "<#999999> - Expired data: <#009999>" + MurmelAPI.getDateFormat().format(duration);
            sendMessage(source, "<#999999>- <#999900>" +
                                "<hover:show_text:'<#990000>Click to remove <#999900>\"%s\"'>" +
                                "<click:suggest_command:%s>%s</click></hover>%s",
                    all, clickMessage + (all.equals("*") || all.endsWith(".*") ? "\"" + all + "\"" : all),
                    all, expiredDate);
        }
        return Command.SINGLE_SUCCESS;
    }

    /*public void permission(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        if (args.length == 3) {
            sendMessage(source, "§3Permissions: ");
            var permissions = isUser ? userPermission.getPermissions(id) : groupPermission.getPermissions(id);
            for (var all : permissions)
                sendMessage(source, "§7- §e%s", all);
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) {
            syntaxPermission(source, isUser);
            return;
        }

        switch (args[3]) {
            case "all" -> allPermission(source, isUser, id);
            case "add" -> addPermission(source, isUser, userLog, id, args);
            case "remove" -> removePermission(source, isUser, userLog, id, args);
            case "clear" -> clearPermission(source, isUser, userLog, id);
            case "info" -> infoPermission(source, isUser, id, args);
            case "time" -> timePermission(source, isUser, userLog, id, args);
            default -> syntaxPermission(source, isUser);
        }
    }*/

    public LiteralArgumentBuilder<CommandSource> getPermissionAll(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("all")
                .executes(context -> {
                    int id = idSupplier.apply(context);
                    if (id == -2) return -1;

                    int executorId = executorSupplier.apply(context);
                    logging(isUser, executorId, id, "Get all permissions with parents", " all");

                    CommandSource source = context.getSource();
                    List<String> list = isUser ? permission.getPermissions(id) : groupPermission.getAllPermissions(groupParent, id);
                    if (list.isEmpty()) {
                        sendMessage(source, "<#990000>No permissions found.");
                        return -2;
                    }

                    sendMessage(source, "<#009999>All permissions of <#990099>%s</#990099>: ", getName(isUser, id));
                    for (String perm : list)
                        sendMessage(source, "<#999999>- <#999900>%s", perm);
                    return Command.SINGLE_SUCCESS;
                });
    }

    /*private void allPermission(CommandSource source, boolean isUser, int id) {
        var permissions = isUser ? this.permission.getPermissions(id) : groupPermission.getAllPermissions(groupParent, id);
        sendMessage(source, "§3All permissions: ");
        for (var all : permissions)
            sendMessage(source, "§7- §e%s", all);
    }*/

    public LiteralArgumentBuilder<CommandSource> getPermissionAdd(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    syntaxPermission(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int id = idSupplier.apply(context);
                            if (id == -2) return -1;

                            CommandSource source = context.getSource();
                            String input = StringArgumentType.getString(context, "permission");
                            logging(isUser, executorId, id, "Add permission (lifetime)", " add " + input);
                            boolean hasPermission = isUser ? userPermission.existsPermission(id, input) : groupPermission.existsPermission(id, input);
                            if (hasPermission) {
                                sendMessage(source, "<#990000>Permission already exists.");
                                return -2;
                            }

                            if (isUser) userPermission.addPermission(executorId, id, input, -1);
                            else groupPermission.addPermission(executorId, id, input, -1);
                            sendMessage(source, "<#999999>Permission <#009999>%s</#009999> is now added to <#990099>%s</#990099>.", input, getName(isUser, id));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(this::getSuggestionTime)
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int id = idSupplier.apply(context);
                                    if (id == -2) return -1;

                                    CommandSource source = context.getSource();
                                    String input = StringArgumentType.getString(context, "permission");
                                    boolean hasPermission = isUser ? userPermission.existsPermission(id, input) : groupPermission.existsPermission(id, input);
                                    if (hasPermission) {
                                        sendMessage(source, "<#990000>Permission already exists.");
                                        return -2;
                                    }

                                    String time = StringArgumentType.getString(context, "time");
                                    logging(isUser, executorId, id, "Add permission with expired time", " add " + input + " " + time);
                                    long duration = TimeUtil.formatTime(time);

                                    if (duration == -2) {
                                        sendMessage(source, "<#ff0000>No negative value allowed");
                                        return -3;
                                    }

                                    if (duration == -3) {
                                        sendMessage(source, "<#ff0000>Invalid time format");
                                        return -4;
                                    }

                                    if (isUser) userPermission.addPermission(executorId, id, input, duration);
                                    else groupPermission.addPermission(executorId, id, input, duration);
                                    sendMessage(source, "<#999999>Permission <#009999>%s</#009999> is now added for <#009999>%s</#009999> to <#990099>%s</#990099>",
                                            input, getPermissionExpiredDate(isUser, id, input), getName(isUser, id));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    /*private void addPermission(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        var permission = args[4];
        if (args.length == 5) {
            if (isUser) userPermission.addPermission(userLog, id, permission, -1);
            else groupPermission.addPermission(userLog, id, permission, -1);
            MurmelEssentials.serverSendRefreshMessage(server);
            sendMessage(source, "§3Permission §e%s §3is now added.", permission);
            return;
        }
        var time = TimeUtil.formatTime(args[5]);
        if (time == -2) {
            sendMessage(source, "§cNo negative value allowed");
            return;
        }
        if (time == -3) {
            sendMessage(source, "§cInvalid time format");
            return;
        }
        if (isUser) userPermission.addPermission(userLog, id, permission, time);
        else groupPermission.addPermission(userLog, id, permission, time);
        MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Permission §e%s §3is now added for §e%s", permission, getPermissionExpiredDate(isUser, id, permission));
    }*/

    public LiteralArgumentBuilder<CommandSource> getPermissionRemove(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("remove")
                .executes(context -> {
                    syntaxPermission(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests((context, builder) -> getPermissionSuggestion(context, builder, isUser, idSupplier))
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int id = idSupplier.apply(context);
                            if (id == -2) return -1;

                            CommandSource source = context.getSource();
                            String input = StringArgumentType.getString(context, "permission");
                            logging(isUser, executorId, id, "Remove the permission", " remove " + input);

                            if (isPermissionNotExist(source, isUser, id, input)) return -2;

                            if (isUser) userPermission.removePermission(id, input);
                            else groupPermission.removePermission(id, input);
                            sendMessage(source, "<#999999>Permission <#009999>%s</#009999> is now removed from <#990099>%s</#990099>.", input, getName(isUser, id));
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    /*private void removePermission(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        if (args.length < 5) {
            syntaxPermission(source, isUser);
            return;
        }
        var permission = args[4];
        if (isPermissionNotExist(source, isUser, id, permission)) return;

        if (isUser) userPermission.removePermission(userLog, id, permission);
        else groupPermission.removePermission(userLog, id, permission);
        MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Permission §e%s §3is now removed.", permission);
    }*/

    public LiteralArgumentBuilder<CommandSource> getPermissionClear(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("clear")
                .executes(context -> {
                    int id = idSupplier.apply(context);
                    if (id == -2) return -1;

                    int executorId = executorSupplier.apply(context);
                    logging(isUser, executorId, id, "Clear permissions", " clear");

                    if (isUser) userPermission.clearPermission(id);
                    else groupPermission.clearPermission(id);
                    sendMessage(context.getSource(), "<#009999>All permissions from <#990099>%s</#990099> are now cleared.", getName(isUser, id));
                    return Command.SINGLE_SUCCESS;
                });
    }

    /*private void clearPermission(CommandSource source, boolean isUser, int userLog, int id) {
        if (isUser) userPermission.clearPermission(userLog, id);
        else groupPermission.clearPermission(userLog, id);
        MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3All permissions are now cleared.");
    }*/

    public LiteralArgumentBuilder<CommandSource> getPermissionInfo(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    syntaxPermission(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                        .suggests((context, builder) -> getPermissionSuggestion(context, builder, isUser, idSupplier))
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int id = idSupplier.apply(context);
                            if (id == -2) return -1;

                            CommandSource source = context.getSource();
                            String input = StringArgumentType.getString(context, "permission");
                            logging(isUser, executorId, id, "Get info about the permission", " info " + input);
                            if (isPermissionNotExist(source, isUser, id, input)) return -2;

                            int creatorId = isUser ? userPermission.getCreatedBy(id, input) : groupPermission.getCreatedBy(id, input);

                            sendMessage(source, "<#999999><st>---</st> <#009999>Permission info</#009999> <st>---</st>");
                            sendMessage(source, "<#009999>Permission: <#999900>" + input);
                            sendMessage(source, isUser ? "<#009999>Player: <#999900>" + user.getUsername(id) : "Group: " + group.getName(id));
                            if (isUser) sendMessage(source, "<#009999>Player UUID: <#999900>" + user.getUniqueId(id));
                            sendMessage(source, isUser ? "<#009999>UserId: <#999900>" + id : "GroupId: " + id);
                            sendMessage(source, "<#009999>Get Date: <#999900>" + (isUser ? userPermission.getCreatedAt(id, input).toString() : groupPermission.getCreatedAt(id, input).toString()));
                            sendMessage(source, "<#009999>Expired Date: <#999900>" + (isUser ? MurmelAPI.getDateFormat().format(userPermission.getExpiredAt(id, input))
                                    : MurmelAPI.getDateFormat().format(groupPermission.getExpiredAt(id, input))));
                            sendCreatorMessage(source, user, creatorId);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    /*private void infoPermission(CommandSource source, boolean isUser, int id, String[] args) {
        if (args.length < 5) {
            syntaxPermission(source, isUser);
            return;
        }
        var permission = args[4];
        if (isPermissionNotExist(source, isUser, id, permission)) return;

        var creator = isUser ? userPermission.getCreatedBy(id, permission) : groupPermission.getCreatedBy(id, permission);
        var createdDate = isUser ? userPermission.getCreatedDate(id, permission) : groupPermission.getCreatedDate(id, permission);
        var name = isUser ? "§3Username: §e%s" : "§3Rank: §e%s";
        sendMessage(source, "§8--- §3Info permission: §e%s §8---", permission);
        sendMessage(source, name, args[1]);
        sendCreatorMessage(source, user, creator);
        sendMessage(source, "§3Created date: §e%s", createdDate);
        sendMessage(source, "§3Expired date: §e%s", getPermissionExpiredDate(isUser, id, permission));
    }*/

    public LiteralArgumentBuilder<CommandSource> getPermissionTime(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .executes(context -> {
                    syntaxPermission(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("set")
                        .executes(context -> {
                            syntaxPermission(context.getSource(), isUser);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getTimePermissionArgument(isUser, idSupplier)
                                .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                        .suggests(this::getSuggestionTime)
                                        .executes(context -> getTimeArgument(context, isUser, executorSupplier.apply(context), idSupplier.apply(context), (byte) 1))
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("add")
                        .executes(context -> {
                            syntaxPermission(context.getSource(), isUser);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getTimePermissionArgument(isUser, idSupplier)
                                .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                        .suggests(this::getSuggestionTime)
                                        .executes(context -> getTimeArgument(context, isUser, executorSupplier.apply(context), idSupplier.apply(context), (byte) 2))
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("remove")
                        .executes(context -> {
                            syntaxPermission(context.getSource(), isUser);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getTimePermissionArgument(isUser, idSupplier)
                                .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                        .suggests(this::getSuggestionTime)
                                        .executes(context -> getTimeArgument(context, isUser, executorSupplier.apply(context), idSupplier.apply(context), (byte) 3))
                                )

                        )
                );
    }

    private int getTimeArgument(CommandContext<CommandSource> context, boolean isUser, int executorId, int id, byte type) {
        if (id == -2) return -1;
        CommandSource source = context.getSource();
        String input = StringArgumentType.getString(context, "permission");
        if (isPermissionNotExist(source, isUser, id, input)) return -2;

        String time = StringArgumentType.getString(context, "time");
        long duration = TimeUtil.formatTime(time);

        if (duration == -2) {
            sendMessage(source, "<#ff0000>No negative value allowed");
            return -3;
        }

        if (duration == -3) {
            sendMessage(source, "<#ff0000>Invalid time format");
            return -4;
        }

        String expiredDate;
        switch (type) {
            case 1 -> {
                logging(isUser, executorId, id, "Set expired time to permission", " time set " + input + " " + time);
                if (isUser) userPermission.setExpiredAt(executorId, id, input, duration);
                else groupPermission.setExpiredAt(executorId, id, input, duration);
                expiredDate = isUser ? MurmelAPI.getDateFormat().format(userPermission.getExpiredAt(id, input)) : MurmelAPI.getDateFormat().format(groupPermission.getExpiredAt(id, input));
            }
            case 2 -> {
                logging(isUser, executorId, id, "Add expired time to permission", " time add " + input + " " + time);
                long newTime = isUser ? userPermission.getExpiredAt(id, input).getTime() + duration : groupPermission.getExpiredAt(id, input).getTime() + duration;
                if (isUser) userPermission.setExpiredAt(executorId, id, input, newTime);
                else groupPermission.setExpiredAt(executorId, id, input, newTime);
                expiredDate = isUser ? MurmelAPI.getDateFormat().format(userPermission.getExpiredAt(id, input)) : MurmelAPI.getDateFormat().format(groupPermission.getExpiredAt(id, input));
            }
            case 3 -> {
                logging(isUser, executorId, id, "Remove expired time to permission", " time remove " + input + " " + time);
                long newTime = isUser ? userPermission.getExpiredAt(id, input).getTime() - duration : groupPermission.getExpiredAt(id, input).getTime() - duration;
                if (isUser) userPermission.setExpiredAt(executorId, id, input, newTime);
                else groupPermission.setExpiredAt(executorId, id, input, newTime);
                expiredDate = isUser ? MurmelAPI.getDateFormat().format(userPermission.getExpiredAt(id, input)) : MurmelAPI.getDateFormat().format(groupPermission.getExpiredAt(id, input));
            }
            default -> expiredDate = null;
        }
        sendMessage(source, "<#999999>Expired time for <#009999>%s</#009999> is now <#009999>%s</#009999> for <#990099>%s</#990099>", input, expiredDate, getName(isUser, id));
        return Command.SINGLE_SUCCESS;
    }

    private RequiredArgumentBuilder<CommandSource, String> getTimePermissionArgument(boolean isUser, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.requiredArgumentBuilder("permission", StringArgumentType.string())
                .suggests((context, builder) -> getPermissionSuggestion(context, builder, isUser, idSupplier))
                .executes(context -> {
                    syntaxPermission(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private CompletableFuture<Suggestions> getPermissionSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder, boolean isUser, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        String prefix = builder.getRemaining();
        int id = idSupplier.apply(context);
        List<String> permissions = isUser ? userPermission.getPermissions(id) : groupPermission.getPermissions(id);
        permissions.stream()
                .map(input -> {
                    if ("*".equals(input) || input.endsWith(".*")) return "\"" + input + "\"";
                    return input;
                })
                .filter(s -> StringUtil.startsWithIgnoreCase(s, prefix))
                .sorted()
                .forEach(s -> builder.suggest(s, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + s))));
        return builder.buildFuture();
    }

    /*private void timePermission(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        if (args.length < 6) {
            syntaxPermission(source, isUser);
            return;
        }
        var permission = args[4];
        if (isPermissionNotExist(source, isUser, id, permission)) return;
        var time = TimeUtil.formatTime(args[5]);

        if (time == -2) {
            sendMessage(source, "§cNo negative value allowed");
            return;
        }
        if (time == -3) {
            sendMessage(source, "§cInvalid time format");
            return;
        }

        switch (args[6]) {
            case "set" -> {
                var expiredDate = isUser ? userPermission.setExpiredTime(userLog, id, permission, time) : groupPermission.setExpiredTime(userLog, id, permission, time);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, expiredDate);
            }
            case "add" -> {
                var expiredDate = isUser ? userPermission.addExpiredTime(userLog, id, permission, time) : groupPermission.addExpiredTime(userLog, id, permission, time);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, expiredDate);
            }
            case "remove" -> {
                var expiredDate = isUser ? userPermission.removeExpiredTime(userLog, id, permission, time) : groupPermission.removeExpiredTime(userLog, id, permission, time);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Expired time for §e%s §3is now §e%s", permission, expiredDate);
            }
            default -> syntaxPermission(source, isUser);
        }
    }*/

    private void logging(boolean isUser, int executorId, int id, String doing, String fullCommand) {
        String command = isUser ? "/permission user permission " + user.getUsername(id) : "/permission group permission " + group.getName(id);
        loggingToConsole(logger, user, group, isUser, executorId, id, doing, command + fullCommand);
    }

    private String getName(boolean isUser, int id) {
        return getName(user, group, isUser, id);
    }

    private String getPermissionExpiredDate(boolean isUser, int id, String permission) {
        return isUser ? MurmelAPI.getDateFormat().format(userPermission.getExpiredAt(id, permission)) : MurmelAPI.getDateFormat().format(groupPermission.getExpiredAt(id, permission));
    }

    private boolean isPermissionNotExist(CommandSource source, boolean isUser, int id, String permission) {
        boolean exist = isUser ? userPermission.existsPermission(id, permission) : groupPermission.existsPermission(id, permission);
        if (!exist) {
            sendMessage(source, "<#990000>Permission does not exist.");
            return true;
        } else return false;
    }
}
