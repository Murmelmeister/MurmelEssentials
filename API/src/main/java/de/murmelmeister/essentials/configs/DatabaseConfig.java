package de.murmelmeister.essentials.configs;

import de.murmelmeister.murmelapi.MurmelAPI;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class DatabaseConfig {
    private final Path dataDirectory;
    private final String file = "database.properties";

    public DatabaseConfig(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        createFile();
    }

    public DatabaseConfig(String pluginName) {
        this.dataDirectory = Path.of("./plugins/" + pluginName + "/");
        createFile();
    }

    public void createFile() {
        if (Files.notExists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Could not create data directory for plugin.", e);
            }
        }

        if (Files.notExists(dataDirectory.resolve(file))) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(file)) {
                if (in == null) throw new IllegalStateException("Could not find database.properties file.");
                Files.copy(in, dataDirectory.resolve(file));
            } catch (IOException e) {
                throw new RuntimeException("Could not create database.properties file.", e);
            }
        }
    }

    public void connect() {
        // Load properties
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(dataDirectory.resolve(file))) {
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not load database.properties file.", e);
        }

        // Get properties
        String url = properties.getProperty("jdbcUrl");
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");
        if (url == null || username == null || password == null
                || url.isEmpty() || username.isEmpty() || password.isEmpty()
                || url.isBlank() || username.isBlank() || password.isBlank())
            throw new IllegalArgumentException("Database configuration is incomplete or contains placeholder. Please check your database.properties file.");

        // Connect to database
        MurmelAPI.connect(properties);
    }

    public void disconnect() {
        MurmelAPI.disconnect();
    }
}
