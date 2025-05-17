package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.command.CommandSource;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.murmelapi.MurmelAPI;

import java.sql.Timestamp;

public abstract class PermissionUtil extends CommandManager {
    public PermissionUtil(MurmelEssentials plugin) {
        super(plugin);
    }

    public String getName(boolean isUser, int id) {
        return isUser ? user.getUsername(id) : group.getGroupName(id);
    }

    public void loggingToConsole(int executorId, String fullCommand) {
        logger.info("Executor: {} (ID: {}) - Command: {}", user.getUsername(executorId), executorId, fullCommand);
    }

    public void loggingToConsole(boolean isUser, int executorId, int id, String fullCommand) {
        logger.info("Executor: {} (ID: {}) - Target: {} ({}) (ID: {}) - Command: {}",
                user.getUsername(executorId), executorId, getName(isUser, id), (isUser ? "User" : "Group"), id, fullCommand);
    }

    public int getGroupId(CommandSource source, String groupName) {
        if (!existsGroup(source, groupName)) return 0;
        return group.getId(groupName);
    }

    public int getUserId(CommandSource source, String username) {
        if (!existsUser(source, username)) return -2;
        return user.getId(username);
    }

    public String formatExpiredMessage(Timestamp expiredAt, String expiredDate) {
        String currentTimeDate = MurmelAPI.getDateFormat().format(System.currentTimeMillis());
        String formattedTime = formatTimeUntil(expiredAt);
        return expiredDate == null ? "" : "<#555555>(Expired: <#00cc88><hover:show_text:'<#999999>Expired: <#00cc88>" + formattedTime +
                                          "<br><#999999>Current time: </#999999>" + currentTimeDate + "'>" +
                                          expiredDate + "</hover></#00cc88>)";
    }

    public String formatExpiredInfoMessage(Timestamp expiredAt, String expiredDate) {
        String currentTimeDate = MurmelAPI.getDateFormat().format(System.currentTimeMillis());
        String formattedTime = formatTimeUntil(expiredAt);
        return expiredDate == null ? "never" : "<hover:show_text:'<#999999>Expired: <#00cc88>" + formattedTime +
                                               "<br><#999999>Current time: </#999999>" + currentTimeDate + "'>" +
                                               expiredDate + "</hover>";
    }

    public String syntax() {
        String group = syntaxGroup();
        String user = syntaxUser();
        return """
                 <#454545>- <#999999>/permission <#bb00bb>groups<#bb00bb>
                %s
                <#454545>- <#999999>/permission <#00bbBB>users</#00bbBB>
                %s
                """.stripIndent()
                .trim()
                .formatted(group, user);
    }

    public String syntaxGroup() {
        String command = "<#990099>group</#990099> <#999900><group></#999900>";

        String groupInfo = """
                 <#454545>- <#999999>/permission %1$s info
                <#454545>- <#999999>/permission %1$s create <white><priority> <teamId>
                <#454545>- <#999999>/permission %1$s delete
                <#454545>- <#999999>/permission %1$s rename <white><newName>
                """.stripIndent()
                .trim()
                .formatted(command);

        String groupParent = syntaxParent(false);
        String groupPermission = syntaxPermission(false);
        String groupEdit = syntaxGroupEdit();

        return String.join("\n", groupInfo, groupParent, groupPermission, groupEdit);
    }

    public String syntaxUser() {
        String userParent = syntaxParent(true);
        String userPermission = syntaxPermission(true);
        return String.join("\n", userParent, userPermission);
    }

    public String syntaxParent(boolean isUser) {
        String command = isUser ? "<#009999>user</#009999> <#999900><user></#999900>" : "<#990099>group</#990099> <#999900><group></#999900>";
        return """
                 <#454545>- <#999999>/permission %1$s parent
                <#454545>- <#999999>/permission %1$s parent add <white><parent> [time]
                <#454545>- <#999999>/permission %1$s parent remove <white><parent>
                <#454545>- <#999999>/permission %1$s parent clear
                <#454545>- <#999999>/permission %1$s parent info <white><parent>
                <#454545>- <#999999>/permission %1$s parent time <white><parent> <time>
                """.stripIndent()
                .trim()
                .formatted(command);
    }

    public String syntaxPermission(boolean isUser) {
        String command = isUser ? "<#009999>user</#009999> <#999900><user></#999900>" : "<#990099>group</#990099> <#999900><group></#999900>";
        return """
                 <#454545>- <#999999>/permission %1$s permission
                <#454545>- <#999999>/permission %1$s permission all
                <#454545>- <#999999>/permission %1$s permission add <white><permission> [time]
                <#454545>- <#999999>/permission %1$s permission remove <white><permission>
                <#454545>- <#999999>/permission %1$s permission clear
                <#454545>- <#999999>/permission %1$s permission info <white><permission>
                <#454545>- <#999999>/permission %1$s permission time <white><permission> <time>
                """.stripIndent()
                .trim()
                .formatted(command);
    }

    public String syntaxGroupEdit() {
        String command = "<#990099>group</#990099> <#999900><group></#999900>";
        return """
                 <#454545>- <#999999>/permission %1$s edit chat <white>prefix <value>
                <#454545>- <#999999>/permission %1$s edit chat <white>suffix <value>
                <#454545>- <#999999>/permission %1$s edit chat <white>color <value>
                <#454545>- <#999999>/permission %1$s edit chat <white>message <value>
                <#454545>- <#999999>/permission %1$s edit tab <white>prefix <value>
                <#454545>- <#999999>/permission %1$s edit tab <white>suffix <value>
                <#454545>- <#999999>/permission %1$s edit tab <white>color <value>
                <#454545>- <#999999>/permission %1$s edit team <white>prefix <value>
                <#454545>- <#999999>/permission %1$s edit team <white>suffix <value>
                <#454545>- <#999999>/permission %1$s edit team <white>color <value>
                <#454545>- <#999999>/permission %1$s edit team <white>id <value>
                <#454545>- <#999999>/permission %1$s edit priority <white><value>
                """.stripIndent()
                .trim()
                .formatted(command);
    }
}
