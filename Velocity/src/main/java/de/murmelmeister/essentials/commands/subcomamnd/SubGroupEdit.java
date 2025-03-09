package de.murmelmeister.essentials.commands.subcomamnd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SubGroupEdit extends PermissionUtil {
    private final Logger logger;
    private final User user;
    private final Group group;
    private final GroupColor color;

    public SubGroupEdit(Logger logger, User user, Group group, GroupColor color) {
        this.logger = logger;
        this.user = user;
        this.group = group;
        this.color = color;
    }

    /*public void groupEdit(CommandSource source, int userLog, int groupId, String[] args) {
        if (args.length == 3) {
            syntax(source, false);
            return;
        }
        var builder = new StringBuilder();
        for (int i = 5; i < args.length; i++)
            builder.append(args[i]).append(" ");
        var message = builder.toString().trim();
        message = message.replace("\"", "");

        switch (args[3]) {
            case "chat" -> groupColor(GroupColorType.CHAT, source, userLog, groupId, message, args);
            case "tab" -> groupColor(GroupColorType.TAB, source, userLog, groupId, message, args);
            case "tag" -> groupColor(GroupColorType.TEAM, source, userLog, groupId, message, args);
            case "sort" -> sortId(source, userLog, groupId, args);
            case "team" -> teamId(source, userLog, groupId, args);
            default -> syntaxGroupEdit(source);
        }
    }*/

    /*private void groupColor(GroupColorType type, CommandSource source, int userLog, int groupId, String message, String[] args) {
        // var creator = color.getCreatorId(groupId) == -2 ? creatorId : groupColorSettings.getCreatorId(groupId);
        if (args.length == 4) {
            syntaxGroupEdit(source);
            return;
        }
        switch (args[4]) {
            case "prefix" -> {
                if (args.length == 5) {
                    //commandManager.sendCreatorMessage(source, user, creator);
                    //sendMessage(source, "§3EditedTime: §e%s", color.getEditedDate(groupId));
                    sendMessage(source, "§3Prefix: §e%s", color.getPrefix(groupId, type));
                    break;
                }
                color.setPrefix(userLog, groupId, type, message);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Prefix is now §e%s", message);
            }
            case "suffix" -> {
                if (args.length == 5) {
                    //commandManager.sendCreatorMessage(source, user, creator);
                    //sendMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendMessage(source, "§3Suffix: §e%s", color.getSuffix(groupId, type));
                    break;
                }
                color.setSuffix(userLog, groupId, type, message);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Suffix is now §e%s", message);
            }
            case "color" -> {
                if (args.length == 5) {
                    //commandManager.sendCreatorMessage(source, user, creator);
                    //sendMessage(source, "§3EditedTime: §e%s", groupColorSettings.getEditedDate(groupId));
                    sendMessage(source, "§3Color: §e%s", color.getColor(groupId, type));
                    break;
                }
                color.setColor(userLog, groupId, type, message);
                MurmelEssentials.serverSendRefreshMessage(server);
                sendMessage(source, "§3Color is now §e%s", message);
            }
            default -> syntaxGroupEdit(source);
        }
    }*/

    /*private void sortId(CommandSource source, int userLog, int groupId, String[] args) {
        if (args.length == 4) {
            sendMessage(source, "§3SortID: §e%s", group.getPriority(groupId));
            return;
        }
        try {
            group.setPriority(userLog, groupId, Integer.parseInt(args[4]));
            MurmelEssentials.serverSendRefreshMessage(server);
            sendMessage(source, "§3Sort id is now set to §e%s", args[4]);
        } catch (NumberFormatException e) {
            sendMessage(source, "§cInvalid sort id");
        }
    }*/

    /*private void teamId(CommandSource source, int userLog, int groupId, String[] args) {
        if (args.length == 4) {
            sendMessage(source, "§3TeamID: §e%s", group.getTeamSort(groupId));
            return;
        }
        String id = args[4];
        Matcher matcher = Pattern.compile("^[0-9]+$").matcher(id);
        if (!matcher.matches()) {
            sendMessage(source, "§cInvalid team id. Please use numbers.");
            return;
        }
        var teamId = id + group.getName(groupId);
        group.setTeamSort(userLog, groupId, teamId);
        MurmelEssentials.serverSendRefreshMessage(server);
        sendMessage(source, "§3Team id is now set to §e%s", teamId);
    }*/

    public LiteralArgumentBuilder<CommandSource> getEditChat(Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> groupSupplier) {
        return BrigadierCommand.literalArgumentBuilder("chat")
                .executes(context -> {
                    syntaxGroupEdit(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("prefix")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the chat prefix", " chat prefix");

                            CommandSource source = context.getSource();
                            String chatPrefix = color.getPrefix(groupId, GroupColorType.CHAT);

                            if (chatPrefix == null) {
                                sendMessage(source, "<#990000>Chat prefix is not set.");
                                return -2;
                            }

                            String escapedChatPrefix = chatPrefix.replace("<", "< ").replace(">", " >");
                            sendMessage(context.getSource(), "<#999999>The chat prefix of <#009999>%s</#009999> is <#999900>\"%s\"</#999900>.", group.getName(groupId), escapedChatPrefix);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the chat prefix", " chat prefix " + value);
                                    color.setPrefix(executorId, groupId, GroupColorType.CHAT, value);
                                    sendMessage(context.getSource(), "<#999999>The chat prefix of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("suffix")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the chat suffix", " chat suffix");

                            CommandSource source = context.getSource();
                            String chatSuffix = color.getSuffix(groupId, GroupColorType.CHAT);

                            if (chatSuffix == null) {
                                sendMessage(source, "<#990000>Chat suffix is not set.");
                                return -2;
                            }

                            String escapedChatSuffix = chatSuffix.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The chat suffix of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedChatSuffix);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the chat suffix", " chat suffix " + value);
                                    color.setSuffix(executorId, groupId, GroupColorType.CHAT, value);
                                    sendMessage(context.getSource(), "<#999999>The chat suffix of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("color")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the chat color", " chat color");

                            CommandSource source = context.getSource();
                            String chatColor = color.getColor(groupId, GroupColorType.CHAT);

                            if (chatColor == null) {
                                sendMessage(source, "<#990000>Chat color is not set.");
                                return -2;
                            }

                            String escapedChatColor = chatColor.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The chat color of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedChatColor);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the chat color", " chat color " + value);
                                    color.setColor(executorId, groupId, GroupColorType.CHAT, value);
                                    sendMessage(context.getSource(), "<#999999>The chat color of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getEditTab(Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> groupSupplier) {
        return BrigadierCommand.literalArgumentBuilder("tab")
                .executes(context -> {
                    syntaxGroupEdit(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("prefix")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the tab prefix", " tab prefix");

                            CommandSource source = context.getSource();
                            String tabPrefix = color.getPrefix(groupId, GroupColorType.TAB);

                            if (tabPrefix == null) {
                                sendMessage(source, "<#990000>Tab prefix is not set.");
                                return -2;
                            }

                            String escapedTabPrefix = tabPrefix.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The tab prefix of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedTabPrefix);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the tab prefix", " tab prefix " + value);
                                    color.setPrefix(executorId, groupId, GroupColorType.TAB, value);
                                    sendMessage(context.getSource(), "<#999999>The tab prefix of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("suffix")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the tab suffix", " tab suffix");

                            CommandSource source = context.getSource();
                            String tabSuffix = color.getSuffix(groupId, GroupColorType.TAB);

                            if (tabSuffix == null) {
                                sendMessage(source, "<#990000>Tab suffix is not set.");
                                return -2;
                            }

                            String escapedTabSuffix = tabSuffix.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The tab suffix of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedTabSuffix);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the tab suffix", " tab suffix " + value);
                                    color.setSuffix(executorId, groupId, GroupColorType.TAB, value);
                                    sendMessage(context.getSource(), "<#999999>The tab suffix of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("color")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the tab color", " tab color");

                            CommandSource source = context.getSource();
                            String tabColor = color.getColor(groupId, GroupColorType.TAB);

                            if (tabColor == null) {
                                sendMessage(source, "<#990000>Tab color is not set.");
                                return -2;
                            }

                            String escapedTabColor = tabColor.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The tab color of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedTabColor);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the tab color", " tab color " + value);
                                    color.setColor(executorId, groupId, GroupColorType.TAB, value);
                                    sendMessage(context.getSource(), "<#999999>The tab color of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getEditTeam(Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> groupSupplier) {
        return BrigadierCommand.literalArgumentBuilder("team")
                .executes(context -> {
                    syntaxGroupEdit(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("prefix")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the team prefix", " team prefix");

                            CommandSource source = context.getSource();
                            String teamPrefix = color.getPrefix(groupId, GroupColorType.TEAM);

                            if (teamPrefix == null) {
                                sendMessage(source, "<#990000>Team prefix is not set.");
                                return -2;
                            }

                            String escapedTeamPrefix = teamPrefix.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The team prefix of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedTeamPrefix);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the team prefix", " team prefix " + value);
                                    color.setPrefix(executorId, groupId, GroupColorType.TEAM, value);
                                    sendMessage(context.getSource(), "<#999999>The team prefix of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("suffix")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the team suffix", " team suffix");

                            CommandSource source = context.getSource();
                            String teamSuffix = color.getSuffix(groupId, GroupColorType.TEAM);

                            if (teamSuffix == null) {
                                sendMessage(source, "<#990000>Team suffix is not set.");
                                return -2;
                            }

                            String escapedTeamSuffix = teamSuffix.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The team suffix of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedTeamSuffix);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the team suffix", " team suffix " + value);
                                    color.setSuffix(executorId, groupId, GroupColorType.TEAM, value);
                                    sendMessage(context.getSource(), "<#999999>The team suffix of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("color")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the team color", " team color");

                            CommandSource source = context.getSource();
                            String teamColor = color.getColor(groupId, GroupColorType.TEAM);

                            if (teamColor == null) {
                                sendMessage(source, "<#990000>Team color is not set.");
                                return -2;
                            }

                            String escapedTeamColor = teamColor.replace("<", "< ").replace(">", " >");
                            sendMessage(source, "<#999999>The team color of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), escapedTeamColor);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;

                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the team color", " team color " + value);
                                    color.setColor(executorId, groupId, GroupColorType.TEAM, value);
                                    sendMessage(context.getSource(), "<#999999>The team color of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("id")
                        .executes(context -> {
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int executorId = executorSupplier.apply(context);
                            logging(executorId, groupId, "Get the team id", " team id");

                            sendMessage(context.getSource(), "<#999999>The team id of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), group.getTeamSort(groupId));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                                .executes(context -> {
                                    int executorId = executorSupplier.apply(context);
                                    int groupId = groupSupplier.apply(context);
                                    if (groupId == -2) return -1;


                                    CommandSource source = context.getSource();
                                    String value = StringArgumentType.getString(context, "value");
                                    logging(executorId, groupId, "Set the team id", " team id " + value);
                                    Matcher matcher = Pattern.compile("^[0-9]+$").matcher(value);

                                    if (!matcher.matches()) {
                                        sendMessage(source, "<#990000>Invalid team id. Please use numbers.");
                                        return -2;
                                    }

                                    String teamId = value + group.getName(groupId);
                                    group.setTeamSort(executorId, groupId, teamId);
                                    sendMessage(source, "<#999999>The team id of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), teamId);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getEditPriority(Function<CommandContext<CommandSource>, Integer> executorSupplier, Function<CommandContext<CommandSource>, Integer> groupSupplier) {
        return BrigadierCommand.literalArgumentBuilder("priority")
                .executes(context -> {
                    int groupId = groupSupplier.apply(context);
                    if (groupId == -2) return -1;

                    int executorId = executorSupplier.apply(context);
                    logging(executorId, groupId, "Get the priority", " priority");

                    sendMessage(context.getSource(), "<#999999>The priority of <#009999>%s</#009999> is <#999900>%s</#999900>.", group.getName(groupId), group.getPriority(groupId));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("value", IntegerArgumentType.integer())
                        .executes(context -> {
                            int executorId = executorSupplier.apply(context);
                            int groupId = groupSupplier.apply(context);
                            if (groupId == -2) return -1;

                            int value = IntegerArgumentType.getInteger(context, "value");
                            logging(executorId, groupId, "Set the priority", " priority " + value);
                            group.setPriority(executorId, groupId, value);
                            sendMessage(context.getSource(), "<#999999>The priority of <#009999>%s</#009999> is now <#999900>%s</#999900>.", group.getName(groupId), value);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private void logging(int executorId, int groupId, String doing, String fullCommand) {
        String command = "/permission group edit " + group.getName(groupId);
        loggingToConsole(logger, user, group, false, executorId, groupId, doing, command + fullCommand);
    }
}
