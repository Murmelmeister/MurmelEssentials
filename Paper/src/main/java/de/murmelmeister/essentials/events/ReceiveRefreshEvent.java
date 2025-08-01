package de.murmelmeister.essentials.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class ReceiveRefreshEvent extends PlayerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final String type;
    private final String key;

    public ReceiveRefreshEvent(@NotNull Player player, String type) {
        this(player, type, null);
    }

    public ReceiveRefreshEvent(@NotNull Player player, String type, String key) {
        super(player);
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
