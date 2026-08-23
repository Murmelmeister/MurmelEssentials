package de.murmelmeister.essentials.configs;

import de.murmelmeister.essentials.utils.ConfigValue;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The default config file for the plugin.
 */
public final class PluginConfig {
    private final Path file;
    private YamlConfiguration config;

    public PluginConfig(Path dataFolder) {
        this.file = dataFolder.resolve("config.yml");

        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory: " + dataFolder, e);
        }

        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file.toFile());
        loadDefaults();
    }

    private void save() {
        try {
            config.save(file.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config file: " + file, e);
        }
    }

    private void loadDefaults() {
        boolean changed = false;

        for (ConfigValue value : ConfigValue.VALUES)
            if (!config.contains(value.getPath())) {
                set(value);
                changed = true;
            }

        if (changed)
            save();
    }

    private void set(ConfigValue value) {
        config.set(value.getPath(), value.getDefaultValue());
    }

    public boolean getBoolean(ConfigValue value) {
        if (!(value.getDefaultValue() instanceof Boolean defaultValue))
            throw new IllegalArgumentException("Config value is not a boolean: " + value.getPath());

        return config.getBoolean(value.getPath(), defaultValue);
    }

    public String getString(ConfigValue value) {
        if (!(value.getDefaultValue() instanceof String defaultValue))
            throw new IllegalArgumentException("Config value is not a String: " + value.getPath());

        return config.getString(value.getPath(), defaultValue);
    }
}
