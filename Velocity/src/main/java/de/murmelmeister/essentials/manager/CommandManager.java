package de.murmelmeister.essentials.manager;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.commands.*;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.LoginHistory;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.punishment.PunishmentIP;
import de.murmelmeister.murmelapi.punishment.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.StringUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public abstract class CommandManager {
    public static void register(MurmelEssentials plugin, Logger logger, ProxyServer server) {
        Group group = plugin.getGroup();
        User user = plugin.getUser();
        Permission permission = plugin.getPermission();
        PlayTime playTime = plugin.getPlayTime();
        ActiveSession activeSession = plugin.getActiveSession();
        LoginHistory loginHistory = plugin.getLoginHistory();
        PunishmentReason punishmentReason = plugin.getPunishmentReason();
        PunishmentLog punishmentLog = plugin.getPunishmentLog();
        PunishmentIP punishmentIP = plugin.getPunishmentIP();
        PunishmentUser punishmentUser = plugin.getPunishmentUser();

        addCommand(server, new PermissionCommand(logger, permission, user, group).createCommand());
        addCommand(server, new PlayTimeCommand(user).createCommand(playTime, activeSession, loginHistory));
        addCommand(server, new UserInfoCommand(user, activeSession, loginHistory, permission, playTime, punishmentReason, punishmentLog, punishmentUser).createCommand());
        addCommand(server, new ShowTeamCommand().createCommand(permission, user, server, activeSession, loginHistory));
        addCommand(server, new ReasonCommand(user, punishmentReason, permission).createCommand());
        addCommand(server, new PunishedCommand(server, user, permission, activeSession, loginHistory, punishmentReason, punishmentIP, punishmentUser).createCommand());
        addCommand(server, new UnpunishedCommand(user, punishmentIP, punishmentUser).createCommand());
    }

    private static void addCommand(ProxyServer server, BrigadierCommand command) {
        CommandMeta meta = server.getCommandManager().metaBuilder(command).build();
        server.getCommandManager().register(meta, command);
    }

    private static void addCommand(ProxyServer server, BrigadierCommand command, String... aliases) {
        CommandMeta meta = server.getCommandManager().metaBuilder(command).aliases(aliases).build();
        server.getCommandManager().register(meta, command);
    }

    protected void sendMessage(CommandSource source, String message, Object... objects) {
        source.sendMessage(MiniMessage.miniMessage().deserialize(String.format(message, objects)));
    }

    protected Player getPlayer(CommandSource source) {
        return source instanceof Player ? (Player) source : null;
    }

    protected boolean existPlayer(CommandSource source) {
        Player player = getPlayer(source);
        if (player == null) {
            sendMessage(source, "<#880000>This command does not work in the console.");
            return false;
        } else return true;
    }

    protected boolean isGroupNotExist(CommandSource source, Group group, String groupName) {
        if (!group.existsGroup(groupName)) {
            sendMessage(source, "<#990000>Group does not exist.");
            return true;
        } else return false;
    }

    protected boolean isUserNotExist(CommandSource source, User user, String username) {
        if (!user.existsUser(username)) {
            sendMessage(source, "<#990000>User does not exist.");
            return true;
        } else return false;
    }

    protected int getExecutorId(CommandContext<CommandSource> context, User user) {
        CommandSource source = context.getSource();
        Player player = getPlayer(source);
        UUID playerId = player != null ? player.getUniqueId() : null;
        return playerId == null ? -1 : user.getId(playerId);
    }

    public CompletableFuture<Suggestions> getSuggestionTime(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String prefix = builder.getRemaining();
        Stream.of("1s", "1m", "1h", "1d", "1w", "1M", "1y")
                .filter(s -> StringUtil.startsWithIgnoreCase(s, prefix))
                .sorted().forEach(s -> builder.suggest(s,
                        VelocityBrigadierMessage.tooltip(MiniMessage.miniMessage().deserialize("<rainbow>" + s))));
        return builder.buildFuture();
    }

    protected void sendCreatorMessage(CommandSource source, User user, int creatorId) {
        sendMessage(source, "<#009999>Creator: ");
        sendMessage(source, "<#999999>- <#009999>UserId: <#999900>%s", creatorId);
        sendMessage(source, "<#999999>- <#009999>UUID: <#999900>%s", user.getUniqueId(creatorId));
        sendMessage(source, "<#999999>- <#009999>Username: <#999900>%s", user.getUsername(creatorId));
    }
}
