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
import de.murmelmeister.essentials.utils.Messages;
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
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static de.murmelmeister.murmelapi.group.GroupProviderImpl.DEFAULT_GROUP_ID;
import static de.murmelmeister.murmelapi.user.UserProviderImpl.CONSOLE_USER_ID;

public abstract class CommandManager implements CommandBrigadier {
    private final MurmelEssentials plugin;
    private final Logger logger;
    private final UserProvider userProvider;
    private final UserPlayTimeProvider userPlayTimeProvider;
    private final UserSessionProvider userSessionProvider;
    private final UserLoginProvider userLoginProvider;
    private final GroupProvider groupProvider;
    private final MessageService messageService;

    public CommandManager(MurmelEssentials plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.userProvider = plugin.getUserProvider();
        this.userPlayTimeProvider = plugin.getUserPlayTimeProvider();
        this.userSessionProvider = plugin.getUserSessionProvider();
        this.userLoginProvider = plugin.getUserLoginProvider();
        this.groupProvider = plugin.getGroupProvider();
        this.messageService = plugin.getMessageService();
    }

    public static void register(MurmelEssentials plugin) {
        ProxyServer server = plugin.getServer();
        addCommand(server, new PlayTimeCommand(plugin));
        addCommand(server, new PermissionCommand(plugin));
        addCommand(server, new RefreshCommand(plugin));
        addCommand(server, new ShowTeamCommand(plugin));
        addCommand(server, new UserInfoCommand(plugin));
        addCommand(server, new LanguageCommand(plugin));
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

    public void sendRawMessage(CommandSource source, String message) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void sendMessage(CommandSource source, String message, Object... args) {
        sendRawMessage(source, String.format(message, args));
    }

    public void sendDebugMessage(CommandSource source, int languageId, String message, Object... args) {
        String debugPrefix = messageService.getMessage(Messages.DEBUG_PREFIX, languageId);
        sendMessage(source, debugPrefix + message, args);
    }

    public void sendMessage(CommandSource source, int languageId, Messages message, Object... args) {
        String msg = messageService.getMessage(message, languageId);
        for (int i = 0; i < args.length; i++) {
            if (i % 2 != 0) continue; // Skip odd indices, they are values
            if (i + 1 >= args.length) break; // Prevent ArrayIndexOutOf
            String part = String.valueOf(args[i]);
            String value = String.valueOf(args[i + 1]);
            msg = msg.replace(part, value);
        }
        sendRawMessage(source, msg);
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

    public Group getDefaultGroup(int languageId) {
        Group group = groupProvider.findById(DEFAULT_GROUP_ID); // Default group ID is always 1
        if (group == null)
            throw new CommandException(messageService.getMessage(Messages.DEFAULT_GROUP_NOT_FOUND, languageId));
        return group;
    }

    public Group getGroup(int languageId, String groupName) {
        Group group = groupProvider.findByName(groupName);
        if (group == null)
            throw new CommandException(messageService.getMessage(Messages.GROUP_NOT_FOUND, languageId)
                    .replace("[GROUP]", groupName));
        return group;
    }

    public Group getGroup(int languageId, int groupId) {
        Group group = groupProvider.findById(groupId);
        if (group == null)
            throw new CommandException(messageService.getMessage(Messages.GROUP_NOT_FOUND, languageId)
                    .replace("[GROUP]", String.valueOf(groupId)));
        return group;
    }

    public User getUser(int languageId, UUID mojangId) {
        User user = userProvider.findByMojangId(mojangId);
        if (user == null)
            throw new CommandException(messageService.getMessage(Messages.USER_NOT_FOUND, languageId)
                    .replace("[USER]", mojangId.toString()));
        return user;
    }

    public User getUser(int languageId, String username) {
        User user = userProvider.findByUsername(username);
        if (user == null)
            throw new CommandException(messageService.getMessage(Messages.USER_NOT_FOUND, languageId)
                    .replace("[USER]", username));
        return user;
    }

    public User getUser(int languageId, int userId) {
        User user = userProvider.findById(userId);
        if (user == null)
            throw new CommandException(messageService.getMessage(Messages.USER_NOT_FOUND, languageId)
                    .replace("[USER]", String.valueOf(userId)));
        return user;
    }

    public UserPlayTime getUserPlayTime(int userId) {
        UserPlayTime playTime = userPlayTimeProvider.findByUserId(userId);
        if (playTime == null)
            throw new CommandException("User playtime not found for user ID: " + userId); // TODO: Add language support
        return playTime;
    }

    public String getOnlineStatus(int languageId, int userId) {
        UserLogin lastLogin = userLoginProvider.getLastLogin(userId);
        return userSessionProvider.isOnline(userId) ? "<#00cc88>online" :
                (lastLogin != null ? "<#cc0099>" + lastLogin.logoutTime().format(getDateTimeFormatter(languageId)) : "<#cc0099>unknown");
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

    public long parseTime(int languageId, String time) {
        if (time == null || time.isEmpty())
            throw new CommandException(messageService.getMessage(Messages.PARSE_TIME_INVALID, languageId)
                    .replace("[TIME]", ""));

        long result = TimeUtil.parseDurationInSeconds(time);

        if (result == -2)
            throw new CommandException(messageService.getMessage(Messages.PARSE_TIME_INVALID, languageId)
                    .replace("[TIME]", time));

        if (result == -3 || result == -4)
            throw new CommandException(messageService.getMessage(Messages.PARSE_TIME_INVALID, languageId)
                    .replace("[TIME]", ""));

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
            sendMessage(source, messageService.getMessage(Messages.COMMAND_ERROR_MESSAGE, 1) // No user found -> language fallback is 1 (English)
                    .replace("[ERROR]", messageService.getMessage(Messages.COMMAND_ERROR_NO_EXECUTOR, 1)));
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
            sendMessage(source, messageService.getMessage(Messages.COMMAND_ERROR_MESSAGE, languageId)
                    .replace("[ERROR]", e.getMessage()));
            if (executor.debugMode()) {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                sendDebugMessage(source, languageId, messageService.getMessage(Messages.COMMAND_DEBUG_EXECUTION_TIME_FAILED, languageId)
                        .replace("[EXECUTION_TIME]", String.valueOf(durationMs)));
            }
            return -1;
        } catch (Exception e) {
            logger.error("Error executing command", e);
            sendMessage(source, messageService.getMessage(Messages.COMMAND_ERROR_MESSAGE, languageId));
            return -1;
        }

        if (executor.debugMode()) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            sendDebugMessage(source, languageId, messageService.getMessage(Messages.COMMAND_DEBUG_EXECUTION_TIME_SUCCESS, languageId)
                    .replace("[EXECUTION_TIME]", String.valueOf(durationMs)));
            if (result.rowsAffected() != null)
                sendDebugMessage(source, languageId, messageService.getMessage(Messages.COMMAND_DEBUG_EXECUTION_SUCCESS_ROWS, languageId)
                        .replace("[ROWS]", String.valueOf(result.rowsAffected())));
        }
        return result.code();
    }
}
