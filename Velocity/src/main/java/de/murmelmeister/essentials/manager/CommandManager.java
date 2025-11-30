package de.murmelmeister.essentials.manager;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.velocitypowered.api.command.*;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.*;
import de.murmelmeister.essentials.manager.command.*;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProvider;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static de.murmelmeister.murmelapi.MurmelAPI.DEFAULT_GROUP_ID;
import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public abstract class CommandManager implements CommandBrigadier {
    private final MurmelEssentials plugin;
    private final Logger logger;
    private final UserProvider userProvider;
    private final UserPlayTimeProvider userPlayTimeProvider;
    private final UserSessionProvider userSessionProvider;
    private final UserLoginProvider userLoginProvider;
    private final GroupProvider groupProvider;
    private final MessageService messageService;

    private final MiniMessage miniMessage;

    public CommandManager(MurmelEssentials plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.userProvider = plugin.getUserProvider();
        this.userPlayTimeProvider = plugin.getUserPlayTimeProvider();
        this.userSessionProvider = plugin.getUserSessionProvider();
        this.userLoginProvider = plugin.getUserLoginProvider();
        this.groupProvider = plugin.getGroupProvider();
        this.messageService = plugin.getMessageService();
        this.miniMessage = MiniMessage.miniMessage();
    }

    public static void register(MurmelEssentials plugin) {
        ProxyServer server = plugin.getServer();
        addCommand(server, new PlayTimeCommand(plugin));
        addCommand(server, new PermissionCommand(plugin));
        addCommand(server, new RefreshCommand(plugin));
        addCommand(server, new ShowTeamCommand(plugin));
        addCommand(server, new UserInfoCommand(plugin));
        //addCommand(server, new LanguageCommand(plugin));
        addCommand(server, new ReasonCommand(plugin));
        addCommand(server, new PunishCommand(plugin));
        addCommand(server, new UnpunishCommand(plugin));
    }

    private static void addCommand(ProxyServer server, CommandManager manager) {
        BrigadierCommand command = manager.createCommand();
        addCommand(server, command);
    }

    private static void addCommand(ProxyServer server, CommandManager manager, String... aliases) {
        BrigadierCommand command = manager.createCommand();
        addCommand(server, command, aliases);
    }

    private static void addCommand(ProxyServer server, BrigadierCommand command) {
        CommandMeta meta = server.getCommandManager().metaBuilder(command).build();
        server.getCommandManager().register(meta, command);
    }

    private static void addCommand(ProxyServer server, BrigadierCommand command, String... aliases) {
        CommandMeta meta = server.getCommandManager().metaBuilder(command).aliases(aliases).build();
        server.getCommandManager().register(meta, command);
    }

    public void sendMessage(CommandSource source, int languageId, Message message, TagResolver... resolvers) {
        source.sendRichMessage(messageService.getMessage(message.getTag(), languageId), resolvers);
    }

    public void sendRawMessage(CommandSource source, String message, TagResolver... resolvers) {
        source.sendRichMessage(message, resolvers);
    }

    public Component component(String message, TagResolver... resolvers) {
        return miniMessage.deserialize(message, resolvers);
    }

    public VelocityBrigadierMessage tooltip(String message, TagResolver... resolvers) {
        return VelocityBrigadierMessage.tooltip(component(message, resolvers));
    }

    public VelocityBrigadierMessage tooltip(int languageId, Message message, TagResolver... resolvers) {
        return VelocityBrigadierMessage.tooltip(component(languageId, message, resolvers));
    }

    public Component component(int languageId, Message message, TagResolver... resolvers) {
        return component(messageService.getMessage(message.getTag(), languageId), resolvers);
    }

    public <T> TagResolver.Single tagParsed(@TagPattern String key, T value) {
        return Placeholder.parsed(key, String.valueOf(value));
    }

    public TagResolver.Single tagParsed(@TagPattern String key, int languageId, Message message) {
        return Placeholder.parsed(key, messageService.getMessage(message.getTag(), languageId));
    }

    public <T> TagResolver.Single tagUnparsed(@TagPattern String key, T value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    public void sendDebugMessage(CommandSource source, int languageId, String message, TagResolver... resolvers) {
        String debugPrefix = messageService.getMessage(Message.DEBUG_PREFIX.getTag(), languageId);
        sendRawMessage(source, debugPrefix + message, resolvers);
    }

    public void sendDebugMessage(CommandSource source, int languageId, Message message, TagResolver... resolvers) {
        sendDebugMessage(source, languageId, messageService.getMessage(message.getTag(), languageId), resolvers);
    }

    public DateTimeFormatter getDateTimeFormatter(int languageId) {
        return plugin.getDateTimeFormatter(languageId);
    }

    public Player getPlayer(CommandSource source) {
        return source instanceof Player ? (Player) source : null;
    }

    public User getExecutor(CommandSource source) {
        Player player = getPlayer(source);
        return player != null ? userProvider.findByMojangId(player.getUniqueId()) : userProvider.findById(CONSOLE_USER_ID);
    }

    public Group getDefaultGroup() {
        Group group = groupProvider.findById(DEFAULT_GROUP_ID); // Default group ID is always 1
        if (group == null)
            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_NOT_FOUND);
        return group;
    }

    public Group getGroup(String groupName) {
        Group group = groupProvider.findByName(groupName);
        if (group == null)
            throw new CommandException(Message.PERMISSION_GROUP_NOT_FOUND, tagUnparsed("group", groupName));
        return group;
    }

    public Group getGroup(int groupId) {
        Group group = groupProvider.findById(groupId);
        if (group == null)
            throw new CommandException(Message.PERMISSION_GROUP_NOT_FOUND, tagUnparsed("group", groupId));
        return group;
    }

    public User getUser(UUID mojangId) {
        User user = userProvider.findByMojangId(mojangId);
        if (user == null)
            throw new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagUnparsed("user", mojangId.toString()));
        return user;
    }

    public User getUser(String username) {
        User user = userProvider.findByUsername(username);
        if (user == null)
            throw new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagUnparsed("user", username));
        return user;
    }

    public User getUser(int userId) {
        User user = userProvider.findById(userId);
        if (user == null)
            throw new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagUnparsed("user", userId));
        return user;
    }

    public UserPlayTime getUserPlayTime(int userId) {
        UserPlayTime playTime = userPlayTimeProvider.findByUserId(userId);
        if (playTime == null)
            throw new CommandException(Message.PLAY_TIME_USER_NOT_FOUND, tagParsed("user_id", userId));
        return playTime;
    }

    public String getOnlineStatus(int languageId, int userId) {
        UserLogin lastLogin = userLoginProvider.getLastLogin(userId);
        return userSessionProvider.isOnline(userId) ? "<#00cc88>online</#00cc88>" :
                (lastLogin != null ? "<#cc0099>" + lastLogin.logoutTime().format(getDateTimeFormatter(languageId)) + "</#cc0099>" : "<#cc0099>unknown</#cc0099>"); // TODO: Add language support
    }

    public String getOnlineAgo(int languageId, int userId) {
        UserLogin lastLogin = userLoginProvider.getLastLogin(userId);
        return userSessionProvider.isOnline(userId) ? "" :
                (lastLogin != null ? "           <#454545>(<#cc0099>" + formatTimeAgo(languageId, lastLogin.logoutTime()) + "</#cc0099>)</#454545><br>" : ""); // TODO: Add language support
    }

    public SuggestionProvider<CommandSource> getSuggestionTime() {
        return (context, builder) -> {
            String prefix = builder.getRemaining();
            Stream.of("1s", "1m", "1h", "1d", "1w", "1M", "1y")
                    .filter(s -> StringUtil.startsWithIgnoreCase(s, prefix))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    public long parseTime(String time) {
        if (time == null || time.isEmpty())
            throw new CommandException(Message.INVALID_TIME_FORMAT,tagParsed("time", ""));

        long result = TimeUtil.parseDurationInSeconds(time);

        if (result == -2)
            throw new CommandException(Message.INVALID_TIME_NEGATIVE, tagUnparsed("time", time));

        if (result == -3 || result == -4)
            throw new CommandException(Message.INVALID_TIME_FORMAT, tagUnparsed("time", time));

        return result;
    }

    public String formatTimeAgo(int languageId, long seconds) {
        long difference = System.currentTimeMillis() / 1000 - seconds;
        return TimeUtil.formatDuration(messageService, languageId, difference);
    }

    public String formatTimeAgo(int languageId, LocalDateTime dateTime) {
        long seconds = dateTime == null ? -1 : dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        return formatTimeAgo(languageId, seconds);
    }

    public String formatTimeUntil(int languageId, long seconds) {
        long difference = seconds - System.currentTimeMillis() / 1000;
        return TimeUtil.formatDuration(messageService, languageId, difference);
    }

    public String formatTimeUntil(int languageId, LocalDateTime dateTime) {
        long seconds = dateTime == null ? -1 : dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        return formatTimeUntil(languageId, seconds);
    }

    /**
     * Executes the command and measures the execution time.
     * If an exception occurs, it sends an error message to the source.
     * Code -1 is returned for errors, and 0 for success.
     *
     * @param context The command context containing the source and arguments.
     * @param handler The command handler to execute.
     * @return The result code of the command execution.
     */
    public int runWithTiming(CommandContext<CommandSource> context, CommandHandler handler) {
        long startTime = System.nanoTime();
        CommandSource source = context.getSource();
        User executor = getExecutor(source);
        if (executor == null) {
            logger.warn("Executor user not found for command execution: {}", context.getInput());
            sendMessage(source, 1, Message.MESSAGE_ERROR_COMMAND,
                    tagParsed("error", 1, Message.MESSAGE_ERROR_NO_EXECUTOR)); // No user found -> language fallback is 1 (English)
            return -1;
        }

        int languageId = executor.languageId();
        CommandResult result;
        try {
            result = handler.handle(source, executor);
            if (result.log())
                logger.info("The user '{} (ID: {})' executes the command '{}'",
                        executor.username(), executor.id(), context.getInput());
        } catch (CommandException e) {
            logger.info("Command '{}' execution failed for user {} (ID: {}): {}",
                    context.getInput(), executor.username(), executor.id(), e.getMessage());
            if (e.getMessageKey() != null)
                sendMessage(source, languageId, e.getMessageKey(), e.getResolvers() == null ? new TagResolver[0] : e.getResolvers());
            else
                sendMessage(source, languageId, Message.MESSAGE_ERROR_COMMAND, tagParsed("error", e.getMessage()));
            if (executor.debugMode()) {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                sendDebugMessage(source, languageId, Message.MESSAGE_DEBUG_EXECUTION_TIME_FAILED, tagParsed("execution_time", durationMs));
            }
            return -1;
        } catch (Exception e) {
            logger.error("Error executing command", e);
            sendMessage(source, languageId, Message.MESSAGE_ERROR_COMMAND, tagParsed("error", e.getMessage()));
            return -1;
        }

        if (executor.debugMode()) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            sendDebugMessage(source, languageId, Message.MESSAGE_DEBUG_EXECUTION_TIME_SUCCESS, tagParsed("execution_time", durationMs));
            if (result.rowsAffected() != null)
                sendDebugMessage(source, languageId, Message.MESSAGE_DEBUG_EXECUTION_SUCCESS_ROWS, tagParsed("rows", result.rowsAffected()));
        }
        return result.code();
    }
}
