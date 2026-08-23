package de.murmelmeister.essentials.configurations;

import de.murmelmeister.essentials.utils.ConfigValue;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PluginConfig {
    private final Path path;
    private final YamlConfigurationLoader loader;

    private ConfigurationNode config;

    public PluginConfig(Path dataFolder) {
        this.path = dataFolder.resolve("config.yml");

        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory: " + dataFolder, e);
        }

        this.loader = YamlConfigurationLoader.builder()
                .path(path)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();

        reload();
    }

    public void reload() {
        try {
            config = loader.load();
            loadDefaults();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + path, e);
        }
    }

    private void save() {
        try {
            loader.save(config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file: " + path, e);
        }
    }

    private void loadDefaults() {
        boolean changed = false;

        for (ConfigValue value : ConfigValue.VALUES) {
            ConfigurationNode node = node(value);

            if (node.virtual()) {
                set(node, value);
                changed = true;
            }
        }

        if (changed)
            save();
    }

    public void addCommandDefaults(Map<String, ?> defaults) {
        boolean changed = false;

        for (Map.Entry<String, ?> entry : defaults.entrySet()) {
            ConfigurationNode node = node(entry.getKey());
            if (!node.virtual()) continue;

            try {
                node.set(entry.getValue());
                changed = true;
            } catch (SerializationException e) {
                throw new RuntimeException("Failed to set config value: " + entry.getKey(), e);
            }
        }

        if (changed)
            save();
    }

    private ConfigurationNode node(ConfigValue value) {
        return config.node(Arrays.stream(value.getPath().split("\\.")).toArray());
    }

    private ConfigurationNode node(String path) {
        return config.node(Arrays.stream(path.split("\\.")).toArray());
    }

    private void set(ConfigurationNode node, ConfigValue value) {
        try {
            node.set(value.getDefaultValue());
        } catch (SerializationException e) {
            throw new RuntimeException("Failed to set config value: " + value.getPath(), e);
        }
    }

    public boolean getBoolean(ConfigValue value) {
        if (!(value.getDefaultValue() instanceof Boolean defaultValue))
            throw new IllegalArgumentException("Config value is not a boolean: " + value.getPath());

        return node(value).getBoolean(defaultValue);
    }

    public int getInt(ConfigValue value) {
        if (!(value.getDefaultValue() instanceof Integer defaultValue))
            throw new IllegalArgumentException("Config value is not an integer: " + value.getPath());

        return node(value).getInt(defaultValue);
    }

    public long getLong(ConfigValue value) {
        if (!(value.getDefaultValue() instanceof Long defaultValue))
            throw new IllegalArgumentException("Config value is not a long: " + value.getPath());

        return node(value).getLong(defaultValue);
    }

    public boolean getBoolean(String path, Boolean defaultValue) {
        return node(path).getBoolean(defaultValue);
    }

    public String getString(String path, String defaultValue) {
        return node(path).getString(defaultValue);
    }

    public List<String> getStringList(String path, List<String> defaultValue) {
        try {
            return node(path).getList(String.class, defaultValue);
        } catch (SerializationException e) {
            throw new RuntimeException("Failed to get string list from config path: " + path, e);
        }
    }
}
