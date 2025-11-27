package de.murmelmeister.essentials.configurations;

import de.murmelmeister.essentials.utils.ConfigValue;
import de.murmelmeister.library.configuration.YamlMurmel;

import java.nio.file.Path;

public class Config {
    private final YamlMurmel config;

    public Config(Path dataDirectory) {
        this.config = YamlMurmel.builder(dataDirectory.resolve("config.yml").toString()).build();
    }

    private <T> T getValue(ConfigValue value, Class<T> type) {
        return config.getValue(value.getPath(), type, type.cast(value.getFallback()));
    }

    public boolean getAutoRefresh() {
        return getValue(ConfigValue.CACHE_AUTO_UPDATE, Boolean.class);
    }

    public boolean getTablistEnable() {
        return getValue(ConfigValue.TABLIST_ENABLE, Boolean.class);
    }

    public long getTablistRefresh() {
        return Long.parseLong(getValue(ConfigValue.TABLIST_REFRESH, String.class));
    }

    public String geTablistHeader() {
        return getValue(ConfigValue.TABLIST_HEADER, String.class);
    }

    public String getTablistFooter() {
        return getValue(ConfigValue.TABLIST_FOOTER, String.class);
    }

    public boolean getPlayerListEnable() {
        return getValue(ConfigValue.PLAYER_LIST_ENABLE, Boolean.class);
    }
}
