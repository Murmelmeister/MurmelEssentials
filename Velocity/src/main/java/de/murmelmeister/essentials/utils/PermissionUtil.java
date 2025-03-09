package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;
import org.slf4j.Logger;

public abstract class PermissionUtil extends CommandManager {
    public String getName(User user, Group group, boolean isUser, int id) {
        return isUser ? user.getUsername(id) : group.getName(id);
    }

    public void loggingToConsole(Logger logger, User user, int executorId, String doing, String fullCommand) {
        logger.info("Command: Permission - Executor: {} (ID: {}) - Doing: {} - Full command: {}",
                user.getUsername(executorId), executorId, doing, fullCommand);
    }

    public void loggingToConsole(Logger logger, User user, Group group, boolean isUser, int executorId, int id, String doing, String fullCommand) {
        logger.info("Command: Permission - Executor: {} (ID: {}) - Target: {} ({}) (ID: {}) - Doing: {} - Full command: {}",
                user.getUsername(executorId), executorId, getName(user, group, isUser, id), (isUser ? "User" : "Group"), id, doing, fullCommand);
    }

    public void syntax(CommandSource source) {
        sendMessage(source, """
                 <#454545>- <#999999>/permission <#bb00bb>groups<#bb00bb>
                <#454545>- <#999999>/permission <#990099>group</#990099> info <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> create <#999900><group></#999900> <white><priority> <teamId>
                <#454545>- <#999999>/permission <#990099>group</#990099> delete <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> rename <#999900><group></#999900> <white><newName>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> add <white><parent> [time]
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> remove <white><parent>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> clear
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> info <white><parent>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> time <set|add|remove> <white><parent> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> all
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> add <white><permission> [time]
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> remove <white><permission>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> clear
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> info <white><permission>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> time <set|add|remove> <white><permission> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> chat <white><prefix|suffix|color> [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> tab <white><prefix|suffix|color> [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> team <white><prefix|suffix|color|id> [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> priority <white>[value]
                <#454545>- <#999999>/permission <#00bbBB>users</#00bbBB>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> add <white><parent> [time]
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> remove <white><parent>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> clear
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> info <white><parent>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> time <set|add|remove> <white><parent> <time>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> all
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> add <white><permission> [time]
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> remove <white><permission>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> clear
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> info <white><permission>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> time <set|add|remove> <white><permission> <time>
                """);
    }

    public  void syntax(CommandSource source, boolean isUser) {
        if (isUser) sendMessage(source, """
                 <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> add <white><parent> [time]
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> remove <white><parent>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> clear
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> info <white><parent>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> time <set|add|remove> <white><parent> <time>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> all
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> add <white><permission> [time]
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> remove <white><permission>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> clear
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> info <white><permission>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> time <set|add|remove> <white><permission> <time>
                """);
        else sendMessage(source, """
                 <#454545>- <#999999>/permission <#990099>group</#990099> info <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> create <#999900><group></#999900> <white><priority> <teamId>
                <#454545>- <#999999>/permission <#990099>group</#990099> delete <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> rename <#999900><group></#999900> <white><newName>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> add <white><parent> [time]
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> remove <white><parent>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> clear
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> info <white><parent>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> time <set|add|remove> <white><parent> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> all
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> add <white><permission> [time]
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> remove <white><permission>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> clear
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> info <white><permission>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> time <set|add|remove> <white><permission> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> chat <white><prefix|suffix|color> [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> tab <white><prefix|suffix|color> [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> team <white><prefix|suffix|color|id> [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> priority <white>[value]
                """);
    }

    public void syntaxParent(CommandSource source, boolean isUser) {
        if (isUser) sendMessage(source, """
                 <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> add <white><parent> [time]
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> remove <white><parent>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> clear
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> info <white><parent>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> time set <white><parent> <time>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> time add <white><parent> <time>
                <#454545>- <#999999>/permission <#009999>user</#009999> parent <#999900><user></#999900> time remove <white><parent> <time>
                """);
        else sendMessage(source, """
                 <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> add <white><parent> [time]
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> remove <white><parent>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> clear
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> info <white><parent>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> time set <white><parent> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> time add <white><parent> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> parent <#999900><group></#999900> time remove <white><parent> <time>
                """);
    }

    public void syntaxPermission(CommandSource source, boolean isUser) {
        if (isUser) sendMessage(source, """
                 <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> all
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> add <white><permission> [time]
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> remove <white><permission>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> clear
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> info <white><permission>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> time set <white><permission> <time>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> time add <white><permission> <time>
                <#454545>- <#999999>/permission <#009999>user</#009999> permission <#999900><user></#999900> time remove <white><permission> <time>
                """);
        else sendMessage(source, """
                 <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> all
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> add <white><permission> [time]
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> remove <white><permission>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> clear
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> info <white><permission>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> time set <white><permission> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> time add <white><permission> <time>
                <#454545>- <#999999>/permission <#990099>group</#990099> permission <#999900><group></#999900> time remove <white><permission> <time>
                """);
    }

    public void syntaxGroupEdit(CommandSource source) {
        sendMessage(source, """
                 <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> chat <white>prefix [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> chat <white>suffix [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> chat <white>color [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> tab <white>prefix [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> tab <white>suffix [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> tab <white>color [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> team <white>prefix [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> team <white>suffix [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> team <white>color [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> team <white>id [value]
                <#454545>- <#999999>/permission <#990099>group</#990099> edit <#999900><group></#999900> priority <white>[value]
                """);
    }
}
