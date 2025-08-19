package de.murmelmeister.essentials.utils;

public enum ConfigValue {
    DB_DRIVER("database.driver", "mysql"),
    DB_DATABASE("database.name", "MurmelAPI"),
    DB_HOSTNAME("database.hostname", "localhost"),
    DB_PORT("database.port", 3306),
    DB_USERNAME("database.username", "<USERNAME>"),
    DB_PASSWORD("database.password", "<PASSWORD>"),
    CACHE_AUTO_UPDATE("cache.auto-update", true),
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
