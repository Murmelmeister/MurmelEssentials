package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.command.CommandSource;

import static de.murmelmeister.essentials.manager.CommandManager.sendSourceMessage;

public final class PermissionSyntaxUtil {
    public static void syntax(CommandSource source) {
        sendSourceMessage(source, """
                - /permission group §9<group>§r create <sortId> <teamId>
                - /permission group §9<group>§r delete
                - /permission group §9<group>§r rename <newName>
                - /permission group §9<group>§6 parent§r
                - /permission group §9<group>§6 parent§r add <parent> [duration]
                - /permission group §9<group>§6 parent§r remove <parent>
                - /permission group §9<group>§6 parent§r clear
                - /permission group §9<group>§6 parent§r info <parent>
                - /permission group §9<group>§6 parent§r time <parent> <duration> <set|add|remove>
                - /permission group §9<group>§e permission§r
                - /permission group §9<group>§e permission§r all
                - /permission group §9<group>§e permission§r add <permission> [duration]
                - /permission group §9<group>§e permission§r remove <permission>
                - /permission group §9<group>§e permission§r clear
                - /permission group §9<group>§e permission§r info <permission>
                - /permission group §9<group>§e permission§r time <permission> <duration> <set|add|remove>
                - /permission group §9<group>§c edit§r chat <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tab <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tag <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r sort <sortId>
                - /permission group §9<group>§c edit§r team <teamId>
                - /permission groups
                - /permission user §b<user>§6 parent§r
                - /permission user §b<user>§6 parent§r add <parent> [duration]
                - /permission user §b<user>§6 parent§r remove <parent>
                - /permission user §b<user>§6 parent§r clear
                - /permission user §b<user>§6 parent§r info <parent>
                - /permission user §b<user>§6 parent§r time <parent> <duration> <set|add|remove>
                - /permission user §b<user>§e permission§r
                - /permission user §b<user>§e permission§r all
                - /permission user §b<user>§e permission§r add <permission> [duration]
                - /permission user §b<user>§e permission§r remove <permission>
                - /permission user §b<user>§e permission§r clear
                - /permission user §b<user>§e permission§r info <permission>
                - /permission user §b<user>§e permission§r time <permission> <duration> <set|add|remove>
                - /permission users
                """);
    }

    public static void syntax(CommandSource source, boolean isUser) {
        if (isUser) sendSourceMessage(source, """
                - /permission user §b<user>§6 parent§r
                - /permission user §b<user>§6 parent§r add <parent> [duration]
                - /permission user §b<user>§6 parent§r remove <parent>
                - /permission user §b<user>§6 parent§r clear
                - /permission user §b<user>§6 parent§r info <parent>
                - /permission user §b<user>§6 parent§r time <parent> <duration> <set|add|remove>
                - /permission user §b<user>§e permission§r
                - /permission user §b<user>§e permission§r all
                - /permission user §b<user>§e permission§r add <permission> [duration]
                - /permission user §b<user>§e permission§r remove <permission>
                - /permission user §b<user>§e permission§r clear
                - /permission user §b<user>§e permission§r info <permission>
                - /permission user §b<user>§e permission§r time <permission> <duration> <set|add|remove>
                """);
        else sendSourceMessage(source, """
                - /permission group §9<group>§r create <sortId> <teamId>
                - /permission group §9<group>§r delete
                - /permission group §9<group>§r rename <newName>
                - /permission group §9<group>§6 parent§r
                - /permission group §9<group>§6 parent§r add <parent> [duration]
                - /permission group §9<group>§6 parent§r remove <parent>
                - /permission group §9<group>§6 parent§r clear
                - /permission group §9<group>§6 parent§r info <parent>
                - /permission group §9<group>§6 parent§r time <parent> <duration> <set|add|remove>
                - /permission group §9<group>§e permission§r
                - /permission group §9<group>§e permission§r all
                - /permission group §9<group>§e permission§r add <permission> [duration]
                - /permission group §9<group>§e permission§r remove <permission>
                - /permission group §9<group>§e permission§r clear
                - /permission group §9<group>§e permission§r info <permission>
                - /permission group §9<group>§e permission§r time <permission> <duration> <set|add|remove>
                - /permission group §9<group>§c edit§r chat <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tab <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tag <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r sort <sortId>
                - /permission group §9<group>§c edit§r team <teamId>
                """);
    }

    public static void syntaxParent(CommandSource source, boolean isUser) {
        if (isUser) sendSourceMessage(source, """
                - /permission user §b<user>§6 parent§r
                - /permission user §b<user>§6 parent§r add <parent> [duration]
                - /permission user §b<user>§6 parent§r remove <parent>
                - /permission user §b<user>§6 parent§r clear
                - /permission user §b<user>§6 parent§r info <parent>
                - /permission user §b<user>§6 parent§r time <parent> <duration> <set|add|remove>
                """);
        else sendSourceMessage(source, """
                - /permission group §9<group>§6 parent§r
                - /permission group §9<group>§6 parent§r add <parent> [duration]
                - /permission group §9<group>§6 parent§r remove <parent>
                - /permission group §9<group>§6 parent§r clear
                - /permission group §9<group>§6 parent§r info <parent>
                - /permission group §9<group>§6 parent§r time <parent> <duration> <set|add|remove>
                """);
    }

    public static void syntaxPermission(CommandSource source, boolean isUser) {
        if (isUser) sendSourceMessage(source, """
                - /permission user §b<user>§e permission§r
                - /permission user §b<user>§e permission§r all
                - /permission user §b<user>§e permission§r add <permission> [duration]
                - /permission user §b<user>§e permission§r remove <permission>
                - /permission user §b<user>§e permission§r clear
                - /permission user §b<user>§e permission§r info <permission>
                - /permission user §b<user>§e permission§r time <permission> <duration> <set|add|remove>
                """);
        else sendSourceMessage(source, """
                - /permission group §9<group>§e permission§r
                - /permission group §9<group>§e permission§r all
                - /permission group §9<group>§e permission§r add <permission> [duration]
                - /permission group §9<group>§e permission§r remove <permission>
                - /permission group §9<group>§e permission§r clear
                - /permission group §9<group>§e permission§r info <permission>
                - /permission group §9<group>§e permission§r time <permission> <duration> <set|add|remove>
                """);
    }

    public static void syntaxGroupEdit(CommandSource source) {
        sendSourceMessage(source, """
                - /permission group §9<group>§c edit§r chat <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tab <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r tag <prefix|suffix|color> <value>
                - /permission group §9<group>§c edit§r sort <sortId>
                - /permission group §9<group>§c edit§r team <teamId>
                """);
    }
}
