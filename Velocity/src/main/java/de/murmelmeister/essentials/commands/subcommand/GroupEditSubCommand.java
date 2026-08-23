package de.murmelmeister.essentials.commands.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.essentials.utils.PermissionUtil;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GroupEditSubCommand extends PermissionUtil {
    private final GroupProvider groupProvider;
    private final GroupColorProvider colorProvider;

    public GroupEditSubCommand(MurmelEssentials plugin) {
        super(plugin);
        this.groupProvider = plugin.getGroupProvider();
        this.colorProvider = plugin.getGroupColorProvider();
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> createCommand(String commandName) {
        return null;
    }

    private LiteralArgumentBuilder<CommandSource> createGroupValueCommand(String loggingCommand, String literalName, GroupColorType groupType) {
        ColorType colorType = switch (literalName) {
            case "prefix" -> ColorType.PREFIX;
            case "suffix" -> ColorType.SUFFIX;
            case "color" -> ColorType.COLOR;
            case "message" -> ColorType.CHAT_MESSAGE;
            case "priority" -> ColorType.PRIORITY;
            default -> null;
        };

        Matcher matcher = Pattern.compile("^(.)").matcher(loggingCommand);
        String nameSuffix = loggingCommand.equals(literalName) ? "" : " " + literalName;
        String formatLiteralName = matcher.find() ? matcher.group(1).toUpperCase() + loggingCommand.substring(1) + nameSuffix
                : loggingCommand + nameSuffix;

        return BrigadierCommand.literalArgumentBuilder(literalName)
                .executes(context -> executeGetGroupValue(context, formatLiteralName, colorType, groupType))
                .then(BrigadierCommand.requiredArgumentBuilder("value", StringArgumentType.string())
                        .executes(context ->
                                executeSetGroupValue(context, formatLiteralName, colorType, groupType)
                        )
                );
    }

    private int executeGetGroupValue(CommandContext<CommandSource> context, String formatLiteralName, ColorType colorType, GroupColorType groupType) {
        return runWithTiming(context, (source, executor) ->
                {
                    String inputGroup = StringArgumentType.getString(context, "groupName");
                    int languageId = executor.languageId();
                    Group group = getGroup(inputGroup);

                    if (colorType == null)
                        throw new CommandException(Message.PERMISSION_GROUP_COLOR_TYPE_INVALID);

                    if (colorType == ColorType.PRIORITY) {
                        sendMessage(source, languageId, Message.PERMISSION_GROUP_COLOR_VALUE,
                                tagParsed("color_type", formatLiteralName),
                                tagParsed("group_name", group.groupName()),
                                tagParsed("value", group.priority())
                        );
                        return CommandResult.of(Command.SINGLE_SUCCESS);
                    }

                    GroupColor groupColor = colorProvider.findGroupColor(group.id(), groupType.getId()).orElseThrow(() ->
                            new CommandException(Message.PERMISSION_GROUP_COLOR_NOT_SET,
                                    tagParsed("color_type", formatLiteralName),
                                    tagParsed("group_name", group.groupName())
                            )
                    );

                    User creator = getUser(groupColor.createdBy());
                    User changer = groupColor.changedBy() == null ? null : getUser(groupColor.changedBy());
                    String createdDate = groupColor.createdAt().format(getDateTimeFormatter(languageId));
                    String changedDate = groupColor.changedAt() == null ? null : groupColor.changedAt().format(getDateTimeFormatter(languageId));

                    Component changedText = (changer == null || changedDate == null) ? Component.empty() :
                            component(languageId, Message.PERMISSION_INFO_CHANGE_STUFF,
                                    tagParsed("changed_name", changer.username()),
                                    tagParsed("changed_id", changer.id()),
                                    tagParsed("changed_at", changedDate)
                            );

                    sendMessage(source, languageId, Message.PERMISSION_GROUP_COLOR_INFO_MESSAGE,
                            tagParsed("group_name", group.groupName()),
                            tagParsed("group_id", group.id()),
                            tagParsed("color_type", formatLiteralName),
                            tagParsed("value", groupColor.value()),
                            tagParsed("created_name", creator.username()),
                            tagParsed("created_id", creator.id()),
                            tagParsed("created_at", createdDate),
                            Placeholder.component("changed", changedText)
                    );
                    return CommandResult.of(Command.SINGLE_SUCCESS);
                }
        );
    }

    private int executeSetGroupValue(CommandContext<CommandSource> context, String formatLiteralName, ColorType colorType, GroupColorType groupType) {
        return runWithTiming(context, (source, executor) ->
                {
                    String inputGroup = StringArgumentType.getString(context, "groupName");
                    int languageId = executor.languageId();
                    Group group = getGroup(inputGroup);
                    String inputValue = StringArgumentType.getString(context, "value");

                    if (colorType == null)
                        throw new CommandException(Message.PERMISSION_GROUP_COLOR_TYPE_INVALID);

                    if (colorType == ColorType.PRIORITY) {
                        try {
                            int priority = Integer.parseInt(inputValue);
                            if (priority < 0)
                                throw new CommandException(Message.PERMISSION_GROUP_PRIORITY_NEGATIVE, tagUnparsed("priority", priority));

                            Group success = groupProvider.update(group.id(), inputGroup, priority, executor.id()).orElseThrow(() ->
                                    new CommandException(Message.PERMISSION_GROUP_COLOR_UPDATE_FAILED,
                                            tagParsed("color_type", formatLiteralName),
                                            tagParsed("group_name", group.groupName())
                                    )
                            );

                            sendMessage(source, languageId, Message.PERMISSION_GROUP_COLOR_UPDATE_SUCCESS,
                                    tagParsed("color_type", formatLiteralName),
                                    tagParsed("group_name", success.groupName()),
                                    tagParsed("value", success.priority())
                            );

                            return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                        } catch (NumberFormatException e) {
                            throw new CommandException(Message.PERMISSION_GROUP_PRIORITY_INVALID, tagUnparsed("priority", inputValue));
                        }
                    }

                    GroupColor groupColor = colorProvider.findGroupColor(group.id(), groupType.getId()).orElse(null);

                    if (groupColor == null) {
                        GroupColor success = colorProvider.upsert(group.id(), groupType.getId(), inputValue, executor.id()).orElseThrow(() ->
                                new CommandException(Message.PERMISSION_GROUP_COLOR_ADD_FAILED,
                                        tagParsed("color_type", formatLiteralName),
                                        tagParsed("group_name", group.groupName())
                                )
                        );

                        sendMessage(source, languageId, Message.PERMISSION_GROUP_COLOR_ADD_SUCCESS,
                                tagParsed("color_type", formatLiteralName),
                                tagParsed("group_name", group.groupName()),
                                tagParsed("value", success.value())
                        );
                        return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                    }

                    if (inputValue.isEmpty()) {
                        int result = colorProvider.remove(groupColor.groupId(), groupColor.typeId());

                        sendMessage(source, languageId, Message.PERMISSION_GROUP_COLOR_REMOVE_SUCCESS,
                                tagParsed("color_type", formatLiteralName),
                                tagParsed("group_name", group.groupName())
                        );
                        return CommandResult.of(Command.SINGLE_SUCCESS, result);
                    }

                    GroupColor success = colorProvider.upsert(groupColor.groupId(), groupColor.typeId(), inputValue, executor.id()).orElseThrow(() ->
                            new CommandException(Message.PERMISSION_GROUP_COLOR_UPDATE_FAILED,
                                    tagParsed("color_type", formatLiteralName),
                                    tagParsed("group_name", group.groupName())
                            )
                    );

                    sendMessage(source, languageId, Message.PERMISSION_GROUP_COLOR_UPDATE_SUCCESS,
                            tagParsed("color_type", formatLiteralName),
                            tagParsed("group_name", group.groupName()),
                            tagUnparsed("value", success.value())
                    );
                    return CommandResult.of(Command.SINGLE_SUCCESS, 1);
                }
        );
    }

    public LiteralArgumentBuilder<CommandSource> getEditedChatCommand() {
        String literalName = "chat";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .then(createGroupValueCommand(literalName, "prefix", GroupColorType.CHAT_PREFIX))
                .then(createGroupValueCommand(literalName, "suffix", GroupColorType.CHAT_SUFFIX))
                .then(createGroupValueCommand(literalName, "color", GroupColorType.CHAT_COLOR))
                .then(createGroupValueCommand(literalName, "message", GroupColorType.CHAT_MESSAGE));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedTabCommand() {
        String literalName = "tab";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .then(createGroupValueCommand(literalName, "prefix", GroupColorType.TAB_PREFIX))
                .then(createGroupValueCommand(literalName, "suffix", GroupColorType.TAB_SUFFIX))
                .then(createGroupValueCommand(literalName, "color", GroupColorType.TAB_COLOR));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedTeamCommand() {
        String literalName = "team";
        return BrigadierCommand.literalArgumentBuilder(literalName)
                .then(createGroupValueCommand(literalName, "prefix", GroupColorType.TEAM_PREFIX))
                .then(createGroupValueCommand(literalName, "suffix", GroupColorType.TEAM_SUFFIX))
                .then(createGroupValueCommand(literalName, "color", GroupColorType.TEAM_COLOR));
    }

    public LiteralArgumentBuilder<CommandSource> getEditedPriorityCommand() {
        String literalName = "priority";
        return createGroupValueCommand(literalName, literalName, null);
    }

    private enum ColorType {
        PREFIX, SUFFIX, COLOR, CHAT_MESSAGE, PRIORITY
    }
}
