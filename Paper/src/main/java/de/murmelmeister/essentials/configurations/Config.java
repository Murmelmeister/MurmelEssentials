package de.murmelmeister.essentials.configurations;

import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.essentials.utils.ConfigValue;
import de.murmelmeister.library.configuration.YamlMurmel;

public class Config {
    private final YamlMurmel config = YamlMurmel.builder(MurmelEssentials.PLUGIN_PATH + "config.yml").build();

    public <T> T getValue(ConfigValue value, Class<T> type) {
        return config.getValue(value.getPath(), type, type.cast(value.getFallback()));
    }

    public boolean getAutoRefresh() {
        return getValue(ConfigValue.CACHE_AUTO_UPDATE, Boolean.class);
    }
}
