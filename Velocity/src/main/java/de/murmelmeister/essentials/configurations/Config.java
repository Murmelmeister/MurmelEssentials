package de.murmelmeister.essentials.configurations;

import de.murmelmeister.essentials.utils.ConfigValue;
import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.library.configuration.YamlMurmel;

import java.nio.file.Path;

public class Config {
    private final YamlMurmel config;

    public Config(Path dataDirectory) {
        this.config = YamlMurmel.builder(dataDirectory.resolve("config.yml").toString()).build();
    }

    public <T> T getValue(ConfigValue value, Class<T> type) {
        return config.getValue(value.getPath(), type, type.cast(value.getFallback()));
    }

    public void connectToDatabase() {
        String databaseName = getValue(ConfigValue.DB_DATABASE, String.class);
        String dbDriver = getValue(ConfigValue.DB_DRIVER, String.class);
        String dbHostname = getValue(ConfigValue.DB_HOSTNAME, String.class);
        Integer dbPort = getValue(ConfigValue.DB_PORT, Integer.class);
        String dbUsername = getValue(ConfigValue.DB_USERNAME, String.class);
        String dbPassword = getValue(ConfigValue.DB_PASSWORD, String.class);
        if (databaseName == null || dbDriver == null || dbHostname == null || dbPort == null || dbUsername == null || dbPassword == null ||
            dbUsername.equalsIgnoreCase("<USERNAME>") || dbPassword.equalsIgnoreCase("<PASSWORD>"))
            throw new IllegalArgumentException("Database configuration is incomplete or contains placeholder. Please check your config.yml file.");

        MurmelAPI.setDatabaseName(databaseName);
        String url = String.format("jdbc:%s://%s:%s/%s", dbDriver, dbHostname, dbPort, databaseName);
        MurmelAPI.connect("com.mysql.cj.jdbc.Driver", url, dbUsername, dbPassword);
    }

    public void disconnectFromDatabase() {
        MurmelAPI.disconnect();
    }
}
