package de.murmelmeister.essentials.utils;

public enum ConfigValue {
    TABLIST_ENABLE("tablist.enable", true),
    TABLIST_REFRESH("tablist.refresh", 1000L),
    PLUGIN_PREFIX("plugin.prefix", true),
    COMMAND_PAGED_SIZE("command.paged.size", 10),
    ;
    public static final ConfigValue[] VALUES = values();

    private final String path;
    private final Object defaultValue;

    ConfigValue(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public String getPath() {
        return path;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
