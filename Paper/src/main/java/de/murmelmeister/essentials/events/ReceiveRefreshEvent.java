package de.murmelmeister.essentials.events;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class ReceiveRefreshEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final String type;
    private final String key;

    /**
     * A Redis refresh belongs to the server and has no carrier player.
     */
    public ReceiveRefreshEvent(String type, String key) {
        this.type = type;
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
