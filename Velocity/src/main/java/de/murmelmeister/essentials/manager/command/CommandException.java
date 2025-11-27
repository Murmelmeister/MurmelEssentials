package de.murmelmeister.essentials.manager.command;

import de.murmelmeister.essentials.messages.Message;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class CommandException extends RuntimeException {
    private final Message messageKey;
    private final TagResolver[] resolvers;

    public CommandException(String message) {
        super(message);
        this.messageKey = null;
        this.resolvers = null;
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
        this.messageKey = null;
        this.resolvers = null;
    }

    public CommandException(Throwable cause) {
        super(cause);
        this.messageKey = null;
        this.resolvers = null;
    }

    public CommandException(Message messageKey, TagResolver... resolvers) {
        super(messageKey.getTag());
        this.messageKey = messageKey;
        this.resolvers = resolvers;
    }

    public Message getMessageKey() {
        return messageKey;
    }

    public TagResolver[] getResolvers() {
        return resolvers;
    }
}
