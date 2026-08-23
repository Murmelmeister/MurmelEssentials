package de.murmelmeister.essentials.utils;

public enum ConfigValue {
    TABLIST_ENABLE("tablist.enable", true),
    TABLIST_REFRESH("tablist.refresh", 1000L),
    PLUGIN_PREFIX("plugin.prefix", true),
    COMMAND_PAGED_SIZE("command.paged.size", 10),
    MAINTENANCE_ENABLE("maintenance.enable", false),
    MAINTENANCE_MOTD("maintenance.motd", "<#00ffFF>MurmelAPI <#454545>| <#ff0000>Maintenance\n<#ff00bb>Please try again later."),
    MAINTENANCE_PROTOCOL_VERSION("maintenance.protocol.version", 0),
    MAINTENANCE_PROTOCOL_NAME("maintenance.protocol.name", "Maintenance"),
    COMMAND_LOGGER_ENABLE("command.logger.enable", true),
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
