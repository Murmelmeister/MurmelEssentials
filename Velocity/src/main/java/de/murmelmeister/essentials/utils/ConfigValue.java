package de.murmelmeister.essentials.utils;

public enum ConfigValue {
    CACHE_AUTO_UPDATE("cache.auto-update", true),
    TABLIST_ENABLE("tablist.enable", false),
    TABLIST_REFRESH("tablist.refresh", "1000"),
    TABLIST_HEADER("tablist.header", "<#999999>Tab <3"),
    TABLIST_FOOTER("tablist.footer", "<#999999>Powered by MurmelAPI"),
    PLAYER_LIST_ENABLE("playerlist.enable", true),
    ;
    private static final ConfigValue[] VALUES = values();

    private final String path;
    private final Object fallback;

    ConfigValue(String path, Object fallback) {
        this.path = path;
        this.fallback = fallback;
    }

    public String getPath() {
        return path;
    }

    public Object getFallback() {
        return fallback;
    }
}
