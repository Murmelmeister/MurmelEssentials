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
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class SubParent extends PermissionUtil {
    private final Logger logger;
    private final User user;
    private final Group group;
    private final UserParent userParent;
    private final GroupParent groupParent;

    public SubParent(Logger logger, User user, Group group) {
        this.logger = logger;
        this.user = user;
        this.group = group;
        this.userParent = user.getParent();
        this.groupParent = group.getParent();
    }

    public int getParents(boolean isUser, CommandContext<CommandSource> context, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        int id = idSupplier.apply(context);
        if (id == -2) return -1;

        int executorId = executorSupplier.apply(context);
        logging(isUser, executorId, id, "Get all parents", "");

        CommandSource source = context.getSource();
        List<String> parents = isUser ? userParent.getParentNames(group, id) : groupParent.getParentNames(group, id);

        if (parents.isEmpty()) {
            sendMessage(source, "<#990000>No parents found.");
            return -2;
        }

        String clickMessage = isUser ? "/permission user parent " + user.getUsername(id) + " remove " : "/permission group parent " + group.getName(id) + " remove ";
        sendMessage(source, "<#009999>Parents: ");
        for (String all : parents) {
            if (isUser && all.equals("default")) {
                sendMessage(source, "<#999999>- <#999900>%s", all);
                continue;
            }
            Timestamp duration = isUser ? userParent.getExpiredAt(id, group.getUniqueId(all)) : groupParent.getExpiredAt(id, group.getUniqueId(all));
            String expiredDate = duration == null ? "" : "<#999999> - Expired data: <#009999>" + MurmelAPI.getDateFormat().format(duration);
            sendMessage(source, "<#999999>- <#999900>" +
                                "<hover:show_text:'<#990000>Click to remove <#999900>\"%s\"'>" +
                                "<click:suggest_command:%s>%s</click></hover>%s",
                    all, clickMessage + all, all, expiredDate);
        }
        return Command.SINGLE_SUCCESS;
    }

    /*public void parent(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        if (args.length == 3) {
            sendMessage(source, "§3Parents: ");
            var parents = isUser ? userParent.getParentIds(id) : groupParent.getParentIds(id);
            for (var groupIds : parents)
                sendMessage(source, "§7- §e%s", group.getName(groupIds));
            return;
        }

        if (args.length == 4 && (args[3].equals("add") || args[3].equals("remove") || args[3].equals("info") || args[3].equals("time"))) {
            syntaxParent(source, isUser);
            return;
        }

        switch (args[3]) {
            case "add" -> addParent(source, isUser, userLog, id, args);
            case "remove" -> removeParent(source, isUser, userLog, id, args);
            case "clear" -> clearParent(source, isUser, userLog, id);
            case "info" -> infoParent(source, isUser, id, args);
            case "time" -> timeParent(source, isUser, userLog, id, args);
            default -> syntaxParent(source, isUser);
        }
    }*/

    /*private void addParent(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        var parentName = args[4];
        if (isGroupNotExist(source, group, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (args.length == 5) {
            if (isUser) userParent.addParent(userLog, id, parentId, -1);
            else groupParent.addParent(userLog, id, parentId, -1);
            MurmelEssentials.serverSendRefreshMessage(server);
            sendMessage(source, "§3Parent §e%s §3is now added.", parentName);
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
        if (isUser) userParent.addParent(userLog, id, parentId, time);
        else groupParent.addParent(userLog, id, parentId, time);
        MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Parent §e%s §3is now added for §e%s", parentName, getParentExpiredDate(isUser, id, parentId));
    }*/

    public LiteralArgumentBuilder<CommandSource> getParentAdd(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("add")
                .executes(context -> {
                    syntaxParent(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String prefix = builder.getRemaining();
                            int id = idSupplier.apply(context);
                            List<String> haveParents = isUser ? userParent.getParentNames(group, id) : groupParent.getParentNames(group, id);
                            group.getNames().stream()
                                    .parallel()
                                    .filter(name -> !haveParents.contains(name))
                                    .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                                    .sorted().toList().forEach(name -> builder.suggest(name,
                                            VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + name))));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int id = idSupplier.apply(context);
                            if (id == -2) return -1;

                            CommandSource source = context.getSource();
                            String input = StringArgumentType.getString(context, "parent");
                            logging(isUser, executorId, id, "Add parent (lifetime)", " add " + input);
                            if (isGroupNotExist(source, group, input)) return -2;

                            int parentId = group.getUniqueId(input);
                            boolean hasParent = isUser ? userParent.existsParent(id, parentId) : groupParent.existsParent(id, parentId);
                            if (hasParent) {
                                sendMessage(source, "<#990000>Parent already exists.");
                                return -3;
                            }

                            if (isUser) userParent.addParent(executorId, id, parentId, -1);
                            else groupParent.addParent(executorId, id, parentId, -1);
                            sendMessage(source, "<#999999>Parent <#009999>%s</#009999> is now added to <#990099>%s</#990099>.", input, getName(isUser, id));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                .suggests(this::getSuggestionTime)
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int id = idSupplier.apply(context);
                                    if (id == -2) return -1;

                                    CommandSource source = context.getSource();
                                    String input = StringArgumentType.getString(context, "parent");
                                    if (isGroupNotExist(source, group, input)) return -2;

                                    int parentId = group.getUniqueId(input);
                                    boolean hasParent = isUser ? userParent.existsParent(id, parentId) : groupParent.existsParent(id, parentId);
                                    if (hasParent) {
                                        sendMessage(source, "<#990000>Parent already exists.");
                                        return -3;
                                    }

                                    String time = StringArgumentType.getString(context, "time");
                                    logging(isUser, executorId, id, "Add parent with expired time", " add " + input + " " + time);
                                    long duration = TimeUtil.formatTime(time);

                                    if (duration == -2) {
                                        sendMessage(source, "<#ff0000>No negative value allowed");
                                        return -4;
                                    }

                                    if (duration == -3) {
                                        sendMessage(source, "<#ff0000>Invalid time format");
                                        return -5;
                                    }

                                    if (isUser) userParent.addParent(executorId, id, parentId, duration);
                                    else groupParent.addParent(executorId, id, parentId, duration);
                                    sendMessage(source, "<#999999>Parent <#009999>%s</#009999> is now added for <#009999>%s</#009999> to <#990099>%s</#990099>",
                                            input, getParentExpiredDate(isUser, id, parentId), getName(isUser, id));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    /*private void removeParent(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        if (args.length < 5) {
            syntaxParent(source, isUser);
            return;
        }
        var parentName = args[4];
        if (isGroupNotExist(source, group, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (isParentNotExist(source, isUser, id, parentId)) return;

        if (isUser) {
            if (parentId == group.getUniqueId("default")) {
                sendMessage(source, "§cYou can't remove the default group.");
                return;
            }
            userParent.removeParent(userLog, id, parentId);
        } else groupParent.removeParent(userLog, id, parentId);
        MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Parent §e%s §3is now removed.", parentName);
    }*/

    public LiteralArgumentBuilder<CommandSource> getParentRemove(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("remove")
                .executes(context -> {
                    syntaxParent(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests((context, builder) -> getParentSuggestion(context, builder, isUser, idSupplier))
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int id = idSupplier.apply(context);
                            if (id == -2) return -1;

                            CommandSource source = context.getSource();
                            String input = StringArgumentType.getString(context, "parent");
                            logging(isUser, executorId, id, "Remove the parent", " remove " + input);
                            if (isGroupNotExist(source, group, input)) return -2;

                            int parentId = group.getUniqueId(input);
                            if (isParentNotExist(source, isUser, id, parentId)) return -3;

                            if (isUser && parentId == group.getUniqueId("default")) {
                                sendMessage(source, "<#990000>You can't remove the default group.");
                                return -4;
                            }

                            if (isUser) userParent.removeParent(id, parentId);
                            else groupParent.removeParent(id, parentId);
                            sendMessage(source, "<#999999>Parent <#009999>%s</#009999> is now removed from <#990099>%s</#990099>.", input, getName(isUser, id));
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    /*private void clearParent(CommandSource source, boolean isUser, int userLog, int id) {
        if (isUser) {
            userParent.clearParent(userLog, id);
            userParent.addParent(userLog, id, group.getUniqueId("default"), -1);
        } else groupParent.clearParent(userLog, id);
        MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3All parents are now cleared.");
    }*/

    public LiteralArgumentBuilder<CommandSource> getParentClear(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("clear")
                .executes(context -> {
                    int id = idSupplier.apply(context);
                    if (id == -2) return -1;

                    int executorId = executorSupplier.apply(context);
                    logging(isUser, executorId, id, "Clear parents", " clear");

                    if (isUser) {
                        userParent.clearParent(id);
                        userParent.addParent(executorId, id, group.getUniqueId("default"), -1);
                    } else groupParent.clearParent(id);
                    sendMessage(context.getSource(), "<#009999>All parents from <#990099>%s</#990099> are now cleared.", getName(isUser, id));
                    return Command.SINGLE_SUCCESS;
                });
    }

    /*private void infoParent(CommandSource source, boolean isUser, int id, String[] args) {
        if (args.length < 5) {
            syntaxParent(source, isUser);
            return;
        }
        var parentName = args[4];
        if (isGroupNotExist(source, group, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (isParentNotExist(source, isUser, id, parentId)) return;

        var creator = isUser ? userParent.getCreatedBy(id, parentId) : groupParent.getCreatedBy(id, parentId);
        var createdDate = isUser ? userParent.getCreatedDate(id, parentId) : groupParent.getCreatedDate(id, parentId);
        var name = isUser ? "§3Username: §e%s" : "§3Rank: §e%s";
        sendMessage(source, "§8--- §3Info parent: §e%s §8---", parentName);
        sendMessage(source, name, args[1]);
        sendCreatorMessage(source, user, creator);
        sendMessage(source, "§3Created date: §e%s", createdDate);
        sendMessage(source, "§3Expired date: §e%s", getParentExpiredDate(isUser, id, parentId));
    }*/

    public LiteralArgumentBuilder<CommandSource> getParentInfo(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("info")
                .executes(context -> {
                    syntaxParent(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                        .suggests((context, builder) -> getParentSuggestion(context, builder, isUser, idSupplier))
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int id = idSupplier.apply(context);
                            if (id == -2) return -1;

                            CommandSource source = context.getSource();
                            String input = StringArgumentType.getString(context, "parent");
                            logging(isUser, executorId, id, "Get info about the parent", " info " + input);
                            if (isGroupNotExist(source, group, input)) return -2;

                            int parentId = group.getUniqueId(input);
                            if (isParentNotExist(source, isUser, id, parentId)) return -3;

                            int creatorId = isUser ? userParent.getCreatedBy(id, parentId) : groupParent.getCreatedBy(id, parentId);

                            sendMessage(source, "<#999999><st>---</st> <#009999>Parent info</#009999> <st>---</st>");
                            sendMessage(source, "<#009999>Parent: <#999900>" + input);
                            sendMessage(source, isUser ? "<#009999>Player: <#999900>" + user.getUsername(id) : "Group: " + group.getName(id));
                            if (isUser) sendMessage(source, "<#009999>Player UUID: <#999900>" + user.getUniqueId(id));
                            sendMessage(source, isUser ? "<#009999>UserId: <#999900>" + id : "GroupId: " + id);
                            sendMessage(source, "<#009999>Get Date: <#999900>" + (isUser ? userParent.getCreatedAt(id, parentId).toString() : groupParent.getCreatedAt(id, parentId).toString()));
                            sendMessage(source, "<#009999>Expired Date: <#999900>" + (isUser ? MurmelAPI.getDateFormat().format(userParent.getExpiredAt(id, parentId)) : MurmelAPI.getDateFormat().format(groupParent.getExpiredAt(id, parentId))));
                            sendCreatorMessage(source, user, creatorId);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    /*private void timeParent(CommandSource source, boolean isUser, int userLog, int id, String[] args) {
        if (args.length < 6) {
            syntax(source, isUser);
            return;
        }

        var parentName = args[4];
        if (isGroupNotExist(source, group, parentName)) return;
        var parentId = group.getUniqueId(parentName);
        if (isParentNotExist(source, isUser, id, parentId)) return;
        var time = TimeUtil.formatTime(args[5]);

        if (parentId == group.getUniqueId("default") && isUser) {
            sendMessage(source, "§cYou can't change the time of the default group.");
            return;
        }

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
                var expiredDate = isUser ? userParent.setExpiredTime(userLog, id, parentId, time) : groupParent.setExpiredTime(userLog, id, parentId, time);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, expiredDate);
            }
            case "add" -> {
                var expiredDate = isUser ? userParent.addExpiredTime(userLog, id, parentId, time) : groupParent.addExpiredTime(userLog, id, parentId, time);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, expiredDate);
            }
            case "remove" -> {
                var expiredDate = isUser ? userParent.removeExpiredTime(userLog, id, parentId, time) : groupParent.removeExpiredTime(userLog, id, parentId, time);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Expired time for §e%s §3is now §e%s", parentName, expiredDate);
            }
            default -> syntaxParent(source, isUser);
        }
    }*/

    public LiteralArgumentBuilder<CommandSource> getParentTime(boolean isUser, Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.literalArgumentBuilder("time")
                .executes(context -> {
                    syntaxParent(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("set")
                        .executes(context -> {
                            syntaxParent(context.getSource(), isUser);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getTimeParentArgument(isUser, idSupplier)
                                .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                        .suggests(this::getSuggestionTime)
                                        .executes(context -> getTimeArgument(context, isUser, executorSupplier.apply(context), idSupplier.apply(context), (byte) 1))
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("add")
                        .executes(context -> {
                            syntaxParent(context.getSource(), isUser);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getTimeParentArgument(isUser, idSupplier)
                                .then(BrigadierCommand.requiredArgumentBuilder("time", StringArgumentType.word())
                                        .suggests(this::getSuggestionTime)
                                        .executes(context -> getTimeArgument(context, isUser, executorSupplier.apply(context), idSupplier.apply(context), (byte) 2))
                                )
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("remove")
                        .executes(context -> {
                            syntaxParent(context.getSource(), isUser);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(getTimeParentArgument(isUser, idSupplier)
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
        String input = StringArgumentType.getString(context, "parent");
        if (isGroupNotExist(source, group, input)) return -2;

        int parentId = group.getUniqueId(input);
        if (isParentNotExist(source, isUser, executorId, parentId)) return -3;

        String time = StringArgumentType.getString(context, "time");
        long duration = TimeUtil.formatTime(time);

        if (duration == -2) {
            sendMessage(source, "<#990000>No negative value allowed.");
            return -4;
        }

        if (duration == -3) {
            sendMessage(source, "<#990000>Invalid time format.");
            return -5;
        }

        String expiredDate;
        switch (type) {
            case 1 -> {
                logging(isUser, executorId, id, "Set expired time to parent", " time set " + input + " " + time);
                if (isUser) userParent.setExpiredAt(executorId, id, parentId, duration);
                else groupParent.setExpiredAt(executorId, id, parentId, duration);
                expiredDate = isUser ? MurmelAPI.getDateFormat().format(userParent.getExpiredAt(id, parentId)) : MurmelAPI.getDateFormat().format(groupParent.getExpiredAt(id, parentId));
            }
            case 2 -> {
                logging(isUser, executorId, id, "Add expired time to parent", " time add " + input + " " + time);
                long newTime = isUser ? userParent.getExpiredAt(id, parentId).getTime() + duration : groupParent.getExpiredAt(id, parentId).getTime() + duration;
                if (isUser) userParent.setExpiredAt(executorId, id, parentId, newTime);
                else groupParent.setExpiredAt(executorId, id, parentId, newTime);
                expiredDate = isUser ? MurmelAPI.getDateFormat().format(userParent.getExpiredAt(id, parentId)) : MurmelAPI.getDateFormat().format(groupParent.getExpiredAt(id, parentId));
            }
            case 3 -> {
                logging(isUser, executorId, id, "Remove expired time to parent", " time remove " + input + " " + time);
                long newTime = isUser ? userParent.getExpiredAt(id, parentId).getTime() - duration : groupParent.getExpiredAt(id, parentId).getTime() - duration;
                if (isUser) userParent.setExpiredAt(executorId, id, parentId, newTime);
                else groupParent.setExpiredAt(executorId, id, parentId, newTime);
                expiredDate = isUser ? MurmelAPI.getDateFormat().format(userParent.getExpiredAt(id, parentId)) : MurmelAPI.getDateFormat().format(groupParent.getExpiredAt(id, parentId));
            }
            default -> expiredDate = null;
        }
        sendMessage(source, "<#999999>Expired time for <#009999>%s</#009999> is now <#009999>%s</#009999> for <#990099>%s</#990099>", input, expiredDate, getName(isUser, id));
        return Command.SINGLE_SUCCESS;
    }

    private RequiredArgumentBuilder<CommandSource, String> getTimeParentArgument(boolean isUser, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        return BrigadierCommand.requiredArgumentBuilder("parent", StringArgumentType.word())
                .suggests((context, builder) -> getParentSuggestion(context, builder, isUser, idSupplier))
                .executes(context -> {
                    syntaxParent(context.getSource(), isUser);
                    return Command.SINGLE_SUCCESS;
                });
    }

    private CompletableFuture<Suggestions> getParentSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder, boolean isUser, Function<CommandContext<CommandSource>, Integer> idSupplier) {
        String prefix = builder.getRemaining();
        int id = idSupplier.apply(context);
        List<String> parents = isUser ? userParent.getParentNames(group, id) : groupParent.getParentNames(group, id);
        parents.stream()
                .filter(name -> StringUtil.startsWithIgnoreCase(name, prefix))
                .sorted()
                .forEach(name -> builder.suggest(name, VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + name))));
        return builder.buildFuture();
    }

    private void logging(boolean isUser, int executorId, int id, String doing, String fullCommand) {
        String command = isUser ? "/permission user parent " + user.getUsername(id) : "/permission group parent " + group.getName(id);
        loggingToConsole(logger, user, group, isUser, executorId, id, doing, command + fullCommand);
    }

    private String getName(boolean isUser, int id) {
        return getName(user, group, isUser, id);
    }

    private String getParentExpiredDate(boolean isUser, int id, int parentId) {
        return isUser ? MurmelAPI.getDateFormat().format(userParent.getExpiredAt(id, parentId)) : MurmelAPI.getDateFormat().format(groupParent.getExpiredAt(id, parentId));
    }

    private boolean isParentNotExist(CommandSource source, boolean isUser, int id, int parentId) {
        var exist = isUser ? userParent.existsParent(id, parentId) : groupParent.existsParent(id, parentId);
        if (!exist) {
            sendMessage(source, "<#990000>Parent does not exist.");
            return true;
        } else return false;
    }
}
