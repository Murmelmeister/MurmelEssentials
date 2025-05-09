package de.murmelmeister.essentials.commands.subcomamnd;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GroupEditSubCommand extends PermissionUtil {
    private final GroupColor color;

    private final GroupColorType typeChat = GroupColorType.CHAT;
    private final GroupColorType typeTab = GroupColorType.TAB;
    private final GroupColorType typeTeam = GroupColorType.TEAM;

    private final Pattern teamIdPattern = Pattern.compile("^[0-9]+$");

    public GroupEditSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.color = plugin.getGroup().getColor();
    }

    @Override // Not used
    public BrigadierCommand createCommand() {
        return null;
    }

    private LiteralArgumentBuilder<CommandSource> createGroupValueCommand(String loggingCommand, String literalName, GroupColorType groupType) {
        ColorType colorType = switch (literalName) {
            case "prefix" -> ColorType.PREFIX;
            case "suffix" -> ColorType.SUFFIX;
            case "color" -> ColorType.COLOR;
            case "message" -> ColorType.CHAT_MESSAGE;
            case "id" -> ColorType.TEAM_ID;
            case "priority" -> ColorType.PRIORITY;
            default -> null;
        };

        Matcher matcher = Pattern.compile("^(.)").matcher(loggingCommand);
        String nameSuffix = loggingCommand.equals(literalName) ? "" : " " + literalName;
        String formatLiteralName = matcher.find() ? matcher.group(1).toUpperCase() + loggingCommand.substring(1) + nameSuffix
                : loggingCommand + nameSuffix;

        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> {
                    long startTime = System.nanoTime();
                    CommandSource source = context.getSource();

                    int executorId = getExecutorId(source);
                    String groupName = StringArgumentType.getString(context, "groupName");
                    int groupId = getGroupId(source, groupName);
                    if (executorId == -2 || groupId == 0) return -1;

                    if (!color.existsGroup(groupId)) {
                        sendMessage(source, "<#990000>Group does not exist.");
                        return -2;
                    }

                    String value = colorType == null ? null : switch (colorType) {
                        case PREFIX -> color.getPrefix(groupId, groupType);
                        case SUFFIX -> color.getSuffix(groupId, groupType);
                        case COLOR -> color.getColor(groupId, groupType);
                        case CHAT_MESSAGE -> color.getMessage(groupId);
                        case TEAM_ID -> group.getTeamSort(groupId);
                        case PRIORITY -> String.valueOf(group.getPriority(groupId));
                    };

                    if (value == null) {
                        sendMessage(source, "<#990000>%s is not set.", formatLiteralName);
                        return -3;
                    }

                    sendMessage(source, "<#999999>%s of <#cc8800>%s</#cc8800>: <#00cc88>%s", formatLiteralName, groupName, value);
                    logging(executorId, groupId, "Get the " + loggingCommand + " " + literalName + " of the group",
                            loggingCommand + " " + literalName);

                    if (user.isDebugMode(executorId)) {
                        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                        sendMessage(source, "<#999900>Command executed in %s ms", durationMs);
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                        .executes(context -> {
                            long startTime = System.nanoTime();
                            CommandSource source = context.getSource();

                            int executorId = getExecutorId(source);
                            String groupName = StringArgumentType.getString(context, "groupName");
                            int groupId = getGroupId(source, groupName);
                            if (executorId == -2 || groupId == 0) return -1;

                            if (!color.existsGroup(groupId)) {
                                sendMessage(source, "<#990000>Group does not exist.");
                                return -2;
                            }

                            if (colorType == null) {
                                sendMessage(source, "<#990000>Invalid color type.");
                                return -3;
                            }

                            String value = StringArgumentType.getString(context, "value");
                            int row = switch (colorType) {
                                case PREFIX -> color.setPrefix(groupId, groupType, value, executorId);
                                case SUFFIX -> color.setSuffix(groupId, groupType, value, executorId);
                                case COLOR -> color.setColor(groupId, groupType, value, executorId);
                                case CHAT_MESSAGE -> color.setMessage(groupId, value, executorId);
                                case TEAM_ID -> {
                                    Matcher teamIdMatcher = teamIdPattern.matcher(value);

                                    if (!teamIdMatcher.matches()) {
                                        sendMessage(source, "<#990000>Invalid team ID.");
                                        yield -4;
                                    }

                                    String teamId = value + groupName;
                                    yield group.setTeamSort(groupId, teamId, executorId);
                                }
                                case PRIORITY -> {
                                    try {
                                        int priority = Integer.parseInt(value);

                                        if (priority < 0) {
                                            sendMessage(source, "<#990000>Priority must be a positive number.");
                                            yield -4;
                                        }

                                        yield group.setPriority(groupId, priority, executorId);
                                    } catch (NumberFormatException e) {
                                        sendMessage(source, "<#990000>Invalid priority value.");
                                        yield -4;
                                    }
                                }
                            };

                            if (row == -4) return -4;
                            sendMessage(source, "<#999999>%s of <#cc8800>%s</#cc8800> is now set to <#00cc88>%s", formatLiteralName, groupName, value);
                            logging(executorId, groupId, "Set the " + loggingCommand + " " + literalName + " of the group",
                                    loggingCommand + " " + literalName + " " + value);
                            RefreshUtil.markAsRefreshed("global"); // TODO: changing the right cache name

                            if (user.isDebugMode(executorId)) {
                                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                                sendDebugMessage(source, "Command executed in %s ms", durationMs);
                                sendDebugMessage(source, "Row affected: %s", row);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    public LiteralArgumentBuilder<CommandSource> getEditedChatCommand() {
        String literalName = "chat";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                .then(createGroupValueCommand(literalName, "prefix", typeChat))
                .then(createGroupValueCommand(literalName, "suffix", typeChat))
                .then(createGroupValueCommand(literalName, "color", typeChat))
                .then(createGroupValueCommand(literalName, "message", null));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedTabCommand() {
        String literalName = "tab";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                .then(createGroupValueCommand(literalName, "prefix", typeTab))
                .then(createGroupValueCommand(literalName, "suffix", typeTab))
                .then(createGroupValueCommand(literalName, "color", typeTab));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedTeamCommand() {
        String literalName = "team";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                .then(createGroupValueCommand(literalName, "prefix", typeTeam))
                .then(createGroupValueCommand(literalName, "suffix", typeTeam))
                .then(createGroupValueCommand(literalName, "color", typeTeam))
                .then(createGroupValueCommand(literalName, "id", typeTeam));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedPriorityCommand() {
        String literalName = "priority";
        return createGroupValueCommand(literalName, literalName, null);
    }

    private void logging(int executorId, int groupId, String doing, String fullCommand) {
        String command = "/permission group edit " + group.getGroupName(groupId) + " ";
        loggingToConsole(executorId, doing, command + fullCommand);
    }

    private enum ColorType {
        PREFIX, SUFFIX, COLOR, CHAT_MESSAGE, TEAM_ID, PRIORITY
    }
}
