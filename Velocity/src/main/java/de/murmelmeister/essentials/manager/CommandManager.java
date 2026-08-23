package de.murmelmeister.essentials.manager;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.configurations.PluginConfig;
import de.murmelmeister.essentials.manager.command.CommandConfig;
import de.murmelmeister.essentials.manager.command.CommandException;
import de.murmelmeister.essentials.manager.command.CommandHandler;
import de.murmelmeister.essentials.manager.command.CommandResult;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.essentials.utils.ConfigValue;
import de.murmelmeister.library.utils.MojangUtils;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelException;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.stats.UserStats;
import de.murmelmeister.murmelapi.user.stats.UserStatsProvider;
import de.murmelmeister.murmelapi.utils.TimeFilterUtil;
import de.murmelmeister.murmelapi.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.StyleBuilderApplicable;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.TagPattern;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;
import static de.murmelmeister.murmelapi.MurmelAPI.DEFAULT_GROUP_ID;

public abstract class CommandManager {
    private static final Map<String, CommandMeta> REGISTERED_COMMANDS = new HashMap<>();
    private static volatile List<Class<? extends CommandManager>> autoRegistry;

    private final MurmelEssentials plugin;
    private final PluginConfig pluginConfig;
    private final Logger logger;
    private final UserProvider userProvider;
    private final UserStatsProvider userStatsProvider;
    private final GroupProvider groupProvider;
    private final MessageService messageService;
    private final UserService userService;

    private final MiniMessage miniMessage;

    public CommandManager(@NotNull MurmelEssentials plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getPluginConfig();
        this.logger = plugin.getLogger();
        this.userProvider = plugin.getUserProvider();
        this.userStatsProvider = plugin.getUserStatsProvider();
        this.groupProvider = plugin.getGroupProvider();
        this.messageService = plugin.getMessageService();
        this.userService = plugin.getUserService();
        this.miniMessage = MiniMessage.miniMessage();
    }

    public abstract LiteralArgumentBuilder<CommandSource> createCommand(String commandName);

    public static void reload(MurmelEssentials plugin) {
        unregister(plugin.getServer());
        register(plugin, plugin.getLogger());
    }

    public static void register(MurmelEssentials plugin, Logger logger) {
        final String basePackage = plugin.getClass().getPackageName() + ".commands";
        List<Class<? extends CommandManager>> commandClasses = getAutoRegistry(plugin, logger, basePackage);

        addMissingCommandDefaults(plugin.getPluginConfig(), commandClasses);

        for (Class<? extends CommandManager> commandClass : commandClasses) {
            try {
                CommandManager commandInstance = commandClass.getDeclaredConstructor(MurmelEssentials.class)
                        .newInstance(plugin);
                addCommand(commandInstance);
            } catch (Exception e) {
                logger.error("Failed to register command: {}", commandClass.getName(), e);
            }
        }

        logger.info("Registered {} commands.", commandClasses.size());
    }

    private static void addMissingCommandDefaults(
            PluginConfig config,
            List<Class<? extends CommandManager>> commandClasses
    ) {
        Map<String, Object> defaults = new LinkedHashMap<>();

        for (Class<? extends CommandManager> commandClass : commandClasses) {
            CommandConfig command = commandClass.getAnnotation(CommandConfig.class);
            if (command == null) continue;

            String path = "commands." + command.id() + ".";
            if (!command.bypass()) defaults.put(path + "enabled", true);
            defaults.put(path + "name", command.name());
            defaults.put(path + "aliases", List.of(command.aliases()));
        }

        config.addCommandDefaults(defaults);
    }

    public static void unregister(ProxyServer server) {
        REGISTERED_COMMANDS.values().forEach(
                server.getCommandManager()::unregister
        );
        REGISTERED_COMMANDS.clear();
    }

    private static void addCommand(CommandManager clazz) {
        CommandConfig command = clazz.getClass().getAnnotation(CommandConfig.class);
        if (command == null)
            return;

        PluginConfig config = clazz.plugin.getPluginConfig();
        if (!command.bypass() && !config.getBoolean("commands." + command.id() + ".enabled", true))
            return;

        String commandName = config.getString("commands." + command.id() + ".name", command.name());
        List<String> aliases = config.getStringList("commands." + command.id() + ".aliases", List.of(command.aliases()));
        LiteralCommandNode<CommandSource> node = clazz.createCommand(commandName).build();
        BrigadierCommand brigadierCommand = new BrigadierCommand(node);
        CommandMeta meta = aliases.isEmpty()
                ? clazz.plugin.getServer().getCommandManager()
                .metaBuilder(brigadierCommand)
                .build()
                : clazz.plugin.getServer().getCommandManager()
                .metaBuilder(brigadierCommand)
                .aliases(aliases.toArray(new String[0]))
                .build();

        clazz.plugin.getServer().getCommandManager().register(meta, brigadierCommand);
        REGISTERED_COMMANDS.put(command.id(), meta);
    }

    private static List<Class<? extends CommandManager>> getAutoRegistry(MurmelEssentials plugin, Logger logger, String basePackage) {
        List<Class<? extends CommandManager>> local = autoRegistry;
        if (local != null) return local;

        synchronized (CommandManager.class) {
            if (autoRegistry != null) return autoRegistry;

            final List<Class<? extends CommandManager>> result = new ArrayList<>();
            final String packagePath = basePackage.replace(".", "/") + "/";

            File jarFile = new File(URLDecoder.decode(new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getPath(), StandardCharsets.UTF_8));
            ClassLoader classLoader = plugin.getClass().getClassLoader();

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(packagePath) || !name.endsWith(".class")) continue;
                    if (name.contains("$")) continue;

                    String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (!CommandManager.class.isAssignableFrom(clazz)) continue;
                        if (!clazz.isAnnotationPresent(CommandConfig.class)) continue;

                        result.add(clazz.asSubclass(CommandManager.class));
                    } catch (ClassNotFoundException e) {
                        logger.warn("Failed to load command class: {}", className, e);
                    }
                }
            } catch (IOException e) {
                logger.error("Could not find generated command registry.", e);
                return Collections.emptyList();
            }

            autoRegistry = Collections.unmodifiableList(result);
            return result;
        }
    }

    public void sendRawMessage(@NotNull CommandSource source, String message, TagResolver... resolvers) {
        source.sendRichMessage(message, resolvers);
    }

    public void sendRawMessage(@NotNull CommandSource source, int languageId, String message, TagResolver... resolvers) {
        if (pluginConfig.getBoolean(ConfigValue.PLUGIN_PREFIX)) {
            String prefix = messageService.getMessage(Message.PREFIX.getTag(), languageId);
            sendRawMessage(source, prefix + message, resolvers);
        } else {
            sendRawMessage(source, message, resolvers);
        }
    }

    public void sendPagedMessage(@NotNull CommandSource source, List<Component> messages, String commandBase, int page) {
        int configPageSize = pluginConfig.getInt(ConfigValue.COMMAND_PAGED_SIZE);
        int totalPages = (int) Math.ceil((double) messages.size() / configPageSize);
        if (totalPages == 0) totalPages = 1;
        page = Math.clamp(page, 1, totalPages);

        int start = (page - 1) * configPageSize;
        int end = Math.min(start + configPageSize, messages.size());
        List<Component> pageMessages = messages.subList(start, end);
        pageMessages.forEach(source::sendMessage);

        if (totalPages > 1) {
            // TODO: Add to message file
            String prevButton = page > 1
                    ? "<click:run_command:'/" + commandBase + " " + (page - 1) + "'><hover:show_text:'<#00cc88>Previous page</#00cc88>'><#00cc88>«</#00cc88></hover></click>"
                    : "<#454545>«</#454545>";
            String nextButton = page < totalPages
                    ? "<click:run_command:'/" + commandBase + " " + (page + 1) + "'><hover:show_text:'<#00cc88>Next page</#00cc88>'><#00cc88>»</#00cc88></hover></click>"
                    : "<#454545>»</#454545>";
            String pageInfo = "<#999999>Page <#999900>" + page + "</#999900> of <#999900>" + totalPages + "</#999900></#999999>";
            sendRawMessage(source, prevButton + " " + pageInfo + " " + nextButton);
        }
    }

    public void sendMessage(@NotNull CommandSource source, int languageId, @NotNull Message message, TagResolver... resolvers) {
        sendRawMessage(source, languageId, messageService.getMessage(message.getTag(), languageId), resolvers);
    }

    public void sendDebugMessage(@NotNull CommandSource source, int languageId, String message, TagResolver... resolvers) {
        final String debugPrefix = messageService.getMessage(Message.DEBUG_PREFIX.getTag(), languageId);
        sendRawMessage(source, debugPrefix + message, resolvers);
    }

    public void sendDebugMessage(@NotNull CommandSource source, int languageId, @NotNull Message message, TagResolver... resolvers) {
        sendDebugMessage(source, languageId, messageService.getMessage(message.getTag(), languageId), resolvers);
    }

    public Component component(String message, TagResolver... resolvers) {
        return miniMessage.deserialize(message, resolvers);
    }

    public Component component(int languageId, @NotNull Message message, TagResolver... resolvers) {
        return component(messageService.getMessage(message.getTag(), languageId), resolvers);
    }

    public VelocityBrigadierMessage tooltip(String message, TagResolver... resolvers) {
        return VelocityBrigadierMessage.tooltip(component(message, resolvers));
    }

    public VelocityBrigadierMessage tooltip(int languageId, @NotNull Message message, TagResolver... resolvers) {
        return VelocityBrigadierMessage.tooltip(component(languageId, message, resolvers));
    }

    public <T> TagResolver.Single tagParsed(@TagPattern String key, T value) {
        return Placeholder.parsed(key, String.valueOf(value));
    }

    public TagResolver.Single tagParsed(@TagPattern String key, int languageId, @NotNull Message message) {
        return Placeholder.parsed(key, messageService.getMessage(message.getTag(), languageId));
    }

    public <T> TagResolver.Single tagUnparsed(@TagPattern String key, T value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    public TagResolver.Single tagUnparsed(@TagPattern String key, int languageId, @NotNull Message message) {
        return Placeholder.unparsed(key, messageService.getMessage(message.getTag(), languageId));
    }

    public TagResolver.Single tagStyling(@TagPattern String key, @NotNull StyleBuilderApplicable style) {
        return Placeholder.styling(key, style);
    }

    public TagResolver.Single tagStyling(@TagPattern String key, int languageId, @NotNull Message message) {
        final String color = messageService.getMessage(message.getTag(), languageId);
        TextColor textColor = TextColor.fromHexString(color);
        if (textColor == null)
            textColor = TextColor.color(255, 255, 255);
        return Placeholder.styling(key, textColor);
    }

    public DateTimeFormatter getDateTimeFormatter(int languageId) {
        return plugin.getDateTimeFormatter(languageId);
    }

    public @Nullable Player getPlayer(@NotNull CommandSource source) {
        return source instanceof Player ? (Player) source : null;
    }

    public @NotNull User getExecutor(@NotNull CommandSource source) {
        Player player = getPlayer(source);
        Optional<User> user = player != null ? userProvider.findByMojangId(player.getUniqueId()) : userProvider.findById(CONSOLE_USER_ID);
        if (user.isEmpty())
            throw new CommandException(Message.MESSAGE_ERROR_NO_EXECUTOR);
        return user.get();
    }

    public @NotNull Group getDefaultGroup() {
        Group group = groupProvider.findById(DEFAULT_GROUP_ID).orElse(null); // Default group ID is always 1
        if (group == null)
            throw new CommandException(Message.PERMISSION_DEFAULT_GROUP_NOT_FOUND);
        return group;
    }

    public @NotNull Group getGroup(String groupName) {
        Group group = groupProvider.findByName(groupName).orElse(null);
        if (group == null)
            throw new CommandException(Message.PERMISSION_GROUP_NOT_FOUND, tagUnparsed("group", groupName));
        return group;
    }

    public @NotNull Group getGroup(int groupId) {
        Group group = groupProvider.findById(groupId).orElse(null);
        if (group == null)
            throw new CommandException(Message.PERMISSION_GROUP_NOT_FOUND, tagUnparsed("group", groupId));
        return group;
    }

    public @NotNull User getUser(UUID mojangId) {
        User user = userProvider.findByMojangId(mojangId).orElse(null);
        if (user == null)
            throw new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagUnparsed("user", mojangId.toString()));
        return user;
    }

    public @NotNull User getUser(String username) {
        User user = userProvider.findByUsername(username).orElse(null);
        if (user == null)
            throw new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagUnparsed("user", username));
        return user;
    }

    public @NotNull User getUser(int userId) {
        User user = userProvider.findById(userId).orElse(null);
        if (user == null)
            throw new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagUnparsed("user", userId));
        return user;
    }

    public @NotNull UserStats getUserStats(int userId) {
        UserStats userStats = userStatsProvider.findByUserId(userId).orElse(null);
        if (userStats == null)
            throw new CommandException(Message.PLAY_TIME_USER_NOT_FOUND, tagParsed("user_id", userId));
        return userStats;
    }

    public @NotNull UUID getMojangId(String username) {
        try {
            return MojangUtils.getUUID(username);
        } catch (IOException | URISyntaxException e) {
            throw new CommandException(Message.PERMISSION_USER_NOT_FOUND, tagUnparsed("user", username));
        }
    }

    public @NotNull InetAddress getInetAddress(@NotNull String raw) {
        Objects.requireNonNull(raw, "raw cannot be null");
        try {
            return InetAddress.getByName(raw);
        } catch (UnknownHostException e) {
            throw new CommandException(Message.IP_ADDRESS_INVALID, tagUnparsed("ip", raw));
        }
    }

    public String getOnlineStatus(int languageId, int userId) {
        UserLogin lastLogin = userService.getLastLogin(userId);
        return userService.isOnline(userId) ? "<#00cc88>online</#00cc88>" :
                (lastLogin != null ? "<#cc0099>" + lastLogin.logoutTime().format(getDateTimeFormatter(languageId)) + "</#cc0099>" : "<#cc0099>unknown</#cc0099>"); // TODO: Add language support
    }

    public String getOnlineAgo(int languageId, int userId, TimeFilterUtil... filters) {
        UserLogin lastLogin = userService.getLastLogin(userId);
        return userService.isOnline(userId) ? "" :
                (lastLogin != null ? "           <#454545>(<#cc0099>" + formatTimeAgo(languageId, lastLogin.logoutTime(), filters) + "</#cc0099>)</#454545><br>" : ""); // TODO: Add language support
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
            throw new CommandException(Message.INVALID_TIME_FORMAT, tagParsed("time", ""));

        long result = TimeUtil.parseDurationInSeconds(time);

        if (result == -2)
            throw new CommandException(Message.INVALID_TIME_NEGATIVE, tagUnparsed("time", time));

        if (result == -3 || result == -4)
            throw new CommandException(Message.INVALID_TIME_FORMAT, tagUnparsed("time", time));

        return result;
    }

    public String formatTimeAgo(int languageId, long seconds, TimeFilterUtil... filters) {
        long difference = System.currentTimeMillis() / 1000 - seconds;
        return TimeUtil.formatDuration(messageService, languageId, difference, filters);
    }

    public String formatTimeAgo(int languageId, LocalDateTime dateTime, TimeFilterUtil... filters) {
        long seconds = dateTime == null ? -1 : dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        return formatTimeAgo(languageId, seconds, filters);
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
    public int runWithTiming(@NotNull CommandContext<CommandSource> context, CommandHandler handler) {
        long startTime = System.nanoTime();
        CommandSource source = context.getSource();
        User executor = getExecutor(source);
        int languageId = executor.languageId();

        CommandResult result;
        try {
            result = handler.handle(source, executor);
            if (pluginConfig.getBoolean(ConfigValue.COMMAND_LOGGER_ENABLE) && result.log())
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
        } catch (MurmelException e) {
            logger.info("Command '{}' execution failed for user {} (ID: {}): {}",
                    context.getInput(), executor.username(), executor.id(), e.getMessage());
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
