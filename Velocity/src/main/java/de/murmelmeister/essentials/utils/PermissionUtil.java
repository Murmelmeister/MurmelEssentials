package de.murmelmeister.essentials.utils;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.manager.CommandManager;
import de.murmelmeister.essentials.messages.Message;
import de.murmelmeister.murmelapi.language.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDateTime;

public abstract class PermissionUtil extends CommandManager {
    private final MessageService messageService;

    public PermissionUtil(MurmelEssentials plugin) {
        super(plugin);
        this.messageService = plugin.getMessageService();
    }

    public Component formatExpiredMessage(int languageId, LocalDateTime expiresAt) {
        String now = LocalDateTime.now().format(getDateTimeFormatter(languageId));
        String formattedTime = formatTimeUntil(languageId, expiresAt);
        return expiresAt == null ? Component.empty() : MiniMessage.miniMessage().deserialize(messageService.getMessage(Message.PERMISSION_FORMAT_EXPIRED_TIME.getTag(), languageId),
                tagParsed("expired_time", formattedTime),
                tagParsed("current_time", now),
                tagParsed("expired_at", expiresAt.format(getDateTimeFormatter(languageId)))
        );
    }

    public Component formatExpiredInfoMessage(int languageId, LocalDateTime expiresAt) {
        String now = LocalDateTime.now().format(getDateTimeFormatter(languageId));
        String formattedTime = formatTimeUntil(languageId, expiresAt);
        return expiresAt == null ? MiniMessage.miniMessage().deserialize(messageService.getMessage(Message.MESSAGE_NOT_EXPIRE.getTag(), languageId))
                : MiniMessage.miniMessage().deserialize(messageService.getMessage(Message.PERMISSION_FORMAT_EXPIRED_INFO_TIME.getTag(), languageId),
                tagParsed("expired_time", formattedTime),
                tagParsed("current_time", now),
                tagParsed("expired_at", expiresAt.format(getDateTimeFormatter(languageId)))
        );
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
                <#454545>- <#999999>/permission %1$s create <white><priority>
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
