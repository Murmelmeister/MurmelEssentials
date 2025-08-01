package de.murmelmeister.essentials.commands.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.utils.Messages;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GroupEditSubCommand extends PermissionUtil {
    private final GroupProvider groupProvider;
    private final GroupColorProvider colorProvider;

    private final MessageService messageService;
    private final Pattern teamIdPattern = Pattern.compile("^[0-9]+$");

    public GroupEditSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupProvider = plugin.getGroupProvider();
        this.colorProvider = plugin.getGroupColorProvider();
        this.messageService = plugin.getMessageService();
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
                .executes(context ->
                        runWithTiming(context, (source, executor) -> {
                            String groupName = StringArgumentType.getString(context, "groupName");
                            int languageId = executor.languageId();
                            Group group = getGroup(languageId, groupName);

                            if (colorType == ColorType.PRIORITY || colorType == ColorType.TEAM_ID) {
                                String value = colorType == ColorType.PRIORITY ? String.valueOf(group.priority()) : group.teamTagId();
                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_VALUE, languageId)
                                        .replace("[COLOR_TYPE]", formatLiteralName)
                                        .replace("[GROUP_NAME]", groupName)
                                        .replace("[VALUE]", value));
                                return CommandResult.of(Command.SINGLE_SUCCESS);
                            }

                            GroupColor groupColor = colorProvider.getGroupColor(group.id(), groupType.getId());

                            if (groupColor == null) {
                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_NOT_SET, languageId)
                                        .replace("[COLOR_TYPE]", formatLiteralName)
                                        .replace("[GROUP_NAME]", groupName));
                                return CommandResult.of(-2);
                            }

                            User creator = getUser(languageId, groupColor.createdBy());
                            User changer = groupColor.changedBy() == null ? null : getUser(languageId, groupColor.changedBy());
                            String createdDate = groupColor.createdAt().format(getDateTimeFormatter(languageId));
                            String changedDate = groupColor.changedAt() == null ? null : groupColor.changedAt().format(getDateTimeFormatter(languageId));

                            String changedText = (changer == null || changedDate == null) ? null :
                                    messageService.getMessage(Messages.PERMISSION_INFO_CHANGE_STUFF, languageId)
                                            .replace("[CHANGED_NAME]", changer.username())
                                            .replace("[CHANGED_ID]", String.valueOf(changer.id()))
                                            .replace("[CHANGED_AT]", changedDate);

                            sendMessage(source, (messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_INFO_MESSAGE, languageId)
                                    .replace("[GROUP_NAME]", groupName)
                                    .replace("[GROUP_ID]", String.valueOf(group.id()))
                                    .replace("[COLOR_TYPE]", formatLiteralName)
                                    .replace("[VALUE]", groupColor.value())
                                    .replace("[CREATED_NAME]", creator.username())
                                    .replace("[CREATED_ID]", String.valueOf(creator.id()))
                                    .replace("[CREATED_AT]", createdDate)
                                    .replace("[CHANGED]", changedText == null ? "" : changedText)).trim());
                            return CommandResult.of(Command.SINGLE_SUCCESS);
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                        .executes(context ->
                                runWithTiming(context, (source, executor) -> {
                                    String groupName = StringArgumentType.getString(context, "groupName");
                                    int languageId = executor.languageId();
                                    Group group = getGroup(languageId, groupName);
                                    String value = StringArgumentType.getString(context, "value");

                                    GroupColor groupColor = colorProvider.getGroupColor(group.id(), groupType.getId());

                                    if (groupColor == null) {
                                        GroupColor success = colorProvider.add(group.id(), groupType.getId(), value, executor.id());
                                        if (success == null)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_ADD_FAILED, languageId)
                                                    .replace("[COLOR_TYPE]", formatLiteralName)
                                                    .replace("[GROUP_NAME]", groupName));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_ADD_SUCCESS, languageId)
                                                .replace("[COLOR_TYPE]", formatLiteralName)
                                                .replace("[GROUP_NAME]", groupName)
                                                .replace("[VALUE]", value));
                                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                                    }

                                    if (colorType == null) {
                                        sendMessage(source, messageService.getMessage(Messages.INVALID_COLOR_TYPE, languageId));
                                        return CommandResult.of(-2);
                                    }

                                    boolean isColor = switch (colorType) {
                                        case PREFIX, SUFFIX, COLOR, CHAT_MESSAGE -> true;
                                        case TEAM_ID, PRIORITY -> false;
                                    };

                                    Integer row = null;
                                    if (isColor) {
                                        GroupColor success = colorProvider.update(groupColor.groupId(), groupColor.typeId(), value, executor.id());
                                        if (success == null)
                                            throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_UPDATE_FAILED, languageId)
                                                    .replace("[COLOR_TYPE]", formatLiteralName)
                                                    .replace("[GROUP_NAME]", groupName));
                                        sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_UPDATE_SUCCESS, languageId)
                                                .replace("[COLOR_TYPE]", formatLiteralName)
                                                .replace("[GROUP_NAME]", groupName)
                                                .replace("[VALUE]", value));
                                        row = 1;
                                    } else {
                                        switch (colorType) {
                                            case TEAM_ID -> {
                                                Matcher teamIdMatcher = teamIdPattern.matcher(value);
                                                if (!teamIdMatcher.matches()) {
                                                    sendMessage(source, "<#990000>Invalid team ID.");
                                                    return CommandResult.of(-3);
                                                }
                                                String teamId = value + groupName;
                                                Group success = groupProvider.update(group.id(), groupName, group.priority(), teamId, executor.id());
                                                if (success == null)
                                                    throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_UPDATE_FAILED, languageId)
                                                            .replace("[COLOR_TYPE]", formatLiteralName)
                                                            .replace("[GROUP_NAME]", groupName));
                                                sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_UPDATE_SUCCESS, languageId)
                                                        .replace("[COLOR_TYPE]", formatLiteralName)
                                                        .replace("[GROUP_NAME]", groupName)
                                                        .replace("[VALUE]", teamId));
                                                row = 1;
                                            }
                                            case PRIORITY -> {
                                                try {
                                                    int priority = Integer.parseInt(value);
                                                    if (priority < 0) {
                                                        sendMessage(source, messageService.getMessage(Messages.PRIORITY_NEGATIVE, languageId)
                                                                .replace("[PRIORITY]", String.valueOf(priority)));
                                                        return CommandResult.of(-3);
                                                    }

                                                    Group success = groupProvider.update(group.id(), groupName, priority, group.teamTagId(), executor.id());
                                                    if (success == null)
                                                        throw new CommandException(messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_UPDATE_FAILED, languageId)
                                                                .replace("[COLOR_TYPE]", formatLiteralName)
                                                                .replace("[GROUP_NAME]", groupName));
                                                    sendMessage(source, messageService.getMessage(Messages.PERMISSION_GROUP_COLOR_UPDATE_SUCCESS, languageId)
                                                            .replace("[COLOR_TYPE]", formatLiteralName)
                                                            .replace("[GROUP_NAME]", groupName)
                                                            .replace("[VALUE]", String.valueOf(priority)));
                                                    row = 1;
                                                } catch (NumberFormatException e) {
                                                    throw new CommandException(messageService.getMessage(Messages.PRIORITY_INVALID, languageId)
                                                            .replace("[PRIORITY]", value));
                                                }
                                            }
                                            default ->
                                                    throw new CommandException(messageService.getMessage(Messages.INVALID_COLOR_TYPE, languageId));
                                        }
                                    }
                                    return CommandResult.of(Command.SINGLE_SUCCESS, row);
                                })
                        )
                );
    }

    public LiteralArgumentBuilder<CommandSource> getEditedChatCommand() {
        String literalName = "chat";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                .then(createGroupValueCommand(literalName, "prefix", GroupColorType.CHAT_PREFIX))
                .then(createGroupValueCommand(literalName, "suffix", GroupColorType.CHAT_SUFFIX))
                .then(createGroupValueCommand(literalName, "color", GroupColorType.CHAT_COLOR))
                .then(createGroupValueCommand(literalName, "message", GroupColorType.CHAT_MESSAGE));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedTabCommand() {
        String literalName = "tab";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                .then(createGroupValueCommand(literalName, "prefix", GroupColorType.TAB_PREFIX))
                .then(createGroupValueCommand(literalName, "suffix", GroupColorType.TAB_SUFFIX))
                .then(createGroupValueCommand(literalName, "color", GroupColorType.TAB_COLOR));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedTeamCommand() {
        String literalName = "team";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> {
                    sendMessage(context.getSource(), syntaxGroupEdit());
                    return Command.SINGLE_SUCCESS;
                })
                .then(createGroupValueCommand(literalName, "prefix", GroupColorType.TEAM_PREFIX))
                .then(createGroupValueCommand(literalName, "suffix", GroupColorType.TEAM_SUFFIX))
                .then(createGroupValueCommand(literalName, "color", GroupColorType.TEAM_COLOR))
                .then(createGroupValueCommand(literalName, "id", null));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedPriorityCommand() {
        String literalName = "priority";
        return createGroupValueCommand(literalName, literalName, null);
    }

    private enum ColorType {
        PREFIX, SUFFIX, COLOR, CHAT_MESSAGE, TEAM_ID, PRIORITY
    }
}
