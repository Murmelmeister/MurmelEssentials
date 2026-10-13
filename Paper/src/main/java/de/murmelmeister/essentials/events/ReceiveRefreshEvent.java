package de.murmelmeister.essentials.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

public class ReceiveRefreshEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final String type;
    private final String key;

    public ReceiveRefreshEvent(@NotNull Player player, String type) {
        this(player, type, null);
    }

    public ReceiveRefreshEvent(@NotNull Player player, String type, String key) {
        this.player = player;
        this.type = type;
        this.key = key;
    }

    /** A Redis refresh belongs to the server and has no carrier player. */
    public ReceiveRefreshEvent(String type, String key) {
        this.player = null;
        this.type = type;
        this.key = key;
    }

    public @Nullable Player getPlayer() {
        return player;
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
